package com.exo.styleswap.firstopen.language

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.exo.styleswap.databinding.ActivityLanguageFo2Binding
import com.exo.styleswap.firstopen.languageCode
import com.exo.styleswap.firstopen.survey.SurveyActivity

/**
 * The language screen in its "selected" state. It receives the tapped language
 * (and FO1's scroll position) so it looks identical to FO1 with the chosen row
 * now highlighted. Tapping another row just moves the highlight (stays here).
 * Confirm:
 *  - First-open: persists the language (no immediate apply — the next screen's
 *    `attachBaseContext` wrap handles it) and continues to the survey.
 *  - From Settings ([startFromSettings]): applies immediately and finishes back.
 */
class LanguageFO2Activity : BaseLanguageActivity<ActivityLanguageFo2Binding>() {

    private var fromSettings = false

    override fun inflateBinding(inflater: LayoutInflater) =
        ActivityLanguageFo2Binding.inflate(inflater)

    override fun recyclerView(): RecyclerView = binding.rvLanguages

    override fun initialSelectedIndex(): Int {
        val code = intent.getStringExtra(EXTRA_CODE) ?: applicationContext.languageCode
        return indexOfCode(code).let { if (it >= 0) it else systemIndex() }
    }

    override fun onLanguageClicked(position: Int) {
        adapter.select(position) // change the highlight, stay on this screen
    }

    override fun onViewReady() {
        fromSettings = intent.getBooleanExtra(EXTRA_FROM_SETTINGS, false)
        binding.btnBack.visibility = if (fromSettings) View.VISIBLE else View.GONE
        binding.btnBack.setOnClickListener { finish() }
        binding.btnConfirm.setOnClickListener { confirm() }
        restoreScroll()
        loadNativeAd()
    }

    /** Match FO1's scroll so the highlight appears in place (no visible jump). */
    private fun restoreScroll() {
        val lm = binding.rvLanguages.layoutManager as LinearLayoutManager
        if (intent.hasExtra(EXTRA_SCROLL_POS)) {
            lm.scrollToPositionWithOffset(
                intent.getIntExtra(EXTRA_SCROLL_POS, 0),
                intent.getIntExtra(EXTRA_SCROLL_OFFSET, 0)
            )
        } else {
            lm.scrollToPosition(adapter.currentSelect.coerceAtLeast(0))
        }
    }

    private fun confirm() {
        val code = selectedLanguage()?.code ?: return
        if (fromSettings) {
            val changed = code != applicationContext.languageCode
            commitLanguage(code, applyNow = changed)
            finish()
        } else {
            commitLanguage(code, applyNow = false)
            SurveyActivity.start(this)
            finish()
        }
    }

    private fun loadNativeAd() {
        // TODO: ADS — show a (different) native ad in binding.frAds for this state.
    }

    companion object {
        private const val EXTRA_CODE = "preselect_code"
        private const val EXTRA_FROM_SETTINGS = "from_settings"
        private const val EXTRA_SCROLL_POS = "scroll_pos"
        private const val EXTRA_SCROLL_OFFSET = "scroll_offset"

        /** First-open: seamless swap from FO1, preselecting [code] at FO1's scroll. */
        fun start(context: Context, code: String, scrollPos: Int, scrollOffset: Int) {
            context.startActivity(
                Intent(context, LanguageFO2Activity::class.java)
                    .putExtra(EXTRA_CODE, code)
                    .putExtra(EXTRA_SCROLL_POS, scrollPos)
                    .putExtra(EXTRA_SCROLL_OFFSET, scrollOffset)
            )
        }

        /** From Settings: open as a standalone language switcher (back + immediate apply). */
        fun startFromSettings(context: Context) {
            context.startActivity(
                Intent(context, LanguageFO2Activity::class.java)
                    .putExtra(EXTRA_FROM_SETTINGS, true)
            )
        }
    }
}
