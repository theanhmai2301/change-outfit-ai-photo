package com.exo.styleswap.firstopen

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.exo.styleswap.databinding.ActivityPermissionBinding
import com.exo.styleswap.ui.main.MainActivity

/**
 * Final first-open step: opt in to notifications (POST_NOTIFICATIONS, Android 13+).
 * Tapping the switch requests the permission; if it was permanently denied we send
 * the user to the system notification settings. Continue always proceeds to Main
 * and records the step as done so it is never shown again.
 */
class PermissionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionBinding
    private var requestedOnce = false

    private val requestNotif = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { reflectGranted(isNotificationPermissionGranted(this)) }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.switchNotif.setOnClickListener { onToggleClicked() }
        binding.btnStart.setOnClickListener { continueToMain() }
    }

    override fun onResume() {
        super.onResume()
        reflectGranted(isNotificationPermissionGranted(this))
    }

    private fun onToggleClicked() {
        if (isNotificationPermissionGranted(this)) {
            // Already granted; can't be revoked from inside the app.
            binding.switchNotif.isChecked = true
            return
        }
        binding.switchNotif.isChecked = false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val canPrompt = !requestedOnce ||
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)
        if (canPrompt) {
            requestedOnce = true
            requestNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openAppNotificationSettings()
        }
    }

    private fun continueToMain() {
        applicationContext.isPermissionDone = true
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    private fun reflectGranted(granted: Boolean) {
        binding.switchNotif.isChecked = granted
    }

    private fun openAppNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", packageName, null))
        }
        runCatching { startActivity(intent) }
    }

    companion object {
        /** Notifications are runtime-gated only on Android 13+. */
        fun isNotificationPermissionGranted(context: Context): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

        fun start(context: Context) {
            context.startActivity(Intent(context, PermissionActivity::class.java))
        }

        @Suppress("UNUSED_PARAMETER")
        fun preloadAds(activity: Activity): Boolean = false // TODO: ADS
    }
}
