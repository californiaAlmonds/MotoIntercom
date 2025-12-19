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
import android.widget.Button;

public class OverlayService extends Service {

    private WindowManager windowManager;
    private View overlayView;

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

        Button btnSwitch = overlayView.findViewById(R.id.btnSwitch);
        Button btnStop = overlayView.findViewById(R.id.btnStop);

        btnSwitch.setOnClickListener(v -> {
            Intent intent = new Intent(this, IntercomService.class);
            intent.putExtra("ACTION", "SWITCH_MIC");
            startService(intent);
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
