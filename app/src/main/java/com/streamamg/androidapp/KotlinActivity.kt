package com.streamamg.androidapp

import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kaltura.playersdk.KPPlayerConfig
import com.kaltura.playersdk.PlayerViewController
import com.kaltura.playersdk.events.*
import com.kaltura.playersdk.types.KPError
import com.kaltura.playersdk.utils.LogUtils

class KotlinActivity : AppCompatActivity(), KPErrorEventListener, KPPlayheadUpdateEventListener, KPFullScreenToggledEventListener, KPStateChangedEventListener {

    var mPlayerView: PlayerViewController? = null
    var SERVICE_URL: String = ""
    var UI_CONF_ID: String = ""
    var PARTNER_ID: String = ""
    var ENTRY_ID: String = ""
    var KS: String = ""
    var izsession: String = ""
    var adLink: String = ""

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

        initPlayer()
    }

    override fun onDestroy() {
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

    private fun initPlayer(): PlayerViewController? {
        if (mPlayerView == null) {
            mPlayerView = findViewById(R.id.player)
            if (mPlayerView != null) {
                mPlayerView!!.loadPlayerIntoActivity(this)
                if (!SERVICE_URL.startsWith("http",true)) {
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
//                config.addConfig("chromecast.receiverLogo", "true")
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

    override fun onKPlayerStateChanged(playerViewController: PlayerViewController?, state: KPlayerState?) {
        Toast.makeText(this, "onKPlayerStateChanged: " + state.toString(), Toast.LENGTH_LONG).show()
    }

    override fun onKPlayerFullScreenToggled(playerViewController: PlayerViewController?, isFullscreen: Boolean) {
        Toast.makeText(this, "onKPlayerFullScreenToggled: " + if(isFullscreen) "fullscreen" else "NO fullscreen", Toast.LENGTH_LONG).show()
    }

    override fun onKPlayerPlayheadUpdate(playerViewController: PlayerViewController?, currentTimeMilliSeconds: Long) {
        Toast.makeText(this, "onKPlayerPlayheadUpdate: " + currentTimeMilliSeconds, Toast.LENGTH_LONG).show()
    }

    override fun onKPlayerError(playerViewController: PlayerViewController?, error: KPError?) {
        Toast.makeText(this, "onKPlayerError: " + error.toString(), Toast.LENGTH_LONG).show()
    }
}
