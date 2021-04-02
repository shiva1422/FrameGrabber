package com.geekyengineer.framegrabber_screenrecorder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

public class Pradarsh extends Activity {
    final String[] PERMISSIONSNEEDED={Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.FOREGROUND_SERVICE};
    final int PERMISSIONCODE=1422;
    MediaProjectionManager mediaProjectionManager;
    MediaProjection mediaProjection;
    MediaProjectionCallback mediaProjectionCallback;
     final static int REQUESTCODE=1422;
     VirtualDisplay virtualDisplay;
     Surface mirrorSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ConstraintLayout layout=new ConstraintLayout(this);
        layout.setId(View.generateViewId());
        SurfaceView surfaceView=new SurfaceView(this);
        surfaceView.setId(View.generateViewId());
        Button play=new Button(this);
        Button pause= new Button(this);
        play.setId(View.generateViewId());
        pause.setId(View.generateViewId());
        pause.setText("pause");
        play.setText("play");
        layout.addView(play,200,200);
        layout.addView(pause,200,200);
        layout.addView(surfaceView,1080,720);
        ConstraintSet set=new ConstraintSet();
        set.clone(layout);
        set.connect(play.getId(),ConstraintSet.TOP,layout.getId(),ConstraintSet.TOP,0);
        set.connect(play.getId(),ConstraintSet.LEFT,layout.getId(),ConstraintSet.LEFT,0);
        set.connect(pause.getId(),ConstraintSet.TOP,layout.getId(),ConstraintSet.TOP,0);
        set.connect(pause.getId(),ConstraintSet.LEFT,play.getId(),ConstraintSet.RIGHT,0);
        set.connect(surfaceView.getId(),ConstraintSet.TOP,pause.getId(),ConstraintSet.BOTTOM,20);
        set.applyTo(layout);
        setContentView(layout);
        mirrorSurface=surfaceView.getHolder().getSurface();
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

               // Thread thread=new Thread(new BackThread());
                //thread.start();
                Intent intent=new Intent(Pradarsh.this,RecordingService.class);
                intent.putExtra("todo","start");
               startService(intent);
                checkPermissions();
//startRecordingService();
            }
        });
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(Pradarsh.this,RecordingService.class);
                intent.putExtra("todo","stop");
                startService(intent);

            }
        });
        mediaProjectionManager=(MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);

    }
    public void startRecordingService()
    {
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),REQUESTCODE);

      /*  if(mediaProjection==null)///////for screenRecord
        {
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),REQUESTCODE);
            return;
        }

virtualDisplay=createVirtualDisplay();*/


    }
    private VirtualDisplay createVirtualDisplay()
    {
        return mediaProjection.createVirtualDisplay("VirtualDisplay",1080,720,450, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,mirrorSurface,null,null);
    }
    public void checkPermissions()                                                 //PERMISSIONS
    {
        if((ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)+ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE))== PackageManager.PERMISSION_GRANTED)
        {
            startRecordingService();
        }
        else if(shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)||shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))
        {
            ////////////////////////////more showrequestRationale
        }
else {
    ActivityCompat.requestPermissions(Pradarsh.this,PERMISSIONSNEEDED,PERMISSIONCODE);

        }
    }
   @Override
 public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults)
   {
       super.onRequestPermissionsResult(requestCode,permissions,grantResults);//////////////////////////////more for filling
       startRecordingService();



   }
                                                                                   //PERMISSIONS
    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data)
    {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=REQUESTCODE)
        {
            Log.e("activity result","error unknown");
            return;

        }
        if(resultCode!=RESULT_OK)
        {
            Log.e("activity result","Permission denied");
            return;

        }
        Intent intent=new Intent(Pradarsh.this,RecordingService.class);
        intent.putExtra("todo","data");
        intent.putExtra("data",data);
        intent.putExtra("resultCode",resultCode);
        startService(intent);
        /*mediaProjectionCallback=new MediaProjectionCallback();
        mediaProjection=mediaProjectionManager.getMediaProjection(resultCode,data);
        mediaProjection.registerCallback(mediaProjectionCallback,null);*/
        finishAndRemoveTask();
        recordAudio();




    }
    public void recordAudio()
    {


    }
    private class MediaProjectionCallback extends MediaProjection.Callback{
        @Override
        public void onStop()
        {
            //stop recording
            mediaProjection.stop();
            mediaProjection=null;
            if(virtualDisplay==null)
                return;
            virtualDisplay.release();
            if(mediaProjection!=null)
            {
                mediaProjection.unregisterCallback(mediaProjectionCallback);
                mediaProjection.stop();
                mediaProjection=null;
            }
            super.onStop();
        }

    }


}
