package com.nextaitechnology.antidetect.core.security

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Trình Quản Lý Mã Hóa Két Sắt Bảo Mật AES-256-GCM Nâng Cao
 * Hardened Enterprise AES-256-GCM Cryptographic Vault Engine with Dynamic Salt & Integrity Tag
 *
 * @author NextAI Technology Security Lead
 */
class AesVaultManager {

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_DERIVATION_ALGO = "PBKDF2WithHmacSHA256"
        private const val ITERATIONS = 10000
        private const val KEY_LENGTH_BITS = 256
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val SALT_LENGTH_BYTES = 16
        private const val BUFFER_SIZE = 64 * 1024 // 64 KB chunk buffer

        // Salt mặc định dự phòng tương thích ngược (Legacy fallback)
        private val SYSTEM_SALT_FALLBACK = byteArrayOf(
            0x4E.toByte(), 0x65.toByte(), 0x78.toByte(), 0x74.toByte(),
            0x41.toByte(), 0x49.toByte(), 0x54.toByte(), 0x65.toByte(),
            0x63.toByte(), 0x68.toByte(), 0x56.toByte(), 0x61.toByte(),
            0x75.toByte(), 0x6C.toByte(), 0x74.toByte(), 0x31.toByte()
        )
    }

    /**
     * Sinh SecretKey từ mã PIN người dùng và Salt động
     * Derive 256-bit AES key from user PIN and random salt
     */
    private fun deriveKey(pin: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGO)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Mã hóa video sang file bảo mật trong két sắt với Salt ngẫu nhiên và IV 12 bytes
     * Encrypt a plain video file into a secure .vault container
     * Cấu trúc file: [16 Bytes SALT] + [12 Bytes IV] + [Mã hóa dữ liệu theo khối 64KB + Tag GCM 16 Bytes]
     *
     * @param sourceFile File video gốc
     * @param destVaultFile File đích trong két sắt bảo mật
     * @param pin Mã PIN 4 số của người dùng
     */
    fun encryptFile(sourceFile: File, destVaultFile: File, pin: String): Boolean {
        return try {
            // Sinh Salt 16 bytes ngẫu nhiên cho mỗi file để chống Rainbow Table / Dictionary Attack
            val salt = ByteArray(SALT_LENGTH_BYTES)
            SecureRandom().nextBytes(salt)

            val key = deriveKey(pin, salt)
            val iv = ByteArray(GCM_IV_LENGTH_BYTES)
            SecureRandom().nextBytes(iv)

            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

            FileInputStream(sourceFile).use { fis ->
                FileOutputStream(destVaultFile).use { fos ->
                    // 1. Ghi Salt 16 bytes vào đầu file
                    fos.write(salt)

                    // 2. Ghi IV 12 bytes vào tiếp theo
                    fos.write(iv)

                    // 3. Mã hóa dữ liệu theo khối buffer 64KB
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        val output = cipher.update(buffer, 0, bytesRead)
                        if (output != null) {
                            fos.write(output)
                        }
                    }
                    val finalBytes = cipher.doFinal()
                    if (finalBytes != null) {
                        fos.write(finalBytes)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            destVaultFile.delete()
            false
        }
    }

    /**
     * Giải mã video từ két sắt với cơ chế xác thực toàn vẹn tự động
     * Decrypt an encrypted vault file back to a playable video file
     */
    fun decryptFile(vaultFile: File, destPlainFile: File, pin: String): Boolean {
        return try {
            FileInputStream(vaultFile).use { fis ->
                // Đọc 16 bytes Salt
                val salt = ByteArray(SALT_LENGTH_BYTES)
                val saltRead = fis.read(salt)
                if (saltRead != SALT_LENGTH_BYTES) return false

                // Đọc 12 bytes IV
                val iv = ByteArray(GCM_IV_LENGTH_BYTES)
                val ivRead = fis.read(iv)
                if (ivRead != GCM_IV_LENGTH_BYTES) return false

                val key = deriveKey(pin, salt)
                val cipher = Cipher.getInstance(ALGORITHM)
                val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
                cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

                FileOutputStream(destPlainFile).use { fos ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        val output = cipher.update(buffer, 0, bytesRead)
                        if (output != null) {
                            fos.write(output)
                        }
                    }
                    val finalBytes = cipher.doFinal()
                    if (finalBytes != null) {
                        fos.write(finalBytes)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            destPlainFile.delete()
            false
        }
    }
}
