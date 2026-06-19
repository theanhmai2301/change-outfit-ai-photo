package com.exo.styleswap.ui.edit

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.widget.GridLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.exo.styleswap.R
import com.exo.styleswap.data.TryOnApi
import com.exo.styleswap.data.TryOnException
import com.exo.styleswap.data.TryOnResult
import com.exo.styleswap.databinding.ActivityEditBinding
import com.exo.styleswap.firstopen.LocaleHelper
import com.exo.styleswap.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class EditActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    companion object {
        const val EXTRA_PERSON_PATH = "person_path"
        const val SUPPORT_EMAIL = "exostudio.feedback@gmail.com"
    }

    private lateinit var binding: ActivityEditBinding

    private lateinit var personFile: File
    private var garmentFile: File? = null

    private var selectedCategory = 0
    private var selectedPoses = 1
    private val selectedQuality = "low"

    private var isProcessing = false
    private var isEdited = false
    private var saved = false

    private var resultUrl: String? = null
    private var resultBase64: String? = null
    private var resultMime: String = "image/png"
    private var resultRequestId: String? = null
    private var resultBytes: ByteArray? = null

    private val categories = listOf(
        Category("auto", R.string.cat_auto, "✨", R.color.cat_auto),
        Category("top", R.string.cat_top, "👕", R.color.cat_top),
        Category("bottom", R.string.cat_bottom, "👖", R.color.cat_bottom),
        Category("dress", R.string.cat_dress, "👗", R.color.cat_dress),
        Category("outerwear", R.string.cat_outerwear, "🧥", R.color.cat_outerwear),
        Category("full_outfit", R.string.cat_full, "🧍", R.color.cat_full)
    )

    private val categoryTiles = mutableListOf<View>()
    private val poseChips = mutableListOf<TextView>()
    private val dots = mutableListOf<View>()

    private data class Category(val key: String, val labelRes: Int, val emoji: String, val colorRes: Int)

    private val pickGarment = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) onGarmentPicked(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val path = intent.getStringExtra(EXTRA_PERSON_PATH)
        if (path == null || !File(path).exists()) {
            toast(getString(R.string.err_read_image))
            finish()
            return
        }
        personFile = File(path)

        showPerson()
        buildCategories()
        buildPoses()
        buildDots()

        binding.btnBack.setOnClickListener { handleBack() }
        onBackPressedDispatcher.addCallback(this) { handleBack() }
        binding.btnChooseGarment.setOnClickListener {
            pickGarment.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.btnClearGarment.setOnClickListener { clearGarment() }
        binding.btnApply.setOnClickListener { applyTryOn() }
        binding.btnRestore.setOnClickListener { confirmRestore() }
        binding.btnSave.setOnClickListener { saveResult() }
        binding.btnReport.setOnClickListener { showReportDialog() }

        updateApplyState()
        updateActionButtons()
    }

    // ── Preview ──────────────────────────────────────────────────────────

    private fun showPerson() {
        Glide.with(this).load(personFile)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .signature(ObjectKey("${personFile.lastModified()}_${personFile.length()}"))
            .into(binding.imgPreview)
        setAiBadge(false)
        binding.outfitBadge.visibility = View.GONE
        binding.btnReport.visibility = View.GONE
    }

    private fun setAiBadge(edited: Boolean) {
        if (edited) {
            binding.aiBadge.setBackgroundResource(R.drawable.bg_badge_accent)
            binding.aiBadgeText.setText(R.string.ai_generated)
            val c = ContextCompat.getColor(this, R.color.accent)
            binding.aiBadgeText.setTextColor(c)
            binding.aiBadgeIcon.setColorFilter(c)
        } else {
            binding.aiBadge.setBackgroundResource(R.drawable.bg_badge_neutral)
            binding.aiBadgeText.setText(R.string.not_edited)
            val c = ContextCompat.getColor(this, R.color.muted_foreground)
            binding.aiBadgeText.setTextColor(c)
            binding.aiBadgeIcon.setColorFilter(c)
        }
    }

    // ── Garment ──────────────────────────────────────────────────────────

    private fun onGarmentPicked(uri: Uri) {
        lifecycleScope.launch {
            val file = withContext(Dispatchers.IO) {
                ImageUtils.prepareForUpload(this@EditActivity, uri, "garment.jpg")
            }
            if (file == null) {
                toast(getString(R.string.err_read_garment))
                return@launch
            }
            garmentFile = file
            binding.garmentThumbCard.visibility = View.VISIBLE
            Glide.with(this@EditActivity).load(file)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .signature(ObjectKey("${file.lastModified()}_${file.length()}"))
                .into(binding.garmentThumb)
            binding.chooseGarmentText.setText(R.string.change_garment)
            binding.chooseGarmentIcon.setImageResource(R.drawable.ic_image)
            updateApplyState()
        }
    }

    private fun clearGarment() {
        garmentFile = null
        binding.garmentThumbCard.visibility = View.GONE
        binding.chooseGarmentText.setText(R.string.choose_garment)
        binding.chooseGarmentIcon.setImageResource(R.drawable.ic_add)
        updateApplyState()
    }

    // ── Selectors ────────────────────────────────────────────────────────

    private fun buildCategories() {
        val grid = binding.categoryGrid
        grid.removeAllViews()
        categoryTiles.clear()
        val m = dp(4)
        categories.forEachIndexed { index, cat ->
            val tile = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
            }
            val emoji = TextView(this).apply {
                text = cat.emoji
                textSize = 22f
                gravity = Gravity.CENTER
            }
            val label = TextView(this).apply {
                setText(cat.labelRes)
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(0, dp(5), 0, 0)
            }
            tile.addView(emoji)
            tile.addView(label)
            tile.setOnClickListener {
                selectedCategory = index
                refreshCategorySelection()
            }
            val lp = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(76)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(m, m, m, m)
            }
            grid.addView(tile, lp)
            categoryTiles.add(tile)
        }
        refreshCategorySelection()
    }

    private fun refreshCategorySelection() {
        categoryTiles.forEachIndexed { index, tile ->
            val selected = index == selectedCategory
            tile.background = tileBackground(selected)
            val label = (tile as LinearLayout).getChildAt(1) as TextView
            label.setTextColor(
                ContextCompat.getColor(this, if (selected) R.color.foreground else R.color.muted_foreground)
            )
        }
    }

    private fun tileBackground(selected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dp(12).toFloat()
            if (selected) {
                setColor(ContextCompat.getColor(this@EditActivity, R.color.surface_selected))
                setStroke(dp(2), ContextCompat.getColor(this@EditActivity, R.color.border_selected))
            } else {
                setColor(ContextCompat.getColor(this@EditActivity, R.color.surface))
                setStroke(dp(2), Color.TRANSPARENT)
            }
        }
    }

    private fun buildPoses() {
        val row = binding.posesRow
        row.removeAllViews()
        poseChips.clear()
        for (n in 1..6) {
            val chip = TextView(this).apply {
                text = n.toString()
                textSize = 14f
                gravity = Gravity.CENTER
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            val lp = LinearLayout.LayoutParams(0, dp(42), 1f)
            val m = dp(3)
            lp.setMargins(if (n == 1) 0 else m, 0, if (n == 6) 0 else m, 0)
            chip.layoutParams = lp
            chip.setOnClickListener {
                selectedPoses = n
                refreshPoseSelection()
            }
            row.addView(chip)
            poseChips.add(chip)
        }
        refreshPoseSelection()
    }

    private fun refreshPoseSelection() {
        poseChips.forEachIndexed { i, chip ->
            val selected = (i + 1) == selectedPoses
            chip.setBackgroundResource(if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
            chip.setTextColor(
                ContextCompat.getColor(this, if (selected) R.color.primary else R.color.muted_foreground)
            )
        }
    }

    private fun buildDots() {
        binding.processingDots.removeAllViews()
        dots.clear()
        for (i in 0 until 4) {
            val dot = View(this)
            val lp = LinearLayout.LayoutParams(dp(6), dp(6))
            lp.setMargins(dp(3), 0, dp(3), 0)
            dot.layoutParams = lp
            binding.processingDots.addView(dot)
            dots.add(dot)
        }
        updateDots(0)
    }

    private fun updateDots(active: Int) {
        dots.forEachIndexed { i, dot ->
            val lp = dot.layoutParams as LinearLayout.LayoutParams
            lp.width = if (i == active) dp(20) else dp(6)
            dot.layoutParams = lp
            val bg = GradientDrawable().apply {
                cornerRadius = dp(3).toFloat()
                setColor(
                    if (i <= active) ContextCompat.getColor(this@EditActivity, R.color.primary)
                    else withAlpha(Color.WHITE, 0x33)
                )
            }
            dot.background = bg
        }
    }

    // ── Apply ────────────────────────────────────────────────────────────

    private fun updateApplyState() {
        val enabled = garmentFile != null && !isProcessing
        binding.btnApply.isClickable = enabled
        binding.btnApply.setBackgroundResource(
            if (enabled) R.drawable.bg_gradient_primary else R.drawable.bg_button_disabled
        )
        val color = ContextCompat.getColor(this, if (enabled) R.color.white else R.color.disabled_foreground)
        binding.btnApplyText.setTextColor(color)
        binding.btnApplyIcon.setColorFilter(color)
    }

    private fun applyTryOn() {
        val garment = garmentFile
        if (garment == null) {
            toast(getString(R.string.select_garment_first))
            return
        }
        if (isProcessing) return

        isProcessing = true
        saved = false
        showProcessing(true)
        updateApplyState()
        updateActionButtons()
        setControlsEnabled(false)

        val steps = listOf(
            R.string.step_analyze, R.string.step_detect, R.string.step_apply, R.string.step_finish
        )
        val animJob: Job = lifecycleScope.launch {
            var i = 0
            while (isActive) {
                val idx = i % steps.size
                binding.processingText.setText(steps[idx])
                binding.btnApplyText.setText(steps[idx])
                updateDots(idx)
                delay(900)
                i++
            }
        }

        lifecycleScope.launch {
            try {
                val result = TryOnApi.tryOn(
                    person = personFile,
                    garment = garment,
                    category = categories[selectedCategory].key,
                    poses = selectedPoses,
                    quality = selectedQuality,
                    responseFormat = "url"
                )
                val bytes = withContext(Dispatchers.IO) { fetchResultBytes(result) }
                animJob.cancel()
                when {
                    bytes != null -> onTryOnSuccess(result, bytes)
                    result.url == null && result.imageBase64 == null ->
                        onTryOnError(getString(R.string.err_garment_unrecognized))
                    else -> onTryOnError(getString(R.string.err_network))
                }
            } catch (e: TryOnException) {
                animJob.cancel()
                onTryOnError(friendlyTryOnError(e))
            } catch (e: java.io.InterruptedIOException) {
                animJob.cancel()
                onTryOnError(getString(R.string.err_timeout))
            } catch (e: Exception) {
                animJob.cancel()
                onTryOnError(getString(R.string.err_network))
            }
        }
    }

    /**
     * Materializes the result image to bytes once, here, so the preview and Save
     * both use the same in-memory copy. The /v1/tryon URL can briefly 404 right
     * after the call returns, so retry a few times before giving up.
     */
    private suspend fun fetchResultBytes(result: TryOnResult): ByteArray? {
        result.imageBase64?.let {
            return runCatching { Base64.decode(it, Base64.DEFAULT) }.getOrNull()
        }
        val url = result.url ?: return null
        repeat(4) {
            val bytes = runCatching { TryOnApi.downloadBytes(url) }.getOrNull()
            if (bytes != null && bytes.isNotEmpty()) return bytes
            delay(1500)
        }
        return null
    }

    private fun onTryOnSuccess(result: TryOnResult, bytes: ByteArray) {
        isProcessing = false
        isEdited = true
        resultBytes = bytes
        resultUrl = result.url
        resultBase64 = result.imageBase64
        resultMime = result.mimeType
        resultRequestId = result.requestId
        showProcessing(false)
        binding.btnReport.visibility = View.VISIBLE

        Glide.with(this).load(bytes)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(binding.imgPreview)

        setAiBadge(true)
        val cat = categories[selectedCategory]
        binding.outfitBadge.text = "${cat.emoji} ${getString(cat.labelRes)}"
        binding.outfitBadge.visibility = View.VISIBLE

        resetApplyLabel()
        updateApplyState()
        updateActionButtons()
        setControlsEnabled(true)
        toast(getString(R.string.ai_generated) + " ✓")
    }

    private fun onTryOnError(message: String) {
        isProcessing = false
        showProcessing(false)
        resetApplyLabel()
        updateApplyState()
        updateActionButtons()
        setControlsEnabled(true)
        toast(message)
    }

    /**
     * Chuyển lỗi từ server thành thông báo dễ hiểu cho người dùng.
     * KHÔNG bao giờ hiển thị nội dung lỗi gốc (tiếng Anh / mã HTTP / JSON) ra giao diện.
     */
    private fun friendlyTryOnError(e: TryOnException): String {
        val detail = (e.message ?: "").lowercase()
        return when {
            detail.contains("valid image") -> getString(R.string.err_garment_invalid)
            detail.contains("provide") && detail.contains("image") -> getString(R.string.err_image_missing)
            e.code == 503 || e.code == 401 || e.code == 403 -> getString(R.string.err_service_unavailable)
            e.code == 413 -> getString(R.string.err_too_large)
            e.code == 429 -> getString(R.string.err_rate_limited)
            e.code in 500..599 -> getString(R.string.err_server_busy)
            e.code == 400 || e.code == 422 -> getString(R.string.err_bad_request)
            else -> getString(R.string.err_generic)
        }
    }

    private fun resetApplyLabel() {
        binding.btnApplyText.setText(R.string.apply_ai)
    }

    private fun showProcessing(show: Boolean) {
        binding.processingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.applySpinner.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnApplyIcon.visibility = if (show) View.GONE else View.VISIBLE
    }

    // ── Restore / Save ───────────────────────────────────────────────────

    /** Confirm before reverting — the Restore button sits next to Save and is easy to mis-tap. */
    private fun confirmRestore() {
        if (!isEdited || isProcessing) return
        showStyledConfirm(
            title = getString(R.string.restore_confirm_title),
            message = getString(R.string.restore_confirm_msg),
            positiveText = getString(R.string.restore_confirm_yes),
            negativeText = getString(R.string.report_cancel),
            destructive = true
        ) { restore() }
    }

    private fun restore() {
        if (!isEdited || isProcessing) return
        isEdited = false
        saved = false
        resultUrl = null
        resultBase64 = null
        resultBytes = null
        showPerson()
        updateActionButtons()
        toast(getString(R.string.restored))
    }

    private fun saveResult() {
        if (!isEdited || isProcessing) return
        val bytes = resultBytes
        if (bytes == null) {
            toast(getString(R.string.save_failed))
            return
        }
        lifecycleScope.launch {
            val ext = if (resultMime.contains("jpeg") || resultMime.contains("jpg")) "jpg" else "png"
            val name = "styleswap_${System.currentTimeMillis()}.$ext"
            val ok = withContext(Dispatchers.IO) {
                ImageUtils.saveImageToGallery(this@EditActivity, bytes, name, resultMime)
            }
            if (ok) {
                saved = true
                updateActionButtons()
                showSavedDialog()
            } else {
                toast(getString(R.string.save_failed))
            }
        }
    }

    /** After a successful save: offer to go back to the picker or stay. */
    private fun showSavedDialog() {
        if (isFinishing) return
        showStyledConfirm(
            title = getString(R.string.save_dialog_title),
            message = getString(R.string.save_dialog_msg),
            positiveText = getString(R.string.save_dialog_positive),
            negativeText = getString(R.string.save_dialog_negative),
            destructive = false
        ) {
            // Tell the picker to reset so the user can choose a different photo.
            setResult(RESULT_OK)
            finish()
        }
    }

    /** Warn before leaving when there is an unsaved generated result. */
    private fun handleBack() {
        if (isEdited && !saved && !isProcessing) {
            showStyledConfirm(
                title = getString(R.string.exit_dialog_title),
                message = getString(R.string.exit_dialog_msg),
                positiveText = getString(R.string.exit_dialog_positive),
                negativeText = getString(R.string.exit_dialog_negative),
                destructive = true
            ) { finish() }
        } else {
            finish()
        }
    }

    /** A dark, app-themed confirm dialog (transparent window, clear buttons). */
    private fun showStyledConfirm(
        title: String,
        message: String,
        positiveText: String,
        negativeText: String,
        destructive: Boolean,
        onPositive: () -> Unit
    ) {
        val view = layoutInflater.inflate(R.layout.dialog_confirm, null)
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )

        view.findViewById<TextView>(R.id.dialogTitle).text = title
        view.findViewById<TextView>(R.id.dialogMessage).text = message
        val pos = view.findViewById<TextView>(R.id.dialogPositive)
        val neg = view.findViewById<TextView>(R.id.dialogNegative)
        pos.text = positiveText
        neg.text = negativeText
        pos.setBackgroundResource(
            if (destructive) R.drawable.bg_dialog_btn_destructive else R.drawable.bg_dialog_btn_primary
        )
        pos.setOnClickListener { dialog.dismiss(); onPositive() }
        neg.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // ── Report ───────────────────────────────────────────────────────────

    private fun showReportDialog() {
        if (!isEdited) return
        showStyledConfirm(
            title = getString(R.string.report_dialog_title),
            message = getString(R.string.report_dialog_msg),
            positiveText = getString(R.string.report_send),
            negativeText = getString(R.string.report_cancel),
            destructive = true
        ) { sendReport() }
    }

    private fun sendReport() {
        val body = buildString {
            append("Request ID: ").append(resultRequestId ?: "N/A").append('\n')
            append("Category: ").append(categories[selectedCategory].key).append('\n')
            append("Poses: ").append(selectedPoses).append('\n')
            if (resultUrl != null) append("URL: ").append(TryOnApi.absoluteUrl(resultUrl!!)).append('\n')
            append("\n---\n")
        }
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.report_subject))
            putExtra(Intent.EXTRA_TEXT, body)
        }
        try {
            startActivity(intent)
            toast(getString(R.string.report_thanks))
        } catch (e: ActivityNotFoundException) {
            toast(getString(R.string.no_email_app))
        }
    }

    private fun updateActionButtons() {
        // Restore
        if (isEdited && !isProcessing) {
            binding.btnRestore.alpha = 1f
            binding.btnRestore.isClickable = true
            binding.btnRestore.setBackgroundResource(R.drawable.bg_icon_button_destructive)
            binding.btnRestoreIcon.setColorFilter(ContextCompat.getColor(this, R.color.destructive))
        } else {
            binding.btnRestore.alpha = 0.5f
            binding.btnRestore.isClickable = false
            binding.btnRestore.setBackgroundResource(R.drawable.bg_icon_button)
            binding.btnRestoreIcon.setColorFilter(ContextCompat.getColor(this, R.color.disabled_foreground))
        }
        // Save
        if (isEdited && !isProcessing) {
            binding.btnSave.alpha = 1f
            binding.btnSave.isClickable = true
            if (saved) {
                binding.btnSave.setBackgroundResource(R.drawable.bg_icon_button_accent)
                binding.btnSaveIcon.setImageResource(R.drawable.ic_check_circle)
            } else {
                binding.btnSave.setBackgroundResource(R.drawable.bg_icon_button_primary)
                binding.btnSaveIcon.setImageResource(R.drawable.ic_download)
            }
            binding.btnSaveIcon.setColorFilter(ContextCompat.getColor(this, R.color.white))
        } else {
            binding.btnSave.alpha = 0.5f
            binding.btnSave.isClickable = false
            binding.btnSave.setBackgroundResource(R.drawable.bg_icon_button)
            binding.btnSaveIcon.setImageResource(R.drawable.ic_download)
            binding.btnSaveIcon.setColorFilter(ContextCompat.getColor(this, R.color.disabled_foreground))
        }
    }

    /** Locks every input control while a generation is in flight (re-enabled after). */
    private fun setControlsEnabled(enabled: Boolean) {
        val dim = if (enabled) 1f else 0.4f
        binding.btnChooseGarment.isClickable = enabled
        binding.btnChooseGarment.alpha = dim
        binding.btnClearGarment.isClickable = enabled
        binding.btnClearGarment.alpha = dim
        binding.categoryGrid.alpha = dim
        binding.posesRow.alpha = dim
        categoryTiles.forEach { it.isClickable = enabled }
        poseChips.forEach { it.isClickable = enabled }
    }

    // ── Utils ────────────────────────────────────────────────────────────

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
