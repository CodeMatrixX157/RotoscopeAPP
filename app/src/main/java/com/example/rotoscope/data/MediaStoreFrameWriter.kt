package com.example.rotoscope.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

/**
 * Saves converted frames somewhere the user can actually find them afterward — Pictures/RotoscopeFrames/<folderName>/ —
 * instead of buried in this app's private storage. On API 29+ this uses MediaStore + scoped storage
 * (no permission needed for the app's own contributions). On API 24-28 it needs WRITE_EXTERNAL_STORAGE,
 * requested by the caller before starting.
 */
class MediaStoreFrameWriter(private val context: Context) {

    fun requiresLegacyWritePermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    suspend fun writeFrame(folderName: String, index: Int, bitmap: Bitmap) {
        val fileName = "frame_%06d.png".format(index)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/RotoscopeFrames/$folderName")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("MediaStore refused to create an entry for $fileName")
            context.contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } ?: throw IllegalStateException("Couldn't open output stream for $fileName")
        } else {
            // Legacy path (API 24-28): caller must have already been granted WRITE_EXTERNAL_STORAGE.
            @Suppress("DEPRECATION")
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val dir = File(picturesDir, "RotoscopeFrames/$folderName").apply { mkdirs() }
            val outFile = File(dir, fileName)
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            // Without this, files written directly to external storage on API 24-28 are invisible
            // in Gallery/Photos/file pickers until the next full media scan (e.g. a reboot) —
            // MediaStore's index doesn't know about them yet.
            MediaScannerConnection.scanFile(context, arrayOf(outFile.absolutePath), arrayOf("image/png"), null)
        }
    }
}
