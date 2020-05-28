package com.streamamg.androidapp;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.SensorManager;
import android.media.MediaRouter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.connectsdk.discovery.DiscoveryManager;
import com.kaltura.playersdk.KPPlayerConfig;
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.events.KPErrorEventListener;
import com.kaltura.playersdk.events.KPFullScreenToggledEventListener;
import com.kaltura.playersdk.events.KPPlayheadUpdateEventListener;
import com.kaltura.playersdk.events.KPStateChangedEventListener;
import com.kaltura.playersdk.events.KPlayerState;
import com.kaltura.playersdk.types.KPError;
import com.kaltura.playersdk.utils.LogUtils;

import java.io.File;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class MainActivity  extends AppCompatActivity implements KPErrorEventListener, KPPlayheadUpdateEventListener, KPStateChangedEventListener, KPFullScreenToggledEventListener {

    private static final String TAG = "MainActivity";
    public String SERVICE_URL = "http://{your_mp}/";
    public String PARTNER_ID = "{partner_id}";
    public String UI_CONF_ID = "{ui_conf_id}";
    public String ENTRY_ID = "{entry_id}";
    public String KS = "";
    public String izsession = "";

    public boolean saveSettings = false;

    private View popupOptions;
    private PopupWindow popupWindow;

    private PlayerViewController mPlayerView;

    private SensorStateChangeActions mSensorStateChanges;
    private OrientationEventListener sensorEvent;
    private enum SensorStateChangeActions {
        WATCH_FOR_LANDSCAPE_CHANGES, SWITCH_FROM_LANDSCAPE_TO_STANDARD, WATCH_FOR_POTRAIT_CHANGES, SWITCH_FROM_POTRAIT_TO_STANDARD;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        File dir = new File(getCacheDir().getAbsolutePath());
        if (dir.exists()) {
            for (File f : dir.listFiles()) {
                Log.d(TAG, "FILE: " + f.getName());
            }
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String check = preferences.getString("SERVICE_URL", "");
        if ((!check.equalsIgnoreCase("") && saveSettings) || savedInstanceState != null)
        {
            SERVICE_URL = preferences.getString("SERVICE_URL", "");
            PARTNER_ID = preferences.getString("PARTNER_ID", "");
            UI_CONF_ID = preferences.getString("UI_CONF_ID", "");
            ENTRY_ID = preferences.getString("ENTRY_ID", "");
            KS = preferences.getString("KS", "");
            izsession = preferences.getString("IZsession", "");
        }

        Button btnFragment = findViewById(R.id.btnFragment);
        btnFragment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showOptions(true);
            }
        });

        Button btnWebview = findViewById(R.id.btnWebview);
        btnWebview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, WebviewActivity.class);
                String url = SERVICE_URL + "/index.php/extwidget/preview/partner_id/" + PARTNER_ID + "/uiconf_id/" + UI_CONF_ID + "/entry_id/" + ENTRY_ID + "/embed/auto?flashvars[streamerType]=auto";
