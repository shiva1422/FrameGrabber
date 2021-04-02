package com.geekyengineer.framegrabber_screenrecorder;

import android.os.Process;
import android.util.Log;

import java.security.Policy;

public class BackThread implements Runnable {
    int i=100000;
    @Override
    public void run()
    {
        while(i>0)
        {android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            try{Thread.sleep(50);}
            catch (InterruptedException ie){ie.printStackTrace();}
            Log.e("Background Thead","the number is"+i+" no of processores"+Runtime.getRuntime().availableProcessors());
            i--;
        }
    }
}
