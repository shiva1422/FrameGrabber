package com.geekyengineer.framegrabber_screenrecorder;

import android.media.AudioRecord;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioRecording extends Thread {
  AtomicBoolean recordingStatus=new AtomicBoolean();
    public static  int BUFFERSIZE;/////////////////////////need more work
    AudioRecord audioRecord;
    EncodeHandlerThread encodeHandlerThread;
    Handler encodeHandler;
   Message msg;
   int i=0;
   static PipedOutputStream pipedOutputStream;

    @Override
    public void run()
    {
        encodeHandlerThread=new EncodeHandlerThread("encodeHandling Thread",Thread.MAX_PRIORITY);
        encodeHandlerThread.start();
        try{//sleep to initialize enocdeThread handler
        sleep(5);}catch (InterruptedException ie){ie.printStackTrace();}
        encodeHandler=encodeHandlerThread.getHandler();
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
        final ByteBuffer byteBuffer=ByteBuffer.allocateDirect(BUFFERSIZE);
        while(recordingStatus.get())
            {

                int result=audioRecord.read(byteBuffer,BUFFERSIZE);
                Log.e("the bytes","read from audioRecord is" +result);
                if (result < 0) {
                    throw new RuntimeException("Reading of audio buffer failed: " +
                            getBufferReadFailureReason(result));
                }
                msg=Message.obtain(encodeHandlerThread.getHandler());
                msg.arg1=i++;
                msg.obj=byteBuffer;
               encodeHandler.sendMessage(msg);
                byteBuffer.clear();

            }
        try{
        encodeHandlerThread.join();}catch (InterruptedException e){e.printStackTrace();}
        encodeHandlerThread.quitSafely();



    }
    private String getBufferReadFailureReason(int errorCode) {
        switch (errorCode) {
            case AudioRecord.ERROR_INVALID_OPERATION:
                return "ERROR_INVALID_OPERATION";
            case AudioRecord.ERROR_BAD_VALUE:
                return "ERROR_BAD_VALUE";
            case AudioRecord.ERROR_DEAD_OBJECT:
                return "ERROR_DEAD_OBJECT";
            case AudioRecord.ERROR:
                return "ERROR";
            default:
                return "Unknown (" + errorCode + ")";
        }
    }
}
