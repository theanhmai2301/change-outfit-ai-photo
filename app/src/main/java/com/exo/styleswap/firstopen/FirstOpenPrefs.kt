package com.exo.styleswap.firstopen

import android.content.Context
import java.io.File

/**
 * State for the first-open flow, stored in the dedicated "first_open"
 * SharedPreferences. Each step persists its progress so re-opening the app
 * skips everything already completed.
 *
 * Exposed as [Context] extension properties so any Activity can read/write
 * naturally: `isSurveyDone`, `languageCode`, etc.
 */
object FirstOpenPrefs {
    const val FILE = "first_open"

    private const val KEY_LANGUAGE = "language_code"
    private const val KEY_SURVEY = "complete_survey"
    private const val KEY_ONBOARDING = "complete_onboarding"

    /** Optional convenience: the styles the user picked in the survey. */
    private const val KEY_SURVEY_CHOICE = "survey_choice"

    private const val MARKER = "first_open.initialized"

    internal fun prefs(c: Context) =
        c.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /**
     * Defends against Auto Backup / device-transfer "restoring" the completed
     * first-open state onto what is actually a brand-new install.
     *
     * A marker file is created in [Context.getNoBackupFilesDir], which Android
     * never backs up or transfers. A fresh install therefore lacks the marker
     * even if the SharedPreferences were restored — so we clear the prefs and
     * recreate the marker. Call once from [android.app.Application.onCreate],
     * before any splash routing.
     */
    fun ensureFreshInstallResetsFirstOpen(c: Context) {
        val marker = File(c.applicationContext.noBackupFilesDir, MARKER)
        if (!marker.exists()) {
            prefs(c).edit().clear().apply()
            runCatching { marker.parentFile?.mkdirs(); marker.createNewFile() }
        }
    }
}

/** Language tag chosen by the user (e.g. "en"), or null until the picker is used. */
var Context.languageCode: String?
    get() = FirstOpenPrefs.prefs(this).getString("language_code", null)
    set(value) = FirstOpenPrefs.prefs(this).edit().putString("language_code", value).apply()

/** True once the multi-step survey is finished. */
var Context.isSurveyDone: Boolean
    get() = FirstOpenPrefs.prefs(this).getBoolean("complete_survey", false)
    set(value) = FirstOpenPrefs.prefs(this).edit().putBoolean("complete_survey", value).apply()

/** True once onboarding is finished. */
var Context.isOnboardingDone: Boolean
    get() = FirstOpenPrefs.prefs(this).getBoolean("complete_onboarding", false)
    set(value) = FirstOpenPrefs.prefs(this).edit().putBoolean("complete_onboarding", value).apply()

/**
 * True once the user has passed the permission screen (tapped Continue), whether
 * or not they granted. Prevents re-prompting the notification screen every launch.
 */
var Context.isPermissionDone: Boolean
    get() = FirstOpenPrefs.prefs(this).getBoolean("complete_permission", false)
    set(value) = FirstOpenPrefs.prefs(this).edit().putBoolean("complete_permission", value).apply()

/** The styles/topics the user selected during the survey (free-form, for personalization). */
var Context.surveyChoice: String?
    get() = FirstOpenPrefs.prefs(this).getString("survey_choice", null)
    set(value) = FirstOpenPrefs.prefs(this).edit().putString("survey_choice", value).apply()
