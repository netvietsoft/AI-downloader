package com.nextaitechnology.antidetect.feature.downloader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nextaitechnology.antidetect.NextAIApplication
import com.nextaitechnology.antidetect.R
import com.nextaitechnology.antidetect.core.model.DownloadStatus
import com.nextaitechnology.antidetect.core.model.DownloadTask
import com.nextaitechnology.antidetect.feature.player.AcePlayerActivity
import kotlinx.coroutines.launch
import java.io.File
import java.text.DecimalFormat
import android.content.Intent
import com.nextaitechnology.antidetect.core.storage.MediaStoreExporter
import com.nextaitechnology.antidetect.feature.settings.PaymentWallActivity
import com.nextaitechnology.antidetect.feature.settings.SettingsRepository

/**
 * Màn hình Quản lý Tiến độ Tải & Danh sách Video hoàn thành (ProgressFragment)
 * Real-time 0%-100% Download Progress Tracking, Pause/Resume & Direct Offline Playback
 *
 * @author NextAI Technology Core Team
 */
class ProgressFragment : Fragment() {

    private lateinit var settingsRepository: SettingsRepository

    private lateinit var tabDownloading: TextView
    private lateinit var tabDownloaded: TextView
    private lateinit var sectionDownloading: View
    private lateinit var sectionDownloaded: View
    private lateinit var containerDownloading: LinearLayout
    private lateinit var containerDownloaded: LinearLayout
    private lateinit var tvEmptyDownloading: TextView
    private lateinit var tvEmptyDownloaded: TextView

    private val downloadEngine: DownloadEngine
        get() = (requireActivity().application as NextAIApplication).downloadEngine

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_progress, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabDownloading = view.findViewById(R.id.tab_downloading)
        tabDownloaded = view.findViewById(R.id.tab_downloaded)
        sectionDownloading = view.findViewById(R.id.section_downloading)
        sectionDownloaded = view.findViewById(R.id.section_downloaded)
        containerDownloading = view.findViewById(R.id.container_downloading)
        containerDownloaded = view.findViewById(R.id.container_downloaded)
        tvEmptyDownloading = view.findViewById(R.id.tv_empty_downloading)
        tvEmptyDownloaded = view.findViewById(R.id.tv_empty_downloaded)

        settingsRepository = SettingsRepository(requireContext())

        setupSubtabs()

        view.findViewById<View>(R.id.card_stability_banner).setOnClickListener {
            showStabilityDialog()
        }

