package com.app.data.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KeyStoreManager {
    @RequiresApi(Build.VERSION_CODES.M)
    private fun getKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return if (keyStore.containsAlias("NoteZyKey")) {
            (keyStore.getEntry("NoteZyKey", null) as KeyStore.SecretKeyEntry).secretKey
        } else {
            createKey()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun createKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            "NoteZyKey",
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun encrypt(data: String): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        return Pair(encryptedBytes, iv)
    }


    @RequiresApi(Build.VERSION_CODES.M)
    fun decrypt(encryptedData: ByteArray, iv: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)
        val decoded = cipher.doFinal(encryptedData)
        return String(decoded, Charsets.UTF_8)
    }
}
