package com.example.transport1;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash_screen); // Must exist in res/layout/
		
		// 3-second delay then move to LoginActivity
		new Handler().postDelayed(() -> {
			Intent i = new Intent(SplashActivity.this, LoginActivity.class);
			startActivity(i);
			finish();
		}, 3000);
	}
}