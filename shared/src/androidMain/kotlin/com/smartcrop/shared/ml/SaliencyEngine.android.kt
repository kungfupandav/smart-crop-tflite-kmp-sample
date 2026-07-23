package com.smartcrop.shared.ml

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.smartcrop.shared.domain.model.CropRegion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.tensorflow.lite.Interpreter
import smartcropkmp.shared.generated.resources.Res
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val INPUT_SIZE = 320
private const val THRESHOLD = 0.5f
private const val MIN_SIZE = 0.4f
private const val MODEL_PATH = "files/u2netp_320_float32.tflite"
private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)

/**
 * Android implementation of [SaliencyEngine] backed by TensorFlow Lite.
 *
 * Runs the validated u2netp (320x320, float32) model on-device to produce a
 * saliency map, then delegates the mask -> crop math to [CropCalculator].
 *
 * The interpreter is created lazily on the first [findSalientRegion] call and
 * cached. All access to the interpreter (init and inference) is serialized with
 * a [Mutex] so concurrent callers are safe, and the heavy work runs on
 * [Dispatchers.Default].
 */
@OptIn(ExperimentalResourceApi::class)
actual class SaliencyEngine {

    private val mutex = Mutex()
    private var interpreter: Interpreter? = null

    actual suspend fun findSalientRegion(
        imageBytes: ByteArray,
        targetAspectRatio: Float
    ): CropRegion = withContext(Dispatchers.Default) {
        try {
            // Decode the source image up front so we can bail cheaply on garbage input.
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ?: return@withContext CropRegion.CENTER

            val scaled = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
            try {
                val inputBuffer = preprocess(scaled)

                mutex.withLock {
                    val engine = interpreter ?: run {
                        val modelBytes = Res.readBytes(MODEL_PATH)
                        val modelBuffer = ByteBuffer
                            .allocateDirect(modelBytes.size)
                            .order(ByteOrder.nativeOrder())
                            .apply {
                                put(modelBytes)
                                rewind()
                            }
                        Interpreter(modelBuffer).also { interpreter = it }
                    }

                    // The model exposes multiple outputs; TFLite requires a buffer for
                    // each of them even though we only read output index 0 (the main
                    // saliency map). Each output is [1, 320, 320, 1] float32.
                    val outputCount = engine.outputTensorCount
                    val outputBuffers = ArrayList<ByteBuffer>(outputCount)
                    val outputs = HashMap<Int, Any>(outputCount)
                    for (i in 0 until outputCount) {
                        val buf = ByteBuffer
                            .allocateDirect(INPUT_SIZE * INPUT_SIZE * 1 * 4)
                            .order(ByteOrder.nativeOrder())
                        outputBuffers.add(buf)
                        outputs[i] = buf
                    }

                    engine.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)

                    val mask = postprocess(outputBuffers[0])
                    CropCalculator.computeCropRegion(
                        mask = mask,
                        maskWidth = INPUT_SIZE,
                        maskHeight = INPUT_SIZE,
                        targetAspectRatio = targetAspectRatio,
                        threshold = THRESHOLD,
                        minSize = MIN_SIZE,
                    )
                }
            } finally {
                scaled.recycle()
            }
        } catch (e: Throwable) {
            CropRegion.CENTER
        }
    }

    /**
     * Normalize the scaled bitmap into a direct, native-order float input buffer
     * laid out as [1, 320, 320, 3] in R, G, B channel order.
     */
    private fun preprocess(scaled: Bitmap): ByteBuffer {
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        val input = ByteBuffer
            .allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
            .order(ByteOrder.nativeOrder())

        var idx = 0
        for (y in 0 until INPUT_SIZE) {
            for (x in 0 until INPUT_SIZE) {
                val px = pixels[idx++]
                val r = (px shr 16) and 0xFF
                val g = (px shr 8) and 0xFF
                val b = px and 0xFF
                input.putFloat(((r / 255f) - MEAN[0]) / STD[0])
                input.putFloat(((g / 255f) - MEAN[1]) / STD[1])
                input.putFloat(((b / 255f) - MEAN[2]) / STD[2])
            }
        }
        input.rewind()
        return input
    }

    /**
     * Read output-0 into a row-major float mask and min-max normalize it to [0, 1].
     */
    private fun postprocess(output: ByteBuffer): FloatArray {
        output.rewind()
        val floats = output.asFloatBuffer()
        val mask = FloatArray(INPUT_SIZE * INPUT_SIZE)
        floats.get(mask)

        var min = Float.POSITIVE_INFINITY
        var max = Float.NEGATIVE_INFINITY
        for (v in mask) {
            if (v < min) min = v
            if (v > max) max = v
        }

        if (max <= min) {
            java.util.Arrays.fill(mask, 0f)
        } else {
            val range = max - min
            for (i in mask.indices) {
                mask[i] = (mask[i] - min) / range
            }
        }
        return mask
    }

    actual fun close() {
        // Best-effort release. Guard with the interpreter reference itself so we
        // don't need the suspend Mutex here; concurrent inference holds its own
        // buffers and a double close is harmless.
        val current = interpreter
        interpreter = null
        try {
            current?.close()
        } catch (e: Throwable) {
            // Ignore: close is best-effort.
        }
    }
}
