package com.motointercom;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OverlayService extends Service {

    private WindowManager windowManager;
    private View overlayView;
    private boolean isMenuOpen = false;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_view, null);

        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        windowManager.addView(overlayView, params);

        // Make overlay draggable
        overlayView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case android.view.MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(overlayView, params);
                        return true;
                }
                return false;
            }
        });

        FrameLayout btnMain = overlayView.findViewById(R.id.btnMain);
        LinearLayout menuContainer = overlayView.findViewById(R.id.menuContainer);
        View btnSwitch = overlayView.findViewById(R.id.btnSwitch);
        View btnMute = overlayView.findViewById(R.id.btnMute);
        View btnStop = overlayView.findViewById(R.id.btnStop);

        ImageView iconMic = overlayView.findViewById(R.id.iconMic);
        TextView textMic = overlayView.findViewById(R.id.textMic);
        ImageView iconMute = overlayView.findViewById(R.id.iconMute);
        TextView textMute = overlayView.findViewById(R.id.textMute);
        ImageView iconMain = overlayView.findViewById(R.id.iconMain);

        btnMain.setOnClickListener(v -> {
            if (isMenuOpen) {
                closeMenuWithAnimation(menuContainer, iconMain);
            } else {
                openMenuWithAnimation(menuContainer, iconMain);
            }
        });

        btnSwitch.setOnClickListener(v -> {
            Intent intent = new Intent(this, IntercomService.class);
            intent.putExtra("ACTION", "SWITCH_MIC");
            startService(intent);

            boolean isRider = "Rider".equals(textMic.getText());
            if (isRider) {
                textMic.setText("Pillion");
                iconMic.setImageResource(R.drawable.ic_headset_bt);
            } else {
                textMic.setText("Rider");
                iconMic.setImageResource(R.drawable.ic_headset_wired);
            }
        });

        btnMute.setOnClickListener(v -> {
            Intent intent = new Intent(this, IntercomService.class);
            intent.putExtra("ACTION", "TOGGLE_MUTE");
            startService(intent);

            boolean isMuted = "Unmute".equals(textMute.getText());
            if (isMuted) {
                textMute.setText("Mute");
                iconMute.setAlpha(1.0f);
            } else {
                textMute.setText("Unmute");
                iconMute.setAlpha(0.5f);
            }
        });

        btnStop.setOnClickListener(v -> {
            Intent intent = new Intent(this, IntercomService.class);
            intent.putExtra("ACTION", "STOP");
            startService(intent);
            stopSelf();
        });
    }

    private void openMenuWithAnimation(LinearLayout menu, ImageView icon) {
        isMenuOpen = true;
        menu.setVisibility(View.VISIBLE);

        android.view.animation.Animation popupAnim = android.view.animation.AnimationUtils.loadAnimation(this,
                R.anim.popup_show);
        menu.startAnimation(popupAnim);

        // Rotate main icon
        icon.animate().rotation(135f).setDuration(200).start();
    }

    private void closeMenuWithAnimation(LinearLayout menu, ImageView icon) {
        isMenuOpen = false;

        android.view.animation.Animation hideAnim = android.view.animation.AnimationUtils.loadAnimation(this,
                R.anim.popup_hide);
        hideAnim.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
            @Override
            public void onAnimationStart(android.view.animation.Animation animation) {
            }

            @Override
            public void onAnimationEnd(android.view.animation.Animation animation) {
                menu.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(android.view.animation.Animation animation) {
            }
        });
        menu.startAnimation(hideAnim);

        // Rotate main icon back
        icon.animate().rotation(0f).setDuration(200).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null) {
            windowManager.removeView(overlayView);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
