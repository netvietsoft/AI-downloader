package com.nextaitechnology.antidetect.core.model

import java.io.Serializable

/**
 * Định nghĩa cấu trúc chất lượng video bóc tách
 * Video Quality specification parsed from stream
 *
 * @author NextAI Technology Core Team
 */
data class VideoQuality(
    val label: String,       // 1080p FHD, 720p HD, 480p SD, MP3 Audio
    val format: String,      // mp4, m3u8, mp3
    val approximateSize: Long, // Bytes
    val downloadUrl: String,
    val isAudioOnly: Boolean = false
) : Serializable

/**
 * Thực thể thông tin video trích xuất được từ trang web
 * Video Information entity extracted by Sniffer
 */
data class VideoInfo(
    val title: String,
    val sourcePageUrl: String,
    val thumbnailUrl: String,
    val durationSeconds: Int = 0,
    val qualities: List<VideoQuality> = emptyList(),
    val extractedHeaders: Map<String, String> = emptyMap(),
    val isStreamM3u8: Boolean = false
) : Serializable

/**
 * Trạng thái tiến trình tải video
 * Download Task Execution Status
 */
enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Nhiệm vụ tải xuống trong hàng đợi
 * Download Task representation in queue
 */
data class DownloadTask(
    val id: String,
    val title: String,
    val downloadUrl: String,
    val targetFilePath: String,
    val qualityLabel: String,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val errorMessage: String? = null,
    val referer: String? = null
) {
    val progressPercent: Int
        get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
}

/**
 * Mô hình thẻ duyệt web đa nhiệm
 * Multi-Tab Model for Browser
 */
data class TabModel(
    val id: String,
    val title: String,
    val currentUrl: String,
    val faviconUrl: String? = null,
    val isIncognito: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val lastAccessed: Long = System.currentTimeMillis()
)

/**
 * Mô hình tệp tin trong Két sắt bảo mật
 * Encrypted Vault Media Item
 */
data class VaultItem(
    val id: String,
    val originalFileName: String,
    val encryptedFilePath: String,
    val fileSize: Long,
    val durationMs: Long = 0L,
    val encryptedAt: Long = System.currentTimeMillis(),
    val mimeType: String = "video/mp4"
)
