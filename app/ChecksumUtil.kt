package com.xianfeng.wjcscs

import java.io.File
import java.security.MessageDigest

object ChecksumUtil {
    fun calculateChecksum(file: File): String {
        val buffer = ByteArray(1024)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                messageDigest.update(buffer, 0, bytesRead)
            }
        }
        return messageDigest.digest().joinToString("") { "%02x".format(it) }
    }

    fun verifyChecksum(file: File, checksum: String): Boolean {
        return calculateChecksum(file) == checksum
    }
}