package com.ansaldi.sensorpoller.SensorListeners;


import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVWriter;

public class MicrophoneListener {

    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format

    public void startRecording() {

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        recorder.startRecording();

        isRecording = true;

        recordingThread = new Thread(new Runnable() {

            public void run() {

                writeAudioDataToFile();

            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }


    private String[] short2String(short[] sData){
        int shortArrsize = sData.length;
        String[] stringArray = new String[shortArrsize];

        for(int i = 0; i < shortArrsize; i++){
            stringArray[i] = String.valueOf(sData[i]);
        }

        return stringArray;
    }

    private void writeAudioDataToFile() {
        // Write the output audio in byte
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = "microphone_8k16bitMono.csv";
        String filePath = baseDir + File.separator + fileName;

        short sData[] = new short[BufferElements2Rec];

        File f = new File(filePath );
        CSVWriter writer;

        while (isRecording) {
            // gets the voice output from microphone to byte format
            recorder.read(sData, 0, BufferElements2Rec);
            System.out.println("Short writing to file" + sData.toString());
            // File exist
            try {
                if (f.exists() && !f.isDirectory()) {
                    FileWriter mFileWriter = new FileWriter(filePath, true);
                    writer = new CSVWriter(mFileWriter);
                } else {
                    writer = new CSVWriter(new FileWriter(filePath));
                }

                String[] data = short2String(sData);
                for(int i = 0; i < sData.length; i++) {
                    writer.writeNext(new String[]{data[i]});
                }

                writer.close();
            }catch (IOException e){
                System.out.println("Error writing");
            }

        }


    }

    public void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;


            recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
        }
    }


}
