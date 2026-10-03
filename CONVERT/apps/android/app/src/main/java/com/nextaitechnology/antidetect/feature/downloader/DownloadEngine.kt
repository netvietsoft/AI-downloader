package com.nextaitechnology.antidetect.feature.downloader

import android.content.Context
import android.os.PowerManager
import android.util.Log
import com.nextaitechnology.antidetect.core.model.DownloadStatus
import com.nextaitechnology.antidetect.core.model.DownloadTask
import com.nextaitechnology.antidetect.core.network.SnifferEngine
import com.nextaitechnology.antidetect.feature.player.AcePlayerActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Trình Quản Lý & Tải Xuống Đa Luồng (Multi-Thread Download Engine)
 * High-performance concurrent download engine with OkHttp streaming, dynamic referer & 0%-100% progress tracking,
 * automatic HTTP Range resume, per-segment HLS retry, and WakeLock for uninterrupted long video downloads.
 *
 * @author NextAI Technology Core Team
 */
class DownloadEngine(
    val downloadDir: File,
    val context: Context? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    private val tasksMap = ConcurrentHashMap<String, DownloadTask>()
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private var wakeLock: PowerManager.WakeLock? = null

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _tasksFlow = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasksFlow: StateFlow<List<DownloadTask>> = _tasksFlow.asStateFlow()

    init {
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        scanExistingDownloads()
    }

    @Synchronized
    private fun acquireWakeLock() {
        try {
            if (wakeLock == null && context != null) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NextAI:DownloadWakeLock")?.apply {
                    setReferenceCounted(false)
                    acquire(2 * 60 * 60 * 1000L) // Giữ tối đa 2 giờ để tải các video dung lượng lớn
                }
                Log.d("DownloadEngine", "Đã kích hoạt WakeLock để tải video không bị gián đoạn")
            }
        } catch (e: Exception) {
            Log.w("DownloadEngine", "Không thể kích hoạt WakeLock: ${e.message}")
        }
    }

    @Synchronized
    private fun releaseWakeLockIfIdle() {
        try {
            val hasActive = tasksMap.values.any { it.status == DownloadStatus.DOWNLOADING }
            if (!hasActive && wakeLock?.isHeld == true) {
                wakeLock?.release()
                wakeLock = null
                Log.d("DownloadEngine", "Đã giải phóng WakeLock (hàng đợi tải đã rảnh)")
            }
        } catch (e: Exception) {
            Log.w("DownloadEngine", "Lỗi giải phóng WakeLock: ${e.message}")
        }
    }

    private fun emitUpdate() {
        _tasksFlow.value = tasksMap.values.toList().sortedByDescending { it.createdAt }
    }

    /**
     * Tự động quét các tệp video đã có sẵn trong thư mục tải về khi khởi động
     * Scan existing video files in download folder and register as completed tasks
     */
    fun scanExistingDownloads() {
        if (!downloadDir.exists()) return
        val files = downloadDir.listFiles { f ->
            f.isFile && f.length() > 50 * 1024L && (f.name.endsWith(".mp4", ignoreCase = true) ||
                    f.name.endsWith(".mkv", ignoreCase = true) ||
                    f.name.endsWith(".webm", ignoreCase = true) ||
                    f.name.endsWith(".mp3", ignoreCase = true))
        } ?: return

        for (file in files) {
            val taskId = "SAVED_${file.name.hashCode()}"
            if (!tasksMap.containsKey(taskId)) {
                tasksMap[taskId] = DownloadTask(
                    id = taskId,
                    title = file.nameWithoutExtension,
                    downloadUrl = file.absolutePath,
                    targetFilePath = file.absolutePath,
                    qualityLabel = "HD Video",
                    totalBytes = file.length(),
                    downloadedBytes = file.length(),
                    status = DownloadStatus.COMPLETED,
                    createdAt = file.lastModified(),
                    completedAt = file.lastModified()
                )
            }
        }
        emitUpdate()
    }

    /**
     * Thêm nhiệm vụ tải mới vào hàng đợi
     * Enqueue a new video download task with anti-hotlink Referer support
     */
    fun enqueueDownload(
        title: String,
        url: String,
        qualityLabel: String,
        format: String = "mp4",
        referer: String? = null
    ): DownloadTask {
        val taskId = "TASK_${System.currentTimeMillis()}_${(1000..9999).random()}"
        val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9_.-]"), "_")
            .ifBlank { "AI_Video_${System.currentTimeMillis()}" }
        val fileName = if (sanitizedTitle.endsWith(".$format", ignoreCase = true)) {
            sanitizedTitle
        } else {
            "$sanitizedTitle.$format"
        }
        val targetFile = File(downloadDir, fileName)

        val resolvedReferer = AcePlayerActivity.getRefererForUrl(url, referer)

        val task = DownloadTask(
            id = taskId,
            title = title.ifBlank { "Video $sanitizedTitle" },
            downloadUrl = url,
            targetFilePath = targetFile.absolutePath,
            qualityLabel = qualityLabel,
            status = DownloadStatus.DOWNLOADING,
            referer = resolvedReferer
        )

        tasksMap[taskId] = task
        emitUpdate()

        // Bắt đầu tải trong coroutine
        val job = scope.launch {
            runDownload(task, targetFile, null)
        }
        activeJobs[taskId] = job

        return task
    }

    /**
     * Thực thi tải luồng dữ liệu đa luồng OkHttp với cơ chế Tự Động Thử Lại (Auto-Retry) & Range Resume
     * Execute streaming download with smooth 0%-100% progress updates and connection recovery
     */
    private suspend fun runDownload(
        initialTask: DownloadTask,
        targetFile: File,
        resumeFrom: Long? = null
    ) = withContext(Dispatchers.IO) {
        acquireWakeLock()
        var currentDownloaded = if (resumeFrom != null && resumeFrom > 0 && targetFile.exists()) resumeFrom else (if (targetFile.exists()) targetFile.length() else 0L)
        var totalBytes = initialTask.totalBytes
        val maxRetries = 5
        var attempt = 0
        var isSuccess = false

        try {
            targetFile.parentFile?.let {
                if (!it.exists()) it.mkdirs()
            }

            while (attempt < maxRetries && !isSuccess && coroutineContext.isActive) {
                attempt++
                val isResume = (currentDownloaded > 0 && targetFile.exists())
                val requestBuilder = Request.Builder()
                    .url(initialTask.downloadUrl)
                    .header("User-Agent", SnifferEngine.DESKTOP_USER_AGENT)
                    .header("Accept", "*/*")

                if (!initialTask.referer.isNullOrEmpty()) {
                    requestBuilder.header("Referer", initialTask.referer)
                }

                if (isResume) {
                    requestBuilder.header("Range", "bytes=$currentDownloaded-")
                }

                try {
                    val response = okHttpClient.newCall(requestBuilder.build()).execute()
                    val responseCode = response.code

                    if (responseCode == 416) {
                        response.close()
                        if (targetFile.exists() && targetFile.length() > 50 * 1024L) {
                            isSuccess = true
                            break
                        } else {
                            failTask(initialTask.id, "HTTP 416 Range Not Satisfiable")
                            return@withContext
                        }
                    }

                    if (responseCode !in 200..299) {
                        response.close()
                        if (responseCode in 400..499 && responseCode != 408 && responseCode != 429) {
                            failTask(initialTask.id, "HTTP Error $responseCode (Link may be expired)")
                            return@withContext
                        }
                        if (attempt < maxRetries) {
                            Log.w("DownloadEngine", "Lỗi HTTP $responseCode khi tải task ${initialTask.id}, thử lại lần $attempt sau ${attempt * 2}s")
                            delay(attempt * 2000L)
                            continue
                        } else {
                            failTask(initialTask.id, "HTTP Error: $responseCode")
                            return@withContext
                        }
                    }

                    val body = response.body
                    if (body == null) {
                        response.close()
                        if (attempt < maxRetries) {
                            delay(2000L)
                            continue
                        } else {
                            failTask(initialTask.id, "Empty response body")
                            return@withContext
                        }
                    }

                    val contentType = response.header("Content-Type") ?: ""
                    val isHls = initialTask.downloadUrl.contains(".m3u8") || contentType.contains("mpegurl", ignoreCase = true)

                    if (isHls) {
                        val playlistContent = body.string()
                        response.close()
                        downloadHlsStream(initialTask, targetFile, playlistContent)
                        isSuccess = true
                        return@withContext
                    }

                    val streamLength = body.contentLength()
                    val isAppendMode = (responseCode == 206)
                    if (!isAppendMode && isResume) {
                        currentDownloaded = 0L
                    }

                    if (streamLength > 0) {
                        totalBytes = currentDownloaded + streamLength
                    } else if (totalBytes <= 0) {
                        totalBytes = 25 * 1024 * 1024L
                    }

                    var currentTask = tasksMap[initialTask.id] ?: initialTask
                    currentTask = currentTask.copy(
                        totalBytes = totalBytes,
                        downloadedBytes = currentDownloaded,
                        status = DownloadStatus.DOWNLOADING,
                        errorMessage = null
                    )
                    tasksMap[initialTask.id] = currentTask
                    emitUpdate()

                    body.byteStream().use { input ->
                        FileOutputStream(targetFile, isAppendMode).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var bytesRead: Int
                            var lastUpdateTime = System.currentTimeMillis()
                            var bytesSinceLastUpdate = 0L

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                if (!coroutineContext.isActive) {
                                    return@withContext
                                }

                                output.write(buffer, 0, bytesRead)
                                currentDownloaded += bytesRead
                                bytesSinceLastUpdate += bytesRead

                                val now = System.currentTimeMillis()
                                val timeDiff = now - lastUpdateTime
                                if (timeDiff >= 250) {
                                    val speed = if (timeDiff > 0) (bytesSinceLastUpdate * 1000) / timeDiff else 0L
                                    currentTask = currentTask.copy(
                                        downloadedBytes = currentDownloaded,
                                        speedBytesPerSec = speed,
                                        status = DownloadStatus.DOWNLOADING
                                    )
                                    tasksMap[initialTask.id] = currentTask
                                    emitUpdate()

                                    lastUpdateTime = now
                                    bytesSinceLastUpdate = 0L
                                }
                            }
                            output.flush()
                        }
                    }

                    // Kiểm tra sau khi tải hết luồng
                    if (currentDownloaded < 20 * 1024L) {
                        val sample = try { targetFile.readText().take(50) } catch (e: Exception) { "" }
                        if (sample.startsWith("<html", ignoreCase = true) || sample.startsWith("<!doctype", ignoreCase = true) || sample.startsWith("#EXTM3U")) {
                            targetFile.delete()
                            failTask(initialTask.id, "Invalid video stream (HTML/HLS text response)")
                            return@withContext
                        }
                    }

                    if (targetFile.exists() && targetFile.length() < 2048L) {
                        val sample = try { targetFile.readText().take(100) } catch (e: Exception) { "" }
                        if (sample.contains("sabr", ignoreCase = true) || sample.startsWith("<html", ignoreCase = true) || sample.startsWith("#EXTM3U")) {
                            Log.e("DownloadEngine", "Tệp tải về không hợp lệ: ${targetFile.name}")
                            targetFile.delete()
                            failTask(initialTask.id, "Invalid stream or DRM protected")
                            return@withContext
                        }
                    }

                    // Đạt 100% - Hoàn tất tải tệp an toàn
                    currentTask = currentTask.copy(
                        totalBytes = currentDownloaded,
                        downloadedBytes = currentDownloaded,
                        status = DownloadStatus.COMPLETED,
                        completedAt = System.currentTimeMillis(),
                        speedBytesPerSec = 0L,
                        errorMessage = null
                    )
                    tasksMap[initialTask.id] = currentTask
                    emitUpdate()
                    Log.i("DownloadEngine", "Hoàn tất tải 100%: ${targetFile.name} (${currentDownloaded} bytes)")
                    isSuccess = true

                } catch (e: Exception) {
                    if (!coroutineContext.isActive) return@withContext
                    Log.w("DownloadEngine", "Ngoại lệ khi tải tệp (lần $attempt/$maxRetries): ${e.message}")
                    if (attempt < maxRetries) {
                        if (targetFile.exists()) {
                            currentDownloaded = targetFile.length()
                        }
                        delay(attempt * 2000L)
                    } else {
                        failTask(initialTask.id, e.message ?: "Download exception after $maxRetries retries")
                    }
                }
            }
        } finally {
            activeJobs.remove(initialTask.id)
            releaseWakeLockIfIdle()
        }
    }

    /**
     * Tải và ghép các phân đoạn HLS (.ts / .m4s) thành tệp video hoàn chỉnh với cơ chế Thử lại từng phân đoạn (Per-Segment Retry)
     * Download and stitch HLS segments into a playable offline video file
     */
    private suspend fun downloadHlsStream(
        initialTask: DownloadTask,
        targetFile: File,
        initialPlaylist: String
    ) = withContext(Dispatchers.IO) {
        acquireWakeLock()
        try {
            var mediaPlaylist = initialPlaylist
            var playlistUrl = initialTask.downloadUrl

            // 1. Nếu là Master Playlist (#EXT-X-STREAM-INF), phân tích các biến thể (variants)
            if (initialPlaylist.contains("#EXT-X-STREAM-INF")) {
                data class VariantInfo(
                    val url: String,
                    val bandwidth: Long,
                    val width: Int,
                    val height: Int
                )

                val variants = mutableListOf<VariantInfo>()
                val lines = initialPlaylist.lines()
                var currentBandwidth = 0L
                var currentWidth = 0
                var currentHeight = 0

                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("#EXT-X-STREAM-INF")) {
                        val bwMatch = Regex("""BANDWIDTH=(\d+)""").find(trimmed)
                        currentBandwidth = bwMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L

                        val resMatch = Regex("""RESOLUTION=(\d+)x(\d+)""").find(trimmed)
                        if (resMatch != null) {
                            currentWidth = resMatch.groupValues[1].toIntOrNull() ?: 0
                            currentHeight = resMatch.groupValues[2].toIntOrNull() ?: 0
                        } else {
                            currentWidth = 0
                            currentHeight = 0
                        }
                    } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        variants.add(VariantInfo(trimmed, currentBandwidth, currentWidth, currentHeight))
                        currentBandwidth = 0L
                        currentWidth = 0
                        currentHeight = 0
                    }
                }

                // Chọn variant phù hợp: Tránh tự động chọn 4K/UHD (>1080p) làm crash decoder của máy phổ thông
                val targetQuality = initialTask.qualityLabel.lowercase()
                val targetHeight = when {
                    targetQuality.contains("4k") || targetQuality.contains("2160") -> 2160
                    targetQuality.contains("1440") || targetQuality.contains("2k") -> 1440
                    targetQuality.contains("1080") -> 1080
                    targetQuality.contains("720") -> 720
                    targetQuality.contains("480") -> 480
                    targetQuality.contains("360") -> 360
                    else -> 1080 // Mặc định tối đa 1080p để đảm bảo chip phần cứng tương thích 100%
                }

                val selectedVariant = if (variants.isNotEmpty()) {
                    val withRes = variants.filter { it.height > 0 }
                    if (withRes.isNotEmpty()) {
                        val candidates = withRes.filter { it.height <= targetHeight }
                        if (candidates.isNotEmpty()) {
                            candidates.maxByOrNull { it.height * 10_000_000L + it.bandwidth }
                        } else {
                            withRes.minByOrNull { it.height }
                        }
                    } else {
                        val safeBw = variants.filter { it.bandwidth in 1..6_000_000 }
                        safeBw.maxByOrNull { it.bandwidth } ?: variants.firstOrNull()
                    }
                } else null

                val bestVariantUrl = selectedVariant?.url ?: ""
                if (bestVariantUrl.isNotEmpty()) {
                    playlistUrl = if (bestVariantUrl.startsWith("http")) {
                        bestVariantUrl
                    } else {
                        val base = initialTask.downloadUrl.substringBeforeLast('/')
                        "$base/$bestVariantUrl"
                    }

                    Log.i("DownloadEngine", "Đã chọn variant HLS: ${selectedVariant?.width}x${selectedVariant?.height} (${selectedVariant?.bandwidth} bps) -> $playlistUrl")

                    val reqBuilder = Request.Builder().url(playlistUrl).header("User-Agent", SnifferEngine.DESKTOP_USER_AGENT)
                    if (!initialTask.referer.isNullOrEmpty()) reqBuilder.header("Referer", initialTask.referer)
                    val resp = okHttpClient.newCall(reqBuilder.build()).execute()
                    mediaPlaylist = resp.body?.string() ?: ""
                    resp.close()
                }
            }

            // 2. Kiểm tra nếu có mã hóa DRM
            if (mediaPlaylist.contains("#EXT-X-KEY")) {
                failTask(initialTask.id, "Stream is DRM-protected. Please view online.")
                return@withContext
            }

            // 3. Trích xuất phân đoạn khởi tạo fMP4 (#EXT-X-MAP) và danh sách phân đoạn (Segments)
            var initSegmentUrl: String? = null
            val segments = mutableListOf<String>()

            for (line in mediaPlaylist.lines()) {
                val trimmed = line.trim()
                if (trimmed.startsWith("#EXT-X-MAP:")) {
                    val mapMatch = Regex("""URI=["']?([^"',]+)["']?""").find(trimmed)
                    if (mapMatch != null) {
                        val uri = mapMatch.groupValues[1]
                        initSegmentUrl = if (uri.startsWith("http")) uri else {
                            val base = playlistUrl.substringBeforeLast('/')
                            "$base/$uri"
                        }
                    }
                } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                    val fullUrl = if (trimmed.startsWith("http")) {
                        trimmed
                    } else {
                        val base = playlistUrl.substringBeforeLast('/')
                        "$base/$trimmed"
                    }
                    segments.add(fullUrl)
                }
            }

            if (segments.isEmpty()) {
                failTask(initialTask.id, "No playable video segments found in HLS stream.")
                return@withContext
            }

            // 4. Tải tuần tự init segment (nếu có) và các segments ghi vào tệp đích
            val totalSegments = segments.size
            var totalBytesDownloaded = 0L
            var currentTask = initialTask.copy(
                status = DownloadStatus.DOWNLOADING,
                totalBytes = totalSegments * 500_000L
            )
            tasksMap[initialTask.id] = currentTask
            emitUpdate()

            FileOutputStream(targetFile, false).use { output ->
                // Tải init segment trước nếu là luồng fMP4
                if (!initSegmentUrl.isNullOrEmpty()) {
                    try {
                        val initReq = Request.Builder().url(initSegmentUrl).header("User-Agent", SnifferEngine.DESKTOP_USER_AGENT)
                        if (!initialTask.referer.isNullOrEmpty()) initReq.header("Referer", initialTask.referer)
                        val initResp = okHttpClient.newCall(initReq.build()).execute()
                        if (initResp.isSuccessful && initResp.body != null) {
                            val initBytes = initResp.body!!.bytes()
                            output.write(initBytes)
                            totalBytesDownloaded += initBytes.size
                            Log.i("DownloadEngine", "Đã tải fMP4 Init Segment: ${initBytes.size} bytes từ $initSegmentUrl")
                        }
                        initResp.close()
                    } catch (e: Exception) {
                        Log.w("DownloadEngine", "Không thể tải fMP4 Init Segment: ${e.message}")
                    }
                }

                for ((index, segUrl) in segments.withIndex()) {
                    if (!coroutineContext.isActive) {
                        return@withContext
                    }

                    var segAttempt = 0
                    var segSuccess = false

                    while (segAttempt < 4 && !segSuccess && coroutineContext.isActive) {
                        segAttempt++
                        try {
                            val segReq = Request.Builder()
                                .url(segUrl)
                                .header("User-Agent", SnifferEngine.DESKTOP_USER_AGENT)
                            if (!initialTask.referer.isNullOrEmpty()) segReq.header("Referer", initialTask.referer)

                            val segResp = okHttpClient.newCall(segReq.build()).execute()
                            if (segResp.isSuccessful && segResp.body != null) {
                                val bytes = segResp.body!!.bytes()
                                segResp.close()
                                output.write(bytes)
                                totalBytesDownloaded += bytes.size
                                segSuccess = true

                                val percent = ((index + 1) * 100) / totalSegments
                                currentTask = currentTask.copy(
                                    downloadedBytes = totalBytesDownloaded,
                                    totalBytes = if (index + 1 == totalSegments) totalBytesDownloaded else (totalBytesDownloaded * totalSegments) / (index + 1),
                                    status = DownloadStatus.DOWNLOADING
                                )
                                tasksMap[initialTask.id] = currentTask
                                emitUpdate()
                            } else {
                                segResp.close()
                                if (segAttempt < 4) delay(1000L * segAttempt)
                            }
                        } catch (e: Exception) {
                            if (!coroutineContext.isActive) return@withContext
                            Log.w("DownloadEngine", "Lỗi tải segment $index (lần $segAttempt/4): ${e.message}")
                            if (segAttempt < 4) delay(1000L * segAttempt)
                        }
                    }

                    if (!segSuccess) {
                        Log.w("DownloadEngine", "Bỏ qua segment $index sau 4 lần thử để duy trì video không bị ngắt")
                    }
                }
                output.flush()
            }

            // 5. Kiểm tra tính toàn vẹn của video HLS sau khi ghép
            if (targetFile.exists() && targetFile.length() > 50 * 1024L) {
                currentTask = currentTask.copy(
                    downloadedBytes = targetFile.length(),
                    totalBytes = targetFile.length(),
                    status = DownloadStatus.COMPLETED,
                    completedAt = System.currentTimeMillis(),
                    speedBytesPerSec = 0L,
                    errorMessage = null
                )
                tasksMap[initialTask.id] = currentTask
                emitUpdate()
                Log.i("DownloadEngine", "Hoàn tất tải HLS: ${targetFile.name} (${targetFile.length()} bytes)")
            } else {
                targetFile.delete()
                failTask(initialTask.id, "Downloaded file is corrupted or too small (< 50KB)")
            }

        } catch (e: Exception) {
            if (targetFile.exists() && targetFile.length() > 500 * 1024L) {
                // Đã tải được lượng dữ liệu lớn, giữ lại file thay vì xóa sạch
                Log.w("DownloadEngine", "HLS bị ngắt nhưng giữ lại video đã tải ${targetFile.length()} bytes: ${e.message}")
                val partialTask = tasksMap[initialTask.id]?.copy(
                    status = DownloadStatus.COMPLETED,
                    downloadedBytes = targetFile.length(),
                    totalBytes = targetFile.length(),
                    completedAt = System.currentTimeMillis()
                )
                if (partialTask != null) {
                    tasksMap[initialTask.id] = partialTask
                    emitUpdate()
                }
            } else {
                targetFile.delete()
                failTask(initialTask.id, e.message ?: "HLS download error")
            }
        } finally {
            releaseWakeLockIfIdle()
        }
    }

    private fun failTask(taskId: String, error: String) {
        val existing = tasksMap[taskId] ?: return
        tasksMap[taskId] = existing.copy(
            status = DownloadStatus.FAILED,
            errorMessage = error,
            speedBytesPerSec = 0L
        )
        emitUpdate()
        releaseWakeLockIfIdle()
    }

    /**
     * Tạm dừng tác vụ tải
     */
    fun pauseTask(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        val existing = tasksMap[taskId] ?: return
        tasksMap[taskId] = existing.copy(status = DownloadStatus.PAUSED, speedBytesPerSec = 0L)
        emitUpdate()
        releaseWakeLockIfIdle()
    }

    /**
     * Tiếp tục tải từ vị trí đã dừng (Resume download)
     */
    fun resumeTask(taskId: String) {
        val task = tasksMap[taskId] ?: return
        if (task.status == DownloadStatus.DOWNLOADING) return

        val targetFile = File(task.targetFilePath)
        val resumeFrom = if (targetFile.exists()) targetFile.length() else 0L

        val updatedTask = task.copy(
            status = DownloadStatus.DOWNLOADING,
            downloadedBytes = resumeFrom,
            errorMessage = null
        )
        tasksMap[taskId] = updatedTask
        emitUpdate()

        acquireWakeLock()
        val job = scope.launch {
            runDownload(updatedTask, targetFile, resumeFrom)
        }
        activeJobs[taskId] = job
    }

    /**
     * Xóa tác vụ tải khỏi danh sách và tùy chọn xóa file vật lý
     */
    fun deleteTask(taskId: String, deletePhysicalFile: Boolean = true) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        val existing = tasksMap.remove(taskId)
        if (deletePhysicalFile && existing != null) {
            try {
                val file = File(existing.targetFilePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Log.e("DownloadEngine", "Không thể xóa file: ${existing.targetFilePath}", e)
            }
        }
        emitUpdate()
        releaseWakeLockIfIdle()
    }
}
