package com.exo.styleswap.firstopen.language

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.ConfigurationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.exo.styleswap.firstopen.LocaleHelper
import com.exo.styleswap.firstopen.languageCode

/**
 * Shared base for the ONE language screen, realized as two activities representing
 * its states: FO1 = nothing selected, FO2 = a language selected. Both render the
 * SAME fixed-order list (no reordering), so swapping FO1 → FO2 only makes the
 * tapped row highlight appear — the user perceives a single screen. The scroll
 * position is carried across the swap to keep it seamless.
 */
abstract class BaseLanguageActivity<VB : ViewBinding> : AppCompatActivity() {

    protected lateinit var binding: VB
    protected val languages: List<Language> = Language.ALL
    protected lateinit var adapter: LanguageAdapter

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = inflateBinding(layoutInflater)
        setContentView(binding.root)

        adapter = LanguageAdapter(languages, currentSelect = initialSelectedIndex()) {
            onLanguageClicked(it)
        }
        recyclerView().apply {
            layoutManager = LinearLayoutManager(this@BaseLanguageActivity)
            adapter = this@BaseLanguageActivity.adapter
            setHasFixedSize(true)
        }
        onViewReady()
    }

    protected abstract fun inflateBinding(inflater: LayoutInflater): VB
    protected abstract fun recyclerView(): RecyclerView

    /** Index of the highlighted row, or -1 for "nothing selected" (FO1). */
    protected abstract fun initialSelectedIndex(): Int

    /** Row tap handler. FO1 swaps to FO2; FO2 just moves the highlight. */
    protected abstract fun onLanguageClicked(position: Int)

    protected open fun onViewReady() {}

    protected fun selectedLanguage(): Language? = languages.getOrNull(adapter.currentSelect)

    /**
     * Stores the chosen language tag. When [applyNow] is true the locale is also
     * applied immediately (recreates running activities) — used from Settings.
     * During first-open pass false: the next screen's `attachBaseContext` wrap
     * applies it with no flash.
     */
    protected fun commitLanguage(code: String, applyNow: Boolean) {
        applicationContext.languageCode = code
        if (applyNow) LocaleHelper.changeLang(code)
    }

    /** Index in the fixed list of the device language, falling back to English. */
    protected fun systemIndex(): Int {
        val sys = ConfigurationCompat.getLocales(resources.configuration)[0]?.language ?: "en"
        val normalized = if (sys == "id") "in" else sys // legacy Indonesian tag
        return languages.indexOfFirst { it.code == normalized }.coerceAtLeast(0)
    }

    protected fun indexOfCode(code: String?): Int =
        languages.indexOfFirst { it.code == code }
}
