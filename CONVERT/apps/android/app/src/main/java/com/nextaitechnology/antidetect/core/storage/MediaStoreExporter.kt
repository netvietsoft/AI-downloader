package com.nextaitechnology.antidetect.core.storage

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Trình Xuất & Đồng Bộ Video Vào MediaStore (Bộ Sưu Tập Thiết Bị)
 * Exports downloaded videos from app-private storage to system MediaStore
 * ensuring immediate visibility in Samsung Gallery, Google Photos, etc.
 *
 * @author NextAI Technology Core Team
 */
object MediaStoreExporter {

    private const val TAG = "MediaStoreExporter"
    private const val ALBUM_NAME = "AIDownloader"

    /**
     * Xuất video sang MediaStore của hệ điều hành Android
     * @return Uri của video trong MediaStore nếu thành công, null nếu thất bại
     */
    fun exportVideoToGallery(
        context: Context,
        sourceFile: File,
        videoTitle: String? = null
    ): Uri? {
        if (!sourceFile.exists() || sourceFile.length() <= 0) {
            Log.e(TAG, "Source file does not exist or is empty: ${sourceFile.absolutePath}")
            return null
        }

        val cleanTitle = videoTitle?.trim()?.takeIf { it.isNotEmpty() }
            ?: sourceFile.nameWithoutExtension.ifEmpty { "Video_${System.currentTimeMillis()}" }

        val safeFileName = cleanTitle.replace(Regex("[^a-zA-Z0-9._\\- ]"), "_")
            .take(60) + ".mp4"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Scoped Storage (Android 10+)
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, safeFileName)
                    put(MediaStore.Video.Media.TITLE, cleanTitle)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$ALBUM_NAME")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                    put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                    put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
                }

                val resolver = context.contentResolver
                val collectionUri = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val destUri = resolver.insert(collectionUri, contentValues)

                if (destUri != null) {
                    resolver.openOutputStream(destUri)?.use { outStream ->
                        FileInputStream(sourceFile).use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }

                    contentValues.clear()
                    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(destUri, contentValues, null, null)

                    Log.i(TAG, "Successfully exported video to MediaStore: $destUri")
                    return destUri
                }
            } else {
                // Android 9 and lower
                val moviesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), ALBUM_NAME)
                if (!moviesDir.exists()) {
                    moviesDir.mkdirs()
                }

                val destFile = File(moviesDir, safeFileName)
                FileInputStream(sourceFile).use { inStream ->
                    FileOutputStream(destFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }

                MediaScannerConnection.scanFile(
                    context.applicationContext,
                    arrayOf(destFile.absolutePath),
                    arrayOf("video/mp4")
                ) { path, uri ->
                    Log.i(TAG, "MediaScanner scanned $path -> $uri")
                }

                return Uri.fromFile(destFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting video to MediaStore", e)
        }
        return null
    }
}
