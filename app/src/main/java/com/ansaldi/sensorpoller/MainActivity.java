package com.ansaldi.sensorpoller;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.ansaldi.sensorpoller.SensorListeners.AccelerometerListener;
import com.ansaldi.sensorpoller.SensorListeners.CameraService;
import com.ansaldi.sensorpoller.SensorListeners.ContinuousReceiver;
import com.ansaldi.sensorpoller.SensorListeners.GlobalTouchService;
import com.ansaldi.sensorpoller.SensorListeners.GyroListener;
import com.ansaldi.sensorpoller.SensorListeners.LightListener;
import com.ansaldi.sensorpoller.SensorListeners.MicrophoneListener;
import com.ansaldi.sensorpoller.SensorListeners.ProximityListener;
import com.ansaldi.sensorpoller.SensorListeners.UncalibratedAccelerometerListener;
import com.ansaldi.sensorpoller.SensorListeners.WifiListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.kishan.askpermission.AskPermission;
import com.kishan.askpermission.ErrorCallback;
import com.kishan.askpermission.PermissionCallback;
import com.kishan.askpermission.PermissionInterface;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

import au.com.bytecode.opencsv.CSVWriter;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationParams;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, PermissionCallback, ErrorCallback {

    private static final int REQUEST_PERMISSIONS = 20;
    private static final int REQUEST_GPS = 30;
    private static final int REQUEST_NETWORK_FOR_WIFI = 40;
    private static final int PERMISSION_DRAW_OVER_APPS = 50;

    private static final String CHANNEL_ID = "SENSORPOLLER";
    private Integer NOTIFICATION_ID;

    private Switch switch_accelerometer;
    private Switch switch_gyro;
    private Switch switch_light;
    private Switch switch_proximity;
    //private Switch switch_microphone;
    //private Switch switch_gps;
    //private Switch switch_wifi;
    private Switch switch_camera;
    private Switch switch_touch;
    private TextView txt_status;
    private Button btn_startStop;
    private Button btn_upload;

    private Boolean running = false;
    private Boolean check_accelerometer = true;
    private Boolean check_gyro = true;
    private Boolean check_light = true;
    private Boolean check_proximity = true;
    private Boolean check_microphone = true;
    private Boolean check_gps = false;
    private Boolean check_wifi = false;
    private Boolean check_camera = true;
    private Boolean check_touch = true;
    private boolean mHandlingEvent = false;
    private boolean mRecording;
    private Boolean touchRunning = false;

    private SensorManager accelerometerSensorManager;
    private Sensor accelerometerSensor;

    private SensorManager uncalibratedAccelerometerSensorManager;
    private Sensor uncalibratedAccelerometerSensor;

    private SensorManager gyroSensorManager;
    private Sensor gyroSensor;

    private SensorManager lightSensorManager;
    private Sensor lightSensor;

    private SensorManager proximitySensorManager;
    private Sensor proximitySensor;

    private LocationManager locationManager;

    private WifiManager mWifiManager;

    private Intent globalService;

    private AccelerometerListener accelerometerListener;
    private UncalibratedAccelerometerListener uncalibratedAccelerometerListener;
    private GyroListener gyroListener;
    private LightListener lightListener;
    private ProximityListener proximityListener;
    private MicrophoneListener microphoneListener;
    private ContinuousReceiver mWifiScanReceiver;

    private PowerManager.WakeLock wakeLock;

    private Context context;
    private int PROGRESS_MAX;
    private int PROGRESS_CURRENT;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NOTIFICATION_ID = (int)(Math.random()*10000);
        context = this;

        switch_accelerometer = findViewById(R.id.switch_accelerometer);
        switch_gyro = findViewById(R.id.switch_gyro);
        switch_light = findViewById(R.id.switch_light);
        switch_proximity = findViewById(R.id.switch_proximity);
        //switch_microphone = findViewById(R.id.switch_microphone);
        //switch_gps = findViewById(R.id.switch_gps);
        //switch_wifi = findViewById(R.id.switch_wifi);
        switch_camera = findViewById(R.id.switch_camera);
        switch_touch = findViewById(R.id.switch_touch);

        switch_accelerometer.setVisibility(View.INVISIBLE);
        switch_gyro.setVisibility(View.INVISIBLE);
        switch_light.setVisibility(View.INVISIBLE);
        switch_proximity.setVisibility(View.INVISIBLE);
        switch_camera.setVisibility(View.INVISIBLE);
        switch_touch.setVisibility(View.INVISIBLE);

        txt_status = findViewById(R.id.txt_status);
        btn_startStop = findViewById(R.id.btn_startStop);
        btn_upload = findViewById(R.id.btn_upload);


        accelerometerListener = new AccelerometerListener();
        uncalibratedAccelerometerListener = new UncalibratedAccelerometerListener();
        gyroListener = new GyroListener();
        lightListener = new LightListener();
        proximityListener = new ProximityListener();
        microphoneListener = new MicrophoneListener();
        mWifiScanReceiver = new ContinuousReceiver(this, new WifiListener(), 5000);



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

        /*switch_microphone.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
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
        });*/

        switch_camera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                check_camera = b;
            }
        });

        switch_touch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                check_touch = b;
            }
        });

        btn_startStop.setOnClickListener(this);
        btn_upload.setOnClickListener(this);
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
            case R.id.btn_startStop:
                if(!running) {

                    btn_startStop.setText(getString(R.string.stop));

                    new AskPermission.Builder(this)
                            .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.RECORD_AUDIO,
                                    Manifest.permission.CAMERA)
                            .setCallback(this)
                            .setErrorCallback(this)
                            .request(REQUEST_PERMISSIONS);
                }else{
                    running = false;
                    if(wakeLock != null) {
                        if (wakeLock.isHeld()) {
                            wakeLock.release();
                        }
                    }
                    txt_status.setText(getString(R.string.paused));
                    unregisterListeners();
                    makeFilesVisible();

                    btn_startStop.setText(getString(R.string.start));
                }
                break;

            case R.id.btn_upload:
                //stop recording before uploading
                running = false;
                if(wakeLock != null) {
                    if (wakeLock.isHeld()) {
                        wakeLock.release();
                    }
                }
                txt_status.setText(getString(R.string.paused));
                unregisterListeners();
                makeFilesVisible();

                uploadDialog();
                break;
        }
    }


    //This allows MTP to show the file when the phone is connected to a pc
    private void makeFilesVisible() {
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = "LinearAccelerometer.csv";
        String filePath = baseDir + File.separator + fileName;
        File f = new File(filePath );
        MediaScannerConnection.scanFile(this, new String[] {f.toString()}, null, null);

        fileName = "UncalibratedAccelerometer.csv";
        filePath = baseDir + File.separator + fileName;
        f = new File(filePath );
        MediaScannerConnection.scanFile(this, new String[] {f.toString()}, null, null);

        fileName = "Gyroscope.csv";
        filePath = baseDir + File.separator + fileName;
        f = new File(filePath );
        MediaScannerConnection.scanFile(this, new String[] {f.toString()}, null, null);

        fileName = "Light.csv";
        filePath = baseDir + File.separator + fileName;
        f = new File(filePath );
        MediaScannerConnection.scanFile(this, new String[] {f.toString()}, null, null);

        fileName = "Proximity.csv";
        filePath = baseDir + File.separator + fileName;
        f = new File(filePath );
        MediaScannerConnection.scanFile(this, new String[] {f.toString()}, null, null);

        /*fileName = "microphone_8k16bitMono.csv";
        filePath = baseDir + File.separator + fileName;
        f = new File(filePath );
        MediaScannerConnection.scanFile(this, new String[] {f.toString()}, null, null);*/

        fileName = "GPS.csv";
        filePath = baseDir + File.separator + fileName;
        f = new File(filePath );
        MediaScannerConnection.scanFile(this, new String[] {f.toString()}, null, null);


        fileName = "WiFi.csv";
        filePath = baseDir + File.separator + fileName;
        f = new File(filePath );
        MediaScannerConnection.scanFile(this, new String[] {f.toString()}, null, null);

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
        //startMicrophone();
        /*startGPS();
        startWifi();*/
        startCameraAndTouch();
    }

    private void startCameraAndTouch() {
        if(check_camera || check_touch){
            // Check if Android M or higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(!Settings.canDrawOverlays(this)) {
                    // Show alert dialog to the user saying a separate permission is needed
                    drawPermissionAlert();
                }else{
                    //TODO: UWQRTUYWR
                    //request battery optimizations to be removed from the app
                    String packageName = context.getPackageName();
                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                        drawBatteryAlert();
                    }


                    if(check_camera) {
                        startRecording();
                    }
                    startTouch();
                }
            }else{
                if(check_camera) {
                    startRecording();
                }
                startTouch();
            }


        }
    }

    @Override
    public void onPermissionsDenied(int requestCode) {
        Toast.makeText(this, getString(R.string.noPermissions), Toast.LENGTH_SHORT);
    }

    private void startAccelerometer() {
        if (check_accelerometer) {
            //start linear accelerometer
            accelerometerSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            accelerometerSensor = accelerometerSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            accelerometerSensorManager.registerListener(accelerometerListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);

            //start uncalibrated accelerometer
            uncalibratedAccelerometerSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            uncalibratedAccelerometerSensor = uncalibratedAccelerometerSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            uncalibratedAccelerometerSensorManager.registerListener(
                    uncalibratedAccelerometerListener,
                    uncalibratedAccelerometerSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
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
            if (!locationManager.isProviderEnabled(locationManager.GPS_PROVIDER)) {
                startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_GPS);
            } else {
                recordGPS();
            }
        }

    }

    /*private void startCamera(){
        if(check_camera){
            // Check if Android M or higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(!Settings.canDrawOverlays(this)) {
                    // Show alert dialog to the user saying a separate permission is needed
                    drawPermissionAlert();
                }else{

                    startRecording();
                }
            }else{
                startRecording();
            }


        }
    }*/

    private void recordGPS() {
        SmartLocation.with(this).location()
                .config(LocationParams.NAVIGATION)
                .start(new OnLocationUpdatedListener() {
                    @Override
                    public void onLocationUpdated(Location location) {
                        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
                        String fileName = "GPS.csv";
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

                            String[] data = {String.valueOf(System.currentTimeMillis()), String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude())};

                            writer.writeNext(data);

                            writer.close();
                        }catch (IOException e){
                            System.out.println("Error writing");
                        }
                    }
                });
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

        mWifiScanReceiver.startScanning(true);
    }

    private void startTouch(){
        if(check_touch){
            globalService = new Intent(this,GlobalTouchService.class);
            startService(globalService);
            touchRunning = true;
            Toast.makeText(this, "Touch service started...", Toast.LENGTH_SHORT).show();
        }
    }


    private void unregisterListeners(){
        if(accelerometerSensorManager != null) {
            accelerometerSensorManager.unregisterListener(accelerometerListener);
        }
        if(uncalibratedAccelerometerSensorManager != null){
            uncalibratedAccelerometerSensorManager.unregisterListener(uncalibratedAccelerometerListener);
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

        //microphoneListener.stopRecording();

        if(locationManager != null) {
            SmartLocation.with(this).location().stop();
        }

        if(mWifiManager != null) {
            mWifiScanReceiver.stopScanning();
        }

        if(mRecording) {
            stopRecording();
        }

        if(touchRunning){
            stopService(globalService);
            Toast.makeText(this, "Touch service stopped", Toast.LENGTH_SHORT).show();
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
            /*case REQUEST_GPS:
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
                break;*/

            case PERMISSION_DRAW_OVER_APPS:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if(Settings.canDrawOverlays(context)){
                        if(check_camera) {
                            startRecording();
                        }
                        startTouch();
                    }
                }
                //request battery optimizations to be removed from the app
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    String packageName = context.getPackageName();
                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                        drawBatteryAlert();
                    }

                }

        }
    }

    private void startRecording() {
        if (!mHandlingEvent) {
            mHandlingEvent = true;
            ResultReceiver receiver = new ResultReceiver(new Handler()) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    setRecording(true);
                    handleStartRecordingResult(resultCode, resultData);
                    mHandlingEvent = false;
                }
            };

            CameraService.startToStartRecording(this,
                    Camera.CameraInfo.CAMERA_FACING_FRONT,
                    receiver);
        }
    }

    private void stopRecording() {
        if (!mHandlingEvent) {
            mHandlingEvent = true;
            ResultReceiver receiver = new ResultReceiver(new Handler()) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    setRecording(false);
                    handleStopRecordingResult(resultCode, resultData);
                    mHandlingEvent = false;
                }
            };
            CameraService.startToStopRecording(this, receiver);
        }
    }

    private void setRecording(boolean recording) {
        if (recording) {
            mRecording = true;
        } else {
            mRecording = false;
        }
    }

    private void handleStartRecordingResult(int resultCode, Bundle resultData) {
        if (resultCode == CameraService.RECORD_RESULT_OK) {
            Toast.makeText(this, "Start recording...", Toast.LENGTH_SHORT).show();
        } else {
            // start recording failed.
            Toast.makeText(this, "Start recording failed", Toast.LENGTH_SHORT).show();
            setRecording(false);
        }
    }

    private void handleStopRecordingResult(int resultCode, Bundle resultData) {
        if (resultCode == CameraService.RECORD_RESULT_OK) {
            Toast.makeText(this, "Camera service stopped successfully",
                    Toast.LENGTH_SHORT).show();
        } else if (resultCode == CameraService.RECORD_RESULT_UNSTOPPABLE) {
            Toast.makeText(this, "Stop recording failed", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Recording failed", Toast.LENGTH_SHORT).show();
            setRecording(true);
        }
    }

    private void drawPermissionAlert(){
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle(getString(R.string.drawTitle))
                .setMessage(getString(R.string.drawBody))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    public void onClick(DialogInterface dialog, int which) {
                        // Launch the settings activity
                        Intent myIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        startActivityForResult(myIntent, PERMISSION_DRAW_OVER_APPS);

                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void drawBatteryAlert(){
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle(getString(R.string.drawTitle))
                .setMessage(getString(R.string.batteryBody))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    public void onClick(DialogInterface dialog, int which) {
                        // Launch the settings activity to remove battery optimization
                        Intent myIntent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        startActivity(myIntent);

                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void uploadDialog() {

        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = "LinearAccelerometer.csv";
        String filePath = baseDir + File.separator + fileName;
        final File linearAccelerometer = new File(filePath );

        fileName = "UncalibratedAccelerometer.csv";
        filePath = baseDir + File.separator + fileName;
        final File uncalibratedAccelerometer = new File(filePath );

        fileName = "Gyroscope.csv";
        filePath = baseDir + File.separator + fileName;
        final File gyroscope = new File(filePath );

        fileName = "Light.csv";
        filePath = baseDir + File.separator + fileName;
        final File light = new File(filePath );

        fileName = "Proximity.csv";
        filePath = baseDir + File.separator + fileName;
        final File proximity = new File(filePath );

        fileName = "microphone_8k16bitMono.csv";
        filePath = baseDir + File.separator + fileName;
        final File microphone = new File(filePath );

        fileName = "Camera.csv";
        filePath = baseDir + File.separator + fileName;
        final File camera = new File(filePath );

        fileName = "Touch.csv";
        filePath = baseDir + File.separator + fileName;
        final File touch = new File(filePath );


        Double filesize = 0.0;
        filesize += linearAccelerometer.length();
        filesize += uncalibratedAccelerometer.length();
        filesize += gyroscope.length();
        filesize += light.length();
        filesize += proximity.length();
        filesize += microphone.length();
        filesize += camera.length();
        filesize += touch.length();

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                context);

        // set title
        alertDialogBuilder.setTitle(getString(R.string.uploadConfirmation));

        // set dialog message
        alertDialogBuilder
                .setMessage(getString(R.string.filesize) + " " + String.format( "%.2f", filesize/(1024*1024) ) + "MB")
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        //create an upload notification
                        createNotificationChannel();

                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);
                        mBuilder.setContentTitle(getString(R.string.fileUpload))
                                .setContentText(getString(R.string.uploadProgress))
                                .setSmallIcon(R.drawable.ic_baseline_cloud_upload_24px)
                                .setPriority(NotificationCompat.PRIORITY_LOW);

                        Double filesize = 0.0;
                        filesize += linearAccelerometer.length();
                        filesize += uncalibratedAccelerometer.length();
                        filesize += gyroscope.length();
                        filesize += light.length();
                        filesize += proximity.length();
                        filesize += microphone.length();
                        filesize += camera.length();
                        filesize += touch.length();

                        PROGRESS_MAX = filesize.intValue();
                        PROGRESS_CURRENT = 0;
                        mBuilder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
                        notificationManager.notify(NOTIFICATION_ID, mBuilder.build());


                        //upload the files one by one
                        String uuid = UUID.randomUUID().toString();

                        if(linearAccelerometer.length() > 0) {
                            uploadFile(linearAccelerometer, "LinearAccelerometer", uuid, mBuilder, notificationManager);
                        }
                        if(uncalibratedAccelerometer.length() > 0) {
                            uploadFile(uncalibratedAccelerometer, "UncalibratedAccelerometer", uuid, mBuilder, notificationManager);
                        }
                        if(gyroscope.length() > 0) {
                            uploadFile(gyroscope, "Gyroscope", uuid, mBuilder, notificationManager);
                        }
                        if(light.length() > 0) {
                            uploadFile(light, "Light", uuid, mBuilder, notificationManager);
                        }
                        if(proximity.length() > 0) {
                            uploadFile(proximity, "Proximity", uuid, mBuilder, notificationManager);
                        }
                        if(microphone.length() > 0) {
                            uploadFile(microphone, "Microphone", uuid, mBuilder, notificationManager);
                        }
                        if(camera.length() > 0) {
                            uploadFile(camera, "Camera", uuid, mBuilder, notificationManager);
                        }
                        if(touch.length() > 0){
                            uploadFile(touch, "Touch", uuid, mBuilder, notificationManager);
                        }


                    }
                })
                .setNegativeButton(getString(R.string.no),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void updateNotification(NotificationCompat.Builder mBuilder, int PROGRESS_MAX, int PROGRESS_CURRENT, NotificationManagerCompat notificationManager){
        mBuilder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
        notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }



    private void uploadFile(final File f, final String folder, String uuid, final NotificationCompat.Builder mBuilder, final NotificationManagerCompat notificationManager){
        StorageReference mStorageRef;
        mStorageRef = FirebaseStorage.getInstance().getReference();

        Uri file = Uri.fromFile(f);
        StorageReference folderRef = mStorageRef.child(folder + "/" + uuid + "_"  + folder + ".csv");

        folderRef.putFile(file)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content
                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        System.out.println(folder + " uploaded");
                        PROGRESS_CURRENT += f.length();
                        if(PROGRESS_CURRENT < PROGRESS_MAX) {
                            updateNotification(mBuilder, PROGRESS_MAX, PROGRESS_CURRENT, notificationManager);
                        }else{
                            mBuilder
                                    .setPriority(NotificationManager.IMPORTANCE_HIGH)
                                    .setContentText(getString(R.string.uploadCompleted))
                                    .setProgress(0, 0, false);
                            notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
                        }
                        f.delete();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        // ...
                        System.out.println(folder + " failed");
                    }
                });
    }

}
