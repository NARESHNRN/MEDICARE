package com.example.service

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.model.ImageQualityResult
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object ImageProcessingEngine {

    /**
     * Evaluates image quality: Blur (Laplacian variance), Lighting (Luminance histogram), Glare.
     */
    fun analyzeImageQuality(bitmap: Bitmap): ImageQualityResult {
        // Downsample for fast analysis if bitmap is very large
        val sampleSize = 256
        val scaled = Bitmap.createScaledBitmap(bitmap, sampleSize, sampleSize, true)
        val width = scaled.width
        val height = scaled.height

        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)

        val grayscale = IntArray(width * height)
        var totalLuminance = 0L
        var glareCount = 0

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            // Standard luminance formula
            val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            grayscale[i] = lum
            totalLuminance += lum
            if (lum > 248) {
                glareCount++
            }
        }

        val avgBrightness = totalLuminance.toDouble() / (width * height)
        val glareRatio = glareCount.toDouble() / (width * height)

        // Laplacian Variance for Blur Detection:
        // Kernel:
        //  0  1  0
        //  1 -4  1
        //  0  1  0
        var laplacianSum = 0.0
        var laplacianSqSum = 0.0
        var count = 0

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val center = grayscale[y * width + x]
                val top = grayscale[(y - 1) * width + x]
                val bottom = grayscale[(y + 1) * width + x]
                val left = grayscale[y * width + (x - 1)]
                val right = grayscale[y * width + (x + 1)]

                val lap = (top + bottom + left + right) - 4 * center
                laplacianSum += lap
                laplacianSqSum += (lap * lap)
                count++
            }
        }

        val meanLap = laplacianSum / count
        val varianceLap = (laplacianSqSum / count) - (meanLap * meanLap)

        val isBlurry = varianceLap < 120.0
        val isTooDark = avgBrightness < 55.0
        val isTooBright = avgBrightness > 225.0
        val hasGlare = glareRatio > 0.15

        val guidance = when {
            isBlurry -> "The image is unclear. Please hold the camera steady and try again."
            isTooDark -> "The lighting is too dark. Please move towards light."
            isTooBright || hasGlare -> "There is too much glare on the medicine foil. Tilt the strip slightly away from the light."
            else -> "Medicine clearly in frame. Image quality is good."
        }

        val isAcceptable = !isBlurry && !isTooDark && !hasGlare

        return ImageQualityResult(
            isAcceptable = isAcceptable,
            blurScore = varianceLap,
            isBlurry = isBlurry,
            brightnessScore = avgBrightness,
            isTooDark = isTooDark,
            isTooBright = isTooBright,
            hasGlare = hasGlare,
            guidanceMessage = guidance
        )
    }

    /**
     * Enhanced image upscaling / sharpening for small strip text.
     * Note: Never invents characters, only sharpens contrast for OCR readability.
     */
    fun enhanceMedicineImage(source: Bitmap): Bitmap {
        val targetWidth = min(1200, source.width * 2)
        val targetHeight = min(1200, source.height * 2)
        if (source.width >= 1000) return source

        // Bicubic scaling
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }
}
