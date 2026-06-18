package com.exo.styleswap.ui.main

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.signature.ObjectKey
import com.exo.styleswap.R
import com.exo.styleswap.databinding.ActivityMainBinding
import com.exo.styleswap.firstopen.LocaleHelper
import com.exo.styleswap.ui.edit.EditActivity
import com.exo.styleswap.ui.settings.SettingsActivity
import com.exo.styleswap.util.ImageUtils
import com.exo.styleswap.util.PersonDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    private lateinit var binding: ActivityMainBinding

    private var personFile: File? = null
    private var capturedFile: File? = null

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var useFrontCamera = true
    private var hasBothCameras = false

    private enum class CamMode { LOADING, LIVE, REVIEW, ERROR }

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) onPersonPicked(uri)
    }

    private val requestCamera = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else setCameraMode(CamMode.ERROR)
    }

    // Edit screen returns RESULT_OK after "save → choose another photo" → reset the picker.
    private val editLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) resetPicker()
        loadRecentCreations()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.btnUpload.setOnClickListener { launchGalleryPicker() }
        binding.btnTakePhoto.setOnClickListener { showCamera() }
        binding.btnCameraClose.setOnClickListener { hideCamera() }

        binding.btnCapture.setOnClickListener { capturePhoto() }
        binding.btnFlipCamera.setOnClickListener {
            useFrontCamera = !useFrontCamera
            startCamera()
        }
        binding.btnRetake.setOnClickListener { retakePhoto() }
        binding.btnUseCapture.setOnClickListener {
            val file = capturedFile ?: return@setOnClickListener
            openEdit(file)
        }
        binding.btnGrantPermission.setOnClickListener {
            requestCamera.launch(Manifest.permission.CAMERA)
        }
        binding.btnNext.setOnClickListener {
            val file = personFile ?: return@setOnClickListener
            openEdit(file)
        }

        onBackPressedDispatcher.addCallback(this) {
            if (binding.cameraOverlay.visibility == View.VISIBLE) hideCamera() else finish()
        }

        applyHeroAccent()
        updateNextState()
    }

    override fun onResume() {
        super.onResume()
        if (binding.cameraOverlay.visibility == View.VISIBLE && capturedFile == null) ensureCamera()
        loadRecentCreations()
    }

    override fun onPause() {
        super.onPause()
        stopCamera()
    }

    /** Colors the "With AI" part of the hero title with the magenta accent. */
    private fun applyHeroAccent() {
        val full = getString(R.string.home_hero_title)
        val accent = getString(R.string.home_hero_accent)
        val idx = full.indexOf(accent)
        if (idx < 0) {
            binding.heroTitle.text = full
            return
        }
        val span = android.text.SpannableString(full)
        span.setSpan(
            android.text.style.ForegroundColorSpan(ContextCompat.getColor(this, R.color.secondary)),
            idx, idx + accent.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.heroTitle.text = span
    }

    private fun launchGalleryPicker() {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    // ── Recent creations (saved results in the gallery) ──────────────────

    private fun loadRecentCreations() {
        lifecycleScope.launch {
            val uris = withContext(Dispatchers.IO) { queryRecentCreations() }
            val row = binding.recentRow
            row.removeAllViews()
            if (uris.isEmpty()) {
                binding.recentSection.visibility = View.GONE
                return@launch
            }
            binding.recentSection.visibility = View.VISIBLE
            uris.forEach { uri ->
                val card = ImageView(this@MainActivity).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    isClickable = true
                    isFocusable = true
                    contentDescription = getString(R.string.home_recent)
                    setOnClickListener { viewImage(uri) }
                }
                val lp = LinearLayout.LayoutParams(dp(96), dp(124))
                lp.marginEnd = dp(12)
                row.addView(card, lp)
                Glide.with(this@MainActivity).load(uri)
                    .transform(RoundedCorners(dp(14)))
                    .into(card)
            }
        }
    }

    /** Most recent images saved under Pictures/Outfitly (our own files; no permission needed). */
    private fun queryRecentCreations(): List<Uri> {
        val out = mutableListOf<Uri>()
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val (selection, args) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?" to arrayOf("%Outfitly%")
        } else {
            MediaStore.Images.Media.DATA + " LIKE ?" to arrayOf("%Outfitly%")
        }
        val sort = MediaStore.Images.Media.DATE_ADDED + " DESC"
        try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, args, sort
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext() && out.size < 12) {
                    out.add(
                        ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(idCol)
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
        return out
    }

    private fun viewImage(uri: Uri) {
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            )
        } catch (_: Exception) {
            toast(getString(R.string.cant_open_store))
        }
    }

    // ── Person picked / captured ─────────────────────────────────────────

    private fun onPersonPicked(uri: Uri) {
        lifecycleScope.launch {
            val file = withContext(Dispatchers.IO) {
                ImageUtils.prepareForUpload(this@MainActivity, uri, "person.jpg")
            }
            if (file == null) {
                toast(getString(R.string.err_read_image))
                return@launch
            }
            if (!PersonDetector.hasPerson(file)) {
                toast(getString(R.string.err_no_person))
                return@launch
            }
            personFile = file
            showPersonPreview(file)
        }
    }

    private fun showPersonPreview(file: File) {
        hideCamera()
        binding.heroHint.visibility = View.GONE
        Glide.with(this)
            .load(file)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .signature(ObjectKey("${file.lastModified()}_${file.length()}"))
            .into(binding.imgPreview)
        updateNextState()
    }

    private fun updateNextState() {
        val enabled = personFile != null
        binding.btnNext.isClickable = enabled
        binding.btnNext.setBackgroundResource(
            if (enabled) R.drawable.bg_gradient_primary else R.drawable.bg_button_disabled
        )
        val color = ContextCompat.getColor(
            this, if (enabled) R.color.white else R.color.disabled_foreground
        )
        binding.btnNextText.setTextColor(color)
        binding.btnNextIcon.setColorFilter(color)
    }

    // ── Camera overlay ───────────────────────────────────────────────────

    private fun showCamera() {
        binding.cameraOverlay.visibility = View.VISIBLE
        capturedFile = null
        ensureCamera()
    }

    private fun hideCamera() {
        stopCamera()
        binding.cameraOverlay.visibility = View.GONE
    }

    private fun ensureCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestCamera.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        setCameraMode(CamMode.LOADING)
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                val provider = future.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.previewCamera.surfaceProvider)
                }
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val hasFront = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                val hasBack = provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                hasBothCameras = hasFront && hasBack
                if (useFrontCamera && !hasFront) useFrontCamera = false
                if (!useFrontCamera && !hasBack) useFrontCamera = true
                val selector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                provider.unbindAll()
                provider.bindToLifecycle(this, selector, preview, imageCapture)
                setCameraMode(CamMode.LIVE)
            } catch (e: Exception) {
                setCameraMode(CamMode.ERROR)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopCamera() {
        try {
            cameraProvider?.unbindAll()
        } catch (_: Exception) {
        }
    }

    private fun capturePhoto() {
        val capture = imageCapture ?: return
        val file = File(cacheDir, "capture_raw.jpg")
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        capture.takePicture(
            options,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    lifecycleScope.launch {
                        val prepared = withContext(Dispatchers.IO) {
                            ImageUtils.prepareForUpload(
                                this@MainActivity, Uri.fromFile(file), "person.jpg"
                            )
                        }
                        if (prepared == null) {
                            toast(getString(R.string.err_read_image))
                            return@launch
                        }
                        if (!PersonDetector.hasPerson(prepared)) {
                            toast(getString(R.string.err_no_person))
                            return@launch
                        }
                        capturedFile = prepared
                        stopCamera()
                        showCameraReview(prepared)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    toast(getString(R.string.err_capture))
                }
            }
        )
    }

    private fun retakePhoto() {
        capturedFile = null
        startCamera()
    }

    private fun showCameraReview(file: File) {
        Glide.with(this)
            .load(file)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .signature(ObjectKey("${file.lastModified()}_${file.length()}"))
            .into(binding.cameraCaptured)
        setCameraMode(CamMode.REVIEW)
    }

    private fun setCameraMode(mode: CamMode) {
        binding.cameraStatusView.visibility = if (mode == CamMode.LOADING) View.VISIBLE else View.GONE
        binding.cameraErrorView.visibility = if (mode == CamMode.ERROR) View.VISIBLE else View.GONE
        val live = mode == CamMode.LIVE
        binding.previewCamera.visibility = if (live) View.VISIBLE else View.GONE
        binding.viewfinder.visibility = if (live) View.VISIBLE else View.GONE
        binding.btnCapture.visibility = if (live) View.VISIBLE else View.GONE
        binding.btnFlipCamera.visibility = if (live && hasBothCameras) View.VISIBLE else View.GONE
        val review = mode == CamMode.REVIEW
        binding.cameraCaptured.visibility = if (review) View.VISIBLE else View.GONE
        binding.cameraReviewActions.visibility = if (review) View.VISIBLE else View.GONE
    }

    private fun openEdit(file: File) {
        hideCamera()
        editLauncher.launch(
            Intent(this, EditActivity::class.java)
                .putExtra(EditActivity.EXTRA_PERSON_PATH, file.absolutePath)
        )
    }

    /** Clear the current selection so the user starts fresh (after saving). */
    private fun resetPicker() {
        personFile = null
        capturedFile = null
        Glide.with(this).clear(binding.imgPreview)
        binding.imgPreview.setImageResource(R.drawable.change_outfit)
        binding.heroHint.visibility = View.VISIBLE
        updateNextState()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
