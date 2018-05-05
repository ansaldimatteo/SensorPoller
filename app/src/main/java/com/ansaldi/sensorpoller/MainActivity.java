package com.ansaldi.sensorpoller;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.ansaldi.sensorpoller.SensorListeners.AccelerometerListener;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private Switch switch_accelerometer;
    private Button btn_start;

    private Boolean check_accelerometer = false;

    private SensorManager mSensorManager;
    private Sensor mSensor;

    private AccelerometerListener accelerometerListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        switch_accelerometer = findViewById(R.id.switch_accelerometer);
        btn_start = findViewById(R.id.btn_start);

        accelerometerListener = new AccelerometerListener();

        switch_accelerometer.setOnCheckedChangeListener(this);

        btn_start.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mSensorManager != null) {
            mSensorManager.unregisterListener(accelerometerListener);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_start:
                btn_start.setBackgroundColor(Color.RED);
                if(check_accelerometer){
                    startAccelerometer();
                }
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        check_accelerometer = b;
    }

    private void startAccelerometer(){
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorManager.registerListener(accelerometerListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

}
