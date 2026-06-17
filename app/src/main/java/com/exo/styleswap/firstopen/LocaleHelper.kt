package com.exo.styleswap.firstopen

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Two complementary ways to apply the UI language:
 *
 * - [wrap] is used from every Activity's `attachBaseContext`, so an Activity is
 *   already in the right language the moment it is created — no recreate, no
 *   white flash. This is what the first-open flow relies on.
 * - [changeLang] uses [AppCompatDelegate.setApplicationLocales], which recreates
 *   the Activity stack. Used only when switching language from Settings while the
 *   app is running, and once at startup (Splash) to sync the system per-app locale.
 *
 * We deliberately use the legacy `Locale(code)` constructor (not
 * `forLanguageTag`) so the code "in" (Indonesian) keeps matching `values-in/`.
 */
object LocaleHelper {

    /** Wraps [base] with the stored [languageCode], if any. Returns [base] unchanged otherwise. */
    fun wrap(base: Context): Context {
        val code = base.languageCode ?: return base
        val locale = Locale(code)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    /** Applies [code] app-wide via per-app locales (recreates running activities). */
    fun changeLang(code: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(Locale(code)))
    }

    /** The currently applied per-app locale tag, or empty string if none. */
    fun appliedTag(): String =
        AppCompatDelegate.getApplicationLocales().toLanguageTags()
}
