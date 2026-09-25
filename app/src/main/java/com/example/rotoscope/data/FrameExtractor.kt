package com.example.rotoscope.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

sealed class ExtractionProgress {
    data class InProgress(val framesDone: Int, val framesTotal: Int) : ExtractionProgress()
    data class Done(val totalFrames: Int) : ExtractionProgress()
    data class Failed(val reason: String) : ExtractionProgress()
}

/**
 * NOTE (perf): MediaMetadataRetriever.getFrameAtTime is correct but slow — it decodes
 * independently per timestamp rather than walking the stream sequentially. Fine while this is
 * a standalone convert-once tool. If it ever needs to be fast/real-time, swap for a MediaCodec-based
 * sequential decoder (or route through FFmpeg like CAMERA-TRACKER does) — that's the "heavy
 * task → C++" move, and it belongs in its own pass rather than complicating this one.
 *
 * Decode is decoupled from storage: callers supply [writeFrame], so this class doesn't need to
 * know or care whether frames end up in MediaStore, an internal project folder, or anywhere else.
 */
class FrameExtractor(private val context: Context) {

    fun extract(
        videoUri: Uri,
        targetFps: Double,
        writeFrame: suspend (index: Int, bitmap: Bitmap) -> Unit
    ): Flow<ExtractionProgress> = flow {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: run {
                emit(ExtractionProgress.Failed("Couldn't read video duration — file may be corrupt or an unsupported codec."))
                return@flow
            }

            val frameIntervalMs = 1000.0 / targetFps
            val totalFrames = (durationMs / frameIntervalMs).toInt().coerceAtLeast(1)

            for (i in 0 until totalFrames) {
                val timeUs = (i * frameIntervalMs * 1000).toLong()
                val bitmap: Bitmap? = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                if (bitmap == null) {
                    emit(ExtractionProgress.Failed("Frame $i failed to decode — stopping to avoid a corrupt sequence."))
                    return@flow
                }
                writeFrame(i, bitmap)
                bitmap.recycle()
                emit(ExtractionProgress.InProgress(i + 1, totalFrames))
            }

            emit(ExtractionProgress.Done(totalFrames))
        } catch (e: Exception) {
            emit(ExtractionProgress.Failed(e.message ?: "Unknown extraction error"))
        } finally {
            retriever.release()
        }
    }.flowOn(Dispatchers.IO)
}