//                intent.putExtra("URL", getPlayer().getVideoUrl());
                intent.putExtra("URL", url);
                startActivity(intent);
            }
        });

        Button btnBrowser = findViewById(R.id.btnBrowser);
        btnBrowser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = SERVICE_URL + "/index.php/extwidget/preview/partner_id/" + PARTNER_ID + "/uiconf_id/" + UI_CONF_ID + "/entry_id/" + ENTRY_ID + "/embed/auto?flashvars[streamerType]=auto";
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            }
        });

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        DiscoveryManager.init(getApplicationContext());

        getPlayer();

        Button btnFullscreenLandscape = findViewById(R.id.btnFullscreenLandscape);
        btnFullscreenLandscape.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Configuration config = getResources().getConfiguration();
                setRequestedOrientation((config.orientation == ORIENTATION_PORTRAIT) ? SCREEN_ORIENTATION_LANDSCAPE : SCREEN_ORIENTATION_PORTRAIT);
                findViewById(R.id.btnNormalScreen).setVisibility(View.VISIBLE);
            }
        });

        Button btnFullscreenPortrait = findViewById(R.id.btnFullscreenPortrait);
        btnFullscreenPortrait.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ViewGroup.LayoutParams params = findViewById(R.id.playerContainer).getLayoutParams();
                params.height = MATCH_PARENT;
                findViewById(R.id.playerContainer).setLayoutParams(params);
                toggleFullscreen(true);
                findViewById(R.id.btnNormalScreen).setVisibility(View.VISIBLE);
            }
        });

        ImageButton btnNormalScreen = findViewById(R.id.btnNormalScreen);
        btnNormalScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Configuration config = getResources().getConfiguration();
                ViewGroup.LayoutParams params = findViewById(R.id.playerContainer).getLayoutParams();
                params.height = WRAP_CONTENT;
                findViewById(R.id.playerContainer).setLayoutParams(params);
                if (config.orientation == ORIENTATION_PORTRAIT) {
                    toggleFullscreen(false);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                }

                findViewById(R.id.btnNormalScreen).setVisibility(View.INVISIBLE);
            }
        });

        Button btnKotlin = findViewById(R.id.useKotlin);
        btnKotlin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, KotlinActivity.class);

                intent.putExtra("SERVICE_URL", SERVICE_URL);
                intent.putExtra("UI_CONF_ID", UI_CONF_ID);
                intent.putExtra("PARTNER_ID", PARTNER_ID);
                intent.putExtra("ENTRY_ID", ENTRY_ID);
                intent.putExtra("KS", KS);
                intent.putExtra("IZsession", izsession);

                startActivity(intent);
            }
        });

        Button btnPlay = findViewById(R.id.btnPlay);
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPlayerView != null) {
                    if (mPlayerView.getMediaControl() != null) {
                        if (mPlayerView.getMediaControl().isPlaying()) {
                            mPlayerView.getMediaControl().pause();
                        } else {
                            mPlayerView.getMediaControl().start();
                        }
                    }
                }
            }
        });

        Button btnUniversalCast = findViewById(R.id.btnUniversalCast);
        btnUniversalCast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, UniversalCastActivity.class);

                intent.putExtra("SERVICE_URL", SERVICE_URL);
                intent.putExtra("UI_CONF_ID", UI_CONF_ID);
                intent.putExtra("PARTNER_ID", PARTNER_ID);
                intent.putExtra("ENTRY_ID", ENTRY_ID);
                intent.putExtra("KS", KS);
                intent.putExtra("IZsession", izsession);

                startActivity(intent);
            }
        });
    }

    public void openFragmentPlayer() {

        if (mPlayerView != null) {
            if (mPlayerView.getMediaControl() != null) {
                if (mPlayerView.getMediaControl().isPlaying()) {
                    mPlayerView.getMediaControl().pause();
                }
            }
        }

        Intent intent = new Intent(MainActivity.this, FragmentActivity.class);
        startActivity(intent);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.options_menu_item) {
            if (popupWindow != null) {
                popupWindow.dismiss();
                popupWindow = null;
            } else {
                showOptions(false);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showOptions(final boolean openFragment) {

        // inflate your layout
        popupOptions = getLayoutInflater().inflate(R.layout.fragment_change_config, null);

        ((EditText)popupOptions.findViewById(R.id.txtSERVICE_URL)).setText(SERVICE_URL);
        ((EditText)popupOptions.findViewById(R.id.txtUI_CONF_ID)).setText(UI_CONF_ID);
        ((EditText)popupOptions.findViewById(R.id.txtPARTNER_ID)).setText(PARTNER_ID);
        ((EditText)popupOptions.findViewById(R.id.txtENTRY_ID)).setText(ENTRY_ID);
        ((EditText)popupOptions.findViewById(R.id.txtKS)).setText(KS);
        ((EditText)popupOptions.findViewById(R.id.txtIZsession)).setText(izsession);

        // Create the popup window; decide on the layout parameters
        popupWindow = new PopupWindow(popupOptions, MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

        // find and initialize your TextView(s), EditText(s) and Button(s); setup their behavior
        Button btnSave = popupOptions.findViewById(R.id.btnSaveTest);
        if (openFragment) {
            if (saveSettings) {
                btnSave.setText("Save and Open fragment");
            } else {
                btnSave.setText("Open fragment");
            }
        } else {
            if (saveSettings) {
                btnSave.setText("Save and Test");
            } else {
                btnSave.setText("Test");
            }
        }
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
                popupWindow = null;

                SERVICE_URL = ((EditText)popupOptions.findViewById(R.id.txtSERVICE_URL)).getText().toString();
                UI_CONF_ID = ((EditText)popupOptions.findViewById(R.id.txtUI_CONF_ID)).getText().toString();
                PARTNER_ID = ((EditText)popupOptions.findViewById(R.id.txtPARTNER_ID)).getText().toString();
                ENTRY_ID = ((EditText)popupOptions.findViewById(R.id.txtENTRY_ID)).getText().toString();
                KS = ((EditText)popupOptions.findViewById(R.id.txtKS)).getText().toString();
                izsession = ((EditText)popupOptions.findViewById(R.id.txtIZsession)).getText().toString();

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("SERVICE_URL", SERVICE_URL);
                editor.putString("UI_CONF_ID", UI_CONF_ID);
                editor.putString("PARTNER_ID", PARTNER_ID);
                editor.putString("ENTRY_ID", ENTRY_ID);
                editor.putString("KS", KS);
                editor.putString("IZsession", izsession);
                editor.apply();

                if (openFragment) {
                    openFragmentPlayer();
                } else {
                    recreate();
                }
            }
        });

        // display your popup window
        popupWindow.showAtLocation(popupOptions, Gravity.CENTER, 0, 0);
    }

    private PlayerViewController getPlayer() {
        if (mPlayerView == null) {
            mPlayerView = (PlayerViewController)findViewById(R.id.player);
            if (mPlayerView != null) {
                mPlayerView.loadPlayerIntoActivity(this);

                if (!SERVICE_URL.startsWith("http")) {
                    SERVICE_URL = "http://" + SERVICE_URL;
                }

                KPPlayerConfig config = new KPPlayerConfig(SERVICE_URL, UI_CONF_ID, PARTNER_ID).setEntryId(ENTRY_ID);

                if (KS.length() > 0) {
                    config.setKS(KS);
                }
                if (izsession.length() > 0) {
                    config.addConfig("izsession", izsession);
                }

                // Set your flashvars here
                config.addConfig("chromecast.receiverLogo", "true");
                config.addConfig("fullScreenBtn.plugin", "false");

                mPlayerView.initWithConfiguration(config);

                mPlayerView.setOnKPErrorEventListener(this);
                mPlayerView.setOnKPPlayheadUpdateEventListener(this);
                mPlayerView.setOnKPFullScreenToggledEventListener(this);
                mPlayerView.setOnKPStateChangedEventListener(this);

                mPlayerView.addKPlayerEventListener("bitrateChange", "bitrateChange", new PlayerViewController.EventListener() {
                    @Override
                    public void handler(String eventName, String params) {
                        Log.d("bitrateChange", eventName + " - " + params);
                    }
                });

                mPlayerView.addKPlayerEventListener("playerReady", "playerReady", new PlayerViewController.EventListener() {
                    @Override
                    public void handler(String eventName, String params) {
                        Log.d("playerReady", eventName + " - " + params);
                    }
                });

                LogUtils.enableDebugMode();
                LogUtils.enableWebViewDebugMode();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    WebView.setWebContentsDebuggingEnabled(true);
                }
            }

        }
        return mPlayerView;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void disconnect(){
        MediaRouter mMediaRouter = (MediaRouter) getSystemService(Context.MEDIA_ROUTER_SERVICE);
        mMediaRouter.selectRoute(MediaRouter.ROUTE_TYPE_LIVE_VIDEO, mMediaRouter.getDefaultRoute());
    }

    private void hideControlsOnPlay(boolean hide) {
        KPPlayerConfig pConfig = mPlayerView.getConfig();
//        pConfig.setHideControlsOnPlay(hide);
        pConfig.addConfig("controlBarContainer.hover", hide ? "true" : "false");
        mPlayerView.changeConfiguration(pConfig);
    }

    @Override
    protected void onDestroy() {

        if (mPlayerView != null) {
            if (mPlayerView.getMediaControl() != null) {
                if (mPlayerView.getMediaControl().isPlaying()) {
                    mPlayerView.getMediaControl().pause();
                }
            }
            mPlayerView.removePlayer();
        }
        super.onDestroy();
    }

    @Override
    public void onKPlayerError(PlayerViewController playerViewController, KPError error) {

    }

    @Override
    public void onKPlayerPlayheadUpdate(PlayerViewController playerViewController, long currentTimeMilliSeconds) {

    }

    @Override
    public void onKPlayerStateChanged(PlayerViewController playerViewController, KPlayerState state) {

    }

    @Override
    public void onKPlayerFullScreenToggled(PlayerViewController playerViewController, boolean isFullscreen) {
        if (isFullscreen) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            mSensorStateChanges = SensorStateChangeActions.WATCH_FOR_LANDSCAPE_CHANGES;
            if (null == sensorEvent)
                initialiseSensor(true);
            else
                sensorEvent.enable();
        } else {
            setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
            mSensorStateChanges = SensorStateChangeActions.WATCH_FOR_POTRAIT_CHANGES;
            if (null == sensorEvent)
                initialiseSensor(true);
            else
                sensorEvent.enable();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Log.d(TAG, "onConfigurationChanged: " + newConfig.toString());

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            toggleFullscreen(true);
        } else {
            toggleFullscreen(false);
        }
    }

    private void toggleFullscreen(final boolean isFullscreen)
    {
        if (isFullscreen) {
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            getWindow().getDecorView().setSystemUiVisibility(uiOptions);
            getSupportActionBar().hide();

            ConstraintSet set = new ConstraintSet();
            ConstraintLayout playerContainer = findViewById(R.id.playerContainer);
            set.clone(playerContainer);
            set.setDimensionRatio(R.id.player, calculateAspectRatio());
            set.applyTo(playerContainer);
            mPlayerView.sendNotification("onOpenFullScreen", null);

        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE);
            getSupportActionBar().show();

            ConstraintSet set = new ConstraintSet();
            ConstraintLayout playerContainer = findViewById(R.id.playerContainer);
            set.clone(playerContainer);
            set.setDimensionRatio(R.id.player, "16:9");
            set.applyTo(playerContainer);
            mPlayerView.sendNotification("onCloseFullScreen", null);
        }
    }

    private String calculateAspectRatio() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        wm.getDefaultDisplay().getRealSize(size);
        int screenWidth = Math.max(size.x, size.y);
        int screenHeight = Math.min(size.x, size.y);

        int factor = greatestCommonFactor(screenWidth, screenHeight);
        int widthRatio = screenWidth / factor;
        int heightRatio = screenHeight / factor;

        String ratio = widthRatio + ":" + heightRatio;

        Log.d(TAG, "calculateAspectRatio landscape " + screenWidth + "x" + screenHeight + " : " + ratio);

        return ratio;
    }

    private int greatestCommonFactor(int width, int height) {
        return (height == 0) ? width : greatestCommonFactor(height, width % height);
    }

    /**
     * Initialises system sensor to detect device orientation for player changes.
     * Don't enable sensor until playback starts on player
     *
     * @param enable if set, sensor will be enabled.
     */
    private void initialiseSensor(boolean enable) {
        sensorEvent = new OrientationEventListener(this,
                SensorManager.SENSOR_DELAY_NORMAL) {

            @Override
            public void onOrientationChanged(int orientation) {
                /*
                 * This logic is useful when user explicitly changes orientation using player controls, in which case orientation changes gives no callbacks.
                 * we use sensor angle to anticipate orientation and make changes accordingly.
                 */
                if (null != mSensorStateChanges
                        && mSensorStateChanges == SensorStateChangeActions.WATCH_FOR_LANDSCAPE_CHANGES
                        && ((orientation >= 60 && orientation <= 120) || (orientation >= 240 && orientation <= 300))) {
                    mSensorStateChanges = SensorStateChangeActions.SWITCH_FROM_LANDSCAPE_TO_STANDARD;
                } else if (null != mSensorStateChanges
                        && mSensorStateChanges == SensorStateChangeActions.SWITCH_FROM_LANDSCAPE_TO_STANDARD
                        && (orientation <= 40 || orientation >= 320)) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    mSensorStateChanges = null;
                    sensorEvent.disable();
                } else if (null != mSensorStateChanges
                        && mSensorStateChanges == SensorStateChangeActions.WATCH_FOR_POTRAIT_CHANGES
                        && ((orientation >= 300 && orientation <= 359) || (orientation >= 0 && orientation <= 45))) {
                    mSensorStateChanges = SensorStateChangeActions.SWITCH_FROM_POTRAIT_TO_STANDARD;
                } else if (null != mSensorStateChanges
                        && mSensorStateChanges == SensorStateChangeActions.SWITCH_FROM_POTRAIT_TO_STANDARD
                        && ((orientation <= 300 && orientation >= 240) || (orientation <= 130 && orientation >= 60))) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    mSensorStateChanges = null;
                    sensorEvent.disable();
                }
            }
        };
        if (enable)
            sensorEvent.enable();
    }
}
