package com.geekyengineer.framegrabber_screenrecorder;

import android.annotation.SuppressLint;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;


import android.os.Handler;
import android.util.Log;

import java.nio.ByteBuffer;

public class EncodeHandlerThread extends HandlerThread
{
private static final String TAG="encodeHandlerTHREad";
    private Handler handler;
    Codec codec;

    //////can remove messages and handlers
    public EncodeHandlerThread(String name, int priority) {
        super(name, Process.THREAD_PRIORITY_BACKGROUND);
        codec=new Codec();
        codec.codecSetup();

    }
    ////
        /////



    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
        handler=new  Handler(){
            @Override
            public void handleMessage(Message msg)
            {
                Log.e(TAG,"the number is "+msg.arg1);
                ByteBuffer inputData=(ByteBuffer)msg.obj;
                codec.enqueInput(inputData);
                codec.dequeOuput(false);

            }

        };

    }

    @Override
    public void run() {
        super.run();
    }
    public Handler getHandler()
    {
        return handler;
    }


}
