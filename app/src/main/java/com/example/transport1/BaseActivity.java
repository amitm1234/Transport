package com.example.transport1;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class BaseActivity extends AppCompatActivity {

    // 🔥 Admin override UID
    public static String overrideUid = null;

    // 🔥 Common UID method (MOST IMPORTANT)
    protected String getUid(){
        if(overrideUid != null){
            return overrideUid; // Admin viewing other user
        } else {
            return FirebaseAuth.getInstance().getCurrentUser().getUid(); // normal user
        }
    }

    // 🌐 Network check (तुझा existing code)
    protected boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            Network network = cm.getActiveNetwork();
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null &&
                   (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        }
        return false;
    }

    protected boolean checkNetwork() {
        return isNetworkAvailable();
    }
}