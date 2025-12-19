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
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();

        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("ACTION")) {
            String action = intent.getStringExtra("ACTION");
            if ("SWITCH_MIC".equals(action)) {
                switchMic();
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
        restartAudioLoop();
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
        AudioDeviceInfo btA2dp = null;

        for (AudioDeviceInfo device : audioManager.getDevices(AudioManager.GET_DEVICES_ALL)) {
            if (device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET
                    || device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
                wiredHeadset = device;
            } else if (device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                    || device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_HEADSET) {
                btHeadset = device;
            } else if (device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                btA2dp = device;
            }
        }

        // 2. Configure Routing
        AudioDeviceInfo inputDevice;
        AudioDeviceInfo outputDevice;

        if (riderIsMic) {
            // Rider Speaking (Wired Mic) -> Pillion Hearing (BT)
            inputDevice = wiredHeadset;
            outputDevice = btA2dp != null ? btA2dp : btHeadset;
        } else {
            // Pillion Speaking (BT Mic) -> Rider Hearing (Wired)
            inputDevice = btHeadset;
            outputDevice = wiredHeadset;
        }

        // 3. Setup AudioRecord
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        if (inputDevice != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioRecord.setPreferredDevice(inputDevice);
        }

        // 4. Setup AudioTrack
        audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
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
