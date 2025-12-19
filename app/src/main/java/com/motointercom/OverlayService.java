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
import android.widget.LinearLayout;

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
        ImageButton btnSwitch = overlayView.findViewById(R.id.btnSwitch);
        ImageButton btnStop = overlayView.findViewById(R.id.btnStop);

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
            menuContainer.setVisibility(View.GONE);
            isMenuOpen = false;
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
