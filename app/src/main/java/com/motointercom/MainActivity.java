package com.motointercom;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 102;
    private TextView statusText;
    private View btnStartContainer, btnMiuiContainer;
    private TextView startTitle, startDesc;
    private boolean isServiceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        btnStartContainer = findViewById(R.id.btnStartContainer);
        btnMiuiContainer = findViewById(R.id.btnMiuiContainer);
        startTitle = btnStartContainer.findViewById(R.id.startTitle);
        startDesc = btnStartContainer.findViewById(R.id.startDesc);

        btnStartContainer.setOnClickListener(v -> {
            if (isServiceRunning) {
                stopIntercomService();
            } else {
                if (checkPermissions()) {
                    if (checkOverlayPermission()) {
                        startIntercomService();
                    } else {
                        requestOverlayPermission();
                    }
                } else {
                    requestPermissions();
                }
            }
        });

        btnMiuiContainer.setOnClickListener(v -> openAutoStartSettings());
    }

    private void openAutoStartSettings() {
        try {
            Intent intent = new Intent();
            intent.setComponent(new android.content.ComponentName("com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Not a MIUI device or setting not found", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.RECORD_AUDIO);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.RECORD_AUDIO);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
    }

    private boolean checkOverlayPermission() {
        return Settings.canDrawOverlays(this);
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
    }

    private void startIntercomService() {
        Intent serviceIntent = new Intent(this, IntercomService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        isServiceRunning = true;
        updateButtonState();
    }

    private void stopIntercomService() {
        Intent serviceIntent = new Intent(this, IntercomService.class);
        serviceIntent.putExtra("ACTION", "STOP");
        startService(serviceIntent);
        isServiceRunning = false;
        updateButtonState();
    }

    private void updateButtonState() {
        if (isServiceRunning) {
            startTitle.setText("Stop Intercom");
            startDesc.setText("End voice communication");
            statusText.setText("Active");
        } else {
            startTitle.setText("Start Intercom");
            startDesc.setText("Begin voice communication");
            statusText.setText("MotoIntercom");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (checkOverlayPermission()) {
                    startIntercomService();
                } else {
                    requestOverlayPermission();
                }
            } else {
                Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
