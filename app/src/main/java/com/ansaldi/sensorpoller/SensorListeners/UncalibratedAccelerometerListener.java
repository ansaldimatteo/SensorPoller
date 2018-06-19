package com.ansaldi.sensorpoller.SensorListeners;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Created by Matteo on 19/06/2018.
 */

public class UncalibratedAccelerometerListener implements SensorEventListener {
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Float xAcceleration = sensorEvent.values[0];
        Float yAcceleration = sensorEvent.values[1];
        Float zAcceleration = sensorEvent.values[2];

        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = "UncalibratedAccelerometer.csv";
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

            String[] data = {String.valueOf(System.currentTimeMillis()), xAcceleration.toString(), yAcceleration.toString(), zAcceleration.toString()};

            writer.writeNext(data);

            writer.close();
        }catch (IOException e){
            System.out.println("Error writing");
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
