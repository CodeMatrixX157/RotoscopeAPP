package com.example.rotoscope.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

/**
 * A project = one working folder of numbered frame images on disk (never held all in RAM —
 * every later module streams frames in from here one/few at a time).
 */
data class FrameSequence(
    val projectDir: File,
    val frameCount: Int,
    val frameFileNamePattern: String = "frame_%06d.png"
) {
    fun frameFile(index: Int): File = File(projectDir, frameFileNamePattern.format(index))
}

sealed class IngestProgress {
    data class InProgress(val done: Int, val total: Int) : IngestProgress()
    data class Done(val sequence: FrameSequence, val skippedCount: Int) : IngestProgress()
    data class Failed(val reason: String) : IngestProgress()
}

class SequenceRepository(private val context: Context) {

    private fun projectsRoot(): File =
        File(context.getExternalFilesDir(null), "projects").apply { mkdirs() }

    fun newProjectDir(name: String): File =
        File(projectsRoot(), name).apply { mkdirs() }

    /**
     * User picked a folder of images directly (the faster path — no decode needed).
     * Copies them into the project's working dir with normalized, naturally-sorted names,
     * emitting progress per file so large sequences don't look frozen.
     *
     * IMPORTANT: sorts by the file's real DISPLAY_NAME (queried via ContentResolver), not
     * Uri.lastPathSegment — for content:// URIs from the system picker, lastPathSegment is
     * frequently just an internal numeric row ID, not the filename, which would silently sort
     * frames into the wrong order. Wrong order here corrupts every later stage.
     *
     * Each file is decoded and re-encoded to a real PNG rather than byte-copied — a raw copy of,
     * say, a phone-camera JPG renamed to "frame_000001.png" would have a mismatched extension vs.
     * actual format. Android's own Bitmap decoders sniff the real format and wouldn't care, but
     * the native/OpenCV code planned for later modules often trusts the extension — this would be
     * a landmine that only surfaces two modules from now. Re-encoding also normalizes mixed-format
     * sequences (some JPG, some PNG) to one consistent format up front. Trade-off: this is real
     * decode/encode work per frame, not a free copy — still far lighter than video decoding or the
     * AI segmentation to come, so no thermal gating here yet, but worth revisiting if very large
     * sequences turn out to take a while on low-end devices.
     *
     * A file that fails to decode (corrupt, revoked permission, moved mid-pick) is skipped rather
     * than aborting the whole import — the total frame count reflects what actually copied, and
     * the caller finds out via the emitted count, not a full failure of an otherwise-good batch.
     */
    fun ingestImageFolder(sourceUris: List<Uri>, projectName: String): Flow<IngestProgress> = flow {
        if (sourceUris.isEmpty()) {
            emit(IngestProgress.Failed("No images selected."))
            return@flow
        }

        val dir = newProjectDir(projectName)
        val sorted = sourceUris
            .map { uri -> uri to (displayNameOf(uri) ?: uri.lastPathSegment ?: uri.toString()) }
            .sortedWith(compareBy { naturalSortKey(it.second) })

        var written = 0
        var skipped = 0
        sorted.forEachIndexed { index, (uri, _) ->
            try {
                val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                    android.graphics.BitmapFactory.decodeStream(input)
                }
                if (bitmap != null) {
                    File(dir, "frame_%06d.png".format(written)).outputStream().use { output ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
                    }
                    bitmap.recycle()
                    written++
                } else {
                    skipped++
                }
            } catch (e: Exception) {
                skipped++
            }
            emit(IngestProgress.InProgress(index + 1, sorted.size))
        }

        if (written == 0) {
            dir.deleteRecursively()
            emit(IngestProgress.Failed("None of the selected files could be read."))
            return@flow
        }

        emit(IngestProgress.Done(FrameSequence(dir, written), skippedCount = skipped))
    }.flowOn(Dispatchers.IO)

    /** Real display filename via MediaStore/DocumentsProvider metadata, when available. */
    private fun displayNameOf(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Splits a filename into text/number chunks so "frame_10" sorts after "frame_2", not before. */
    private fun naturalSortKey(name: String): String {
        val chunks = Regex("(\\d+)|(\\D+)").findAll(name)
            .map { it.value.let { c -> if (c.firstOrNull()?.isDigit() == true) c.padStart(10, '0') else c } }
        return chunks.joinToString("")
    }
}