        observeDownloadTasks()
    }

    private fun setupSubtabs() {
        tabDownloading.setOnClickListener {
            tabDownloading.setBackgroundResource(R.drawable.bg_pill_btn)
            tabDownloading.setTextColor(resources.getColor(R.color.text_white, null))

            tabDownloaded.setBackgroundResource(R.drawable.bg_pill_btn_outline)
            tabDownloaded.setTextColor(resources.getColor(R.color.text_muted, null))

            sectionDownloading.visibility = View.VISIBLE
            sectionDownloaded.visibility = View.GONE
        }

        tabDownloaded.setOnClickListener {
            tabDownloaded.setBackgroundResource(R.drawable.bg_pill_btn)
            tabDownloaded.setTextColor(resources.getColor(R.color.text_white, null))

            tabDownloading.setBackgroundResource(R.drawable.bg_pill_btn_outline)
            tabDownloading.setTextColor(resources.getColor(R.color.text_muted, null))

            sectionDownloading.visibility = View.GONE
            sectionDownloaded.visibility = View.VISIBLE
        }
    }

    /**
     * Lắng nghe cập nhật tiến trình thời gian thực từ DownloadEngine
     * Observe live download engine tasks and render active 0%-100% progress
     */
    private fun observeDownloadTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                downloadEngine.tasksFlow.collect { tasks ->
                    renderTasks(tasks)
                }
            }
        }
    }

    private fun renderTasks(tasks: List<DownloadTask>) {
        val downloading = tasks.filter {
            it.status == DownloadStatus.DOWNLOADING ||
            it.status == DownloadStatus.PENDING ||
            it.status == DownloadStatus.PAUSED ||
            it.status == DownloadStatus.FAILED
        }
        val downloaded = tasks.filter {
            it.status == DownloadStatus.COMPLETED
        }

        tabDownloading.text = "${getString(R.string.tab_downloading)} (${downloading.size})"
        tabDownloaded.text = "${getString(R.string.tab_downloaded)} (${downloaded.size})"

        // Render Downloading Section
        if (downloading.isEmpty()) {
            tvEmptyDownloading.visibility = View.VISIBLE
            containerDownloading.removeAllViews()
        } else {
            tvEmptyDownloading.visibility = View.GONE
            renderDownloadingTasks(downloading)
        }

        // Render Downloaded Section
        if (downloaded.isEmpty()) {
            tvEmptyDownloaded.visibility = View.VISIBLE
            containerDownloaded.removeAllViews()
        } else {
            tvEmptyDownloaded.visibility = View.GONE
            renderDownloadedTasks(downloaded)
        }
    }

    /**
     * Hiển thị danh sách tiến trình tải 0% đến 100%
     */
    private fun renderDownloadingTasks(tasks: List<DownloadTask>) {
        containerDownloading.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        for (task in tasks) {
            val itemView = inflater.inflate(R.layout.item_task_downloading, containerDownloading, false)

            val tvTitle = itemView.findViewById<TextView>(R.id.tv_task_title)
            val tvStats = itemView.findViewById<TextView>(R.id.tv_task_stats)
            val tvPercent = itemView.findViewById<TextView>(R.id.tv_task_percent)
            val pbProgress = itemView.findViewById<ProgressBar>(R.id.pb_task_progress)
            val btnPause = itemView.findViewById<TextView>(R.id.btn_task_pause)
            val btnMore = itemView.findViewById<TextView>(R.id.btn_task_more)

            tvTitle.text = task.title

            val percent = task.progressPercent
            tvPercent.text = "$percent%"
            pbProgress.progress = percent

            when (task.status) {
                DownloadStatus.DOWNLOADING -> {
                    val speed = formatSpeed(task.speedBytesPerSec)
                    val size = "${formatSize(task.downloadedBytes)} / ${formatSize(task.totalBytes)}"
                    tvStats.text = "$size • $speed"
                    btnPause.text = "⏸"
                    btnPause.setOnClickListener {
                        downloadEngine.pauseTask(task.id)
                        Toast.makeText(context, getString(R.string.toast_paused_task, task.title), Toast.LENGTH_SHORT).show()
                    }
                }
                DownloadStatus.PAUSED -> {
                    val size = "${formatSize(task.downloadedBytes)} / ${formatSize(task.totalBytes)}"
                    tvStats.text = "${getString(R.string.status_paused)} • $size"
                    btnPause.text = "▶"
                    btnPause.setOnClickListener {
                        downloadEngine.resumeTask(task.id)
                        Toast.makeText(context, getString(R.string.toast_resumed_task, task.title), Toast.LENGTH_SHORT).show()
                    }
                }
                DownloadStatus.FAILED -> {
                    tvStats.text = getString(R.string.status_failed, task.errorMessage ?: "Error")
                    btnPause.text = "🔄"
                    btnPause.setOnClickListener {
                        downloadEngine.resumeTask(task.id)
                    }
                }
                else -> {
                    tvStats.text = getString(R.string.status_connecting)
                    btnPause.text = "⏸"
                }
            }

            btnMore.setOnClickListener { v ->
                showDownloadingMenu(v, task)
            }

            // Bấm vào card có thể xem trước luồng video
            itemView.setOnClickListener {
                if (task.downloadUrl.isNotEmpty()) {
                    AcePlayerActivity.start(requireContext(), task.downloadUrl, task.title, task.referer)
                }
            }

            containerDownloading.addView(itemView)
        }
    }

    /**
     * Hiển thị danh sách tệp video đã tải hoàn tất
     */
    private fun renderDownloadedTasks(tasks: List<DownloadTask>) {
        containerDownloaded.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        for (task in tasks) {
            val itemView = inflater.inflate(R.layout.item_task_downloaded, containerDownloaded, false)

            val tvTitle = itemView.findViewById<TextView>(R.id.tv_downloaded_title)
            val tvInfo = itemView.findViewById<TextView>(R.id.tv_downloaded_info)
            val btnPlay = itemView.findViewById<TextView>(R.id.btn_play_downloaded)
            val iconPlay = itemView.findViewById<TextView>(R.id.icon_play_downloaded)
            val btnMore = itemView.findViewById<TextView>(R.id.btn_downloaded_more)

            tvTitle.text = task.title
            val fileSize = if (task.totalBytes > 0) task.totalBytes else File(task.targetFilePath).length()
            tvInfo.text = "${task.qualityLabel} • ${formatSize(fileSize)} • " + getString(R.string.status_completed_100)

            val playAction = View.OnClickListener {
                val file = File(task.targetFilePath)
                if (file.exists() && file.length() > 0) {
                    AcePlayerActivity.start(requireContext(), file.absolutePath, task.title)
                } else if (task.downloadUrl.isNotEmpty()) {
                    AcePlayerActivity.start(requireContext(), task.downloadUrl, task.title, task.referer)
                } else {
                    Toast.makeText(context, getString(R.string.toast_file_not_found), Toast.LENGTH_SHORT).show()
                }
            }

            itemView.setOnClickListener(playAction)
            btnPlay.setOnClickListener(playAction)
            iconPlay.setOnClickListener(playAction)

            btnMore.setOnClickListener { v ->
                showDownloadedMenu(v, task)
            }

            containerDownloaded.addView(itemView)
        }
    }

    private fun showDownloadingMenu(anchor: View, task: DownloadTask) {
        val ctx = context ?: return
        val popup = PopupMenu(ctx, anchor)
        popup.menu.add(0, 1, 0, "🗑️ " + getString(R.string.menu_delete_permanently))
        popup.setOnMenuItemClickListener { item ->
            if (item.itemId == 1) {
                downloadEngine.deleteTask(task.id, deletePhysicalFile = true)
                Toast.makeText(ctx, getString(R.string.toast_removed_from_list, task.title), Toast.LENGTH_SHORT).show()
            }
            true
        }
        popup.show()
    }

    private fun showDownloadedMenu(anchor: View, task: DownloadTask) {
        val ctx = context ?: return
        val popup = PopupMenu(ctx, anchor)
        popup.menu.add(0, 3, 0, "📥 " + getString(R.string.menu_download_to_gallery))
        popup.menu.add(0, 1, 0, "📋 " + getString(R.string.menu_delete_from_list))
        popup.menu.add(0, 2, 0, "🗑️ " + getString(R.string.menu_delete_permanently))
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                3 -> {
                    handleDownloadToGallery(task)
                }
                1 -> {
                    downloadEngine.deleteTask(task.id, deletePhysicalFile = false)
                    Toast.makeText(ctx, getString(R.string.toast_removed_from_list, task.title), Toast.LENGTH_SHORT).show()
                }
                2 -> {
                    AlertDialog.Builder(ctx)
                        .setTitle(getString(R.string.dialog_delete_title))
                        .setMessage(getString(R.string.dialog_delete_confirm) + "\n\n${task.title}")
                        .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                            downloadEngine.deleteTask(task.id, deletePhysicalFile = true)
                            Toast.makeText(ctx, getString(R.string.toast_file_deleted, task.title), Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton(getString(R.string.btn_cancel), null)
                        .show()
                }
            }
            true
        }
        popup.show()
    }

    private fun handleDownloadToGallery(task: DownloadTask) {
        val ctx = context ?: return
        val sourceFile = File(task.targetFilePath)
        if (!sourceFile.exists() || sourceFile.length() <= 50 * 1024L) {
            Toast.makeText(ctx, getString(R.string.toast_file_not_found), Toast.LENGTH_SHORT).show()
            return
        }

        if (!settingsRepository.canDownloadToGallery()) {
            Toast.makeText(ctx, getString(R.string.toast_gallery_limit_reached), Toast.LENGTH_LONG).show()
            val intent = Intent(ctx, PaymentWallActivity::class.java).apply {
                putExtra("EXTRA_REASON", "gallery_limit_reached")
            }
            startActivity(intent)
            return
        }

        val destUri = MediaStoreExporter.exportVideoToGallery(ctx, sourceFile, task.title)
        if (destUri != null) {
            if (!settingsRepository.isProUser) {
                settingsRepository.incrementGalleryDownloadCount()
                val remaining = settingsRepository.getRemainingFreeGalleryDownloads()
                Toast.makeText(
                    ctx,
                    getString(R.string.toast_gallery_saved_quota, remaining),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    ctx,
                    getString(R.string.toast_gallery_saved_vip),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(ctx, getString(R.string.toast_gallery_save_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showStabilityDialog() {
        context?.let { ctx ->
            AlertDialog.Builder(ctx)
                .setTitle(getString(R.string.dialog_stability_title))
                .setMessage(getString(R.string.dialog_stability_msg))
                .setPositiveButton(getString(R.string.btn_ok), null)
                .show()
        }
    }

    companion object {
        fun formatSize(bytes: Long): String {
            if (bytes <= 0) return "0 MB"
            val df = DecimalFormat("#.##")
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> "${df.format(gb)} GB"
                mb >= 1.0 -> "${df.format(mb)} MB"
                kb >= 1.0 -> "${df.format(kb)} KB"
                else -> "$bytes B"
            }
        }

        fun formatSpeed(bytesPerSec: Long): String {
            if (bytesPerSec <= 0) return "0 KB/s"
            val df = DecimalFormat("#.#")
            val kb = bytesPerSec / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1.0 -> "${df.format(mb)} MB/s"
                else -> "${df.format(kb)} KB/s"
            }
        }
    }
}
