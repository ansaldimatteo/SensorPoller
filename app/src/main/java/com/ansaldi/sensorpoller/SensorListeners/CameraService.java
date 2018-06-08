package com.ansaldi.sensorpoller.SensorListeners;

/**
 * Created by Matteo on 08/06/2018.
 */

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;

public class CameraService extends Service {
    private static final String TAG = CameraService.class.getSimpleName();

    public static final String RESULT_RECEIVER = "resultReceiver";
    public static final String VIDEO_PATH = "recordedVideoPath";

    public static final int RECORD_RESULT_OK = 0;
    public static final int RECORD_RESULT_DEVICE_NO_CAMERA= 1;
    public static final int RECORD_RESULT_GET_CAMERA_FAILED = 2;
    public static final int RECORD_RESULT_ALREADY_RECORDING = 3;
    public static final int RECORD_RESULT_NOT_RECORDING = 4;
    public static final int RECORD_RESULT_UNSTOPPABLE = 5;

    private static final String START_SERVICE_COMMAND = "startServiceCommands";
    private static final int COMMAND_NONE = -1;
    private static final int COMMAND_START_RECORDING = 0;
    private static final int COMMAND_STOP_RECORDING = 1;

    private static final String SELECTED_CAMERA_FOR_RECORDING = "cameraForRecording";

    private Camera mCamera;
    private MediaRecorder mMediaRecorder;

    private Context context;

    private boolean continueTakingPhotos = true;
    private boolean mRecording = false;
    private String mRecordingPath = null;

    private long timestamp;

    public CameraService() {
    }

    public static void startToStartRecording(Context context, int cameraId,
                                             ResultReceiver resultReceiver) {
        Intent intent = new Intent(context, CameraService.class);
        intent.putExtra(START_SERVICE_COMMAND, COMMAND_START_RECORDING);
        intent.putExtra(SELECTED_CAMERA_FOR_RECORDING, cameraId);
        intent.putExtra(RESULT_RECEIVER, resultReceiver);
        context.startService(intent);
    }

    public static void startToStopRecording(Context context, ResultReceiver resultReceiver) {
        Intent intent = new Intent(context, CameraService.class);
        intent.putExtra(START_SERVICE_COMMAND, COMMAND_STOP_RECORDING);
        intent.putExtra(RESULT_RECEIVER, resultReceiver);
        context.startService(intent);
    }

    /**
     * Used to take picture.
     */
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = Util.getOutputMediaFile(Util.MEDIA_TYPE_IMAGE);

            if (pictureFile == null) {
                return;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inDither = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap image = BitmapFactory.decodeByteArray(data, 0, data.length, options);

            Matrix matrix = new Matrix();
            matrix.postRotate(270);
            Bitmap rotatedBitmap = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);

            FaceDetector detector = new FaceDetector.Builder(context)
                    .setTrackingEnabled(true)
                    .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                    .build();

            Frame frame = new Frame.Builder().setBitmap(rotatedBitmap).build();

            SparseArray<Face> faces = detector.detect(frame);

            writeToCSV(faces.size());

            if(continueTakingPhotos){
                timestamp = System.currentTimeMillis();
                mCamera.takePicture(null, null, mPicture);
            }
        }
    };

    private void writeToCSV(int numFaces) {
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = "Camera.csv";
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

            String[] data = {String.valueOf(timestamp), String.valueOf(numFaces)};

            writer.writeNext(data);

            writer.close();
        }catch (IOException e){
            System.out.println("Error writing");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            throw new IllegalStateException("Must start the service with intent");
        }
        switch (intent.getIntExtra(START_SERVICE_COMMAND, COMMAND_NONE)) {
            case COMMAND_START_RECORDING:
                handleStartRecordingCommand(intent);
                break;
            case COMMAND_STOP_RECORDING:
                handleStopRecordingCommand(intent);
                break;
            default:
                throw new UnsupportedOperationException("Cannot start service with illegal commands");
        }

        return START_NOT_STICKY;
    }

    private void handleStartRecordingCommand(Intent intent) {

        context = this;

        if (!Util.isCameraExist(this)) {
            throw new IllegalStateException("There is no device, not possible to start recording");
        }

        final ResultReceiver resultReceiver = intent.getParcelableExtra(RESULT_RECEIVER);

        if (mRecording) {
            // Already recording
            resultReceiver.send(RECORD_RESULT_ALREADY_RECORDING, null);
            return;
        }
        mRecording = true;

        final int cameraId = intent.getIntExtra(SELECTED_CAMERA_FOR_RECORDING,
                Camera.CameraInfo.CAMERA_FACING_BACK);
        mCamera = Util.getCameraInstance(cameraId);
        if (mCamera != null) {
            SurfaceView sv = new SurfaceView(this);

            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(1, 1,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT);

            SurfaceHolder sh = sv.getHolder();

            sv.setZOrderOnTop(true);
            sh.setFormat(PixelFormat.TRANSPARENT);

            sh.addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    Camera.Parameters params = mCamera.getParameters();
                    mCamera.setParameters(params);
                    Camera.Parameters p = mCamera.getParameters();

                    List<Camera.Size> listSize;

                    listSize = p.getSupportedPreviewSizes();
                    Camera.Size mPreviewSize = listSize.get(2);
                    Log.v(TAG, "preview width = " + mPreviewSize.width
                            + " preview height = " + mPreviewSize.height);
                    p.setPreviewSize(mPreviewSize.width, mPreviewSize.height);

                    listSize = p.getSupportedPictureSizes();
                    Camera.Size mPictureSize = listSize.get(2);
                    Log.v(TAG, "capture width = " + mPictureSize.width
                            + " capture height = " + mPictureSize.height);
                    p.setPictureSize(mPictureSize.width, mPictureSize.height);
                    mCamera.setParameters(p);

                    try {
                        mCamera.setPreviewDisplay(holder);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mCamera.startPreview();

                    timestamp = System.currentTimeMillis();
                    mCamera.takePicture(null, null, mPicture);

                    resultReceiver.send(RECORD_RESULT_OK, null);
                    Log.d(TAG, "Recording is started");
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                }
            });


            wm.addView(sv, params);

        } else {
            Log.d(TAG, "Get Camera from service failed");
            resultReceiver.send(RECORD_RESULT_GET_CAMERA_FAILED, null);
        }
    }

    private void handleStopRecordingCommand(Intent intent) {
        ResultReceiver resultReceiver = intent.getParcelableExtra(RESULT_RECEIVER);
        continueTakingPhotos = false;
        mCamera.release();

        //make Camera.csv visible via MTP
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = "Camera.csv";
        String filePath = baseDir + File.separator + fileName;
        File f = new File(filePath );
        MediaScannerConnection.scanFile(this, new String[] {f.toString()}, null, null);


        Bundle b = new Bundle();
        b.putString(VIDEO_PATH, mRecordingPath);
        resultReceiver.send(RECORD_RESULT_OK, b);

        Log.d(TAG, "recording is finished.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
