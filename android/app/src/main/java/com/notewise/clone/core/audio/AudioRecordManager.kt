package com.notewise.clone.core.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File

class AudioRecordManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var recordStartTime: Long = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var playbackProgressRunnable: Runnable? = null

    var isRecording: Boolean = false
        private set
    var isPlaying: Boolean = false
        private set

    fun getRecordingElapsedMs(): Long {
        return if (isRecording) System.currentTimeMillis() - recordStartTime else 0L
    }

    fun startRecording(outputFile: File): Boolean {
        if (isRecording) return false
        stopPlayback()

        return try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            recordStartTime = System.currentTimeMillis()
            isRecording = true
            true
        } catch (e: Exception) {
            Log.e("AudioRecordManager", "Error starting recording", e)
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            false
        }
    }

    fun stopRecording(): Long {
        if (!isRecording) return 0L
        val duration = System.currentTimeMillis() - recordStartTime
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecordManager", "Error stopping recording", e)
        } finally {
            mediaRecorder = null
            isRecording = false
        }
        return duration
    }

    fun startPlayback(
        audioFile: File,
        onProgress: (currentMs: Int, totalMs: Int) -> Unit,
        onComplete: () -> Unit
    ): Boolean {
        if (!audioFile.exists()) return false
        stopPlayback()

        return try {
            val player = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()
                start()
            }

            mediaPlayer = player
            isPlaying = true

            playbackProgressRunnable = object : Runnable {
                override fun run() {
                    mediaPlayer?.let { p ->
                        if (p.isPlaying) {
                            onProgress(p.currentPosition, p.duration)
                            handler.postDelayed(this, 100)
                        }
                    }
                }
            }
            handler.post(playbackProgressRunnable!!)

            player.setOnCompletionListener {
                stopPlayback()
                onComplete()
            }
            true
        } catch (e: Exception) {
            Log.e("AudioRecordManager", "Error starting playback", e)
            stopPlayback()
            false
        }
    }

    fun pausePlayback() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPlaying = false
            }
        }
    }

    fun resumePlayback() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                isPlaying = true
                playbackProgressRunnable?.let { r -> handler.post(r) }
            }
        }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
    }

    fun stopPlayback() {
        playbackProgressRunnable?.let { handler.removeCallbacks(it) }
        playbackProgressRunnable = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecordManager", "Error releasing player", e)
        } finally {
            mediaPlayer = null
            isPlaying = false
        }
    }

    fun release() {
        if (isRecording) stopRecording()
        stopPlayback()
    }
}

