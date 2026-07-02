package com.example.transport1;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {
	
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