package com.exo.styleswap.firstopen.language

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.exo.styleswap.databinding.ActivityLanguageFo1Binding

/**
 * The language screen in its "nothing selected" state. Tapping a language hands
 * off — with no animation — to [LanguageFO2Activity] (the "selected" state),
 * carrying the tapped language and the current scroll position so the swap is
 * invisible. The Confirm button stays disabled here (nothing is selected yet).
 */
class LanguageFO1Activity : BaseLanguageActivity<ActivityLanguageFo1Binding>() {

    override fun inflateBinding(inflater: LayoutInflater) =
        ActivityLanguageFo1Binding.inflate(inflater)

    override fun recyclerView(): RecyclerView = binding.rvLanguages

    override fun initialSelectedIndex(): Int = -1 // nothing selected

    override fun onLanguageClicked(position: Int) {
        val lm = binding.rvLanguages.layoutManager as LinearLayoutManager
        val firstPos = lm.findFirstVisibleItemPosition().coerceAtLeast(0)
        val firstTop = lm.findViewByPosition(firstPos)?.top ?: 0
        LanguageFO2Activity.start(this, languages[position].code, firstPos, firstTop)
        @Suppress("DEPRECATION") overridePendingTransition(0, 0)
        finish()
        @Suppress("DEPRECATION") overridePendingTransition(0, 0)
    }

    override fun onViewReady() {
        loadNativeAd()
    }

    private fun loadNativeAd() {
        // TODO: ADS — show a native ad in binding.frAds for this state.
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, LanguageFO1Activity::class.java))
        }

        @Suppress("UNUSED_PARAMETER")
        fun preloadAds(activity: Activity): Boolean = false // TODO: ADS
    }
}
