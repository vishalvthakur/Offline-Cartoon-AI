package com.example.processor

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.util.Log

class FaceTracker {
    private var lastFaceBox: Rect? = null
    private var faceSmoothingAlpha = 0.7f // EMA smoothing factor for face bounding boxes

    /**
     * Detects and tracks faces between video frames completely offline.
     * Uses skin tone segmentation and localized centroid clustering as a highly
     * robust, zero-dependency, local visual face boundary tracker.
     */
    fun trackFace(bitmap: Bitmap): Rect? {
        val width = bitmap.width
        val height = bitmap.height
        val step = 4 // Sub-sample pixels to make it incredibly fast (O(N) / 16)
        
        var sumX = 0L
        var sumY = 0L
        var skinPixelsCount = 0

        val pixels = IntArray(width * height)
        // Sub-sample to avoid complete memory allocations
        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val pix = bitmap.getPixel(x, y)
                val r = (pix shr 16) and 0xFF
                val g = (pix shr 8) and 0xFF
                val b = pix and 0xFF

                // Standard skin color rule in RGB space
                if (r > 95 && g > 40 && b > 20 &&
                    (Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) > 15) &&
                    Math.abs(r - g) > 15 && r > g && r > b) {
                    
                    // Exclude pure bright highlights or extremely dark tones to focus on facial region
                    if (g > b && r < 250 && g < 240) {
                        sumX += x
                        sumY += y
                        skinPixelsCount++
                    }
                }
            }
        }

        if (skinPixelsCount > (width * height / 250)) { // Must have a reasonable number of skin pixels to be a face
            val centerX = (sumX / skinPixelsCount).toInt()
            val centerY = (sumY / skinPixelsCount).toInt()

            // Estimate facial bounding box size based on clustered skin pixel density
            val faceRadiusW = (width * 0.18f).toInt()
            val faceRadiusH = (height * 0.22f).toInt()

            val rawBox = Rect(
                (centerX - faceRadiusW).coerceAtLeast(0),
                (centerY - faceRadiusH).coerceAtLeast(0),
                (centerX + faceRadiusW).coerceAtMost(width),
                (centerY + faceRadiusH).coerceAtMost(height)
            )

            // Smooth face box frame-to-frame using exponential moving average (EMA)
            val smoothedBox = lastFaceBox?.let { last ->
                Rect(
                    (last.left * faceSmoothingAlpha + rawBox.left * (1f - faceSmoothingAlpha)).toInt(),
                    (last.top * faceSmoothingAlpha + rawBox.top * (1f - faceSmoothingAlpha)).toInt(),
                    (last.right * faceSmoothingAlpha + rawBox.right * (1f - faceSmoothingAlpha)).toInt(),
                    (last.bottom * faceSmoothingAlpha + rawBox.bottom * (1f - faceSmoothingAlpha)).toInt()
                )
            } ?: rawBox

            lastFaceBox = smoothedBox
            return smoothedBox
        } else {
            // Gradually decay face box tracking if no face is detected
            lastFaceBox = null
            return null
        }
    }

    /**
     * Stabilizes and blends the face region from the previous frame into the current frame.
     * Prevents flickering, color shifting, or edge jitter of the person's face.
     */
    fun stabilizeFaceRegion(currentFrame: Bitmap, previousFrame: Bitmap?, faceBox: Rect?): Bitmap {
        if (previousFrame == null || faceBox == null) return currentFrame

        val result = currentFrame.copy(currentFrame.config ?: Bitmap.Config.ARGB_8888, true)
        val left = faceBox.left
        val top = faceBox.top
        val right = faceBox.right
        val bottom = faceBox.bottom

        val w = right - left
        val h = bottom - top
        if (w <= 0 || h <= 0) return currentFrame

        val size = w * h
        val currPixels = IntArray(size)
        val prevPixels = IntArray(size)

        try {
            currentFrame.getPixels(currPixels, 0, w, left, top, w, h)
            previousFrame.getPixels(prevPixels, 0, w, left, top, w, h)

            // Blend the face pixels (temporal face smoothing)
            // 75% current frame face details, 25% previous frame face details to reduce temporal flicker
            val blendFactor = 0.25f 
            for (i in 0 until size) {
                val cp = currPixels[i]
                val pp = prevPixels[i]

                val cr = (cp shr 16) and 0xFF
                val cg = (cp shr 8) and 0xFF
                val cb = cp and 0xFF

                val pr = (pp shr 16) and 0xFF
                val pg = (pp shr 8) and 0xFF
                val pb = pp and 0xFF

                val br = (cr * (1f - blendFactor) + pr * blendFactor).toInt().coerceIn(0, 255)
                val bg = (cg * (1f - blendFactor) + pg * blendFactor).toInt().coerceIn(0, 255)
                val bb = (cb * (1f - blendFactor) + pb * blendFactor).toInt().coerceIn(0, 255)

                currPixels[i] = Color.rgb(br, bg, bb)
            }

            result.setPixels(currPixels, 0, w, left, top, w, h)
        } catch (e: Exception) {
            Log.e("FaceTracker", "Face stabilization blending error", e)
        }

        return result
    }

    fun reset() {
        lastFaceBox = null
    }
}
