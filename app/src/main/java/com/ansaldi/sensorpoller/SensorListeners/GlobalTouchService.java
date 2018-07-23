package com.ansaldi.sensorpoller.SensorListeners;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVWriter;

import static android.content.Context.WINDOW_SERVICE;

/**
 * Created by Matteo on 12/07/2018.
 */

public class GlobalTouchService extends Service implements View.OnTouchListener {

    private String TAG = this.getClass().getSimpleName();
    // window manager
    private WindowManager mWindowManager;
    // linear layout will use to detect touch event
    private LinearLayout touchLayout;
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        // create linear layout
        touchLayout = new LinearLayout(this);
        // set layout width and height to 1px
        LayoutParams lp = new LayoutParams(1, 1);
        touchLayout.setLayoutParams(lp);
        // set color if you want layout visible on screen
        //  touchLayout.setBackgroundColor(Color.CYAN);
        // set on touch listener
        touchLayout.setOnTouchListener(this);

        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        // fetch window manager object
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        // set layout parameter of window manager
        WindowManager.LayoutParams mParams = new WindowManager.LayoutParams(
                1, // width of layout 1 px
                1, // height is equal to 1 px
                LAYOUT_FLAG, // Type Phone, These are non-application windows providing user interaction with the phone (in particular incoming calls).
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSPARENT);
        mParams.gravity = Gravity.LEFT | Gravity.TOP;
        Log.i(TAG, "add View");

        mWindowManager.addView(touchLayout, mParams);

    }


    @Override
    public void onDestroy() {
        if(mWindowManager != null) {
            if(touchLayout != null) mWindowManager.removeView(touchLayout);
        }

        //make Camera.csv visible via MTP
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = "Touch.csv";
        String filePath = baseDir + File.separator + fileName;
        File f = new File(filePath );
        //MediaScannerConnection.scanFile(this, new String[] {f.toString()}, null, null);

        Intent mediaScannerIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri fileContentUri = Uri.fromFile(f);
        mediaScannerIntent.setData(fileContentUri);
        this.sendBroadcast(mediaScannerIntent);

        super.onDestroy();
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //touch detected
        Log.i(TAG, "Action :" + event.getAction() + "\t X :" + event.getRawX() + "\t Y :"+ event.getRawY());
        writeToCSV();
        return false;
    }

    private void writeToCSV() {
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = "Touch.csv";
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

            Long timestamp = System.currentTimeMillis();
            String[] data = {timestamp.toString()};

            writer.writeNext(data);

            writer.close();
        }catch (IOException e){
            System.out.println("Error writing");
        }
    }

}
