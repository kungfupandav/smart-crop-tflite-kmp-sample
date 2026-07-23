package com.smartcrop.shared.ml

import com.smartcrop.shared.domain.model.CropRegion
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.Pinned
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.pin
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import smartcropkmp.shared.generated.resources.Res
import cnames.structs.TfLiteInterpreter
import cnames.structs.TfLiteInterpreterOptions
import cnames.structs.TfLiteModel
import tflitec.TfLiteInterpreterAllocateTensors
import tflitec.TfLiteInterpreterCreate
import tflitec.TfLiteInterpreterDelete
import tflitec.TfLiteInterpreterGetInputTensor
import tflitec.TfLiteInterpreterGetOutputTensor
import tflitec.TfLiteInterpreterInvoke
import tflitec.TfLiteInterpreterOptionsCreate
import tflitec.TfLiteInterpreterOptionsDelete
import tflitec.TfLiteInterpreterOptionsSetNumThreads
import tflitec.TfLiteModelCreate
import tflitec.TfLiteModelDelete
import tflitec.TfLiteTensorCopyFromBuffer
import tflitec.TfLiteTensorCopyToBuffer
import tflitec.kTfLiteOk

private const val INPUT_SIZE = 320
private const val THRESHOLD = 0.5f
private const val MIN_SIZE = 0.4f
private const val MODEL_PATH = "files/u2netp_320_float32.tflite"
private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)

/**
 * iOS implementation of [SaliencyEngine] backed by the TensorFlow Lite C API
 * (exposed via cinterop as the `tflitec` package) and CoreGraphics for image
 * decode/resize.
 *
 * Runs the validated u2netp (320x320, float32) model on-device to produce a
 * saliency map, then delegates the mask -> crop math to [CropCalculator]. The
 * interpreter is created lazily on first use and cached; all access is
 * serialized with a [Mutex] and the heavy work runs on [Dispatchers.Default].
 *
 * The model bytes are pinned for the lifetime of the interpreter because
 * `TfLiteModelCreate` does NOT copy them.
 */
