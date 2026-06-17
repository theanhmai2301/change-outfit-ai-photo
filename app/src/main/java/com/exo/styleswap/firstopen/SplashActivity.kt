package com.exo.styleswap.firstopen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.exo.styleswap.firstopen.language.LanguageFO1Activity
import com.exo.styleswap.firstopen.onboarding.OnboardingActivity
import com.exo.styleswap.firstopen.survey.SurveyActivity
import com.exo.styleswap.ui.main.MainActivity

/**
 * Launcher. Shows the Android 12+ system splash, re-applies the saved locale,
 * (optionally) warms up ads for the next screen, then routes to whichever
 * first-open step is still pending — or straight to Main once everything is done.
 */
class SplashActivity : AppCompatActivity() {

    private var completed = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !completed }

        // Keep the system per-app locale in sync with the stored choice, once.
        applicationContext.languageCode?.let { code ->
            if (!LocaleHelper.appliedTag().startsWith(code)) LocaleHelper.changeLang(code)
        }

        preloadAds()

        // Brief hold so the splash is perceptible; replace with an ads-loaded
        // callback when a real ad SDK is wired in.
        window.decorView.postDelayed({ startNextScreen() }, SPLASH_DELAY_MS)
    }

    private fun startNextScreen() {
        val ctx = applicationContext
        val next = when {
            ctx.languageCode == null -> Intent(this, LanguageFO1Activity::class.java)
            !ctx.isSurveyDone -> Intent(this, SurveyActivity::class.java)
            !ctx.isOnboardingDone -> Intent(this, OnboardingActivity::class.java)
            !ctx.isPermissionDone &&
                !PermissionActivity.isNotificationPermissionGranted(this) ->
                Intent(this, PermissionActivity::class.java)
            else -> Intent(this, MainActivity::class.java)
        }
        completed = true
        startActivity(next)
        finish()
    }

    /**
     * Warm up the native/interstitial ad for whichever screen is shown next.
     * Each first-open screen exposes a `preloadAds(activity)` hook that returns
     * true when it will be displayed. Fill these in when AdMob/Meta is integrated.
     */
    private fun preloadAds() {
        // TODO: ADS — chain preloads for the next screen, e.g.:
        // if (!LanguageFO1Activity.preloadAds(this)) {
        //     if (!SurveyActivity.preloadAds(this)) { ... }
        // }
    }

    companion object {
        private const val SPLASH_DELAY_MS = 900L
    }
}
