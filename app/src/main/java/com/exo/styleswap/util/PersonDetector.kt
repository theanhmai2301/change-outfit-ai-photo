package com.exo.styleswap.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * On-device check that a photo contains a person. Accepts if a human body pose is found
 * (best for full-body shots), OR an image label of "Person"/"Selfie", OR a face is detected.
 * This keeps valid try-on photos (full-body, busy background, sunglasses) from being blocked,
 * while clearly non-person photos (scenery, products) are rejected. Fails open on internal error.
 */
object PersonDetector {

    private val poseDetector by lazy {
        PoseDetection.getClient(
            PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
                .build()
        )
    }

    private val labeler by lazy {
        ImageLabeling.getClient(
            ImageLabelerOptions.Builder().setConfidenceThreshold(0.5f).build()
        )
    }

    private val faceDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setMinFaceSize(0.05f)
                .build()
        )
    }

    suspend fun hasPerson(file: File): Boolean {
        val bmp = decode(file) ?: return false
        if (hasPose(bmp)) return true
        if (hasPersonLabel(bmp)) return true
        return hasFace(bmp)
    }

    private fun decode(file: File): Bitmap? = try {
        BitmapFactory.decodeFile(file.absolutePath)
    } catch (e: Exception) {
        null
    }

    private suspend fun hasPose(bmp: Bitmap): Boolean =
        suspendCancellableCoroutine { cont ->
            try {
                poseDetector.process(InputImage.fromBitmap(bmp, 0))
                    .addOnSuccessListener { pose ->
                        val strong = pose.allPoseLandmarks.count { it.inFrameLikelihood > 0.5f }
                        if (cont.isActive) cont.resume(strong >= 5)
                    }
                    .addOnFailureListener { if (cont.isActive) cont.resume(false) }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(false)
            }
        }

    private suspend fun hasPersonLabel(bmp: Bitmap): Boolean =
        suspendCancellableCoroutine { cont ->
            try {
                labeler.process(InputImage.fromBitmap(bmp, 0))
                    .addOnSuccessListener { labels ->
                        val person = labels.any {
                            it.text.equals("Person", true) || it.text.equals("Selfie", true)
                        }
                        if (cont.isActive) cont.resume(person)
                    }
                    .addOnFailureListener { if (cont.isActive) cont.resume(false) }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(false)
            }
        }

    private suspend fun hasFace(bmp: Bitmap): Boolean =
        suspendCancellableCoroutine { cont ->
            try {
                faceDetector.process(InputImage.fromBitmap(bmp, 0))
                    .addOnSuccessListener { faces -> if (cont.isActive) cont.resume(faces.isNotEmpty()) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(true) }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(true)
            }
        }
}
