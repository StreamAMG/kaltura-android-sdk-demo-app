package com.streamamg.androidapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.kaltura.playersdk.KPPlayerConfig;
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.events.KPStateChangedEventListener;
import com.kaltura.playersdk.events.KPlayerState;

public class KalturaFragment extends Fragment {

    private String SERVICE_URL;
    private String PARTNER_ID;
    private String UI_CONF_ID;
    private String ENTRY_ID;
    private String KS;
    private String izsession;
    public String adLink = "";

    View mFragmentView;
    private PlayerViewController mPlayerView;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        if(mFragmentView == null) {
            mFragmentView = inflater.inflate(R.layout.kaltura_fragment, container, false);
        }

        mPlayerView = (PlayerViewController) mFragmentView.findViewById(R.id.player);
        mPlayerView.loadPlayerIntoActivity(getActivity());

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String check = preferences.getString("SERVICE_URL", "");
        if(!check.equalsIgnoreCase(""))
        {
            SERVICE_URL = preferences.getString("SERVICE_URL", "");
            PARTNER_ID = preferences.getString("PARTNER_ID", "");
            UI_CONF_ID = preferences.getString("UI_CONF_ID", "");
            ENTRY_ID = preferences.getString("ENTRY_ID", "");
            KS = preferences.getString("KS", "");
            izsession = preferences.getString("IZsession", "");
            adLink = preferences.getString("AdLink", "");
        }

        KPPlayerConfig config = new KPPlayerConfig(SERVICE_URL, UI_CONF_ID, PARTNER_ID);
        config.setEntryId(ENTRY_ID);
        if (KS.length() > 0) {
            config.setKS(KS);
        }
        if (izsession.length() > 0) {
            config.addConfig("izsession", izsession);
        }

        if (adLink.length() > 0) {
            config.addConfig("doubleClick.plugin", "true");
            config.addConfig("doubleClick.leadWithFlash", "false");
            config.addConfig("doubleClick.adTagUrl", adLink);
        } else {
            config.addConfig("doubleClick.plugin", "false");
            config.addConfig("doubleClick.leadWithFlash", "false");
            config.addConfig("doubleClick.adTagUrl", null);
        }


        mPlayerView.setOnKPStateChangedEventListener(new KPStateChangedEventListener() {
            @Override
            public void onKPlayerStateChanged(PlayerViewController playerViewController, KPlayerState state) {
                Toast.makeText(getActivity(), "onKPlayerStateChanged: " + state.toString(), Toast.LENGTH_LONG).show();
            }
        });

        mPlayerView.initWithConfiguration(config);
        return mFragmentView;
    }

}