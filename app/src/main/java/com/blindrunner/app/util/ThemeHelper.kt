package com.blindrunner.app.util

import android.app.Activity
import com.blindrunner.app.R

object ThemeHelper {

    fun applyTheme(activity: Activity) {
        if (AppPrefs.highContrastMode) {
            activity.setTheme(R.style.Theme_BlindRunner_HighContrast)
        } else {
            activity.setTheme(R.style.Theme_BlindRunner)
        }
    }
}
