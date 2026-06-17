package com.exo.styleswap.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Lightweight SharedPreferences for app-wide settings.
 *
 * First-open state (language, survey, onboarding) lives in
 * [com.exo.styleswap.firstopen.FirstOpenPrefs] instead.
 */
object Prefs {
    private const val FILE = "styleswap_prefs"
    private const val KEY_THEME = "theme_mode"

    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Saved theme mode (AppCompatDelegate.MODE_NIGHT_*); defaults to follow-system. */
    fun themeMode(c: Context): Int =
        sp(c).getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    /** Saves and applies the theme mode app-wide (recreates activities). */
    fun setThemeMode(c: Context, mode: Int) {
        sp(c).edit().putInt(KEY_THEME, mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    /** Applies the saved theme mode (call on app start). */
    fun applyThemeMode(c: Context) {
        AppCompatDelegate.setDefaultNightMode(themeMode(c))
    }
}
