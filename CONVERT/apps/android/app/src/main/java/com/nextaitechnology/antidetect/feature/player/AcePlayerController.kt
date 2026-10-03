package com.nextaitechnology.antidetect.feature.player

import android.content.Context
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Trình Điều Khiển Video Chuyên Nghiệp AcePlayer
 * Professional Media3 ExoPlayer wrapper with 200% Audio Boost & Equalizer
 *
 * @author NextAI Technology Core Team
 */
class AcePlayerController(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null

    var isBoostEnabled: Boolean = false
        private set

    /**
     * Khởi tạo ExoPlayer với MediaItem
     */
    fun initialize(videoUri: Uri, onPlaybackStateChanged: ((Int) -> Unit)? = null): ExoPlayer {
        val player = ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUri)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    onPlaybackStateChanged?.invoke(playbackState)
                }
            })
        }

        exoPlayer = player
        setupAudioEffects(player.audioSessionId)
        return player
    }

    /**
     * Thiết lập hiệu ứng âm thanh Loudness Enhancer & Equalizer
     */
    private fun setupAudioEffects(audioSessionId: Int) {
        try {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                enabled = true
            }
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Kích hoạt chế độ kích âm thanh 200% Audio Boost
     * Boost target gain by up to 2000 mB (20 dB boost)
     */
    fun setAudioBoost(gainPercentage: Int) { // 100 to 200%
        val safePercentage = gainPercentage.coerceIn(100, 200)
        isBoostEnabled = safePercentage > 100
        val targetGainMb = ((safePercentage - 100) * 20) // 0 to 2000 mB
        loudnessEnhancer?.setTargetGain(targetGainMb)
    }

    /**
     * Tua nhanh / tua lại 10 giây
     */
    fun seekRelative(secondsDelta: Long) {
        exoPlayer?.let { player ->
            val target = (player.currentPosition + secondsDelta * 1000).coerceIn(0, player.duration)
            player.seekTo(target)
        }
    }

    fun release() {
        loudnessEnhancer?.release()
        equalizer?.release()
        exoPlayer?.release()
        loudnessEnhancer = null
        equalizer = null
        exoPlayer = null
    }
}
