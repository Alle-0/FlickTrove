package com.cinetrack.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException

class AudioRecorderHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    fun getAudioFile(movieId: Long, mediaType: String): File {
        val dir = File(context.filesDir, "audio_notes")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "${mediaType}_${movieId}.m4a")
    }

    fun hasAudioNote(movieId: Long, mediaType: String): Boolean {
        return getAudioFile(movieId, mediaType).exists()
    }

    fun getAudioDuration(movieId: Long, mediaType: String): Long {
        val file = getAudioFile(movieId, mediaType)
        if (!file.exists()) return 0L
        
        var duration = 0L
        var retriever: android.media.MediaMetadataRetriever? = null
        try {
            retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            duration = time?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Failed to get duration", e)
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                // Ignore release errors
            }
        }
        return duration
    }

    fun deleteAudioNote(movieId: Long, mediaType: String): Boolean {
        stopPlaying()
        val file = getAudioFile(movieId, mediaType)
        if (file.exists()) {
            return file.delete()
        }
        return true
    }

    fun startRecording(movieId: Long, mediaType: String) {
        val file = getAudioFile(movieId, mediaType)
        
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            try {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
                _isRecording.value = true
            } catch (e: IOException) {
                Log.e("AudioRecorderHelper", "prepare() failed", e)
            } catch (e: Exception) {
                Log.e("AudioRecorderHelper", "start() failed", e)
            }
        }
    }

    fun stopRecording() {
        recorder?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                Log.e("AudioRecorderHelper", "stop() failed", e)
            }
        }
        recorder = null
        _isRecording.value = false
    }

    fun startPlaying(movieId: Long, mediaType: String) {
        val file = getAudioFile(movieId, mediaType)
        if (!file.exists()) return

        player = MediaPlayer().apply {
            try {
                setDataSource(file.absolutePath)
                prepare()
                start()
                _isPlaying.value = true
                setOnCompletionListener {
                    stopPlaying()
                }
            } catch (e: IOException) {
                Log.e("AudioRecorderHelper", "prepare() failed", e)
            }
        }
    }

    fun stopPlaying() {
        player?.apply {
            try {
                if (isPlaying) {
                    stop()
                }
                release()
            } catch (e: Exception) {
                Log.e("AudioRecorderHelper", "stopPlaying() failed", e)
            }
        }
        player = null
        _isPlaying.value = false
    }
    
    fun release() {
        stopRecording()
        stopPlaying()
    }
}
