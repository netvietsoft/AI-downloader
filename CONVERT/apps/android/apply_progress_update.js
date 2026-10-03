const fs = require('fs');
const path = require('path');

const layoutPath = 'D:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/CONVERT/apps/android/app/src/main/res/layout/fragment_progress.xml';
const fragmentPath = 'D:/Decompiler/App/Downloader/videoplayer.videodownloader.downloader/CONVERT/apps/android/app/src/main/java/com/nextaitechnology/antidetect/feature/downloader/ProgressFragment.kt';

const layoutContent = `<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/bg_dark"
    android:orientation="vertical"
    android:paddingStart="16dp"
    android:paddingEnd="16dp">

    <!-- Header -->
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="14dp"
        android:text="Progress"
        android:textColor="@color/text_white"
        android:textSize="22sp"
        android:textStyle="bold" />

    <!-- Stability Banner -->
    <androidx.cardview.widget.CardView
        android:id="@+id/card_stability_banner"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        app:cardBackgroundColor="#2D1222"
        app:cardCornerRadius="12dp"
        app:cardElevation="0dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center_vertical"
            android:orientation="horizontal"
            android:padding="12dp">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="⚡"
                android:textSize="16sp" />

            <TextView
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginStart="8dp"
                android:layout_weight="1"
                android:text="Improve download stability in background"
                android:textColor="#F43F5E"
                android:textSize="12sp"
                android:textStyle="bold" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="›"
                android:textColor="#F43F5E"
                android:textSize="18sp" />
        </LinearLayout>
    </androidx.cardview.widget.CardView>

    <!-- Subtabs -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="40dp"
        android:layout_marginTop="14dp"
        android:orientation="horizontal">

        <TextView
            android:id="@+id/tab_downloading"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="@drawable/bg_pill_btn"
            android:gravity="center"
            android:text="Downloading (1)"
            android:textColor="#FFFFFF"
            android:textSize="13sp"
            android:textStyle="bold" />

        <TextView
            android:id="@+id/tab_downloaded"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_marginStart="8dp"
            android:layout_weight="1"
            android:background="@drawable/bg_pill_btn_outline"
            android:gravity="center"
            android:text="Downloaded (2)"
            android:textColor="@color/text_muted"
            android:textSize="13sp"
            android:textStyle="bold" />
    </LinearLayout>

    <!-- Section Downloading -->
    <LinearLayout
        android:id="@+id/section_downloading"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="14dp"
        android:orientation="vertical">

        <!-- Active Task Card -->
        <androidx.cardview.widget.CardView
            android:id="@+id/card_downloading"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="12dp"
            app:cardBackgroundColor="#131B2E"
            app:cardCornerRadius="14dp"
            app:cardElevation="0dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="14dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:gravity="center_vertical"
                    android:orientation="horizontal">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="🎬"
                        android:textSize="22sp" />

                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_marginStart="10dp"
                        android:layout_weight="1"
                        android:orientation="vertical">

                        <TextView
                            android:id="@+id/tv_active_title"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:ellipsize="end"
                            android:maxLines="1"
                            android:text="Nature_Wildlife_4K_Reel.mp4"
                            android:textColor="@color/text_white"
                            android:textSize="14sp"
                            android:textStyle="bold" />

                        <TextView
                            android:id="@+id/tv_active_stats"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="2dp"
                            android:text="3.8 MB/s • 24.5 MB / 36.0 MB (68%)"
                            android:textColor="@color/text_muted"
                            android:textSize="11sp" />
                    </LinearLayout>

                    <TextView
                        android:id="@+id/btn_pause_task"
                        android:layout_width="32dp"
                        android:layout_height="32dp"
                        android:background="@drawable/bg_pill_btn_outline"
                        android:gravity="center"
                        android:text="⏸"
                        android:textColor="@color/primary"
                        android:textSize="14sp" />

                    <!-- 3-dots Menu Button right-aligned -->
                    <TextView
                        android:id="@+id/btn_more_downloading"
                        android:layout_width="32dp"
                        android:layout_height="32dp"
                        android:layout_marginStart="6dp"
                        android:background="?attr/selectableItemBackgroundBorderless"
                        android:gravity="center"
                        android:text="⋮"
                        android:textColor="@color/text_muted"
                        android:textSize="20sp"
                        android:textStyle="bold" />
                </LinearLayout>

                <!-- Progress Bar -->
                <ProgressBar
                    android:id="@+id/pb_active_task"
                    style="?android:attr/progressBarStyleHorizontal"
                    android:layout_width="match_parent"
                    android:layout_height="6dp"
                    android:layout_marginTop="10dp"
                    android:max="100"
                    android:progress="68"
                    android:progressTint="@color/primary" />
            </LinearLayout>
        </androidx.cardview.widget.CardView>

        <!-- Empty State Downloading -->
        <TextView
            android:id="@+id/tv_empty_downloading"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="32dp"
            android:gravity="center"
            android:text="Không có tiến trình tải nào đang chạy"
            android:textColor="@color/text_muted"
            android:textSize="13sp"
            android:visibility="gone" />
    </LinearLayout>

    <!-- Section Downloaded -->
    <LinearLayout
        android:id="@+id/section_downloaded"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="14dp"
        android:orientation="vertical"
        android:visibility="gone">

        <!-- Downloaded Card 1 -->
        <androidx.cardview.widget.CardView
            android:id="@+id/card_downloaded_1"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="10dp"
            app:cardBackgroundColor="#131B2E"
            app:cardCornerRadius="14dp"
            app:cardElevation="0dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:gravity="center_vertical"
                android:orientation="horizontal"
                android:padding="12dp">

                <TextView
                    android:layout_width="44dp"
                    android:layout_height="44dp"
                    android:background="@drawable/bg_pill_btn_outline"
                    android:gravity="center"
                    android:text="▶"
                    android:textColor="@color/primary"
                    android:textSize="18sp" />

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="12dp"
                    android:layout_weight="1"
                    android:orientation="vertical">

                    <TextView
                        android:id="@+id/tv_title_downloaded_1"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:ellipsize="end"
                        android:maxLines="1"
                        android:text="Travel_Vlog_Japan_Kyoto.mp4"
                        android:textColor="@color/text_white"
                        android:textSize="13sp"
                        android:textStyle="bold" />

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="3dp"
                        android:text="1080p • 48.2 MB • 04:15"
                        android:textColor="@color/text_muted"
                        android:textSize="11sp" />
                </LinearLayout>

                <TextView
                    android:id="@+id/btn_play_downloaded_1"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="PLAY"
                    android:textColor="@color/primary"
                    android:textSize="12sp"
                    android:textStyle="bold" />

                <!-- 3-dots Menu Button right-aligned -->
                <TextView
                    android:id="@+id/btn_more_downloaded_1"
                    android:layout_width="36dp"
                    android:layout_height="36dp"
                    android:layout_marginStart="6dp"
                    android:background="?attr/selectableItemBackgroundBorderless"
                    android:gravity="center"
                    android:text="⋮"
                    android:textColor="@color/text_muted"
                    android:textSize="20sp"
                    android:textStyle="bold" />
            </LinearLayout>
        </androidx.cardview.widget.CardView>

        <!-- Downloaded Card 2 -->
        <androidx.cardview.widget.CardView
            android:id="@+id/card_downloaded_2"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="10dp"
            app:cardBackgroundColor="#131B2E"
            app:cardCornerRadius="14dp"
            app:cardElevation="0dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:gravity="center_vertical"
                android:orientation="horizontal"
                android:padding="12dp">

                <TextView
                    android:layout_width="44dp"
                    android:layout_height="44dp"
                    android:background="@drawable/bg_pill_btn_outline"
                    android:gravity="center"
                    android:text="▶"
                    android:textColor="@color/primary"
                    android:textSize="18sp" />

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="12dp"
                    android:layout_weight="1"
                    android:orientation="vertical">

                    <TextView
                        android:id="@+id/tv_title_downloaded_2"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:ellipsize="end"
                        android:maxLines="1"
                        android:text="Tech_Review_Gadgets_2026.mp4"
                        android:textColor="@color/text_white"
                        android:textSize="13sp"
                        android:textStyle="bold" />

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="3dp"
                        android:text="720p • 22.8 MB • 02:40"
                        android:textColor="@color/text_muted"
                        android:textSize="11sp" />
                </LinearLayout>

                <TextView
                    android:id="@+id/btn_play_downloaded_2"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="PLAY"
                    android:textColor="@color/primary"
                    android:textSize="12sp"
                    android:textStyle="bold" />

                <!-- 3-dots Menu Button right-aligned -->
                <TextView
                    android:id="@+id/btn_more_downloaded_2"
                    android:layout_width="36dp"
                    android:layout_height="36dp"
                    android:layout_marginStart="6dp"
                    android:background="?attr/selectableItemBackgroundBorderless"
                    android:gravity="center"
                    android:text="⋮"
                    android:textColor="@color/text_muted"
                    android:textSize="20sp"
                    android:textStyle="bold" />
            </LinearLayout>
        </androidx.cardview.widget.CardView>

        <!-- Empty State Downloaded -->
        <TextView
            android:id="@+id/tv_empty_downloaded"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="32dp"
            android:gravity="center"
            android:text="Chưa có video nào trong danh sách đã tải"
            android:textColor="@color/text_muted"
            android:textSize="13sp"
            android:visibility="gone" />
    </LinearLayout>

</LinearLayout>
`;

