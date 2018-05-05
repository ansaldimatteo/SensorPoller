package com.ansaldi.sensorpoller;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.ansaldi.sensorpoller.SensorListeners.AccelerometerListener;
import com.ansaldi.sensorpoller.SensorListeners.GyroListener;
import com.ansaldi.sensorpoller.SensorListeners.LightListener;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Switch switch_accelerometer;
    private Switch switch_gyro;
    private Switch switch_light;
    private TextView txt_status;
    private Button btn_start;
    private Button btn_stop;

    private Boolean check_accelerometer = false;
    private Boolean check_gyro = false;
    private Boolean check_light = false;

    private SensorManager accelerometerSensorManager;
    private Sensor accelerometerSensor;

    private SensorManager gyroSensorManager;
    private Sensor gyroSensor;

    private SensorManager lightSensorManager;
    private Sensor lightSensor;

    private AccelerometerListener accelerometerListener;
    private GyroListener gyroListener;
    private LightListener lightListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        switch_accelerometer = findViewById(R.id.switch_accelerometer);
        switch_gyro = findViewById(R.id.switch_gyro);
        switch_light = findViewById(R.id.switch_light);
        txt_status = findViewById(R.id.txt_status);
        btn_start = findViewById(R.id.btn_start);
        btn_stop = findViewById(R.id.btn_stop);


        accelerometerListener = new AccelerometerListener();
        gyroListener = new GyroListener();
        lightListener = new LightListener();


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

        btn_start.setOnClickListener(this);
        btn_stop.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(accelerometerSensorManager != null) {
            accelerometerSensorManager.unregisterListener(accelerometerListener);
        }
        if(gyroSensorManager != null){
            gyroSensorManager.unregisterListener(gyroListener);
        }
        if(lightSensorManager != null){
            lightSensorManager.unregisterListener(lightListener);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_start:
                txt_status.setText(getString(R.string.running));
                startAccelerometer();
                startGyro();
                startLight();
                break;

            case R.id.btn_stop:
                txt_status.setText(getString(R.string.paused));
                if(accelerometerSensorManager != null) {
                    accelerometerSensorManager.unregisterListener(accelerometerListener);
                }
                if(gyroSensorManager != null){
                    gyroSensorManager.unregisterListener(gyroListener);
                }
                if(lightSensorManager != null){
                    lightSensorManager.unregisterListener(lightListener);
                }
                break;
        }
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

}
