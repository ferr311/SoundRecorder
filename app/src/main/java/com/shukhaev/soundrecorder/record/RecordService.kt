package com.shukhaev.soundrecorder.record

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.shukhaev.soundrecorder.MainActivity
import com.shukhaev.soundrecorder.R
import com.shukhaev.soundrecorder.dataBase.RecordDataBaseDao
import com.shukhaev.soundrecorder.dataBase.RecordDatabase
import com.shukhaev.soundrecorder.dataBase.RecordingItem
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat

class RecordService : Service() {

    private var mFileName: String? = null
    private var mFilePath: String? = null
    private var mCountRecords: Int? = null

    private var mRecorder: MediaRecorder? = null
    private var mStartingTimeMillis: Long = 0
    private var mElapsedMillis: Long = 0

    private var mDataBase: RecordDataBaseDao? = null

    private val mJob = Job()
    private val mUiScope = CoroutineScope(Dispatchers.Main + mJob)

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mDataBase = RecordDatabase.getInstance(application).recordDataBaseDao
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mCountRecords = intent?.extras?.get("COUNT") as Int?

        startRecording()
        return START_NOT_STICKY
    }

    private fun startRecording() {
        setFileNameAndPath()

        mRecorder = MediaRecorder()
        mRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        mRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mRecorder?.setOutputFile(mFilePath)
        mRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mRecorder?.setAudioChannels(1)
        mRecorder?.setAudioEncodingBitRate(192000)

        try {
            mRecorder?.prepare()
            mRecorder?.start()
            mStartingTimeMillis = System.currentTimeMillis()
            startForeground(1, createNotification())
        } catch (e: IOException) {
            Log.e("Record Service", "prepare failed")
        }
    }

    private fun createNotification(): Notification? {
        val mBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(
                applicationContext,
                R.string.notification_channel_id.toString()
            )
                .setSmallIcon(R.drawable.ic_mic_red)
                .setContentTitle(R.string.app_name.toString())
                .setContentText(R.string.notification_recording.toString())
                .setOngoing(true)
        mBuilder.setContentIntent(
            PendingIntent.getActivities(
                applicationContext, 0, arrayOf(
                    Intent(
                        applicationContext,
                        MainActivity::class.java
                    )
                ), 0
            )
        )
        return mBuilder.build()
    }

    private fun setFileNameAndPath() {

        var count = 0
        var f: File
        val dateTime = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(System.currentTimeMillis())
        do {
            mFileName = R.string.default_file_name.toString() + "_" + dateTime + count + ".mp4"
            mFilePath = application.getExternalFilesDir(null)?.absolutePath
            mFilePath += "/$mFileName"
            count++
            f = File(mFilePath)
        } while (f.exists() && !f.isDirectory)

    }

    private fun stopRecording() {
        val recordingItem = RecordingItem()

        mRecorder?.stop()
        mElapsedMillis = System.currentTimeMillis() - mStartingTimeMillis
        mRecorder?.release()
        Toast.makeText(this, getString(R.string.toast_recording_finish), Toast.LENGTH_SHORT).show()

        recordingItem.name = mFileName.toString()
        recordingItem.filePath = mFilePath.toString()
        recordingItem.length = mElapsedMillis
        recordingItem.time = System.currentTimeMillis()

        mRecorder = null

        try {
            mUiScope.launch {
                withContext(Dispatchers.IO) {
                    mDataBase?.insert(recordingItem)
                }
            }
        } catch (e: Exception) {
            Log.e("RecordService", "exception", e)
        }
    }

    override fun onDestroy() {
        if (mRecorder != null) {
            stopRecording()
        }
        super.onDestroy()

    }
}