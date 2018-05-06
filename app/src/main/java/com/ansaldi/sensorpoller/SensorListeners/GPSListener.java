package com.ansaldi.sensorpoller.SensorListeners;


import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVWriter;

public class GPSListener implements LocationListener{


    @Override
    public void onLocationChanged(Location location) {
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

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
