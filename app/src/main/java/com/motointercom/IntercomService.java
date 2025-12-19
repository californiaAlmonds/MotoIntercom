package com.motointercom;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

public class IntercomService extends Service {

    private static final String TAG = "IntercomService";
    private static final int SAMPLE_RATE = 16000; // Using 16k for compatibility with SCO

    private boolean isRunning = false;
    private Thread audioThread;
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private boolean riderIsMic = true; // Default to Rider
    private AudioManager audioManager;

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        startForegroundService();

        // Start Overlay Service
        Intent overlayIntent = new Intent(this, OverlayService.class);
        startService(overlayIntent);
    }

    private void startForegroundService() {
        String channelId = "IntercomChannel";
        NotificationChannel channel = new NotificationChannel(channelId, "Intercom Service",
                NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Moto Intercom")
                .setContentText("Intercom is running")
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .build();

        startForeground(1, notification);
    }

    private boolean isMuted = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("ACTION")) {
            String action = intent.getStringExtra("ACTION");
            if ("SWITCH_MIC".equals(action)) {
                switchMic();
            } else if ("TOGGLE_MUTE".equals(action)) {
                toggleMute();
            } else if ("STOP".equals(action)) {
                stopSelf();
            }
        } else {
            if (!isRunning) {
                startAudioLoop();
            }
        }
        return START_STICKY;
    }

    private void switchMic() {
        riderIsMic = !riderIsMic;
        if (!isMuted) {
            restartAudioLoop();
        }
    }
    
    private void toggleMute() {
        isMuted = !isMuted;
        if (isMuted) {
            // Stop Audio Loop and Release Mode
            isRunning = false;
            try {
                if (audioThread != null) {
                    audioThread.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Explicitly clear mode here as loop thread finishes
            audioManager.setMode(AudioManager.MODE_NORMAL);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice();
            } else {
                audioManager.setBluetoothScoOn(false);
                audioManager.stopBluetoothSco();
            }
        } else {
            // Resume Audio Loop
            startAudioLoop();
        }
    }

    private void restartAudioLoop() {
        isRunning = false;
        try {
            if (audioThread != null) {
                audioThread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        startAudioLoop();
    }

    private void startAudioLoop() {
        isRunning = true;
        
        // Set Audio Mode to Communication to enable SCO/Voice routing
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        audioThread = new Thread(this::audioLoop);
        audioThread.start();
    }

    private void audioLoop() {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize < 0)
            bufferSize = SAMPLE_RATE * 2;

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // 1. Determine Devices
        AudioDeviceInfo wiredHeadset = null;
        AudioDeviceInfo btHeadset = null;

        for (AudioDeviceInfo device : audioManager.getDevices(AudioManager.GET_DEVICES_ALL)) {
            if (device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET
                    || device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
                wiredHeadset = device;
            } else if (device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                btHeadset = device;
            }
        }

        // 2. Configure Routing & Communication Device
        AudioDeviceInfo inputDevice;
        AudioDeviceInfo outputDevice;

        if (riderIsMic) {
            // Rider Speaking (Wired Mic) -> Pillion Hearing (BT)
            inputDevice = wiredHeadset;
            outputDevice = btHeadset;
            
            // Set Communication Device to Wired to ensure Wired Mic is active
            // We will force Output to BT via AudioTrack
            if (wiredHeadset != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                 audioManager.setCommunicationDevice(wiredHeadset);
            }
        } else {
            // Pillion Speaking (BT Mic) -> Rider Hearing (Wired)
            inputDevice = btHeadset;
            outputDevice = wiredHeadset;

            // Set Communication Device to BT to activate SCO (BT Mic)
            if (btHeadset != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.setCommunicationDevice(btHeadset);
            } else {
                // Fallback for older Android (though we target 33)
                audioManager.startBluetoothSco();
                audioManager.setBluetoothScoOn(true);
            }
        }

        // 3. Setup AudioRecord
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        if (inputDevice != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioRecord.setPreferredDevice(inputDevice);
        }

        // 4. Setup AudioTrack
        audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();

        if (outputDevice != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioTrack.setPreferredDevice(outputDevice);
        }

        // 5. Noise Suppression
        if (NoiseSuppressor.isAvailable()) {
            try {
                NoiseSuppressor ns = NoiseSuppressor.create(audioRecord.getAudioSessionId());
                if (ns != null)
                    ns.setEnabled(true);
            } catch (Exception e) {
                Log.e(TAG, "NS failed", e);
            }
        }

        audioRecord.startRecording();
        audioTrack.play();

        byte[] buffer = new byte[bufferSize];
        while (isRunning) {
            int read = audioRecord.read(buffer, 0, bufferSize);
            if (read > 0) {
                audioTrack.write(buffer, 0, read);
            }
        }

        audioRecord.stop();
        audioRecord.release();
        audioTrack.stop();
        audioTrack.release();
        
        // Cleanup Audio Mode
        audioManager.setMode(AudioManager.MODE_NORMAL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice();
        } else {
            audioManager.setBluetoothScoOn(false);
            audioManager.stopBluetoothSco();
        }
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        stopService(new Intent(this, OverlayService.class));
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
