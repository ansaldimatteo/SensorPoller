package com.ansaldi.sensorpoller;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.ansaldi.sensorpoller.SensorListeners.AccelerometerListener;
import com.ansaldi.sensorpoller.SensorListeners.GyroListener;
import com.ansaldi.sensorpoller.SensorListeners.LightListener;
import com.ansaldi.sensorpoller.SensorListeners.MicrophoneListener;
import com.ansaldi.sensorpoller.SensorListeners.ProximityListener;
import com.kishan.askpermission.AskPermission;
import com.kishan.askpermission.ErrorCallback;
import com.kishan.askpermission.PermissionCallback;
import com.kishan.askpermission.PermissionInterface;

import org.w3c.dom.Text;

import java.security.Permission;
import java.security.Permissions;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, PermissionCallback, ErrorCallback {

    private static final int REQUEST_PERMISSIONS = 20;

    private Switch switch_accelerometer;
    private Switch switch_gyro;
    private Switch switch_light;
    private Switch switch_proximity;
    private Switch switch_microphone;
    private TextView txt_status;
    private Button btn_start;
    private Button btn_stop;

    private Boolean check_accelerometer = false;
    private Boolean check_gyro = false;
    private Boolean check_light = false;
    private Boolean check_proximity = false;
    private Boolean check_microphone = false;

    private SensorManager accelerometerSensorManager;
    private Sensor accelerometerSensor;

    private SensorManager gyroSensorManager;
    private Sensor gyroSensor;

    private SensorManager lightSensorManager;
    private Sensor lightSensor;

    private SensorManager proximitySensorManager;
    private Sensor proximitySensor;

    private AccelerometerListener accelerometerListener;
    private GyroListener gyroListener;
    private LightListener lightListener;
    private ProximityListener proximityListener;
    private MicrophoneListener microphoneListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        switch_accelerometer = findViewById(R.id.switch_accelerometer);
        switch_gyro = findViewById(R.id.switch_gyro);
        switch_light = findViewById(R.id.switch_light);
        switch_proximity = findViewById(R.id.switch_proximity);
        switch_microphone = findViewById(R.id.switch_microphone);
        txt_status = findViewById(R.id.txt_status);
        btn_start = findViewById(R.id.btn_start);
        btn_stop = findViewById(R.id.btn_stop);


        accelerometerListener = new AccelerometerListener();
        gyroListener = new GyroListener();
        lightListener = new LightListener();
        proximityListener = new ProximityListener();
        microphoneListener = new MicrophoneListener();


        switch_accelerometer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                check_accelerometer = b;
            }
        });

        switch_gyro.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                check_gyro = b;
            }
        });

        switch_light.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                check_light = b;
            }
        });

        switch_proximity.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                check_proximity = b;
            }
        });

        switch_microphone.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                check_microphone = b;
            }
        });

        btn_start.setOnClickListener(this);
        btn_stop.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterListeners();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_start:
                new AskPermission.Builder(this)
                        .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO)
                        .setCallback(this)
                        .setErrorCallback(this)
                        .request(REQUEST_PERMISSIONS);
                break;

            case R.id.btn_stop:
                txt_status.setText(getString(R.string.paused));
                unregisterListeners();
                break;
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode) {
        txt_status.setText(getString(R.string.running));
        startAccelerometer();
        startGyro();
        startLight();
        startProximity();
        startMicrophone();
    }

    @Override
    public void onPermissionsDenied(int requestCode) {
        Toast.makeText(this, getString(R.string.noPermissions),Toast.LENGTH_SHORT);
    }

    private void startAccelerometer(){
        if(check_accelerometer) {
            accelerometerSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            accelerometerSensor = accelerometerSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            accelerometerSensorManager.registerListener(accelerometerListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void startGyro(){
        if(check_gyro){
            gyroSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            gyroSensor = gyroSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            gyroSensorManager.registerListener(gyroListener, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void startLight(){
        if(check_light){
            lightSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            lightSensor = lightSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            lightSensorManager.registerListener(lightListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void startProximity(){
        if(check_proximity){
            proximitySensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            proximitySensor = proximitySensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            proximitySensorManager.registerListener(proximityListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void startMicrophone(){
        if(check_microphone) {
            microphoneListener.startRecording();
        }
    }

    private void unregisterListeners(){
        if(accelerometerSensorManager != null) {
            accelerometerSensorManager.unregisterListener(accelerometerListener);
        }
        if(gyroSensorManager != null){
            gyroSensorManager.unregisterListener(gyroListener);
        }
        if(lightSensorManager != null){
            lightSensorManager.unregisterListener(lightListener);
        }
        if(proximitySensorManager != null){
            proximitySensorManager.unregisterListener(proximityListener);
        }

        microphoneListener.stopRecording();
    }

    @Override
    public void onShowRationalDialog(final PermissionInterface permissionInterface, int requestCode) {
        permissionInterface.onDialogShown();
    }

    @Override
    public void onShowSettings(final PermissionInterface permissionInterface, int requestCode) {
        permissionInterface.onSettingsShown();
    }

}
