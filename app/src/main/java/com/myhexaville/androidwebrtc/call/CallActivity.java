/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.myhexaville.androidwebrtc.call;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

import com.myhexaville.androidwebrtc.R;
import com.myhexaville.androidwebrtc.databinding.ActivityCallBinding;
import com.myhexaville.androidwebrtc.web_rtc.AppRTCAudioManager;
import com.myhexaville.androidwebrtc.web_rtc.AppRTCClient;
import com.myhexaville.androidwebrtc.web_rtc.AppRTCClient.RoomConnectionParameters;
import com.myhexaville.androidwebrtc.web_rtc.AppRTCClient.SignalingParameters;
import com.myhexaville.androidwebrtc.web_rtc.PeerConnectionClient;
import com.myhexaville.androidwebrtc.web_rtc.PeerConnectionClient.PeerConnectionParameters;
import com.myhexaville.androidwebrtc.web_rtc.WebSocketRTCClient;

import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.myhexaville.androidwebrtc.util.Constants.CAPTURE_PERMISSION_REQUEST_CODE;
import static com.myhexaville.androidwebrtc.util.Constants.EXTRA_MODE;
import static com.myhexaville.androidwebrtc.util.Constants.EXTRA_ROOMID;
import static com.myhexaville.androidwebrtc.util.Constants.LOCAL_HEIGHT_CONNECTED;
import static com.myhexaville.androidwebrtc.util.Constants.LOCAL_HEIGHT_CONNECTING;
import static com.myhexaville.androidwebrtc.util.Constants.LOCAL_WIDTH_CONNECTED;
import static com.myhexaville.androidwebrtc.util.Constants.LOCAL_WIDTH_CONNECTING;
import static com.myhexaville.androidwebrtc.util.Constants.LOCAL_X_CONNECTED;
import static com.myhexaville.androidwebrtc.util.Constants.LOCAL_X_CONNECTING;
import static com.myhexaville.androidwebrtc.util.Constants.LOCAL_Y_CONNECTED;
import static com.myhexaville.androidwebrtc.util.Constants.LOCAL_Y_CONNECTING;
import static com.myhexaville.androidwebrtc.util.Constants.REMOTE_HEIGHT;
import static com.myhexaville.androidwebrtc.util.Constants.REMOTE_WIDTH;
import static com.myhexaville.androidwebrtc.util.Constants.REMOTE_X;
import static com.myhexaville.androidwebrtc.util.Constants.REMOTE_Y;
import static com.myhexaville.androidwebrtc.util.Constants.STAT_CALLBACK_PERIOD;
import static org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FILL;
import static org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FIT;


/**
 * Activity for peer connection call setup, call waiting
 * and call view.
 */
