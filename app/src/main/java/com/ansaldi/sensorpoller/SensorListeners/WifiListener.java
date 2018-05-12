package com.ansaldi.sensorpoller.SensorListeners;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.wifi.ScanResult;

import com.ansaldi.sensorpoller.SensorListeners.ContinuousReceiver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Created by Matteo on 12/05/2018.
 */

public class WifiListener implements ContinuousReceiver.ScanResultsListener {
    @Override
    public void onScanResultsReceived(List results) {
        Integer max;
        Integer avg = 0;
        List<ScanResult> mScanResults = results;

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
