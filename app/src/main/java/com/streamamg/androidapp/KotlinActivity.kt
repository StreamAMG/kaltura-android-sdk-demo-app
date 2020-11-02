package com.streamamg.androidapp

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Point
import android.hardware.SensorManager
import android.media.MediaRouter
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import android.view.*
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.CastStateListener
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.kaltura.playersdk.KPPlayerConfig
import com.kaltura.playersdk.PlayerViewController
import com.kaltura.playersdk.casting.KCastFactory
import com.kaltura.playersdk.casting.KCastProviderV3Impl
import com.kaltura.playersdk.events.*
import com.kaltura.playersdk.interfaces.KCastMediaRemoteControl
import com.kaltura.playersdk.interfaces.KCastMediaRemoteControl.KCastMediaRemoteControlListener
import com.kaltura.playersdk.interfaces.KCastProvider.KCastProviderListener
import com.kaltura.playersdk.types.KPError
import com.kaltura.playersdk.utils.LogUtils


class KotlinActivity : AppCompatActivity(), KPErrorEventListener, KPPlayheadUpdateEventListener, KPFullScreenToggledEventListener, KPStateChangedEventListener {

    
    var TAG: String = "KotlinPlayer"
    var mPlaybackState: PlaybackState = PlaybackState.IDLE
    var mPlayerView: PlayerViewController? = null
    var SERVICE_URL: String = ""
    var UI_CONF_ID: String = ""
    var PARTNER_ID: String = ""
    var ENTRY_ID: String = ""
    var KS: String = ""
    var izsession: String = ""
    var adLink: String = ""
    var mLocation: PlaybackLocation = PlaybackLocation.LOCAL


    private var mCastStateListener: CastStateListener? = null
    private var mCastProvider: KCastProviderV3Impl? = null


    private var mSensorStateChanges: SensorStateChangeActions? = null
    private var sensorEvent: OrientationEventListener? = null
    private val mMediaRouteButton: MediaRouteButton? = null

    var mSelectedMedia: MediaBrowserCompat.MediaItem? = null

    enum class PlaybackLocation {
        LOCAL, REMOTE
    }

    enum class PlaybackState {
        PLAYING, PAUSED, BUFFERING, IDLE
    }

    enum class SensorStateChangeActions {
        WATCH_FOR_LANDSCAPE_CHANGES, SWITCH_FROM_LANDSCAPE_TO_STANDARD, WATCH_FOR_POTRAIT_CHANGES, SWITCH_FROM_POTRAIT_TO_STANDARD
    }

    lateinit var mCastContext: CastContext
    lateinit var mediaRouteMenuItem: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kotlin)

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



getCast()
        initPlayer()
    }

//    override fun onDestroy() {
//        if (mPlayerView != null) {
//            if (mPlayerView!!.mediaControl != null) {
//                if (mPlayerView!!.mediaControl.isPlaying) {
//                    mPlayerView!!.mediaControl.pause()
//                }
//            }
//            mPlayerView!!.removePlayer()
//        }
//        super.onDestroy()
//    }


