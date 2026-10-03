package com.nextaitechnology.antidetect.feature.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nextaitechnology.antidetect.MainActivity
import com.nextaitechnology.antidetect.NextAIApplication
import com.nextaitechnology.antidetect.R
import com.nextaitechnology.antidetect.core.model.DownloadStatus
import com.nextaitechnology.antidetect.core.model.DownloadTask
import com.nextaitechnology.antidetect.feature.downloader.DownloadEngine
import com.nextaitechnology.antidetect.feature.downloader.ProgressFragment
import kotlinx.coroutines.launch
import java.io.File

import com.google.android.material.bottomsheet.BottomSheetDialog
import com.nextaitechnology.antidetect.feature.settings.SettingsRepository
import android.content.Intent
import com.nextaitechnology.antidetect.core.storage.MediaStoreExporter
import com.nextaitechnology.antidetect.feature.settings.PaymentWallActivity
import com.nextaitechnology.antidetect.core.analytics.FirebaseAnalyticsManager

/**
 * Màn hình Trung Tâm AcePlayer & Thư Viện Video (PlayerFragment)
 * Hardware-Accelerated Local Video Hub & Instant Offline Playback Engine
 *
 * @author NextAI Technology Core Team
 */
class PlayerFragment : Fragment() {

    private lateinit var tvVideoCount: TextView
    private lateinit var containerPlayerDownloads: LinearLayout
    private lateinit var tvEmptyPlayerDownloads: TextView

    private val downloadEngine: DownloadEngine
        get() = (requireActivity().application as NextAIApplication).downloadEngine

    private val settingsRepository: SettingsRepository
        get() = (requireActivity().application as NextAIApplication).settingsRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvVideoCount = view.findViewById(R.id.tv_video_count)
        containerPlayerDownloads = view.findViewById(R.id.container_player_downloads)
        tvEmptyPlayerDownloads = view.findViewById(R.id.tv_empty_player_downloads)

        view.findViewById<View>(R.id.btn_private_vault).setOnClickListener {
            FirebaseAnalyticsManager.logClickPrivateVault()
            showVaultPinDialog()
        }

        // Bấm vào All Videos: Phát video tải về mới nhất
        view.findViewById<View>(R.id.card_all_videos).setOnClickListener {
            playLatestDownloadedVideo()
        }

        // Bấm vào thư mục Downloads: Phát video trong thư mục Downloads
        view.findViewById<View>(R.id.folder_downloads).setOnClickListener {
            playLatestDownloadedVideo()
        }

        // Bấm vào thư mục Camera: Kiểm tra thư mục Camera trên máy
        view.findViewById<View>(R.id.folder_camera).setOnClickListener {
            Toast.makeText(context, getString(R.string.folder_empty_count, getString(R.string.folder_camera)), Toast.LENGTH_SHORT).show()
        }

        // Bấm vào thư mục Movies: Thông báo thư mục Movies
        view.findViewById<View>(R.id.folder_movies).setOnClickListener {
            Toast.makeText(context, getString(R.string.folder_empty_count, getString(R.string.folder_movies)), Toast.LENGTH_SHORT).show()
        }

