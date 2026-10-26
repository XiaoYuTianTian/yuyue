package com.yuyue.app.security

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AppLockStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "yuyue_lock_secure",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        }

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIO, false)
        set(value) {
            prefs.edit().putBoolean(KEY_BIO, value).apply()
        }

    fun hasPin(): Boolean = prefs.contains(KEY_HASH) && prefs.contains(KEY_SALT)

    fun setPin(pin: String) {
        require(AppLockPolicy.isValidPin(pin)) { "PIN 须为 4–8 位数字" }
        val salt = AppLockCrypto.newSalt()
        val hash = AppLockCrypto.hashPin(pin, salt)
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putBoolean(KEY_ENABLED, true)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val saltB64 = prefs.getString(KEY_SALT, null) ?: return false
        val hashB64 = prefs.getString(KEY_HASH, null) ?: return false
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val hash = Base64.decode(hashB64, Base64.NO_WRAP)
        return AppLockCrypto.verifyPin(pin, salt, hash)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_BIO = "biometric"
        private const val KEY_SALT = "salt"
        private const val KEY_HASH = "hash"
    }
}
