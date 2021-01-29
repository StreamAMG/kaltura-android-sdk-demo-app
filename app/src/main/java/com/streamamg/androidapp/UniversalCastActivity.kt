package com.streamamg.androidapp

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.connectsdk.core.MediaInfo
import com.connectsdk.device.ConnectableDevice
import com.connectsdk.device.ConnectableDeviceListener
import com.connectsdk.device.DevicePicker
import com.connectsdk.discovery.DiscoveryManager
import com.connectsdk.discovery.DiscoveryManagerListener
import com.connectsdk.discovery.provider.CastDiscoveryProvider
import com.connectsdk.discovery.provider.FireTVDiscoveryProvider
import com.connectsdk.discovery.provider.SSDPDiscoveryProvider
import com.connectsdk.service.CastService
import com.connectsdk.service.DeviceService
import com.connectsdk.service.FireTVService
import com.connectsdk.service.RokuService
import com.connectsdk.service.capability.MediaPlayer
import com.connectsdk.service.command.ServiceCommandError
import com.kaltura.playersdk.KPPlayerConfig
import com.kaltura.playersdk.PlayerViewController
import com.kaltura.playersdk.events.*
import com.kaltura.playersdk.types.KPError
import com.kaltura.playersdk.utils.LogUtils
import kotlinx.android.synthetic.main.activity_universalcast.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

class UniversalCastActivity : AppCompatActivity(), KPErrorEventListener, KPPlayheadUpdateEventListener, KPFullScreenToggledEventListener, KPStateChangedEventListener, ConnectableDeviceListener, DiscoveryManagerListener {

    private val TAG = this::class.java.simpleName
    var mPlayerView: PlayerViewController? = null
    var SERVICE_URL: String = ""
    var UI_CONF_ID: String = ""
    var PARTNER_ID: String = ""
    var ENTRY_ID: String = ""
    var KS: String = ""
    var izsession: String = ""
    var adLink: String = "";

    private var discoveryManager: DiscoveryManager? = null
    private var mediaPlayer: MediaPlayer? = null
    private var device: ConnectableDevice? = null
    private var castIcon: MenuItem? = null

