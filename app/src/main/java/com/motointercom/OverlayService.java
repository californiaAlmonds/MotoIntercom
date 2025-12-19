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
import android.widget.ImageButton;
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

        ImageButton btnMain = overlayView.findViewById(R.id.btnMain);
        LinearLayout menuContainer = overlayView.findViewById(R.id.menuContainer);
        View btnSwitch = overlayView.findViewById(R.id.btnSwitch);
        View btnMute = overlayView.findViewById(R.id.btnMute);
        View btnStop = overlayView.findViewById(R.id.btnStop);

        ImageView iconMic = overlayView.findViewById(R.id.iconMic);
        TextView textMic = overlayView.findViewById(R.id.textMic);
        ImageView iconMute = overlayView.findViewById(R.id.iconMute);
        TextView textMute = overlayView.findViewById(R.id.textMute);

        btnMain.setOnClickListener(v -> {
            if (isMenuOpen) {
                menuContainer.setVisibility(View.GONE);
                isMenuOpen = false;
            } else {
                menuContainer.setVisibility(View.VISIBLE);
                isMenuOpen = true;
            }
        });

        btnSwitch.setOnClickListener(v -> {
            Intent intent = new Intent(this, IntercomService.class);
            intent.putExtra("ACTION", "SWITCH_MIC");
            startService(intent);

            // Toggle UI state locally (assuming service follows)
            // Ideally we should listen to broadcast, but for simplicity:
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
