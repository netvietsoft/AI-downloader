package com.nextaitechnology.antidetect.feature.vault

import com.nextaitechnology.antidetect.core.model.VaultItem
import com.nextaitechnology.antidetect.core.security.AesVaultManager
import java.io.File

/**
 * Điều Phối Két Sắt Bảo Mật (Private Vault Controller)
 * Orchestrates encrypted video import, export, and PIN authentication
 *
 * @author NextAI Technology Security Team
 */
class VaultController(
    private val vaultDirectory: File,
    private val aesVaultManager: AesVaultManager = AesVaultManager()
) {

    private var unlockedPin: String? = null

    init {
        if (!vaultDirectory.exists()) {
            vaultDirectory.mkdirs()
        }
        // Tạo file .nomedia để tránh Android MediaStore quét các file trong két sắt
        val noMedia = File(vaultDirectory, ".nomedia")
        if (!noMedia.exists()) {
            noMedia.createNewFile()
        }
    }

    /**
     * Xác thực mã PIN người dùng (Mặc định demo: 1234)
     */
    fun unlock(pin: String): Boolean {
        // Trong môi trường thực tế, so sánh với PBKDF2 hash trong EncryptedSharedPreferences
        if (pin.length == 4) {
            unlockedPin = pin
            return true
        }
        return false
    }

    fun isUnlocked(): Boolean = unlockedPin != null

    fun lock() {
        unlockedPin = null
    }

    /**
     * Nhập video thường vào Két sắt mã hóa
     */
    fun importVideo(plainFile: File): VaultItem? {
        val pin = unlockedPin ?: return null
        if (!plainFile.exists()) return null

        val vaultFileName = "ENC_${System.currentTimeMillis()}_${plainFile.name}.vault"
        val encryptedDest = File(vaultDirectory, vaultFileName)

        val success = aesVaultManager.encryptFile(plainFile, encryptedDest, pin)
        if (success) {
            // Xóa file nguồn để hoàn tất giấu kín
            plainFile.delete()

            return VaultItem(
                id = vaultFileName,
                originalFileName = plainFile.name,
                encryptedFilePath = encryptedDest.absolutePath,
                fileSize = encryptedDest.length()
            )
        }
        return null
    }

    /**
     * Lấy danh sách các video đang được bảo vệ trong két sắt
     */
    fun listVaultItems(): List<VaultItem> {
        val files = vaultDirectory.listFiles { f -> f.extension == "vault" } ?: emptyArray()
        return files.map { file ->
            VaultItem(
                id = file.name,
                originalFileName = file.name.removePrefix("ENC_").removeSuffix(".vault"),
                encryptedFilePath = file.absolutePath,
                fileSize = file.length()
            )
        }
    }
}