const fragmentContent = `package com.nextaitechnology.antidetect.feature.downloader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.nextaitechnology.antidetect.MainActivity
import com.nextaitechnology.antidetect.R
import com.nextaitechnology.antidetect.feature.player.AcePlayerActivity

/**
 * Màn hình Quản lý Tiến độ Tải & Danh sách Video hoàn thành (ProgressFragment)
 * Active Downloads & Finished Video Records Management with Delete Operations (3-dots menu)
 *
 * @author NextAI Technology Core Team
 */
class ProgressFragment : Fragment() {

    private lateinit var tabDownloading: TextView
    private lateinit var tabDownloaded: TextView
    private lateinit var sectionDownloading: View
    private lateinit var sectionDownloaded: View
    private lateinit var btnPauseTask: TextView
    private lateinit var pbActiveTask: ProgressBar
    private lateinit var tvActiveStats: TextView

    // Empty states
    private lateinit var tvEmptyDownloading: TextView
    private lateinit var tvEmptyDownloaded: TextView

    // Card views for deletion
    private lateinit var cardDownloading: View
    private lateinit var cardDownloaded1: View
    private lateinit var cardDownloaded2: View

    // 3-dots menu buttons (căn lề phải)
    private lateinit var btnMoreDownloading: TextView
    private lateinit var btnMoreDownloaded1: TextView
    private lateinit var btnMoreDownloaded2: TextView

    private var downloadingCount = 1
    private var downloadedCount = 2
    private var isPaused = false

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
        btnPauseTask = view.findViewById(R.id.btn_pause_task)
        pbActiveTask = view.findViewById(R.id.pb_active_task)
        tvActiveStats = view.findViewById(R.id.tv_active_stats)

        tvEmptyDownloading = view.findViewById(R.id.tv_empty_downloading)
        tvEmptyDownloaded = view.findViewById(R.id.tv_empty_downloaded)

        cardDownloading = view.findViewById(R.id.card_downloading)
        cardDownloaded1 = view.findViewById(R.id.card_downloaded_1)
        cardDownloaded2 = view.findViewById(R.id.card_downloaded_2)

        btnMoreDownloading = view.findViewById(R.id.btn_more_downloading)
        btnMoreDownloaded1 = view.findViewById(R.id.btn_more_downloaded_1)
        btnMoreDownloaded2 = view.findViewById(R.id.btn_more_downloaded_2)

        setupSubtabs()
        setupActions(view)
        setupMoreMenuListeners()
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

    private fun setupActions(root: View) {
        btnPauseTask.setOnClickListener {
            isPaused = !isPaused
            if (isPaused) {
                btnPauseTask.text = "▶"
                tvActiveStats.text = "Tạm dừng • 24.5 MB / 36.0 MB (68%)"
                Toast.makeText(context, "Đã tạm dừng tác vụ tải", Toast.LENGTH_SHORT).show()
            } else {
                btnPauseTask.text = "⏸"
                tvActiveStats.text = "3.8 MB/s • 24.5 MB / 36.0 MB (68%)"
                Toast.makeText(context, "Đang tiếp tục tải đa luồng...", Toast.LENGTH_SHORT).show()
            }
        }

        root.findViewById<View>(R.id.card_stability_banner).setOnClickListener {
            showStabilityDialog()
        }

        // Bấm vào phần nội dung đang tải để xem luồng video trực tiếp trên AcePlayer
        cardDownloading.setOnClickListener {
            val mainAct = activity as? MainActivity
            val playUrl = mainAct?.detectedVideoUrl ?: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            AcePlayerActivity.start(requireContext(), playUrl, "Nature_Wildlife_4K_Reel.mp4")
        }

        // Bấm vào video đã tải để phát ngay lập tức trên AcePlayer
        cardDownloaded1.setOnClickListener {
            AcePlayerActivity.start(
                requireContext(),
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                "Travel_Vlog_Japan_Kyoto.mp4"
            )
        }

        cardDownloaded2.setOnClickListener {
            AcePlayerActivity.start(
                requireContext(),
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                "Tech_Review_Gadgets_2026.mp4"
            )
        }
    }

    /**
     * Thiết lập sự kiện nút 3 chấm căn lề phải cho mỗi video item
     * Setup 3-dots right-aligned menu listeners for each video item
     */
    private fun setupMoreMenuListeners() {
        btnMoreDownloading.setOnClickListener { v ->
            showDeleteMenu(v, "Nature_Wildlife_4K_Reel.mp4", cardDownloading, isDownloading = true)
        }

        btnMoreDownloaded1.setOnClickListener { v ->
            showDeleteMenu(v, "Travel_Vlog_Japan_Kyoto.mp4", cardDownloaded1, isDownloading = false)
        }

        btnMoreDownloaded2.setOnClickListener { v ->
            showDeleteMenu(v, "Tech_Review_Gadgets_2026.mp4", cardDownloaded2, isDownloading = false)
        }
    }

    /**
     * Hiển thị menu tùy chọn 3 chấm (Xóa video)
     * Display 3-dots popup menu (Delete video)
     */
    private fun showDeleteMenu(anchor: View, videoTitle: String, cardView: View, isDownloading: Boolean) {
        val context = context ?: return
        val popup = PopupMenu(context, anchor)
        popup.menu.add(0, 1, 0, "🗑️ Xóa video khỏi danh sách")
        popup.setOnMenuItemClickListener { item ->
            if (item.itemId == 1) {
                confirmDelete(videoTitle, cardView, isDownloading)
            }
            true
        }
        popup.show()
    }

    /**
     * Hộp thoại xác nhận xóa video & hiệu ứng trượt mượt mà
     * Delete confirmation dialog & smooth exit transition
     */
    private fun confirmDelete(videoTitle: String, cardView: View, isDownloading: Boolean) {
        val ctx = context ?: return
        val actionText = if (isDownloading) "hủy tiến trình tải và xóa video" else "xóa video này khỏi danh sách và bộ nhớ máy"

        AlertDialog.Builder(ctx)
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc chắn muốn " + actionText + ":\n\n\"" + videoTitle + "\"?")
            .setPositiveButton("Xóa") { _, _ ->
                // Hiệu ứng trượt ra và mờ dần (Smooth slide-out exit animation)
                cardView.animate()
                    .alpha(0f)
                    .translationX(cardView.width.toFloat())
                    .setDuration(260)
                    .withEndAction {
                        cardView.visibility = View.GONE
                        if (isDownloading) {
                            downloadingCount = 0
                            tabDownloading.text = "Downloading (0)"
                            tvEmptyDownloading.visibility = View.VISIBLE
                        } else {
                            downloadedCount = (downloadedCount - 1).coerceAtLeast(0)
                            tabDownloaded.text = "Downloaded (" + downloadedCount + ")"
                            if (downloadedCount == 0) {
                                tvEmptyDownloaded.visibility = View.VISIBLE
                            }
                        }
                        Toast.makeText(ctx, "Đã xóa: " + videoTitle, Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showStabilityDialog() {
        context?.let { ctx ->
            AlertDialog.Builder(ctx)
                .setTitle("⚡ Tối ưu hóa tải nền (Samsung A07)")
                .setMessage("Để tải các tệp video dung lượng lớn không bị ngắt quãng khi tắt màn hình, hãy cấp quyền không giới hạn pin cho ứng dụng NextAI.")
                .setPositiveButton("Đã hiểu", null)
                .show()
        }
    }
}
`;

fs.writeFileSync(layoutPath, layoutContent, 'utf8');
console.log('Updated layout successfully:', layoutPath);

fs.writeFileSync(fragmentPath, fragmentContent, 'utf8');
console.log('Updated fragment successfully:', fragmentPath);