public class CallActivity extends AppCompatActivity
        implements AppRTCClient.SignalingEvents, PeerConnectionClient.PeerConnectionEvents, OnCallEvents {
    private static final String LOG_TAG = "CallActivity";

    private PeerConnectionClient peerConnectionClient;
    private AppRTCClient appRtcClient;
    private SignalingParameters signalingParameters;
    private AppRTCAudioManager audioManager;
    private EglBase rootEglBase;
    private final List<VideoRenderer.Callbacks> remoteRenderers = new ArrayList<>();
    private Toast logToast;
    private boolean activityRunning;

    private RoomConnectionParameters roomConnectionParameters;
    private PeerConnectionParameters peerConnectionParameters;

    private boolean iceConnected;
    private boolean isError;
    private long callStartedTimeMs;
    private boolean micEnabled = true;

    private ActivityCallBinding binding;

    private SensorManager sensorManager = null;
    private Sensor gyroSensor = null, gravitySensor = null;

    private static final float NS2S = 1.0f / 1000000000.0f;
    private float[] angle = new float[3];
    private BluetoothAdapter BA;
    private Set<BluetoothDevice> pairedDevices;
    private InputStream mmInputStream;
    private static OutputStream mmOutputStream;
    private static boolean isConnect = false;
    private boolean isCamera;
    private int xcurrent,zgravity = 0,xprevious = 0,zcurrent,zprevious;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(LayoutParams.FLAG_DISMISS_KEYGUARD | LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_call);

        final Intent intent = getIntent();
        String roomId = intent.getStringExtra(EXTRA_ROOMID);
        isCamera = intent.getBooleanExtra(EXTRA_MODE, false);

        setupBluetooth(isCamera);

        remoteRenderers.add(binding.remoteVideoView);


        // Create video renderers.
        rootEglBase = EglBase.create();
        binding.localVideoView.init(rootEglBase.getEglBaseContext(), null);
        binding.remoteVideoView.init(rootEglBase.getEglBaseContext(), null);

        binding.localVideoView.setZOrderMediaOverlay(true);
        binding.localVideoView.setEnableHardwareScaler(true);
        binding.remoteVideoView.setEnableHardwareScaler(true);
        updateVideoView();

        // Get Intent parameters.
        Log.d(LOG_TAG, "Room ID: " + roomId);
        if (roomId == null || roomId.length() == 0) {
            logAndToast(getString(R.string.missing_url));
            Log.e(LOG_TAG, "Incorrect room ID in intent!");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        // If capturing format is not specified for screencapture, use screen resolution.
        peerConnectionParameters = PeerConnectionParameters.createDefault();

        // Create connection client. Use DirectRTCClient if room name is an IP otherwise use the
        // standard WebSocketRTCClient.
        appRtcClient = new WebSocketRTCClient(this);

        // Create connection parameters.
        roomConnectionParameters = new RoomConnectionParameters("https://appr.tc", roomId, false);

        //setupListeners();

        peerConnectionClient = PeerConnectionClient.getInstance();

        peerConnectionClient.createPeerConnectionFactory(this, peerConnectionParameters, this);

        if (!isCamera) {
            SensorThread sensorThread = new SensorThread();
            sensorThread.start();
            readBluetoothData();
        }

        startCall();
    }

    class SensorThread extends Thread {

        public void run() {
            View v = findViewById(R.id.remote_video_view);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.wtf(LOG_TAG,"校正");
                    sendWifiData(4);
                }
            });
            Log.d("RunTag", Thread.currentThread().getName()); // To display thread
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            gyroSensor = sensorManager
                    .getDefaultSensor(Sensor.TYPE_ORIENTATION);
            gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
            MySensorListener_gyro msl = new MySensorListener_gyro();
            MySensorListener_gravity mslg = new MySensorListener_gravity();
            sensorManager.registerListener(msl, gyroSensor, SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(mslg,gravitySensor,SensorManager.SENSOR_DELAY_UI);
        }

        private class MySensorListener_gyro implements SensorEventListener {
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            public void onSensorChanged(SensorEvent event) {
                Log.d("ListenerTag", Thread.currentThread().getName()); // To display thread
                xcurrent = Math.round(event.values[0]);
                zcurrent = Math.round(event.values[2]);
                Log.wtf(LOG_TAG, "-----onSensorChanged-----");
                if (xprevious > xcurrent + 1)
                    sendWifiData(1);
                else if (xprevious < xcurrent - 1)
                    sendWifiData(0);

                if ((zprevious > zcurrent + 1 && zgravity>0) || (zprevious < zcurrent - 1 && zgravity<0))
                    sendWifiData(2);//down
                else if ((zprevious < zcurrent - 1 && zgravity>0) || (zprevious > zcurrent + 1 && zgravity<0))
                    sendWifiData(3);//up

                xprevious = xcurrent;
                zprevious = zcurrent;
            }
        }
        private class MySensorListener_gravity implements SensorEventListener {
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            public void onSensorChanged(SensorEvent event) {
                zgravity = Math.round(event.values[2]);
            }
        }
    }

    private void sendWifiData(int angle){
        String data = String.valueOf(angle);
        ByteBuffer buffer = ByteBuffer.wrap(data.getBytes());
        DataChannel.Buffer dcBuffer = new DataChannel.Buffer(buffer, false);
        if(peerConnectionClient.getDataChannel() != null) {
            if (peerConnectionClient.getDataChannel().send(dcBuffer))
                Log.wtf(LOG_TAG, "----------Success----------");
        }
    }

    public static void sendBluetoothData(int angle){
        Log.wtf(LOG_TAG, "-----Angle"+angle+"-----");

        if(isConnect) {
            try {
                mmOutputStream.write(angle);
                mmOutputStream.flush();
            }catch (Exception e){
                e.printStackTrace();
                Log.wtf(LOG_TAG, "sendBluetoothData failed");
            }
        }
    }

    private void readBluetoothData(){
        Log.wtf(LOG_TAG,"Reading bluetooth");
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Log.wtf(LOG_TAG,"In read bluetooth Thread");
                        // Read from the InputStream
                        //int availableBytes = mmInputStream.available();
                        byte[] buffer = new byte[1];
                        int bytes;
                        bytes = mmInputStream.read(buffer,0,buffer.length);
                        String readMessage = new String(buffer, 0, bytes);
                        int data = buffer[0];
                        Log.wtf(LOG_TAG, "JoyStick data Message :: "+data);
                        sendWifiData(data);
                    } catch (IOException e) {
                        break;
                    }
                }
            }
        }).start();
    }

    private void setupBluetooth(boolean isCamera){
        BA = BluetoothAdapter.getDefaultAdapter();
        String bluetoothDevice = isCamera ? "HC-05" : "joystick";
//        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
//        gyroSensor = sensorManager
//                .getDefaultSensor(Sensor.TYPE_ORIENTATION);
//        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        if(BA.isEnabled()) {
            pairedDevices = BA.getBondedDevices();

            final ArrayList list = new ArrayList();
            for (BluetoothDevice bt : pairedDevices)
                list.add(bt.getName());

            Toast.makeText(getApplicationContext(), "Showing Paired Devices",
                    Toast.LENGTH_SHORT).show();
            for (BluetoothDevice bt : pairedDevices) {
                if (bt.getName().equals(bluetoothDevice)) {
                    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                    try {
                        BluetoothSocket mmSocket = bt.createRfcommSocketToServiceRecord(uuid);
                        mmSocket.connect();  // blocking call
                        mmOutputStream = mmSocket.getOutputStream();
                        mmInputStream = mmSocket.getInputStream();
                        Toast.makeText(getApplicationContext(), "成功辣",
                                Toast.LENGTH_SHORT).show();
                        isConnect = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }



    /*private void setupListeners() {
        binding.buttonCallDisconnect.setOnClickListener(view -> onCallHangUp());

        binding.buttonCallSwitchCamera.setOnClickListener(view -> onCameraSwitch());

        binding.buttonCallToggleMic.setOnClickListener(view -> {
            boolean enabled = onToggleMic();
            binding.buttonCallToggleMic.setAlpha(enabled ? 1.0f : 0.3f);
        });
    }*/

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != CAPTURE_PERMISSION_REQUEST_CODE) {
            return;
        }
        startCall();
    }

    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(this);
    }

    private boolean captureToTexture() {
        return true;
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Logging.d(LOG_TAG, "Looking for front facing cameras.");
        /*for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(LOG_TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }*/

        // Front facing camera not found, try something else
        Logging.d(LOG_TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(LOG_TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    // Activity interfaces
    @Override
    public void onPause() {
        super.onPause();
        activityRunning = false;
        // Don't stop the video when using screencapture to allow user to show other apps to the remote
        // end.
        if (peerConnectionClient != null) {
            peerConnectionClient.stopVideoSource();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        activityRunning = true;
        // Video is not paused for screencapture. See onPause.
        if (peerConnectionClient != null) {
            peerConnectionClient.startVideoSource();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        Bundle args = getIntent().getExtras();
        if (args != null) {
            String contactName = args.getString(EXTRA_ROOMID);
            //binding.contactNameCall.setText(contactName);
        }
        binding.captureFormatTextCall.setVisibility(View.GONE);
        binding.captureFormatSliderCall.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        disconnect();
        if (logToast != null) {
            logToast.cancel();
        }
        activityRunning = false;
        rootEglBase.release();
        super.onDestroy();
    }

    // CallFragment.OnCallEvents interface implementation.
    @Override
    public void onCallHangUp() {
        disconnect();
    }

    @Override
    public void onCameraSwitch() {
        if (peerConnectionClient != null) {
            peerConnectionClient.switchCamera();
        }
    }

    @Override
    public void onCaptureFormatChange(int width, int height, int framerate) {
        if (peerConnectionClient != null) {
            peerConnectionClient.changeCaptureFormat(width, height, framerate);
        }
    }

    @Override
    public boolean onToggleMic() {
        if (peerConnectionClient != null) {
            micEnabled = !micEnabled;
            peerConnectionClient.setAudioEnabled(micEnabled);
        }
        return micEnabled;
    }

    private void updateVideoView() {
        binding.remoteVideoLayout.setPosition(REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT);
        binding.remoteVideoView.setScalingType(SCALE_ASPECT_FILL);
        binding.remoteVideoView.setMirror(false);

        if (iceConnected) {
            binding.localVideoLayout.setPosition(
                    LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED, LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED);
            binding.localVideoView.setScalingType(SCALE_ASPECT_FIT);
        } else {
            binding.localVideoLayout.setPosition(
                    LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING, LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING);
            binding.localVideoView.setScalingType(SCALE_ASPECT_FILL);
        }
        binding.localVideoView.setMirror(true);

        binding.localVideoView.requestLayout();
        binding.remoteVideoView.requestLayout();
    }

    private void startCall() {
        callStartedTimeMs = System.currentTimeMillis();

        // Start room connection.
        logAndToast(getString(R.string.connecting_to, roomConnectionParameters.roomUrl));
        appRtcClient.connectToRoom(roomConnectionParameters);

        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        audioManager = AppRTCAudioManager.create(this);
        // Store existing audio settings and change audio mode to
        // MODE_IN_COMMUNICATION for best possible VoIP performance.
        Log.d(LOG_TAG, "Starting the audio manager...");
        audioManager.start(this::onAudioManagerDevicesChanged);
    }

    // Should be called from UI thread
    private void callConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        Log.i(LOG_TAG, "Call connected: delay=" + delta + "ms");
        if (peerConnectionClient == null || isError) {
            Log.w(LOG_TAG, "Call is connected in closed or error state");
            return;
        }
        // Update video view.
        updateVideoView();
        // Enable statistics callback.
        peerConnectionClient.enableStatsEvents(true, STAT_CALLBACK_PERIOD);
    }

    // This method is called when the audio manager reports audio device change,
    // e.g. from wired headset to speakerphone.
    private void onAudioManagerDevicesChanged(
            final AppRTCAudioManager.AudioDevice device, final Set<AppRTCAudioManager.AudioDevice> availableDevices) {
        Log.d(LOG_TAG, "onAudioManagerDevicesChanged: " + availableDevices + ", "
                + "selected: " + device);
        // TODO(henrika): add callback handler.
    }

    // Disconnect from remote resources, dispose of local resources, and exit.
    private void disconnect() {
        activityRunning = false;
        if (appRtcClient != null) {
            appRtcClient.disconnectFromRoom();
            appRtcClient = null;
        }
        if (peerConnectionClient != null) {
            peerConnectionClient.close();
            peerConnectionClient = null;
        }
        binding.localVideoView.release();
        binding.remoteVideoView.release();
        if (audioManager != null) {
            audioManager.stop();
            audioManager = null;
        }
        if (iceConnected && !isError) {
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    private void disconnectWithErrorMessage(final String errorMessage) {
        if (!activityRunning) {
            Log.e(LOG_TAG, "Critical error: " + errorMessage);
            disconnect();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(getText(R.string.channel_error_title))
                    .setMessage(errorMessage)
                    .setCancelable(false)
                    .setNeutralButton(R.string.ok,
                            (dialog, id) -> {
                                dialog.cancel();
                                disconnect();
                            })
                    .create()
                    .show();
        }
    }

    // Log |msg| and Toast about it.
    private void logAndToast(String msg) {
        Log.d(LOG_TAG, msg);
        if (logToast != null) {
            logToast.cancel();
        }
        logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        logToast.show();
    }

    private void reportError(final String description) {
        runOnUiThread(() -> {
            if (!isError) {
                isError = true;
                disconnectWithErrorMessage(description);
            }
        });
    }

    private VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer;
        if (useCamera2()) {
            Logging.d(LOG_TAG, "Creating capturer using camera2 API.");
            videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
        } else {
            Logging.d(LOG_TAG, "Creating capturer using camera1 API.");
            videoCapturer = createCameraCapturer(new Camera1Enumerator(captureToTexture()));
        }
        if (videoCapturer == null) {
            reportError("Failed to open camera");
            return null;
        }
        return videoCapturer;
    }

    // -----Implementation of AppRTCClient.AppRTCSignalingEvents ---------------
    // All callbacks are invoked from websocket signaling looper thread and
    // are routed to UI thread.
    private void onConnectedToRoomInternal(final SignalingParameters params) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;

        signalingParameters = params;
        logAndToast("Creating peer connection, delay=" + delta + "ms");
        VideoCapturer videoCapturer = null;
        if (peerConnectionParameters.videoCallEnabled) {
            videoCapturer = createVideoCapturer();
        }
        peerConnectionClient.createPeerConnection(rootEglBase.getEglBaseContext(), binding.localVideoView,
                remoteRenderers, videoCapturer, signalingParameters);

        if (signalingParameters.initiator) {
            logAndToast("Creating OFFER...");
            // Create offer. Offer SDP will be sent to answering client in
            // PeerConnectionEvents.onLocalDescription event.
            peerConnectionClient.createOffer();
        } else {
            if (params.offerSdp != null) {
                peerConnectionClient.setRemoteDescription(params.offerSdp);
                logAndToast("Creating ANSWER...");
                // Create answer. Answer SDP will be sent to offering client in
                // PeerConnectionEvents.onLocalDescription event.
                peerConnectionClient.createAnswer();
            }
            if (params.iceCandidates != null) {
                // Add remote ICE candidates from room.
                for (IceCandidate iceCandidate : params.iceCandidates) {
                    peerConnectionClient.addRemoteIceCandidate(iceCandidate);
                }
            }
        }
    }

    @Override
    public void onConnectedToRoom(final SignalingParameters params) {
        runOnUiThread(() -> onConnectedToRoomInternal(params));
    }

    @Override
    public void onRemoteDescription(final SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(() -> {
            if (peerConnectionClient == null) {
                Log.e(LOG_TAG, "Received remote SDP for non-initilized peer connection.");
                return;
            }
            logAndToast("Received remote " + sdp.type + ", delay=" + delta + "ms");
            peerConnectionClient.setRemoteDescription(sdp);
            if (!signalingParameters.initiator) {
                logAndToast("Creating ANSWER...");
                // Create answer. Answer SDP will be sent to offering client in
                // PeerConnectionEvents.onLocalDescription event.
                peerConnectionClient.createAnswer();
            }
        });
    }

    @Override
    public void onRemoteIceCandidate(final IceCandidate candidate) {
        runOnUiThread(() -> {
            if (peerConnectionClient == null) {
                Log.e(LOG_TAG, "Received ICE candidate for a non-initialized peer connection.");
                return;
            }
            peerConnectionClient.addRemoteIceCandidate(candidate);
        });
    }

    @Override
    public void onRemoteIceCandidatesRemoved(final IceCandidate[] candidates) {
        runOnUiThread(() -> {
            if (peerConnectionClient == null) {
                Log.e(LOG_TAG, "Received ICE candidate removals for a non-initialized peer connection.");
                return;
            }
            peerConnectionClient.removeRemoteIceCandidates(candidates);
        });
    }

    @Override
    public void onChannelClose() {
        runOnUiThread(() -> {
            logAndToast("Remote end hung up; dropping PeerConnection");
            disconnect();
        });
    }

    @Override
    public void onChannelError(final String description) {
        reportError(description);
    }

    // -----Implementation of PeerConnectionClient.PeerConnectionEvents.---------
    // Send local peer connection SDP and ICE candidates to remote party.
    // All callbacks are invoked from peer connection client looper thread and
    // are routed to UI thread.
    @Override
    public void onLocalDescription(final SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(() -> {
            if (appRtcClient != null) {
                logAndToast("Sending " + sdp.type + ", delay=" + delta + "ms");
                if (signalingParameters.initiator) {
                    appRtcClient.sendOfferSdp(sdp);
                } else {
                    appRtcClient.sendAnswerSdp(sdp);
                }
            }
            if (peerConnectionParameters.videoMaxBitrate > 0) {
                Log.d(LOG_TAG, "Set video maximum bitrate: " + peerConnectionParameters.videoMaxBitrate);
                peerConnectionClient.setVideoMaxBitrate(peerConnectionParameters.videoMaxBitrate);
            }
        });
    }

    @Override
    public void onIceCandidate(final IceCandidate candidate) {
        runOnUiThread(() -> {
            if (appRtcClient != null) {
                appRtcClient.sendLocalIceCandidate(candidate);
            }
        });
    }

    @Override
    public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
        runOnUiThread(() -> {
            if (appRtcClient != null) {
                appRtcClient.sendLocalIceCandidateRemovals(candidates);
            }
        });
    }

    @Override
    public void onIceConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(() -> {
            logAndToast("ICE connected, delay=" + delta + "ms");
            iceConnected = true;
            callConnected();
        });
    }

    @Override
    public void onIceDisconnected() {
        runOnUiThread(() -> {
            logAndToast("ICE disconnected");
            iceConnected = false;
            disconnect();
        });
    }

    @Override
    public void onPeerConnectionClosed() {
    }

    @Override
    public void onPeerConnectionStatsReady(final StatsReport[] reports) {
        runOnUiThread(() -> {
        });
    }

    @Override
    public void onPeerConnectionError(final String description) {
        reportError(description);
    }
}
