package com.xianfeng.wjcscx.utility

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

object EncryptionUtil {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/ECB/PKCS5Padding"

    fun generateKey(): Key {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }

    fun encodeKey(key: Key): String {
        return Base64.encodeToString(key.encoded, Base64.DEFAULT)
    }

    fun decodeKey(encodedKey: String): Key {
        val decodedKey = Base64.decode(encodedKey, Base64.DEFAULT)
        return SecretKeySpec(decodedKey, 0, decodedKey.size, ALGORITHM)
    }

    fun encryptFile(key: Key, inputFile: File, outputFile: File) {
        processFile(Cipher.ENCRYPT_MODE, key, inputFile, outputFile)
    }

    fun decryptFile(key: Key, inputFile: File, outputFile: File) {
        processFile(Cipher.DECRYPT_MODE, key, inputFile, outputFile)
    }

    private fun processFile(cipherMode: Int, key: Key, inputFile: File, outputFile: File) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(cipherMode, key)

        FileInputStream(inputFile).use { fis ->
            FileOutputStream(outputFile).use { fos ->
                CipherOutputStream(fos, cipher).use { cos ->
                    fis.copyTo(cos)
                }
            }
        }
    }
}
