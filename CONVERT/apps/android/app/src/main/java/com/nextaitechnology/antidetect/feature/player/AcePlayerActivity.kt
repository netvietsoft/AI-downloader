package com.nextaitechnology.antidetect.feature.player

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.nextaitechnology.antidetect.R
import com.nextaitechnology.antidetect.core.i18n.LocaleHelper
import com.nextaitechnology.antidetect.core.network.SnifferEngine
import com.nextaitechnology.antidetect.core.analytics.FirebaseAnalyticsManager
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Trình phát Video Chuyên Nghiệp AcePlayer (AcePlayerActivity)
 * Hardware-accelerated full-screen video player with Media3 ExoPlayer, Dynamic Referer bypass & 200% Audio Boost
 *
 * @author NextAI Technology Core Team
 */
class AcePlayerActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    private lateinit var playerView: PlayerView
    private lateinit var tvTitle: TextView
    private lateinit var btnBack: TextView
    private lateinit var btnAudioBoost: TextView
    private lateinit var pbBuffering: ProgressBar

    private var exoPlayer: ExoPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var isBoostActive = false
    private var hasRetriedFallback = false
    private var currentStreamUrl: String = ""

    companion object {
        const val EXTRA_VIDEO_URL = "EXTRA_VIDEO_URL"
        const val EXTRA_VIDEO_TITLE = "EXTRA_VIDEO_TITLE"
        const val EXTRA_REFERER = "EXTRA_REFERER"

        fun start(context: Context, videoUrl: String, title: String, referer: String? = null) {
            val intent = Intent(context, AcePlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_URL, videoUrl)
                putExtra(EXTRA_VIDEO_TITLE, title)
                if (!referer.isNullOrEmpty()) {
                    putExtra(EXTRA_REFERER, referer)
                }
            }
            context.startActivity(intent)
        }

        /**
         * Tự động xác định Referer phù hợp theo tên miền video để vượt qua cơ chế chống hotlink
         * Dynamically resolves anti-hotlink Referer header based on video hosting domain
         */
        fun getRefererForUrl(url: String, explicitReferer: String?): String? {
            if (!explicitReferer.isNullOrEmpty()) return explicitReferer
            val lower = url.lowercase()
            return when {
                lower.contains("fbcdn.net") || lower.contains("facebook.com") -> "https://www.facebook.com/"
                lower.contains("tiktokcdn.com") || lower.contains("tiktok.com") || lower.contains("byteoversea") -> "https://www.tiktok.com/"
                lower.contains("instagram.com") || lower.contains("cdninstagram") -> "https://www.instagram.com/"
                lower.contains("twimg.com") || lower.contains("twitter.com") || lower.contains("x.com") -> "https://twitter.com/"
                lower.contains("googlevideo.com") || lower.contains("youtube.com") -> "https://www.youtube.com/"
                lower.contains("vimeo.com") -> "https://vimeo.com/"
                lower.contains("dailymotion.com") -> "https://www.dailymotion.com/"
                lower.contains("xhamster") || lower.contains("xhcdn") || lower.contains("cdnsolutions.media") || lower.contains("ahcdn") -> "https://xhamster.com/"
                lower.contains("xvideos") || lower.contains("xvcdn") -> "https://www.xvideos.com/"
                lower.contains("pornhub") || lower.contains("phncdn") -> "https://www.pornhub.com/"
                lower.contains("redtube") -> "https://www.redtube.com/"
                else -> null
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ace_player)

        // Giữ màn hình luôn sáng khi phát video (Keep Screen On)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Bật chế độ toàn màn hình tràn viền (Immersive Fullscreen)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        playerView = findViewById(R.id.player_view)
        tvTitle = findViewById(R.id.tv_player_title)
        btnBack = findViewById(R.id.btn_player_back)
        btnAudioBoost = findViewById(R.id.btn_audio_boost)
        pbBuffering = findViewById(R.id.pb_buffering)

        val rawVideoUrl = intent.getStringExtra(EXTRA_VIDEO_URL) ?: ""
        val title = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: "AI Video"
        val explicitReferer = intent.getStringExtra(EXTRA_REFERER)

        tvTitle.text = title
        FirebaseAnalyticsManager.logViewPlayer(title, rawVideoUrl)

        btnBack.setOnClickListener {
            finish()
        }

        btnAudioBoost.setOnClickListener {
            toggleAudioBoost()
        }

        if (rawVideoUrl.isNotEmpty()) {
            val cleanUrl = SnifferEngine.sanitizeMediaUrl(rawVideoUrl)
            val localFile = File(cleanUrl)
            if (localFile.exists() && localFile.isFile) {
                if (localFile.length() < 2048L) {
                    val sample = try { localFile.readText().take(50) } catch (e: Exception) { "" }
                    if (sample.startsWith("#EXTM3U") || sample.startsWith("<html", ignoreCase = true)) {
                        Toast.makeText(this, getString(R.string.error_video_corrupted), Toast.LENGTH_LONG).show()
                        finish()
                        return
                    }
                }
            }

            currentStreamUrl = cleanUrl
            Log.i("AcePlayer", "Khởi tạo AcePlayer với URL: $cleanUrl")
            setupPlayer(cleanUrl, explicitReferer)
        } else {
            Toast.makeText(this, getString(R.string.toast_live_stream_not_found), Toast.LENGTH_LONG).show()
            finish()
        }
    }

    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val rawVideoUrl = intent.getStringExtra(EXTRA_VIDEO_URL) ?: ""
        val title = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: "AI Video"
        val explicitReferer = intent.getStringExtra(EXTRA_REFERER)

        tvTitle.text = title
        if (rawVideoUrl.isNotEmpty()) {
            val cleanUrl = SnifferEngine.sanitizeMediaUrl(rawVideoUrl)
            currentStreamUrl = cleanUrl
            exoPlayer?.stop()
            exoPlayer?.release()
            exoPlayer = null
            hasRetriedFallback = false
            setupPlayer(cleanUrl, explicitReferer)
        }
    }

    private fun setupPlayer(url: String, explicitReferer: String? = null) {
        pbBuffering.visibility = View.VISIBLE
        // Chuyển đổi luồng AV1 sang H.264 để tận dụng 100% phần cứng giải mã (Hardware Decoder) trên các dòng chip MediaTek / Exynos
        // Transform AV1 stream to H.264 for guaranteed universal hardware-accelerated decoding across all Android devices
        val resolvedUrl = if (url.contains(".av1.mp4.m3u8")) {
            url.replace(".av1.mp4.m3u8", ".h264.mp4.m3u8")
        } else if (url.contains(".av1.")) {
            url.replace(".av1.", ".h264.")
        } else {
            url
        }

        val uri = if (resolvedUrl.startsWith("http://") || resolvedUrl.startsWith("https://") || resolvedUrl.startsWith("content://") || resolvedUrl.startsWith("file://")) {
            Uri.parse(resolvedUrl)
        } else {
            Uri.fromFile(File(resolvedUrl))
        }

        val referer = getRefererForUrl(resolvedUrl, explicitReferer)
        Log.i("AcePlayer", "Sử dụng Referer: " + (referer ?: "None") + " cho luồng: " + resolvedUrl.take(70))

        // Chuẩn hóa RFC 6454 Origin (chỉ gồm scheme + host, tuyệt đối không chứa path)
        val validOrigin = referer?.takeIf { it.startsWith("http") }?.let {
            try {
                val refUri = Uri.parse(it)
                "${refUri.scheme}://${refUri.host}"
            } catch (e: Exception) {
                null
            }
        }

        // Tạo OkHttpClient chuyên dụng cho phát trực tuyến Media Streaming
        // Dedicated OkHttpClient configured for media streaming with dynamic anti-hotlinking bypass
        val okHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                    .header("User-Agent", SnifferEngine.DESKTOP_USER_AGENT)
                    .header("Accept", "*/*")

                if (!referer.isNullOrEmpty()) {
                    builder.header("Referer", referer)
                }
                if (!validOrigin.isNullOrEmpty()) {
                    builder.header("Origin", validOrigin)
                }
                val request = builder.build()
                val response = chain.proceed(request)
                Log.d("AcePlayer_HTTP", "${response.code} ${request.method} -> ${request.url}")
                response
            }
            .build()

        val defaultRequestProps = mutableMapOf(
            "User-Agent" to SnifferEngine.DESKTOP_USER_AGENT,
            "Accept" to "*/*"
        )
        if (!referer.isNullOrEmpty()) {
            defaultRequestProps["Referer"] = referer
        }
        if (!validOrigin.isNullOrEmpty()) {
            defaultRequestProps["Origin"] = validOrigin
        }

        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(SnifferEngine.DESKTOP_USER_AGENT)
            .setDefaultRequestProperties(defaultRequestProps)

        // Hỗ trợ tối ưu cả tệp cục bộ (file:// / SDCard) qua FileDataSource và luồng mạng qua OkHttp
        val upstreamDataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(this, httpDataSourceFactory)

        val localFile = File(resolvedUrl)
        val isLocalFile = localFile.exists() && localFile.isFile
        var isTsStream = false
        if (isLocalFile) {
            isTsStream = try {
                localFile.inputStream().use { stream ->
                    val first = stream.read()
                    first == 0x47
                }
            } catch (e: Exception) { false }
        }

        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .apply {
                if (resolvedUrl.contains(".m3u8")) {
                    setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
                } else if (resolvedUrl.contains(".mpd")) {
                    setMimeType(androidx.media3.common.MimeTypes.APPLICATION_MPD)
                } else if (isTsStream) {
                    setMimeType(androidx.media3.common.MimeTypes.VIDEO_MP2T)
                }
            }
            .build()

        val extractorsFactory = androidx.media3.extractor.DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setTsExtractorFlags(
                androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS
            )
            .setMp4ExtractorFlags(androidx.media3.extractor.mp4.Mp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS)

        val mediaSource = if (resolvedUrl.contains(".m3u8")) {
            androidx.media3.exoplayer.hls.HlsMediaSource.Factory(upstreamDataSourceFactory)
                .setAllowChunklessPreparation(false)
                .createMediaSource(mediaItem)
        } else {
            DefaultMediaSourceFactory(this, extractorsFactory)
                .setDataSourceFactory(upstreamDataSourceFactory)
                .createMediaSource(mediaItem)
        }

        val renderersFactory = DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true)

        val player = ExoPlayer.Builder(this, renderersFactory)
            .build()
            .apply {
                setMediaSource(mediaSource)
                prepare()
                playWhenReady = true

                addListener(object : Player.Listener {
                    override fun onRenderedFirstFrame() {
                        pbBuffering.visibility = View.GONE
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) {
                            pbBuffering.visibility = View.GONE
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> pbBuffering.visibility = View.VISIBLE
                            Player.STATE_READY -> {
                                pbBuffering.visibility = View.GONE
                                initLoudnessEnhancer(audioSessionId)
                            }
                            Player.STATE_ENDED -> pbBuffering.visibility = View.GONE
                            Player.STATE_IDLE -> {}
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        pbBuffering.visibility = View.GONE
                        Log.e("AcePlayer", "Lỗi phát luồng video: " + error.errorCodeName, error)
                        FirebaseAnalyticsManager.logAppException(error, "AcePlayerActivity_onPlayerError")

                        val checkFile = File(currentStreamUrl)
                        if (checkFile.exists() && checkFile.isFile) {
                            val resLabel = if (currentStreamUrl.contains("4k", ignoreCase = true) || error.message?.contains("3840") == true) "4K UHD" else "High Resolution"
                            AlertDialog.Builder(this@AcePlayerActivity)
                                .setTitle("AcePlayer")
                                .setMessage(getString(R.string.error_decoder_hardware_limit, resLabel))
                                .setPositiveButton(getString(R.string.btn_open_external)) { _, _ ->
                                    openWithExternalPlayer(checkFile)
                                }
                                .setNegativeButton(getString(R.string.btn_close)) { _, _ ->
                                    finish()
                                }
                                .setCancelable(false)
                                .show()
                            return
                        }

                        // Tự động thử lại với DefaultHttpDataSource tiêu chuẩn nếu gặp lỗi trên luồng mạng
                        if (!hasRetriedFallback && currentStreamUrl.startsWith("http")) {
                            hasRetriedFallback = true
                            Log.w("AcePlayer", "Đang thử lại phát với Clean HttpDataSource Fallback...")
                            runOnUiThread {
                                retryWithStandardFallback(currentStreamUrl)
                            }
                        } else {
                            val msg = error.message ?: error.errorCodeName
                            Toast.makeText(this@AcePlayerActivity, getString(R.string.error_playback, msg), Toast.LENGTH_SHORT).show()
                        }
                    }
                })
            }

        exoPlayer = player
        playerView.player = player
    }

    private fun openWithExternalPlayer(file: File) {
        try {
            val contentUri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.btn_open_external)))
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_stream_cannot_load), Toast.LENGTH_SHORT).show()
        }
    }

    private fun retryWithStandardFallback(url: String) {
        try {
            exoPlayer?.release()

            val cleanHttpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(SnifferEngine.DESKTOP_USER_AGENT)
                .setConnectTimeoutMs(25000)
                .setReadTimeoutMs(35000)
                .setAllowCrossProtocolRedirects(true)

            val cleanSourceFactory = DefaultMediaSourceFactory(this)
                .setDataSourceFactory(androidx.media3.datasource.DefaultDataSource.Factory(this, cleanHttpFactory))

            val renderersFactory = DefaultRenderersFactory(this)
                .setEnableDecoderFallback(true)

            val fallbackPlayer = ExoPlayer.Builder(this, renderersFactory)
                .setMediaSourceFactory(cleanSourceFactory)
                .build()
                .apply {
                    setMediaItem(MediaItem.fromUri(Uri.parse(url)))
                    prepare()
                    playWhenReady = true
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_READY) {
                                pbBuffering.visibility = View.GONE
                                initLoudnessEnhancer(audioSessionId)
                            }
                        }
                        override fun onPlayerError(error: PlaybackException) {
                            pbBuffering.visibility = View.GONE
                            Toast.makeText(this@AcePlayerActivity, getString(R.string.error_stream_cannot_load), Toast.LENGTH_LONG).show()
                        }
                    })
                }

            exoPlayer = fallbackPlayer
            playerView.player = fallbackPlayer
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initLoudnessEnhancer(audioSessionId: Int) {
        try {
            if (loudnessEnhancer == null && audioSessionId != 0) {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                    enabled = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleAudioBoost() {
        isBoostActive = !isBoostActive
        if (isBoostActive) {
            loudnessEnhancer?.setTargetGain(2000) // 20 dB (200% Boost)
            btnAudioBoost.text = "🔥 200% ON"
            btnAudioBoost.setTextColor(resources.getColor(R.color.accent_pink, null))
            Toast.makeText(this, getString(R.string.toast_audio_boost_on), Toast.LENGTH_SHORT).show()
        } else {
            loudnessEnhancer?.setTargetGain(0)
            btnAudioBoost.text = "🔊 200% Boost"
            btnAudioBoost.setTextColor(resources.getColor(R.color.primary, null))
            Toast.makeText(this, getString(R.string.toast_audio_boost_off), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        exoPlayer?.release()
        exoPlayer = null
    }
}
