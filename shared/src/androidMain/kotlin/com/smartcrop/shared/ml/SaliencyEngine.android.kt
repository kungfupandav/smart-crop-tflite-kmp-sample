package com.smartcrop.shared.ml

import com.smartcrop.shared.domain.model.CropRegion

/**
 * Android implementation using TensorFlow Lite interpreter.
 * TODO: Load u2netp.tflite model and run inference.
 */
actual class SaliencyEngine {

    actual suspend fun findSalientRegion(
        imageBytes: ByteArray,
        targetAspectRatio: Float
    ): CropRegion {
        // TODO: Implement TFLite inference
        return CropRegion.CENTER
    }

    actual fun close() {
        // TODO: Release TFLite interpreter
    }
}
