/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.myhexaville.androidwebrtc.main;

import android.Manifest;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.myhexaville.androidwebrtc.R;
import com.myhexaville.androidwebrtc.call.CallActivity;
import com.myhexaville.androidwebrtc.databinding.ActivityMainBinding;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static com.myhexaville.androidwebrtc.util.Constants.EXTRA_MODE;
import static com.myhexaville.androidwebrtc.util.Constants.EXTRA_ROOMID;

/**
 * Handles the initial setup where the user selects which room to join.
 */
public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "MainActivity";
    private static final int CONNECTION_REQUEST = 1;
    private static final int RC_CALL = 111;
    private ActivityMainBinding binding;
    private boolean isCamera = false;
    private Switch sc;
    private EditText editText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.connectButton.setOnClickListener(v -> connect());
        binding.roomEdittext.requestFocus();
        editText = (EditText)findViewById(R.id.room_edittext);
        editText.setHint("請輸入拍攝端房號...");
        sc = (Switch)findViewById(R.id.cameraSwitch);
        sc.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub
                if (isChecked) {
                    int randomRoomNumber = ThreadLocalRandom.current().nextInt(10000, Integer.MAX_VALUE);
                    Log.wtf(LOG_TAG, "Room Number:"+randomRoomNumber);
                    editText.setText(String.valueOf(randomRoomNumber));
                    Toast.makeText(getApplicationContext(), "此號碼為拍攝端房號", Toast.LENGTH_LONG).show();
                    isCamera = true;
                } else {
                    editText.setText("");
                    editText.setHint("請輸入拍攝端房號...");
                    isCamera = false;
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(RC_CALL)
    private void connect() {
        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (EasyPermissions.hasPermissions(this, perms)) {
            connectToRoom(binding.roomEdittext.getText().toString());
        } else {
            EasyPermissions.requestPermissions(this, "Need some permissions", RC_CALL, perms);
        }
    }

    private void connectToRoom(String roomId) {
        Intent intent = new Intent(this, CallActivity.class);
        intent.putExtra(EXTRA_ROOMID, roomId);
        intent.putExtra(EXTRA_MODE, isCamera);
        startActivityForResult(intent, CONNECTION_REQUEST);
    }

//    public void cameraConnectToRoom(View view){
//        isCamera = true;
//    }
}