private fun updatePlaybackLocation(location: KotlinActivity.PlaybackLocation) {
    mLocation = location
//    if (location == PlaybackLocation.LOCAL) {
//        if (mPlaybackState == PlaybackState.PLAYING
//                || mPlaybackState == PlaybackState.BUFFERING) {
//            setCoverArtStatus(null)
//            startControllersTimer()
//        } else {
//            stopControllersTimer()
//            setCoverArtStatus(mSelectedMedia.getImage(0))
//        }
//    } else {
////        stopControllersTimer()
////        setCoverArtStatus(mSelectedMedia.getImage(0))
////        updateControllersVisibility(false)
//    }
}

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu, menu)
        mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(applicationContext, menu, R.id.cast_menu_item)
        return true
    }

    private fun initPlayer(): PlayerViewController? {
        if (mPlayerView == null) {
            mPlayerView = findViewById(R.id.player)
            if (mPlayerView != null) {
                mPlayerView!!.loadPlayerIntoActivity(this)
                if (!SERVICE_URL.startsWith("http", true)) {
                    SERVICE_URL = "http://" + SERVICE_URL
                }

                val config = KPPlayerConfig(SERVICE_URL, UI_CONF_ID, PARTNER_ID)
                config.entryId = ENTRY_ID

                if (KS.length > 0) config.ks = KS
                if (izsession.length > 0) config.addConfig("izsession", izsession)

                if (adLink.length > 0) {
                    config.addConfig("doubleClick.plugin", "true")
                    config.addConfig("doubleClick.leadWithFlash", "false")
                    config.addConfig("doubleClick.adTagUrl", adLink)
                } else {
                    config.addConfig("doubleClick.plugin", "false")
                    config.addConfig("doubleClick.leadWithFlash", "false")
                    config.addConfig("doubleClick.adTagUrl", null)
                }


                // Set your flashvars here
                config.addConfig("chromecast.receiverLogo", "true")
//                config.addConfig("fullScreenBtn.plugin", "true")

                mPlayerView!!.initWithConfiguration(config)

                mPlayerView!!.setOnKPErrorEventListener(this)
                mPlayerView!!.setOnKPPlayheadUpdateEventListener(this)
                mPlayerView!!.setOnKPFullScreenToggledEventListener(this)
                mPlayerView!!.setOnKPStateChangedEventListener(this)

//                var eventListener = PlayerViewController.EventListener { eventName, params -> Toast.makeText(this, "KPlayerEvent: " + eventName, Toast.LENGTH_LONG) }
//                mPlayerView!!.addKPlayerEventListener("onEnableKeyboardBinding", "eventID", eventListener)
//                mPlayerView!!.addKPlayerEventListener("firstPlay", "id", eventListener)

                LogUtils.enableDebugMode()
                LogUtils.enableWebViewDebugMode()
                WebView.setWebContentsDebuggingEnabled(true)
            }
        }

        return mPlayerView
    }



    private fun getCast() {
        mCastProvider?.removeCastStateListener(mCastStateListener)
        mCastStateListener = CastStateListener { i ->
            Log.d(TAG, "onCastStateChanged: $i")
            when (i) {
                CastState.NO_DEVICES_AVAILABLE -> {
                    Log.d(TAG, "onCastStateChanged: NO DEVICES AVAILABLE!")
                    initPlayer()?.sendNotification("chromecastDeviceDisConnected", null)
                    hideControlsOnPlay(true)
                }
                CastState.CONNECTING -> {
                    Log.d(TAG, "onCastStateChanged: CONNECTING...")
                }
                CastState.CONNECTED -> {
                    Log.d(TAG, "onCastStateChanged: CONNECTED!")
                    mCastProvider?.let {
                        mPlayerView?.setActivity(this)
                        mPlayerView?.castProvider = it // AUTO PLAY AFTER CHROMECAST CONNECTION
                    }
                    hideControlsOnPlay(false)
                }
                CastState.NOT_CONNECTED -> {
                    Log.d(TAG, "onCastStateChanged: NOT CONNECTED!")
                    initPlayer()?.sendNotification("chromecastDeviceDisConnected", null)
                    hideControlsOnPlay(true)
                }
            }
        }
        if (isGooglePlayServicesAvailable(this)) {
            try {
                mCastProvider = KCastFactory.createCastProvider(this, CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID, "") as KCastProviderV3Impl
                mCastProvider?.addCastStateListener(mCastStateListener)
                mCastProvider?.setKCastProviderListener(object : KCastProviderListener {
                    override fun onCastMediaRemoteControlReady(castMediaRemoteControl: KCastMediaRemoteControl) {
                        Log.d(TAG, "onCastMediaRemoteControlReady: " + castMediaRemoteControl.hasMediaSession(false))
                        if (mCastProvider == null) return
                        if (mCastProvider!!.getCastMediaRemoteControl() == null) return
                        mCastProvider!!.getCastMediaRemoteControl().addListener(object : KCastMediaRemoteControlListener {
                            override fun onCastMediaProgressUpdate(currentPosition: Long) {
                                Log.d(TAG, "onCastMediaProgressUpdate: $currentPosition")
                            }

                            override fun onCastMediaStateChanged(state: KCastMediaRemoteControl.State) {
                                Log.d(TAG, "onCastMediaStateChanged: $state")
                            }

                            override fun onTextTrackSwitch(trackIndex: Int) {}
                            override fun onError(errorMessage: String, e: Exception) {}
                        })
                    }

                    override fun onCastReceiverError(errorMsg: String, errorCode: Int) {
                        Log.d(TAG, "onCastReceiverError: $errorCode : $errorMsg")
                    }

                    override fun onCastReceiverAdOpen() {
                        Log.d(TAG, "onCastReceiverAdOpen !")
                    }

                    override fun onCastReceiverAdComplete() {
                        Log.d(TAG, "onCastReceiverAdComplete !")
                    }
                })
            } catch (e: Exception) {
                Log.d(TAG, "getCast: Error creating CastProvider of KCastFactory")
            }
        } else {
            Log.d(TAG, "getCast: Google Play Services not available or need to be updated!")
        }
    }

    private fun stopCast() {
        mCastProvider?.let {mCastProvider ->
            if (mCastProvider.getCastMediaRemoteControl() != null) {
                if (mCastProvider.getCastMediaRemoteControl().isPlaying()) {
                    mCastProvider.getCastMediaRemoteControl().pause()
                    mCastProvider.disconnectFromCastDevice()
                }
            }
        }
        disconnect()
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun disconnect() {
        val mMediaRouter = getSystemService(MEDIA_ROUTER_SERVICE) as MediaRouter
        mMediaRouter.selectRoute(MediaRouter.ROUTE_TYPE_LIVE_VIDEO, mMediaRouter.defaultRoute)
    }

    private fun hideControlsOnPlay(hide: Boolean) {
        val pConfig = mPlayerView!!.config
        //        pConfig.setHideControlsOnPlay(hide);
        pConfig.addConfig("controlBarContainer.hover", if (hide) "true" else "false")
        mPlayerView!!.changeConfiguration(pConfig)
    }

    override fun onDestroy() {
        stopCast()
        if (mPlayerView != null) {
            if (mPlayerView!!.mediaControl != null) {
                if (mPlayerView!!.mediaControl.isPlaying) {
                    mPlayerView!!.mediaControl.pause()
                }
            }
            mPlayerView!!.removePlayer()
        }
        super.onDestroy()
    }

    override fun onKPlayerError(playerViewController: PlayerViewController?, error: KPError?) {}

    override fun onKPlayerPlayheadUpdate(playerViewController: PlayerViewController?, currentTimeMilliSeconds: Long) {}

    override fun onKPlayerStateChanged(playerViewController: PlayerViewController?, state: KPlayerState?) {}

    override fun onKPlayerFullScreenToggled(playerViewController: PlayerViewController?, isFullscreen: Boolean) {
        if (isFullscreen) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            mSensorStateChanges = SensorStateChangeActions.WATCH_FOR_LANDSCAPE_CHANGES
            if (null == sensorEvent) initialiseSensor(true) else sensorEvent?.enable()
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            mSensorStateChanges = SensorStateChangeActions.WATCH_FOR_POTRAIT_CHANGES
            if (null == sensorEvent) initialiseSensor(true) else sensorEvent?.enable()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "onConfigurationChanged: $newConfig")
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
            mPlayerView!!.sendNotification("onOpenFullScreen", null)
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE
            supportActionBar!!.show()
            val set = ConstraintSet()
            val playerContainer = findViewById<ConstraintLayout>(R.id.playerContainer)
            set.clone(playerContainer)
            set.setDimensionRatio(R.id.player, "16:9")
            set.applyTo(playerContainer)
            mPlayerView!!.sendNotification("onCloseFullScreen", null)
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
        Log.d(TAG, "calculateAspectRatio landscape " + screenWidth + "x" + screenHeight + " : " + ratio)
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
                if (null != mSensorStateChanges && mSensorStateChanges == SensorStateChangeActions.WATCH_FOR_LANDSCAPE_CHANGES && (orientation >= 60 && orientation <= 120 || orientation >= 240 && orientation <= 300)) {
                    mSensorStateChanges = SensorStateChangeActions.SWITCH_FROM_LANDSCAPE_TO_STANDARD
                } else if (null != mSensorStateChanges && mSensorStateChanges == SensorStateChangeActions.SWITCH_FROM_LANDSCAPE_TO_STANDARD && (orientation <= 40 || orientation >= 320)) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    mSensorStateChanges = null
                    sensorEvent?.disable()
                } else if (null != mSensorStateChanges && mSensorStateChanges == SensorStateChangeActions.WATCH_FOR_POTRAIT_CHANGES && (orientation >= 300 && orientation <= 359 || orientation >= 0 && orientation <= 45)) {
                    mSensorStateChanges = SensorStateChangeActions.SWITCH_FROM_POTRAIT_TO_STANDARD
                } else if (null != mSensorStateChanges && mSensorStateChanges == SensorStateChangeActions.SWITCH_FROM_POTRAIT_TO_STANDARD && (orientation <= 300 && orientation >= 240 || orientation <= 130 && orientation >= 60)) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    mSensorStateChanges = null
                    sensorEvent?.disable()
                }
            }
        }
        if (enable) sensorEvent?.enable()
    }

    // Casting


    // Casting
    private fun isGooglePlayServicesAvailable(activity: Activity?): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val status = googleApiAvailability.isGooglePlayServicesAvailable(activity)
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(activity, status, 2404).show()
            }
            Log.d(TAG, "isGooglePlayServicesAvailable: FALSE")
            return false
        }
        Log.d(TAG, "isGooglePlayServicesAvailable: TRUE")
        return true
    }

    fun isCastContextAvailable(context: Context?): Boolean {
        var castContext: CastContext?
        try {
            castContext = CastContext.getSharedInstance(context!!)
            Log.d(TAG, "isCastContextAvailable: TRUE")
        } catch (e: Exception) {
            castContext = null
            Log.d(TAG, "isCastContextAvailable: FALSE")
        }
        return castContext != null
    }

}
