package com.motointercom;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 102;
    private TextView statusText;
    private FloatingActionButton fabMain, fabStart, fabMiui;
    private boolean isMenuOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        fabMain = findViewById(R.id.fabMain);
        fabStart = findViewById(R.id.fabStart);
        fabMiui = findViewById(R.id.fabMiui);

        fabMain.setOnClickListener(v -> toggleMenu());

        fabStart.setOnClickListener(v -> {
            if (checkPermissions()) {
                if (checkOverlayPermission()) {
                    startIntercomService();
                } else {
                    requestOverlayPermission();
                }
            } else {
                requestPermissions();
            }
            closeMenu();
        });

        fabMiui.setOnClickListener(v -> {
            openAutoStartSettings();
            closeMenu();
        });
    }

    private void toggleMenu() {
        if (isMenuOpen) {
            closeMenu();
        } else {
            openMenu();
        }
    }

    private void openMenu() {
        isMenuOpen = true;
        fabMain.animate().rotation(45f).setInterpolator(new OvershootInterpolator()).start();

        fabStart.setVisibility(View.VISIBLE);
        fabMiui.setVisibility(View.VISIBLE);

        fabStart.animate().translationY(-getResources().getDimension(R.dimen.fab_margin_1)).alpha(1f).setDuration(300)
                .start();
        fabMiui.animate().translationY(-getResources().getDimension(R.dimen.fab_margin_2)).alpha(1f).setDuration(300)
                .start();
    }

    private void closeMenu() {
        isMenuOpen = false;
        fabMain.animate().rotation(0f).setInterpolator(new OvershootInterpolator()).start();

        fabStart.animate().translationY(0).alpha(0f).setDuration(300).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (!isMenuOpen)
                    fabStart.setVisibility(View.INVISIBLE);
            }
        }).start();

        fabMiui.animate().translationY(0).alpha(0f).setDuration(300).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (!isMenuOpen)
                    fabMiui.setVisibility(View.INVISIBLE);
            }
        }).start();
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
        statusText.setText("Intercom Service Started");
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
