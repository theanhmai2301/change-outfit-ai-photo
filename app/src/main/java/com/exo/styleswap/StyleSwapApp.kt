package com.exo.styleswap

import android.app.Application
import com.exo.styleswap.firstopen.FirstOpenPrefs
import com.exo.styleswap.util.Prefs

class StyleSwapApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // A fresh install must always replay first-open, even if Auto Backup
        // restored old state. Must run before any splash routing.
        FirstOpenPrefs.ensureFreshInstallResetsFirstOpen(this)
        // Apply the saved light/dark theme before any activity is created.
        Prefs.applyThemeMode(this)
    }
}
