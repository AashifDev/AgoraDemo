package com.example.agorademo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.agorademo.databinding.ActivityMainBinding
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    private val uid = 0
    // Track the status of your connection
    private var isJoined = false
    // Agora engine instance
    private var agoraEngine: RtcEngine? = null
    private var mRtcEventHandler: IRtcEngineEventHandler? = null
    // UI elements
    lateinit var remoteUserStatus: TextView
    private val joinLeaveButton: Button? = null
    var PERMISSION_REQ_ID_RECORD_AUDIO = 101

    lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)){
            setupVoiceSDKEngine()
        }

        binding.call.setOnClickListener{
            joinChannel()
        }

        binding.endCall.setOnClickListener {
            joinLeaveChannel()
        }

        mRtcEventHandler = object : IRtcEngineEventHandler() {
            // Listen for the remote user joining the channel.
            override fun onUserJoined(uid: Int, elapsed: Int) {
                runOnUiThread { binding.remoteUserStatus.text = "Remote user joined: $uid" }
            }

            override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
                // Successfully joined a channel
                isJoined = true
                showMessage("Joined Channel $channel")
                runOnUiThread { binding.remoteUserStatus.text = "Waiting for a remote user to join" }
            }

            override fun onUserOffline(uid: Int, reason: Int) {
                // Listen for remote users leaving the channel
                showMessage("Remote user offline $uid $reason")
                if (isJoined) runOnUiThread {
                    binding.remoteUserStatus.text = "Waiting for a remote user to join"
                }
            }

            override fun onLeaveChannel(stats: RtcStats) {
                // Listen for the local user leaving the channel
                runOnUiThread { binding.remoteUserStatus.text = "Press the button to join a channel" }
                isJoined = false
            }
        }

    }

    private fun checkSelfPermission(permission: String, permissionReqIdRecordAudio: Int): Boolean {

        if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, Array(1) { permission }, PERMISSION_REQ_ID_RECORD_AUDIO)

            return false
        }
        return true
    }

    private fun setupVoiceSDKEngine() {
        initializeAgoraEngine()
        joinChannel()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun initializeAgoraEngine() {

        GlobalScope.launch (Dispatchers.Main){
            val config = RtcEngineConfig()
            config.mContext = applicationContext
            config.mAppId = getString(R.string.app_id)
            config.mEventHandler = mRtcEventHandler

            agoraEngine = RtcEngine.create(config)
        }

    }

    private fun joinChannel() {
        val options = ChannelMediaOptions()



        options.autoSubscribeAudio = true
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
        agoraEngine?.setEnableSpeakerphone(true)
        agoraEngine?.setDefaultAudioRoutetoSpeakerphone(true)
        agoraEngine?.joinChannel(null, "Testing", 0, options)

        Toast.makeText(this@MainActivity, "Connected.", Toast.LENGTH_SHORT).show()

    }


    fun joinLeaveChannel() {
        if (isJoined) {
            agoraEngine!!.leaveChannel()
            //binding.text.text = "Join"
            Toast.makeText(this, "Connected.", Toast.LENGTH_SHORT).show()
        } else {
            joinChannel()
            //binding.text.text = "Leave"
            Toast.makeText(this, "Disconnected..", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMessage(string: String){
        Toast.makeText(this, string, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraEngine!!.leaveChannel()
    }
}
