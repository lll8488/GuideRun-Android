package com.blindrunner.app.util

import android.content.Context
import android.content.SharedPreferences

object AppPrefs {
    private const val NAME = "app_prefs"
    private lateinit var prefs: SharedPreferences
    private var appContext: Context? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        appContext = context.applicationContext
    }

    fun getContext(): Context? = appContext

    var currentUserPhone: String
        get() = prefs.getString("current_phone", "") ?: ""
        set(v) { prefs.edit().putString("current_phone", v).apply() }

    var firstLogin: Boolean
        get() = prefs.getBoolean("first_login", true)
        set(v) { prefs.edit().putBoolean("first_login", v).apply() }

    var userType: String
        get() = prefs.getString("user_type", "blind") ?: "blind"
        set(v) { prefs.edit().putString("user_type", v).apply() }

    var examPassed: Boolean
        get() = prefs.getBoolean("exam_passed", false)
        set(v) { prefs.edit().putBoolean("exam_passed", v).apply() }

    var examScore: Int
        get() = prefs.getInt("exam_score", 0)
        set(v) { prefs.edit().putInt("exam_score", v).apply() }

    var emergencyContact: String
        get() = prefs.getString("emergency_contact", "110") ?: "110"
        set(v) { prefs.edit().putString("emergency_contact", v).apply() }

    var highContrastMode: Boolean
        get() = prefs.getBoolean("high_contrast", false)
        set(v) { prefs.edit().putBoolean("high_contrast", v).apply() }

    var voicePromptsEnabled: Boolean
        get() = prefs.getBoolean("voice_prompts_enabled", true)
        set(v) { prefs.edit().putBoolean("voice_prompts_enabled", v).apply() }

    var voicePromptInterval: Int
        get() = prefs.getInt("voice_interval", 30)
        set(v) { prefs.edit().putInt("voice_interval", v).apply() }

    var navigationVoiceEnabled: Boolean
        get() = prefs.getBoolean("nav_voice", true)
        set(v) { prefs.edit().putBoolean("nav_voice", v).apply() }

    fun getEmergencyContacts(): List<Pair<String, String>> {
        val names = prefs.getString("ec_names", "") ?: ""
        val phones = prefs.getString("ec_phones", "") ?: ""
        val nameList = names.split("‖").filter { it.isNotEmpty() }
        val phoneList = phones.split("‖").filter { it.isNotEmpty() }
        return nameList.zip(phoneList)
    }

    fun saveEmergencyContacts(contacts: List<Pair<String, String>>) {
        prefs.edit()
            .putString("ec_names", contacts.joinToString("‖") { it.first })
            .putString("ec_phones", contacts.joinToString("‖") { it.second })
            .putString("emergency_contact", contacts.firstOrNull()?.second ?: "110")
            .apply()
    }
}
