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
 * On-device check that a photo contains a person. A person is accepted if a human body
 * pose is found (best for full-body shots), OR an image label of "Person"/"Selfie", OR a
 * face is detected — so valid try-on photos (full-body, busy background, sunglasses) pass.
 *
 * Each detector returns true (person) / false (no person) / null (the detector errored).
 * The photo is rejected when at least one detector ran and none found a person. We only
 * "fail open" (accept) if EVERY detector errored — i.e. ML Kit is unavailable on the device —
 * so a broken ML Kit never silently lets non-person photos through while it works.
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
        val image = InputImage.fromBitmap(bmp, 0)
        var anyDetectorRan = false

        detectPose(image)?.let { anyDetectorRan = true; if (it) return true }
        detectLabel(image)?.let { anyDetectorRan = true; if (it) return true }
        detectFace(image)?.let { anyDetectorRan = true; if (it) return true }

        // No detector found a person. Reject if any detector actually ran; only accept
        // when all three errored (ML Kit unavailable) so valid photos aren't hard-blocked.
        return !anyDetectorRan
    }

    private fun decode(file: File): Bitmap? = try {
        BitmapFactory.decodeFile(file.absolutePath)
    } catch (e: Exception) {
        null
    }

    /** true = body pose found, false = none, null = detector errored. */
    private suspend fun detectPose(image: InputImage): Boolean? =
        suspendCancellableCoroutine { cont ->
            try {
                poseDetector.process(image)
                    .addOnSuccessListener { pose ->
                        val strong = pose.allPoseLandmarks.count { it.inFrameLikelihood > 0.5f }
                        if (cont.isActive) cont.resume(strong >= 5)
                    }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }

    /** true = "Person"/"Selfie" label, false = none, null = detector errored. */
    private suspend fun detectLabel(image: InputImage): Boolean? =
        suspendCancellableCoroutine { cont ->
            try {
                labeler.process(image)
                    .addOnSuccessListener { labels ->
                        val person = labels.any {
                            it.text.equals("Person", true) || it.text.equals("Selfie", true)
                        }
                        if (cont.isActive) cont.resume(person)
                    }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }

    /** true = a face found, false = none, null = detector errored. */
    private suspend fun detectFace(image: InputImage): Boolean? =
        suspendCancellableCoroutine { cont ->
            try {
                faceDetector.process(image)
                    .addOnSuccessListener { faces -> if (cont.isActive) cont.resume(faces.isNotEmpty()) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }
}
