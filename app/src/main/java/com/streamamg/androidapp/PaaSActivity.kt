package com.streamamg.androidapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.kaltura.playersdk.PlayerViewController
import com.kaltura.playersdk.services.BackgroundPlayerService
import com.kaltura.playersdk.types.MediaBundle

class PaaSActivity : AppCompatActivity(){
    var myService: BackgroundPlayerService? = null
    var serviceBound = false

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
        myService?.setupPlayer(this, findViewById(R.id.player))
        var bundle = MediaBundle(SERVICE_URL, PARTNER_ID, UI_CONF_ID, ENTRY_ID, KS, izsession)
            bundle.adURL = adLink
            myService?.updateMedia(bundle)
    }

    override fun onDestroy() {
        super.onDestroy()
        val serviceIntent = Intent(this, BackgroundPlayerService::class.java)
        myService = null
        stopService(serviceIntent)
    }


    private fun initPlayer() {
        val serviceIntent = Intent(this, BackgroundPlayerService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        bindService(serviceIntent, myConnection, BIND_AUTO_CREATE);
    }
}