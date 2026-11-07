package com.example.transport1;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private ImageView imgAppIcon, imgStartPin, imgDestPin, imgTruck;
    private TextView txtAppName, txtTagline;
    private View vRouteSolidProgress;

    private ValueAnimator titleTypewriterAnimator;
    private ValueAnimator truckJourneyAnimator;
    private final Handler splashHandler = new Handler(Looper.getMainLooper());
    private Runnable navigateRunnable;

    private static final String FULL_TITLE = "TRANSPORT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        View rootLayout = findViewById(R.id.rootLayout);
        imgAppIcon = findViewById(R.id.imgAppIcon);
        txtAppName = findViewById(R.id.txtAppName);
        txtTagline = findViewById(R.id.txtTagline);

        vRouteSolidProgress = findViewById(R.id.vRouteSolidProgress);
        imgStartPin = findViewById(R.id.imgStartPin);
        imgDestPin = findViewById(R.id.imgDestPin);
        imgTruck = findViewById(R.id.imgTruck);

        rootLayout.post(this::startJourneyAnimations);
    }

    private void startJourneyAnimations() {
        if (isFinishing() || isDestroyed()) return;

        // --- 0. INITIAL VISIBILITY & POSITION SETUP ---
        imgAppIcon.setAlpha(0f);
        imgAppIcon.setTranslationY(40f);

        txtAppName.setText("");
        txtTagline.setAlpha(0f);

        // --- 1. ICON SLIDE UP & FADE IN (0.0s to 0.5s) ---
        imgAppIcon.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // --- 2. "TRANSPORT" TYPEWRITER ANIMATION (0.5s to 1.0s) ---
        titleTypewriterAnimator = ValueAnimator.ofInt(0, FULL_TITLE.length());
        titleTypewriterAnimator.setStartDelay(500);
        titleTypewriterAnimator.setDuration(500);
        titleTypewriterAnimator.addUpdateListener(animation -> {
            int length = (int) animation.getAnimatedValue();
            txtAppName.setText(FULL_TITLE.substring(0, length));
        });
        titleTypewriterAnimator.start();

        // --- 3. TAGLINE FADE IN (1.0s to 1.4s) ---
        txtTagline.animate()
                .alpha(1f)
                .setStartDelay(1000)
                .setDuration(400)
                .start();

        // --- 4. TRUCK JOURNEY & PROGRESS LINE (0.5s to 2.5s) ---
        // Calculate driving distance between Start Pin and Destination Pin
        float startX = 0f; // Initial position at Start Pin
        float startPinX = imgStartPin.getX() + imgStartPin.getWidth() / 2f;
        float destPinX = imgDestPin.getX() - imgTruck.getWidth() / 2f;
        float totalDistance = Math.max(0f, destPinX - startPinX);

        truckJourneyAnimator = ValueAnimator.ofFloat(0f, 1f);
        truckJourneyAnimator.setStartDelay(500);
        truckJourneyAnimator.setDuration(2000); // 2.0s duration (0.5s -> 2.5s)
        truckJourneyAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        truckJourneyAnimator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();

            // Move truck along route line
            imgTruck.setTranslationX(startX + (fraction * totalDistance));

            // Expand solid green progress line behind the truck
            ViewGroup.LayoutParams params = vRouteSolidProgress.getLayoutParams();
            params.width = (int) (fraction * totalDistance);
            vRouteSolidProgress.setLayoutParams(params);
        });

        truckJourneyAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // --- 5. DESTINATION PIN POP / BOUNCE (At 2.5s) ---
                imgDestPin.animate()
                        .scaleX(1.35f)
                        .scaleY(1.35f)
                        .setDuration(120)
                        .withEndAction(() -> imgDestPin.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(120)
                                .start())
                        .start();
            }
        });

        truckJourneyAnimator.start();

        // --- 6. AUTO NAVIGATE AFTER JOURNEY COMPLETION (~2.75s) ---
        navigateRunnable = this::navigateNext;
        splashHandler.postDelayed(navigateRunnable, 2750);
    }

    private void navigateNext() {
        if (isFinishing() || isDestroyed()) return;

        if (navigateRunnable != null) {
            splashHandler.removeCallbacks(navigateRunnable);
        }

        /*
         * =========================================================================
         * ROUTING LOGIC:
         * Check Firebase Authentication status.
         * If user is logged in -> Navigate to MainActivity (or your Dashboard activity).
         * If user is NOT logged in -> Navigate to LoginActivity.
         *
         * NOTE FOR DEVELOPER:
         * Replace 'MainActivity.class' below with your desired target activity
         * if you want to route elsewhere.
         * =========================================================================
         */
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Intent intent;
        if (currentUser != null) {
            // Target Activity for Logged-In Users (REPLACE MainActivity.class if needed)
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            // Target Activity for Unauthenticated Users (REPLACE LoginActivity.class if needed)
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel all animators and handler callbacks to prevent memory leaks
        if (titleTypewriterAnimator != null) {
            titleTypewriterAnimator.cancel();
        }
        if (truckJourneyAnimator != null) {
            truckJourneyAnimator.cancel();
        }
        if (navigateRunnable != null) {
            splashHandler.removeCallbacks(navigateRunnable);
        }
        splashHandler.removeCallbacksAndMessages(null);
    }
}
