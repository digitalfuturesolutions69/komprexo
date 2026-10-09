package com.komprexo.app.billing

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** AES-GCM authenticated AndroidKeyStore cache; fail closed on tampering/key loss.
 * Backup is disabled. Only timestamp/expiry and token digest are retained for 24h. */
class SealedOwnershipStore(context: Context, fileName: String = "billing-ownership.bin") : OwnershipStore {
    private val file = AtomicFile(File(context.applicationContext.filesDir, fileName))
    private val alias = "komprexo.billing.ownership.v1"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    override suspend fun read(): OwnershipCache? = withContext(Dispatchers.IO) {
        if (!file.baseFile.exists()) return@withContext null
        try {
            check(file.baseFile.length() in 29..2048)
            val bytes = file.readFully()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            cipher.updateAAD("com.komprexo.app:ownership:v1".toByteArray())
            val json = JSONObject(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8))
            OwnershipCache(json.getLong("checked"), json.getLong("expires"), json.getString("digest"))
        } catch (_: Exception) { file.delete(); null }
    }
    override suspend fun write(cache: OwnershipCache?) = withContext(Dispatchers.IO) {
        if (cache == null) { file.delete(); return@withContext }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD("com.komprexo.app:ownership:v1".toByteArray())
        val json = JSONObject().put("checked", cache.checkedAt).put("expires", cache.expiresAt).put("digest", cache.tokenHash).toString()
        val bytes = cipher.iv + cipher.doFinal(json.toByteArray(Charsets.UTF_8))
        val stream = file.startWrite()
        try { stream.write(bytes); file.finishWrite(stream) } catch (e: Exception) { file.failWrite(stream); throw e }
    }
}