@OptIn(
    ExperimentalForeignApi::class,
    ExperimentalResourceApi::class,
    kotlinx.cinterop.BetaInteropApi::class,
)
actual class SaliencyEngine {

    private val mutex = Mutex()

    private var model: CPointer<TfLiteModel>? = null
    private var options: CPointer<TfLiteInterpreterOptions>? = null
    private var interpreter: CPointer<TfLiteInterpreter>? = null
    private var modelPin: Pinned<ByteArray>? = null

    actual suspend fun findSalientRegion(
        imageBytes: ByteArray,
        targetAspectRatio: Float
    ): CropRegion = withContext(Dispatchers.Default) {
        try {
            val input = decodeAndPreprocess(imageBytes) ?: return@withContext CropRegion.CENTER

            mutex.withLock {
                val interp = ensureInterpreter() ?: return@withLock CropRegion.CENTER

                val inputTensor = TfLiteInterpreterGetInputTensor(interp, 0)
                    ?: return@withLock CropRegion.CENTER

                val inputCopied = input.usePinned { pinned ->
                    TfLiteTensorCopyFromBuffer(
                        inputTensor,
                        pinned.addressOf(0),
                        (input.size * 4).convert(),
                    )
                }
                if (inputCopied != kTfLiteOk) return@withLock CropRegion.CENTER

                if (TfLiteInterpreterInvoke(interp) != kTfLiteOk) {
                    return@withLock CropRegion.CENTER
                }

                val outputTensor = TfLiteInterpreterGetOutputTensor(interp, 0)
                    ?: return@withLock CropRegion.CENTER

                val mask = FloatArray(INPUT_SIZE * INPUT_SIZE)
                val outputCopied = mask.usePinned { pinned ->
                    TfLiteTensorCopyToBuffer(
                        outputTensor,
                        pinned.addressOf(0),
                        (mask.size * 4).convert(),
                    )
                }
                if (outputCopied != kTfLiteOk) return@withLock CropRegion.CENTER

                normalizeInPlace(mask)

                CropCalculator.computeCropRegion(
                    mask = mask,
                    maskWidth = INPUT_SIZE,
                    maskHeight = INPUT_SIZE,
                    targetAspectRatio = targetAspectRatio,
                    threshold = THRESHOLD,
                    minSize = MIN_SIZE,
                )
            }
        } catch (e: Throwable) {
            CropRegion.CENTER
        }
    }

    /**
     * Lazily build (and cache) the interpreter. Must be called while holding
     * [mutex]. Returns null if any TFLite step fails.
     */
    private suspend fun ensureInterpreter(): CPointer<TfLiteInterpreter>? {
        interpreter?.let { return it }

        val bytes = Res.readBytes(MODEL_PATH)
        val pin = bytes.pin()
        modelPin = pin

        val createdModel = TfLiteModelCreate(pin.addressOf(0), bytes.size.convert())
        if (createdModel == null) {
            releaseNativeResources()
            return null
        }
        model = createdModel

        val createdOptions = TfLiteInterpreterOptionsCreate()
        if (createdOptions == null) {
            releaseNativeResources()
            return null
        }
        TfLiteInterpreterOptionsSetNumThreads(createdOptions, 2)
        options = createdOptions

        val createdInterpreter = TfLiteInterpreterCreate(createdModel, createdOptions)
        if (createdInterpreter == null) {
            releaseNativeResources()
            return null
        }

        if (TfLiteInterpreterAllocateTensors(createdInterpreter) != kTfLiteOk) {
            TfLiteInterpreterDelete(createdInterpreter)
            releaseNativeResources()
            return null
        }

        interpreter = createdInterpreter
        return createdInterpreter
    }

    /**
     * Decode the source image, resize to 320x320 via CoreGraphics, and build the
     * NHWC [1, 320, 320, 3] float32 input laid out in R, G, B order with the
     * ImageNet normalization. Returns null on any decode failure.
     */
    private fun decodeAndPreprocess(imageBytes: ByteArray): FloatArray? {
        if (imageBytes.isEmpty()) return null

        val data = imageBytes.usePinned { pinned ->
            NSData.create(
                bytes = pinned.addressOf(0),
                length = imageBytes.size.convert(),
            )
        }

        val image = UIImage.imageWithData(data) ?: return null
        val cg = image.CGImage ?: return null

        val rgba = ByteArray(INPUT_SIZE * INPUT_SIZE * 4)
        rgba.usePinned { pinned ->
            val colorSpace = CGColorSpaceCreateDeviceRGB()
            val ctx = CGBitmapContextCreate(
                data = pinned.addressOf(0),
                width = INPUT_SIZE.convert(),
                height = INPUT_SIZE.convert(),
                bitsPerComponent = 8.convert(),
                bytesPerRow = (INPUT_SIZE * 4).convert(),
                space = colorSpace,
                bitmapInfo = CGImageAlphaInfo.kCGImageAlphaNoneSkipLast.value,
            )
            CGContextDrawImage(
                ctx,
                CGRectMake(0.0, 0.0, INPUT_SIZE.toDouble(), INPUT_SIZE.toDouble()),
                cg,
            )
            CGContextRelease(ctx)
            CGColorSpaceRelease(colorSpace)
        }

        val input = FloatArray(INPUT_SIZE * INPUT_SIZE * 3)
        var o = 0
        for (i in 0 until INPUT_SIZE * INPUT_SIZE) {
            val r = rgba[4 * i].toInt() and 0xFF
            val g = rgba[4 * i + 1].toInt() and 0xFF
            val b = rgba[4 * i + 2].toInt() and 0xFF
            input[o++] = ((r / 255f) - MEAN[0]) / STD[0]
            input[o++] = ((g / 255f) - MEAN[1]) / STD[1]
            input[o++] = ((b / 255f) - MEAN[2]) / STD[2]
        }
        return input
    }

    /** Min-max normalize the mask to [0, 1] in place. */
    private fun normalizeInPlace(mask: FloatArray) {
        var min = Float.POSITIVE_INFINITY
        var max = Float.NEGATIVE_INFINITY
        for (v in mask) {
            if (v < min) min = v
            if (v > max) max = v
        }
        if (max <= min) {
            for (i in mask.indices) mask[i] = 0f
        } else {
            val range = max - min
            for (i in mask.indices) mask[i] = (mask[i] - min) / range
        }
    }

    /** Delete native pointers (null-safe) and unpin the model bytes. */
    private fun releaseNativeResources() {
        interpreter?.let { TfLiteInterpreterDelete(it) }
        interpreter = null
        options?.let { TfLiteInterpreterOptionsDelete(it) }
        options = null
        model?.let { TfLiteModelDelete(it) }
        model = null
        modelPin?.unpin()
        modelPin = null
    }

    actual fun close() {
        try {
            releaseNativeResources()
        } catch (e: Throwable) {
            // Best-effort release.
        }
    }
}
