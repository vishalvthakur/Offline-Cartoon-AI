package com.example.processor

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log

class TemporalConsistency {
    private var lastProcessedFrame: Bitmap? = null

    /**
     * Applies adaptive motion-aware temporal smoothing to reduce video flickering.
     * Static regions (backgrounds, motionless hair, stationary clothing) are smoothed heavily,
     * while moving elements remain highly responsive to prevent motion trails.
     */
    fun applyTemporalConsistency(currentFrame: Bitmap): Bitmap {
        val previous = lastProcessedFrame
        if (previous == null) {
            lastProcessedFrame = currentFrame.copy(currentFrame.config ?: Bitmap.Config.ARGB_8888, true)
            return currentFrame
        }

        val width = currentFrame.width
        val height = currentFrame.height
        val size = width * height

        val result = currentFrame.copy(currentFrame.config ?: Bitmap.Config.ARGB_8888, true)

        val currPixels = IntArray(size)
        val prevPixels = IntArray(size)

        try {
            currentFrame.getPixels(currPixels, 0, width, 0, 0, width, height)
            previous.getPixels(prevPixels, 0, width, 0, 0, width, height)

            for (i in 0 until size) {
                val cp = currPixels[i]
                val pp = prevPixels[i]

                val cr = (cp shr 16) and 0xFF
                val cg = (cp shr 8) and 0xFF
                val cb = cp and 0xFF

                val pr = (pp shr 16) and 0xFF
                val pg = (pp shr 8) and 0xFF
                val pb = pp and 0xFF

                // Compute color distance (Euclidean distance in RGB space)
                val diffSq = (cr - pr) * (cr - pr) + (cg - pg) * (cg - pg) + (cb - pb) * (cb - pb)

                // Motion-aware adaptive blend factor
                // If difference is small (no motion), blend heavily with previous frame (alpha = 0.8) to kill flicker
                // If difference is large (fast motion), rely on current frame (alpha = 0.1) to avoid ghosting
                val alpha = when {
                    diffSq < 1500 -> 0.75f  // Static region: 75% previous, 25% current
                    diffSq < 4000 -> 0.40f  // Low motion: 40% previous, 60% current
                    diffSq < 8000 -> 0.15f  // Moderate motion
                    else -> 0.05f           // Fast motion: 5% previous, 95% current (prevents trailing ghosting)
                }

                val r = (cr * (1f - alpha) + pr * alpha).toInt().coerceIn(0, 255)
                val g = (cg * (1f - alpha) + pg * alpha).toInt().coerceIn(0, 255)
                val b = (cb * (1f - alpha) + pb * alpha).toInt().coerceIn(0, 255)

                currPixels[i] = Color.rgb(r, g, b)
            }

            result.setPixels(currPixels, 0, width, 0, 0, width, height)
        } catch (e: Exception) {
            Log.e("TemporalConsistency", "Error applying temporal smoothing, returning raw stylized frame", e)
        }

        // Cache the result for the next frame
        lastProcessedFrame = result.copy(result.config ?: Bitmap.Config.ARGB_8888, true)
        return result
    }

    fun reset() {
        lastProcessedFrame = null
    }
}
