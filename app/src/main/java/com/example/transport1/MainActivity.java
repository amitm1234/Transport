package com.example.transport1;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        Handler clickHandler = new Handler();
        final int[] clickCount = {0};

        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                clickCount[0]++;

                // 2nd click → Fake error
                if (clickCount[0] == 2) {
                    Toast.makeText(MainActivity.this,
                            "Error: Access Denied!", Toast.LENGTH_SHORT).show();
                    return true;
                }

                // 4th click → Admin Page
                if (clickCount[0] == 4) {
                    clickCount[0] = 0;
                    startActivity(new Intent(MainActivity.this, AdminActivity.class));
                    return true;
                }

                // Reset click counter after 2 sec
                clickHandler.removeCallbacksAndMessages(null);
                clickHandler.postDelayed(() -> clickCount[0] = 0, 2000);

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, new HomeFragment())
                        .commit();
                return true;
            } 
            else if (id == R.id.nav_dashboard) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, new DashboardFragment())
                        .commit();
                return true;
            } 
            else if (id == R.id.nav_profile) {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                return true;
            } 
            else if (id == R.id.nav_settings) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, new SettingsFragment())
                        .commit();
                return true;
            }

            return false;
        });

        // Default selection
        if (savedInstanceState == null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }
    }
}