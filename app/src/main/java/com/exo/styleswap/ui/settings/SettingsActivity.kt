package com.exo.styleswap.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.exo.styleswap.BuildConfig
import com.exo.styleswap.R
import com.exo.styleswap.databinding.ActivitySettingsBinding
import com.exo.styleswap.firstopen.LocaleHelper
import com.exo.styleswap.firstopen.language.Language
import com.exo.styleswap.firstopen.language.LanguageFO2Activity
import com.exo.styleswap.firstopen.languageCode
import com.exo.styleswap.ui.edit.EditActivity.Companion.SUPPORT_EMAIL
import com.exo.styleswap.util.Prefs

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.rowLanguage.setOnClickListener {
            LanguageFO2Activity.startFromSettings(this)
        }
        binding.rowTheme.setOnClickListener { showThemeDialog() }
        binding.rowShare.setOnClickListener { shareApp() }
        binding.rowRate.setOnClickListener { rateApp() }
        binding.rowFeedback.setOnClickListener { sendFeedback() }
        binding.rowPrivacy.setOnClickListener {
            startActivity(Intent(this, PrivacyActivity::class.java))
        }

        binding.settingsVersion.text =
            getString(R.string.settings_version) + " " + BuildConfig.VERSION_NAME
    }

    override fun onResume() {
        super.onResume()
        binding.settingsLangValue.text = Language.nameOf(applicationContext.languageCode)
        binding.settingsThemeValue.setText(themeLabelRes(Prefs.themeMode(this)))
    }

    private fun themeLabelRes(mode: Int): Int = when (mode) {
        AppCompatDelegate.MODE_NIGHT_NO -> R.string.theme_light
        AppCompatDelegate.MODE_NIGHT_YES -> R.string.theme_dark
        else -> R.string.theme_system
    }

    private fun showThemeDialog() {
        val modes = intArrayOf(
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES,
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )
        val labels = arrayOf(
            getString(R.string.theme_light),
            getString(R.string.theme_dark),
            getString(R.string.theme_system)
        )
        val current = modes.indexOf(Prefs.themeMode(this)).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_theme)
            .setSingleChoiceItems(labels, current) { dialog, which ->
                dialog.dismiss()
                Prefs.setThemeMode(this, modes[which])
            }
            .setNegativeButton(R.string.report_cancel, null)
            .show()
    }

    private fun shareApp() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text))
        }
        startActivity(Intent.createChooser(intent, getString(R.string.settings_share)))
    }

    private fun rateApp() {
        val market = Uri.parse("market://details?id=$packageName")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, market))
        } catch (e: ActivityNotFoundException) {
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                    )
                )
            } catch (e2: Exception) {
                toast(getString(R.string.cant_open_store))
            }
        }
    }

    private fun sendFeedback() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject))
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast(getString(R.string.no_email_app))
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
