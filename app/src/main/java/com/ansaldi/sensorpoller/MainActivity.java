package com.ansaldi.sensorpoller;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.ansaldi.sensorpoller.SensorListeners.AccelerometerListener;
import com.ansaldi.sensorpoller.SensorListeners.GPSListener;
import com.ansaldi.sensorpoller.SensorListeners.GyroListener;
import com.ansaldi.sensorpoller.SensorListeners.LightListener;
import com.ansaldi.sensorpoller.SensorListeners.MicrophoneListener;
import com.ansaldi.sensorpoller.SensorListeners.ProximityListener;
import com.kishan.askpermission.AskPermission;
import com.kishan.askpermission.ErrorCallback;
import com.kishan.askpermission.PermissionCallback;
import com.kishan.askpermission.PermissionInterface;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, PermissionCallback, ErrorCallback {

    private static final int REQUEST_PERMISSIONS = 20;
    private static final int REQUEST_GPS = 30;
    private static final int REQUEST_NETWORK_FOR_WIFI = 40;

    private Switch switch_accelerometer;
    private Switch switch_gyro;
    private Switch switch_light;
    private Switch switch_proximity;
    private Switch switch_microphone;
    private Switch switch_gps;
    private Switch switch_wifi;
    private TextView txt_status;
    private Button btn_start;
    private Button btn_stop;

    private Boolean running = false;
    private Boolean check_accelerometer = false;
    private Boolean check_gyro = false;
    private Boolean check_light = false;
    private Boolean check_proximity = false;
    private Boolean check_microphone = false;
    private Boolean check_gps = false;
    private Boolean check_wifi = false;

    private SensorManager accelerometerSensorManager;
    private Sensor accelerometerSensor;

    private SensorManager gyroSensorManager;
    private Sensor gyroSensor;

    private SensorManager lightSensorManager;
    private Sensor lightSensor;

    private SensorManager proximitySensorManager;
    private Sensor proximitySensor;

    private LocationManager locationManager;

    private WifiManager mWifiManager;

    private AccelerometerListener accelerometerListener;
    private GyroListener gyroListener;
    private LightListener lightListener;
    private ProximityListener proximityListener;
    private MicrophoneListener microphoneListener;
    private GPSListener gpsListener;

    private PowerManager.WakeLock wakeLock;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        switch_accelerometer = findViewById(R.id.switch_accelerometer);
        switch_gyro = findViewById(R.id.switch_gyro);
        switch_light = findViewById(R.id.switch_light);
        switch_proximity = findViewById(R.id.switch_proximity);
        switch_microphone = findViewById(R.id.switch_microphone);
        switch_gps = findViewById(R.id.switch_gps);
        switch_wifi = findViewById(R.id.switch_wifi);

        txt_status = findViewById(R.id.txt_status);
        btn_start = findViewById(R.id.btn_start);
        btn_stop = findViewById(R.id.btn_stop);


        accelerometerListener = new AccelerometerListener();
        gyroListener = new GyroListener();
        lightListener = new LightListener();
        proximityListener = new ProximityListener();
        microphoneListener = new MicrophoneListener();
        gpsListener = new GPSListener();


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

        switch_gps.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                check_gps = b;
            }
        });

        switch_wifi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                check_wifi = b;
            }
        });

        btn_start.setOnClickListener(this);
        btn_stop.setOnClickListener(this);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterListeners();
        if(wakeLock != null) {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_start:
                if(!running) {
                    new AskPermission.Builder(this)
                            .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECORD_AUDIO)
                            .setCallback(this)
                            .setErrorCallback(this)
                            .request(REQUEST_PERMISSIONS);
                }
                break;

            case R.id.btn_stop:
                running = false;
                if(wakeLock != null) {
                    if (wakeLock.isHeld()) {
                        wakeLock.release();
                    }
                }
                txt_status.setText(getString(R.string.paused));
                unregisterListeners();
                break;
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode) {
        running = true;
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag");
        wakeLock.acquire();
        txt_status.setText(getString(R.string.running));
        startAccelerometer();
        startGyro();
        startLight();
        startProximity();
        startMicrophone();
        startGPS();
        startWifi();
    }

    @Override
    public void onPermissionsDenied(int requestCode) {
        Toast.makeText(this, getString(R.string.noPermissions), Toast.LENGTH_SHORT);
    }

    private void startAccelerometer() {
        if (check_accelerometer) {
            accelerometerSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            accelerometerSensor = accelerometerSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            accelerometerSensorManager.registerListener(accelerometerListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void startGyro() {
        if (check_gyro) {
            gyroSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            gyroSensor = gyroSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            gyroSensorManager.registerListener(gyroListener, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void startLight() {
        if (check_light) {
            lightSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            lightSensor = lightSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            lightSensorManager.registerListener(lightListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void startProximity() {
        if (check_proximity) {
            proximitySensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            proximitySensor = proximitySensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            proximitySensorManager.registerListener(proximityListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void startMicrophone() {
        if (check_microphone) {
            microphoneListener.startRecording();
        }
    }

    private void startGPS() {
        if(check_gps) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (!locationManager.isProviderEnabled(locationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(locationManager.NETWORK_PROVIDER)) {
                startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_GPS);
            } else {
                recordGPS();
            }
        }

    }

    private void recordGPS() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "GPS permissions not granted. Will not record GPS position.", Toast.LENGTH_SHORT);
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, gpsListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, gpsListener);
    }

    private void startWifi(){
        if(check_wifi) {
            //check if network location is on
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (!locationManager.isProviderEnabled(locationManager.NETWORK_PROVIDER)) {
                startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_NETWORK_FOR_WIFI);
            } else {
                recordWifi();
            }
        }
    }

    private void recordWifi(){
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }
        registerReceiver(mWifiScanReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mWifiManager.startScan();
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

        if(locationManager != null) {
            locationManager.removeUpdates(gpsListener);
        }

        if(mWifiManager != null) {
            unregisterReceiver(mWifiScanReceiver);
        }

    }

    @Override
    public void onShowRationalDialog(final PermissionInterface permissionInterface, int requestCode) {
        permissionInterface.onDialogShown();
    }

    @Override
    public void onShowSettings(final PermissionInterface permissionInterface, int requestCode) {
        permissionInterface.onSettingsShown();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case REQUEST_GPS:
                if(locationManager.isProviderEnabled(locationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(locationManager.NETWORK_PROVIDER)){
                    recordGPS();
                }else{
                    Toast.makeText(this, "GPS not on. Will not record GPS position.", Toast.LENGTH_SHORT);
                    switch_gps.setChecked(false);
                }
                break;

            case REQUEST_NETWORK_FOR_WIFI:
                if(locationManager.isProviderEnabled(locationManager.NETWORK_PROVIDER)){
                    recordWifi();
                }else{
                    Toast.makeText(this, "Network location not on. Will not record WiFi data.", Toast.LENGTH_SHORT);
                    switch_wifi.setChecked(false);
                }
                break;

        }
    }

    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                Integer max;
                Integer avg = 0;
                List<ScanResult> mScanResults = mWifiManager.getScanResults();

                max = mScanResults.get(0).level;
                if(mScanResults.size() > 0) {
                    for (ScanResult scanResult : mScanResults) {
                        avg += scanResult.level;
                        if(scanResult.level > max){
                            max = scanResult.level;
                        }
                    }

                    avg = avg / mScanResults.size();

                    String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
                    String fileName = "WiFi.csv";
                    String filePath = baseDir + File.separator + fileName;
                    File f = new File(filePath );
                    CSVWriter writer;
                    // File exist
                    try {
                        if (f.exists() && !f.isDirectory()) {
                            FileWriter mFileWriter = new FileWriter(filePath, true);
                            writer = new CSVWriter(mFileWriter);
                        } else {
                            writer = new CSVWriter(new FileWriter(filePath));
                        }

                        String[] data = {String.valueOf(System.currentTimeMillis()), avg.toString(), max.toString()};

                        writer.writeNext(data);

                        writer.close();
                    }catch (IOException e){
                        System.out.println("Error writing");
                    }
                }
            }
        }
    };

}