    private var listLang: HashMap<String, String>? = null
    private var selectedLang: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_universalcast)

        SERVICE_URL = intent.getStringExtra("SERVICE_URL")
        UI_CONF_ID = intent.getStringExtra("UI_CONF_ID")
        PARTNER_ID = intent.getStringExtra("PARTNER_ID")
        ENTRY_ID = intent.getStringExtra("ENTRY_ID")
        KS = intent.getStringExtra("KS")
        izsession = intent.getStringExtra("IZsession")
        adLink = intent.getStringExtra("AdLink");

        window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)

        initPlayer()

        val actionBar: ActionBar? = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.setHomeButtonEnabled(true)
        actionBar?.setDisplayShowHomeEnabled(true)

        listLanguages.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(adapter: AdapterView<*>?) {
                selectedLang = null
            }

            override fun onItemSelected(adapter: AdapterView<*>?, arg1: View?, position: Int, id: Long) {
                selectedLang = listLang!![listLanguages.selectedItem]
            }
        }

        initUniversalCast()
    }

    private fun initUniversalCast() {

        if (discoveryManager == null) {
            discoveryManager = DiscoveryManager.getInstance()
            discoveryManager?.registerDeviceService(RokuService::class.java, SSDPDiscoveryProvider::class.java)
            discoveryManager?.registerDeviceService(CastService::class.java, CastDiscoveryProvider::class.java)
            discoveryManager?.registerDeviceService(FireTVService::class.java, FireTVDiscoveryProvider::class.java)
            DiscoveryManager.getInstance().pairingLevel = DiscoveryManager.PairingLevel.ON

            CastService.setApplicationID(getString(R.string.app_id))

            discoveryManager?.start()

            discoveryManager?.addListener(this)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.cast, menu)
        castIcon = menu?.findItem(R.id.action_cast)
        castIcon?.isVisible = getCastDevices().isNotEmpty()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return if (id == R.id.action_cast) {
            showCast()
            true
        } else super.onOptionsItemSelected(item)
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
        if (device != null) {
            stopMedia()
            disconnectDevice()
        }

        castIcon?.isVisible = false
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

                mPlayerView!!.addKPlayerEventListener("audioTracksReceived", "audioTracksReceived") { eventName, params ->
                    val lang = JSONObject(params)
                    val lang_array = lang.get("languages") as JSONArray
                    listLang = hashMapOf()
                    if (lang_array.length() > 1) {
                        listLanguages.visibility = View.VISIBLE
                    }
                    for (i in 0 until lang_array.length()) {
                        val item = lang_array.getJSONObject(i)
                        if (item.has("language")) {
                            val trackLang: String = item.get("language").toString()
                            val trackTitle: String = item.get("title").toString()

                            listLang?.set(trackTitle, trackLang)
                        }
                    }

                    val listAdapter = mutableListOf<String>()
                    listLang?.forEach { (key, value) -> listAdapter.add(key) }
                    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listAdapter)
                    listLanguages.adapter = adapter
                }

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

    private fun showCast() {
        if (device?.isConnected == true) {
            stopMedia()
            disconnectDevice()
        } else {
            val devicePicker = DevicePicker(this)
            val dialog: AlertDialog = devicePicker.getPickerDialog("Select device") { adapterView, parent, position, id ->
                device = adapterView.getItemAtPosition(position) as ConnectableDevice?
                device?.addListener(this)
                device?.connect()
            }
            dialog.show()
        }
    }

    private fun playMedia() {
        var url = "${SERVICE_URL}/p/${PARTNER_ID}/sp/${PARTNER_ID}00/playManifest/entryId/${ENTRY_ID}/format/applehttp/protocol/http/a.m3u8?ks=${KS}"
        Log.d("PTOLog", "Cast URL = $url")
        var mediaMimeType = "video/x-mpegurl" // Works on FireTV, Chromecast and Roku
//        mediaMimeType = "application/x-mpegurl" // Works on FireTV and Chromecast (NO Roku)
        mediaMimeType = "video/mp2"

        val mediaInfo = MediaInfo.Builder(url, mediaMimeType)
            .setTitle("Title")
            .setLanguage(selectedLang ?: "")
            .build()

        if (selectedLang != null) {
            mediaInfo.language = selectedLang
        }

        // Play media
        device?.mediaPlayer?.playMedia(mediaInfo, false, object : MediaPlayer.LaunchListener {
            override fun onSuccess(media: MediaPlayer.MediaLaunchObject) {

            }

            override fun onError(error: ServiceCommandError) {
                // Errors
            }
        })
    }

    private fun stopMedia() {
        if (device?.isConnected == true) {
            mediaPlayer?.closeMedia(null, null)
        }
    }

    private fun disconnectDevice() {
        device?.removeListener(this)
        device?.disconnect()
        device = null
        castingIcon(false)
    }

    fun getCastDevices(): List<ConnectableDevice> {
        val castDevices = ArrayList<ConnectableDevice>()

        var casting = false
        for (device in DiscoveryManager.getInstance().compatibleDevices.values) {
            castDevices.add(device)
            if (device.isConnected) {
                casting = true
            }
        }
        castingIcon(casting)

        return castDevices
    }

    private fun castingIcon(connected: Boolean) {
        castIcon?.setIcon(if (connected) R.drawable.ic_casting else R.drawable.ic_cast)
        listLanguages.isEnabled = !connected
    }

    override fun onKPlayerStateChanged(playerViewController: PlayerViewController?, state: KPlayerState?) {

    }

    override fun onKPlayerFullScreenToggled(playerViewController: PlayerViewController?, isFullscreen: Boolean) {

    }

    override fun onKPlayerPlayheadUpdate(playerViewController: PlayerViewController?, currentTimeMilliSeconds: Long) {

    }

    override fun onKPlayerError(playerViewController: PlayerViewController?, error: KPError?) {

    }

    override fun onDeviceDisconnected(device: ConnectableDevice?) {

    }

    override fun onDeviceReady(device: ConnectableDevice?) {
        mediaPlayer = device?.getCapability(com.connectsdk.service.capability.MediaPlayer::class.java)
        if (mediaPlayer == null) {
            mediaPlayer = device?.mediaPlayer
        }

        playMedia()
        getCastDevices()
    }

    override fun onPairingRequired(device: ConnectableDevice?, p1: DeviceService?, p2: DeviceService.PairingType?) {

    }

    override fun onCapabilityUpdated(device: ConnectableDevice?, p1: MutableList<String>?, p2: MutableList<String>?) {

    }

    override fun onConnectionFailed(device: ConnectableDevice?, p1: ServiceCommandError?) {

    }

    override fun onDeviceRemoved(p0: DiscoveryManager?, p1: ConnectableDevice?) {
        castIcon?.isVisible = getCastDevices().isNotEmpty()
    }

    override fun onDeviceAdded(p0: DiscoveryManager?, p1: ConnectableDevice?) {
        castIcon?.isVisible = getCastDevices().isNotEmpty()
    }

    override fun onDeviceUpdated(p0: DiscoveryManager?, p1: ConnectableDevice?) {

    }

    override fun onDiscoveryFailed(p0: DiscoveryManager?, p1: ServiceCommandError?) {

    }
}
