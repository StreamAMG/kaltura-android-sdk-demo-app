package com.streamamg.androidapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Point
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.OrientationEventListener
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.kaltura.playersdk.PlayerViewController
import com.kaltura.playersdk.events.KPlayerServiceListener
import com.kaltura.playersdk.events.KPlayerState
import com.kaltura.playersdk.services.BackgroundPlayerService
import com.kaltura.playersdk.types.KPError
import com.kaltura.playersdk.types.MediaBundle

class PaaSActivity : AppCompatActivity(), KPlayerServiceListener{
    var myService: BackgroundPlayerService? = null
    var serviceBound = false

    var SERVICE_URL: String = ""
    var UI_CONF_ID: String = ""
    var PARTNER_ID: String = ""
    var ENTRY_ID: String = ""
    var KS: String = ""
    var izsession: String = ""
    var adLink: String = ""

    var mPlaybackState: KotlinActivity.PlaybackState = KotlinActivity.PlaybackState.IDLE
    lateinit var mPlayerView: PlayerViewController


    private var mSensorStateChanges: KotlinActivity.SensorStateChangeActions? = null
    private var sensorEvent: OrientationEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kotlin)

        mPlayerView = findViewById(R.id.player)
        SERVICE_URL = intent.getStringExtra("SERVICE_URL")
        UI_CONF_ID = intent.getStringExtra("UI_CONF_ID")
        PARTNER_ID = intent.getStringExtra("PARTNER_ID")
        ENTRY_ID = intent.getStringExtra("ENTRY_ID")
        KS = intent.getStringExtra("KS")
        izsession = intent.getStringExtra("IZsession")
        adLink = intent.getStringExtra("AdLink")


        window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)

        if(savedInstanceState == null) {
            initPlayer()
        }

    }

    val myConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName,
                                        service: IBinder) {
            Log.d("WRD", "Service bound")
            val binder = service as BackgroundPlayerService.MyBinder
            myService = binder.service
            serviceBound = true
            setUpPlayer();
        }

        override fun onServiceDisconnected(name: ComponentName) {
            serviceBound = false
        }
    }

    private fun setUpPlayer() {
        myService?.setupPlayer(this, mPlayerView,this)
        var bundle = MediaBundle(SERVICE_URL, PARTNER_ID, UI_CONF_ID, ENTRY_ID, KS, izsession)
            bundle.adURL = adLink
            myService?.updateMedia(bundle)
    }

    override fun onResume() {
        super.onResume()
        myService?.refreshMedia()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("WRD", "PaaS On Destroy")
        if (mPlayerView.mediaControl != null) {
            if (mPlayerView.mediaControl.isPlaying) {
                mPlayerView.mediaControl.pause()
            }
        }
        mPlayerView.removePlayer()
        val serviceIntent = Intent(this, BackgroundPlayerService::class.java)
        stopService(serviceIntent)
        myService = null
    }


    private fun initPlayer() {

BackgroundPlayerService.setNotificationIcon(R.drawable.ic_cast)

        val serviceIntent = Intent(this, BackgroundPlayerService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        bindService(serviceIntent, myConnection, BIND_AUTO_CREATE);
    }

    override fun onKPlayerError(playerViewController: PlayerViewController?, error: KPError?) {}

    override fun onKPlayerPlayheadUpdate(playerViewController: PlayerViewController?, currentTimeMilliSeconds: Long) {}

    override fun onKPlayerStateChanged(playerViewController: PlayerViewController?, state: KPlayerState?) {}

    override fun onKPlayerFullScreenToggled(playerViewController: PlayerViewController?, isFullscreen: Boolean) {
        if (isFullscreen) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            mSensorStateChanges = KotlinActivity.SensorStateChangeActions.WATCH_FOR_LANDSCAPE_CHANGES
            if (null == sensorEvent) initialiseSensor(true) else sensorEvent?.enable()
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            mSensorStateChanges = KotlinActivity.SensorStateChangeActions.WATCH_FOR_POTRAIT_CHANGES
            if (null == sensorEvent) initialiseSensor(true) else sensorEvent?.enable()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d("PaaSActivity", "onConfigurationChanged: $newConfig")
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            toggleFullscreen(true)
        } else {
            toggleFullscreen(false)
        }
    }

    private fun toggleFullscreen(isFullscreen: Boolean) {
        if (isFullscreen) {
            val uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            window.decorView.systemUiVisibility = uiOptions
            supportActionBar!!.hide()
            val set = ConstraintSet()
            val playerContainer = findViewById<ConstraintLayout>(R.id.playerContainer)
            set.clone(playerContainer)
            set.setDimensionRatio(R.id.player, calculateAspectRatio())
            set.applyTo(playerContainer)
            mPlayerView.sendNotification("onOpenFullScreen", null)
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE
            supportActionBar!!.show()
            val set = ConstraintSet()
            val playerContainer = findViewById<ConstraintLayout>(R.id.playerContainer)
            set.clone(playerContainer)
            set.setDimensionRatio(R.id.player, "16:9")
            set.applyTo(playerContainer)
            mPlayerView.sendNotification("onCloseFullScreen", null)
        }
    }

    private fun calculateAspectRatio(): String? {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val size = Point()
        wm.defaultDisplay.getRealSize(size)
        val screenWidth = Math.max(size.x, size.y)
        val screenHeight = Math.min(size.x, size.y)
        val factor = greatestCommonFactor(screenWidth, screenHeight)
        val widthRatio = screenWidth / factor
        val heightRatio = screenHeight / factor
        val ratio = "$widthRatio:$heightRatio"
        Log.d("PaaSActivity", "calculateAspectRatio landscape " + screenWidth + "x" + screenHeight + " : " + ratio)
        return ratio
    }

    private fun greatestCommonFactor(width: Int, height: Int): Int {
        return if (height == 0) width else greatestCommonFactor(height, width % height)
    }

    /**
     * Initialises system sensor to detect device orientation for player changes.
     * Don't enable sensor until playback starts on player
     *
     * @param enable if set, sensor will be enabled.
     */
    private fun initialiseSensor(enable: Boolean) {
        sensorEvent = object : OrientationEventListener(this,
            SensorManager.SENSOR_DELAY_NORMAL) {
            override fun onOrientationChanged(orientation: Int) {
                /*
                 * This logic is useful when user explicitly changes orientation using player controls, in which case orientation changes gives no callbacks.
                 * we use sensor angle to anticipate orientation and make changes accordingly.
                 */
                if (null != mSensorStateChanges && mSensorStateChanges == KotlinActivity.SensorStateChangeActions.WATCH_FOR_LANDSCAPE_CHANGES && (orientation >= 60 && orientation <= 120 || orientation >= 240 && orientation <= 300)) {
                    mSensorStateChanges = KotlinActivity.SensorStateChangeActions.SWITCH_FROM_LANDSCAPE_TO_STANDARD
                } else if (null != mSensorStateChanges && mSensorStateChanges == KotlinActivity.SensorStateChangeActions.SWITCH_FROM_LANDSCAPE_TO_STANDARD && (orientation <= 40 || orientation >= 320)) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    mSensorStateChanges = null
                    sensorEvent?.disable()
                } else if (null != mSensorStateChanges && mSensorStateChanges == KotlinActivity.SensorStateChangeActions.WATCH_FOR_POTRAIT_CHANGES && (orientation >= 300 && orientation <= 359 || orientation >= 0 && orientation <= 45)) {
                    mSensorStateChanges = KotlinActivity.SensorStateChangeActions.SWITCH_FROM_POTRAIT_TO_STANDARD
                } else if (null != mSensorStateChanges && mSensorStateChanges == KotlinActivity.SensorStateChangeActions.SWITCH_FROM_POTRAIT_TO_STANDARD && (orientation <= 300 && orientation >= 240 || orientation <= 130 && orientation >= 60)) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    mSensorStateChanges = null
                    sensorEvent?.disable()
                }
            }
        }
        if (enable) sensorEvent?.enable()
    }

}