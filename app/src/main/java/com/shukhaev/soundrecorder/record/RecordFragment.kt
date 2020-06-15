package com.shukhaev.soundrecorder.record

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.AlphabeticIndex
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.shukhaev.soundrecorder.MainActivity
import com.shukhaev.soundrecorder.R
import com.shukhaev.soundrecorder.dataBase.RecordDataBaseDao
import com.shukhaev.soundrecorder.dataBase.RecordDatabase
import com.shukhaev.soundrecorder.databinding.FragmentRecordBinding
import kotlinx.android.synthetic.main.fragment_record.*
import java.io.File
import java.util.jar.Manifest

class RecordFragment : Fragment() {


    private lateinit var viewModel: RecordViewModel
    private lateinit var mainActivity: MainActivity

    private var dataBase: RecordDataBaseDao? = null
    private val MY_PERMISSIONS_RECORD_AUDIO = 123


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentRecordBinding>(
            inflater, R.layout.fragment_record, container, false
        )

        dataBase = context?.let { RecordDatabase.getInstance(it).recordDataBaseDao }

        mainActivity = activity as MainActivity
        viewModel = ViewModelProvider(this).get(RecordViewModel::class.java)

        binding.recordViewModel = viewModel
        binding.lifecycleOwner = this.viewLifecycleOwner

        if (!mainActivity.isServiceRunning()) {
            viewModel.resetTimer()
        } else {
            binding.playButton.setImageResource(R.drawable.ic_stop)
        }

        binding.playButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.RECORD_AUDIO),
                    MY_PERMISSIONS_RECORD_AUDIO
                )
            } else {
                if (mainActivity.isServiceRunning()) {
                    onRecord(false)
                    viewModel.stopTimer()
                } else {
                    onRecord(true)
                    viewModel.startTimer()
                }
            }

        }

        createChannel(
            getString(R.string.notification_channel_id),
            getString(R.string.notification_channel_name)
        )

        return binding.root
    }

    private fun onRecord(start: Boolean) {
        val intent = Intent(activity, RecordService::class.java)

        if (start) {
            playButton.setImageResource(R.drawable.ic_stop)
            Toast.makeText(activity, R.string.toast_recording_start, Toast.LENGTH_SHORT).show()

            val folder =
                File(activity?.getExternalFilesDir(null)?.absolutePath.toString() + "/SoundRecorder")
            if (!folder.exists()) {
                folder.mkdir()
            }

            activity?.startService(intent)
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            playButton.setImageResource(R.drawable.ic_mic_red)
            activity?.stopService(intent)
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_RECORD_AUDIO -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onRecord(true)
                    viewModel.startTimer()
                } else {
                    Toast.makeText(
                        activity,
                        R.string.toast_recording_permissions,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    }

    private fun createChannel(channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
                    .apply {
                        setShowBadge(false)
                        setSound(null, null)
                    }
            val notificationManager =
                requireActivity().getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }


}