        observeDownloadedVideos()
    }

    private fun observeDownloadedVideos() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                downloadEngine.tasksFlow.collect { tasks ->
                    renderDownloadedVideos(tasks)
                }
            }
        }
    }

    private fun renderDownloadedVideos(tasks: List<DownloadTask>) {
        val downloaded = tasks.filter {
            it.status == DownloadStatus.COMPLETED &&
            File(it.targetFilePath).exists() &&
            File(it.targetFilePath).length() > 50 * 1024L &&
            !settingsRepository.isTaskInVault(it.id)
        }

        tvVideoCount.text = getString(R.string.videos_found_format, downloaded.size)

        containerPlayerDownloads.removeAllViews()

        if (downloaded.isEmpty()) {
            tvEmptyPlayerDownloads.visibility = View.VISIBLE
        } else {
            tvEmptyPlayerDownloads.visibility = View.GONE
            val inflater = LayoutInflater.from(requireContext())

            for (task in downloaded) {
                val itemView = inflater.inflate(R.layout.item_task_downloaded, containerPlayerDownloads, false)

                val tvTitle = itemView.findViewById<TextView>(R.id.tv_downloaded_title)
                val tvInfo = itemView.findViewById<TextView>(R.id.tv_downloaded_info)
                val btnPlay = itemView.findViewById<TextView>(R.id.btn_play_downloaded)
                val iconPlay = itemView.findViewById<TextView>(R.id.icon_play_downloaded)
                val btnMore = itemView.findViewById<TextView>(R.id.btn_downloaded_more)

                tvTitle.text = task.title
                val file = File(task.targetFilePath)
                val sizeStr = ProgressFragment.formatSize(if (file.exists()) file.length() else task.totalBytes)
                tvInfo.text = "${task.qualityLabel} • $sizeStr • " + getString(R.string.status_ready_to_play)

                val playAction = View.OnClickListener {
                    if (file.exists() && file.length() > 50 * 1024L) {
                        FirebaseAnalyticsManager.logClickPlayVideo(task.title, file.absolutePath)
                        AcePlayerActivity.start(requireContext(), file.absolutePath, task.title)
                    } else if (task.downloadUrl.isNotEmpty() && task.downloadUrl.startsWith("http")) {
                        FirebaseAnalyticsManager.logClickPlayVideo(task.title, task.downloadUrl)
                        AcePlayerActivity.start(requireContext(), task.downloadUrl, task.title, task.referer)
                    } else {
                        Toast.makeText(context, getString(R.string.toast_file_not_found), Toast.LENGTH_SHORT).show()
                    }
                }

                itemView.setOnClickListener(playAction)
                btnPlay.setOnClickListener(playAction)
                iconPlay.setOnClickListener(playAction)

                btnMore.setOnClickListener { v ->
                    showItemMenu(v, task)
                }

                containerPlayerDownloads.addView(itemView)
            }
        }
    }

    private fun playLatestDownloadedVideo() {
        val downloaded = downloadEngine.tasksFlow.value.filter {
            it.status == DownloadStatus.COMPLETED &&
            File(it.targetFilePath).exists() &&
            File(it.targetFilePath).length() > 50 * 1024L &&
            !settingsRepository.isTaskInVault(it.id)
        }

        if (downloaded.isNotEmpty()) {
            val latest = downloaded.first()
            Toast.makeText(context, "${getString(R.string.status_ready_to_play)}: ${latest.title}", Toast.LENGTH_SHORT).show()
            FirebaseAnalyticsManager.logClickPlayVideo(latest.title, latest.targetFilePath)
            AcePlayerActivity.start(requireContext(), latest.targetFilePath, latest.title)
        } else {
            val mainAct = activity as? MainActivity
            val detectedUrl = mainAct?.detectedVideoUrl
            if (!detectedUrl.isNullOrEmpty()) {
                val detectedTitle = mainAct.detectedVideoTitle ?: "Video"
                FirebaseAnalyticsManager.logClickPlayVideo(detectedTitle, detectedUrl)
                AcePlayerActivity.start(requireContext(), detectedUrl, detectedTitle)
            } else {
                showEmptyDownloadPrompt()
            }
        }
    }

    private fun showEmptyDownloadPrompt() {
        context?.let { ctx ->
            AlertDialog.Builder(ctx)
                .setTitle(getString(R.string.dialog_empty_downloads_title))
                .setMessage(getString(R.string.dialog_empty_downloads_msg))
                .setPositiveButton(getString(R.string.btn_go_to_browser)) { _, _ ->
                    (activity as? MainActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_home
                }
                .setNegativeButton(getString(R.string.btn_close), null)
                .show()
        }
    }

    private fun showItemMenu(anchor: View, task: DownloadTask) {
        val ctx = context ?: return
        val popup = PopupMenu(ctx, anchor)
        popup.menu.add(0, 4, 0, "📥 " + getString(R.string.menu_download_to_gallery))
        popup.menu.add(0, 1, 0, "🔒 " + getString(R.string.menu_move_to_vault))
        popup.menu.add(0, 2, 0, "📋 " + getString(R.string.menu_delete_from_list))
        popup.menu.add(0, 3, 0, "🗑️ " + getString(R.string.menu_delete_permanently))
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                4 -> {
                    handleDownloadToGallery(task)
                }
                1 -> {
                    settingsRepository.addTaskToVault(task.id)
                    Toast.makeText(ctx, getString(R.string.private_vault_add_success), Toast.LENGTH_SHORT).show()
                    renderDownloadedVideos(downloadEngine.tasksFlow.value)
                }
                2 -> {
                    downloadEngine.deleteTask(task.id, deletePhysicalFile = false)
                    Toast.makeText(ctx, getString(R.string.toast_removed_from_list, task.title), Toast.LENGTH_SHORT).show()
                }
                3 -> {
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

    /**
     * Tải/Lưu video trực tiếp về Bộ Sưu Tập (MediaStore) thiết bị
     * Miễn phí 10 video đầu tiên. Từ video thứ 11 trở đi:
     * - Nếu đã có Sub: Tải không giới hạn (Unlimited)
     * - Nếu chưa có Sub: Bật Payment Wall yêu cầu đăng ký VIP.
     */
    private fun handleDownloadToGallery(task: DownloadTask) {
        val ctx = context ?: return
        val sourceFile = File(task.targetFilePath)
        if (!sourceFile.exists() || sourceFile.length() <= 50 * 1024L) {
            Toast.makeText(ctx, getString(R.string.toast_file_not_found), Toast.LENGTH_SHORT).show()
            return
        }

        // Kiểm tra hạn mức 10 video miễn phí
        if (!settingsRepository.canDownloadToGallery()) {
            Toast.makeText(ctx, getString(R.string.toast_gallery_limit_reached), Toast.LENGTH_LONG).show()
            val intent = Intent(ctx, PaymentWallActivity::class.java).apply {
                putExtra("EXTRA_REASON", "gallery_limit_reached")
            }
            startActivity(intent)
            return
        }

        // Thực hiện xuất video vào MediaStore của Android
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

    /**
     * Mở popup Két Video Riêng Tư (Private Vault) thiết kế đồng nhất với tông màu ứng dụng (#131B2E)
     */
    private fun showVaultPinDialog() {
        val ctx = context ?: return
        val bottomSheet = BottomSheetDialog(ctx)
        val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_private_vault, null)
        bottomSheet.setContentView(dialogView)

        val btnClose = dialogView.findViewById<View>(R.id.btn_close_vault_dialog)
        val layoutAuth = dialogView.findViewById<View>(R.id.layout_vault_auth)
        val layoutContent = dialogView.findViewById<View>(R.id.layout_vault_content)
        val etPin = dialogView.findViewById<EditText>(R.id.et_vault_pin)
        val tvError = dialogView.findViewById<TextView>(R.id.tv_vault_pin_error)
        val btnCancel = dialogView.findViewById<View>(R.id.btn_vault_cancel)
        val btnUnlock = dialogView.findViewById<View>(R.id.btn_vault_unlock)
        val btnRelock = dialogView.findViewById<View>(R.id.btn_vault_relock)
        val containerVaultItems = dialogView.findViewById<LinearLayout>(R.id.container_vault_items)
        val tvEmptyVault = dialogView.findViewById<TextView>(R.id.tv_empty_vault_detail)
        val tvVaultCount = dialogView.findViewById<TextView>(R.id.tv_vault_items_count)

        btnClose.setOnClickListener { bottomSheet.dismiss() }
        btnCancel.setOnClickListener { bottomSheet.dismiss() }

        fun renderVaultContent() {
            val allTasks = downloadEngine.tasksFlow.value
            val vaultTasks = allTasks.filter {
                settingsRepository.isTaskInVault(it.id) &&
                File(it.targetFilePath).exists()
            }

            containerVaultItems.removeAllViews()
            tvVaultCount.text = getString(R.string.vault_unlocked_badge, vaultTasks.size)

            if (vaultTasks.isEmpty()) {
                tvEmptyVault.visibility = View.VISIBLE
            } else {
                tvEmptyVault.visibility = View.GONE
                val inflater = LayoutInflater.from(ctx)

                for (task in vaultTasks) {
                    val itemView = inflater.inflate(R.layout.item_vault_video, containerVaultItems, false)
                    val tvTitle = itemView.findViewById<TextView>(R.id.tv_vault_item_title)
                    val tvSize = itemView.findViewById<TextView>(R.id.tv_vault_item_size)
                    val btnRestore = itemView.findViewById<TextView>(R.id.btn_vault_item_restore)
                    val btnDelete = itemView.findViewById<TextView>(R.id.btn_vault_item_delete)
                    val clickLayout = itemView.findViewById<View>(R.id.layout_vault_text_click)
                    val iconPlay = itemView.findViewById<View>(R.id.icon_play_vault)

                    tvTitle.text = task.title
                    val file = File(task.targetFilePath)
                    val sizeStr = ProgressFragment.formatSize(if (file.exists()) file.length() else task.totalBytes)
                    tvSize.text = getString(R.string.vault_item_security) + " • ${task.qualityLabel} • $sizeStr"

                    val playVaultAction = View.OnClickListener {
                        if (file.exists() && file.length() > 0) {
                            AcePlayerActivity.start(ctx, file.absolutePath, "🔒 " + task.title)
                        } else {
                            Toast.makeText(ctx, getString(R.string.toast_file_not_found), Toast.LENGTH_SHORT).show()
                        }
                    }

                    clickLayout.setOnClickListener(playVaultAction)
                    iconPlay.setOnClickListener(playVaultAction)

                    btnRestore.setOnClickListener {
                        settingsRepository.removeTaskFromVault(task.id)
                        Toast.makeText(ctx, getString(R.string.private_vault_restore_success), Toast.LENGTH_SHORT).show()
                        renderVaultContent()
                        renderDownloadedVideos(downloadEngine.tasksFlow.value)
                    }

                    btnDelete.setOnClickListener {
                        AlertDialog.Builder(ctx)
                            .setTitle(getString(R.string.dialog_delete_title))
                            .setMessage(getString(R.string.dialog_delete_confirm) + "\n\n${task.title}")
                            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                                settingsRepository.removeTaskFromVault(task.id)
                                downloadEngine.deleteTask(task.id, deletePhysicalFile = true)
                                Toast.makeText(ctx, getString(R.string.toast_file_deleted, task.title), Toast.LENGTH_SHORT).show()
                                renderVaultContent()
                                renderDownloadedVideos(downloadEngine.tasksFlow.value)
                            }
                            .setNegativeButton(getString(R.string.btn_cancel), null)
                            .show()
                    }

                    containerVaultItems.addView(itemView)
                }
            }
        }

        btnUnlock.setOnClickListener {
            val pin = etPin.text.toString().trim()
            if (settingsRepository.verifyVaultPin(pin)) {
                tvError.visibility = View.GONE
                layoutAuth.visibility = View.GONE
                layoutContent.visibility = View.VISIBLE
                renderVaultContent()
            } else {
                tvError.visibility = View.VISIBLE
            }
        }

        btnRelock.setOnClickListener {
            etPin.setText("")
            tvError.visibility = View.GONE
            layoutContent.visibility = View.GONE
            layoutAuth.visibility = View.VISIBLE
        }

        bottomSheet.setOnShowListener {
            (dialogView.parent as? View)?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
        bottomSheet.show()
    }
}

