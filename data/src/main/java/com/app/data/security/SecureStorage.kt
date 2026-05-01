package com.app.data.security

import android.content.Context
import android.os.Build
import android.util.Base64
import androidx.annotation.RequiresApi

class SecureStorage(context: Context) {
    private val prefs = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)

/*

    @RequiresApi(Build.VERSION_CODES.M)
    fun saveAuthToken(token: String) {
        val (iv, encrypted) = KeyStoreManager.encrypt(token)
        prefs.edit()
            .putString("auth_iv", Base64.encodeToString(iv, Base64.DEFAULT))
            .putString("auth_token", Base64.encodeToString(encrypted, Base64.DEFAULT))
            .apply()
    }


    @RequiresApi(Build.VERSION_CODES.M)
    fun getAuthToken(): String? {
        val iv = prefs.getString("auth_iv", null)?.let { Base64.decode(it, Base64.DEFAULT) }
        val encrypted = prefs.getString("auth_token", null)?.let { Base64.decode(it, Base64.DEFAULT) }
        return if (iv != null && encrypted != null) {
            KeyStoreManager.decrypt(iv, encrypted)
        } else null
    }
*/

    fun isWelcomeNotificationShown(): Boolean {
        return prefs.getBoolean("welcome_notification_shown", true)
    }

    @RequiresApi(Build.VERSION_CODES.GINGERBREAD)
    fun setWelcomeNotificationShown(shown: Boolean) {
         prefs.edit().putBoolean("welcome_notification_shown",shown).apply()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun setManualAppPIN(appPin: String) {
        val (iv, encrypted) = KeyStoreManager.encrypt(appPin)
             prefs.edit()
            .putString("auth_iv", Base64.encodeToString(iv, Base64.DEFAULT))
            .putString("manualAppPin", Base64.encodeToString(encrypted, Base64.DEFAULT))
            .apply()
    }
    @RequiresApi(Build.VERSION_CODES.M)
    fun getManualAppPIN():String? {
        val iv = prefs.getString("auth_iv", null)?.let { Base64.decode(it, Base64.DEFAULT) }
        val encrypted = prefs.getString("manualAppPin", "")?.let { Base64.decode(it, Base64.DEFAULT) }
        return if (iv != null && encrypted != null) {
            KeyStoreManager.decrypt(iv, encrypted)
        } else null
    }

    fun setFingerprintEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("fingerprint_enabled", enabled).apply()
    }

    fun isFingerprintEnabled(): Boolean {
        return prefs.getBoolean("fingerprint_enabled", false)
    }
}