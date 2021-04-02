package com.geekyengineer.framegrabber_screenrecorder;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.MediaStore;
import android.renderscript.RenderScript;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import static android.app.Activity.RESULT_OK;
import static com.geekyengineer.framegrabber_screenrecorder.Pradarsh.REQUESTCODE;

public class RecordingService extends Service {
    public static final String CHANNEL_ID="serviceChannel";
    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private HandlerThread handlerThread=null;
    Notification notification;
    private Surface recordingSurface;//persistant surface
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 30;               // 30fps
    private static final int IFRAME_INTERVAL = 15;// 10 seconds between I-frames
    private MediaCodec encoder;
    private MediaMuxer muxer;
    private int trackIndex;
    private boolean muxerStarted;
    private MediaCodec.BufferInfo bufferInfo;
    private static boolean isRecording=true;
    private static File ouputDir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
    String intentMsg;
    String TAG="RECORDING SERVICE";
    long presentationTimeUs=0;
    MediaProjection mediaProjection;
    AudioRecord audioRecord;
    AudioRecording recording;



    public RecordingService() {
        super();
    }
    /////////////////////////                SERVICE HANDLER
    private final class ServiceHandler extends Handler{

        public ServiceHandler(Looper looper)
    {
        super(looper);

        }
        @Override
        public void handleMessage(@NonNull Message msg) {
            if(msg.arg1==1422)
            internalAudioCapture();

            //  int i=100;
        //    while(i>0) {


             /*   try {
                   // Thread.sleep(50);
                    prepareEncoder(ouputDir);
                    for(int i=0;i<3000&&isRecording==true;i++)
                    {
                        drainEncoder(false);
                      //  presentationTimeUs+=1000000000/FRAME_RATE;
                        Canvas canvas=recordingSurface.lockHardwareCanvas();
                        canvas.drawColor(Color.RED);
                        Paint paint = new Paint();
                        paint.setColor(Color.WHITE);
                        paint.setStyle(Paint.Style.FILL);
                        canvas.drawPaint(paint);
                        paint.setColor(Color.RED);
                        paint.setTextSize(50);
                        canvas.drawText("Some Text"+presentationTimeUs, 10, 25, paint);
                       // canvas.drawText("booming "+new Random().nextInt(),100,100,1000,1000,paint);
                        recordingSurface.unlockCanvasAndPost(canvas);

                    }
                    drainEncoder(true);

                } catch (Exception ie) {
                    ie.printStackTrace();
                }
                finally {
                    releaseEncoder();
                    if(intentMsg.equals("stop"))
                        stopSelf();
                }
                Log.e("Service Thead", "the number is"  + " no of processores" + Runtime.getRuntime().availableProcessors());

               // i--;*/ //from try to here
            }

        }


    /////////////////////////                SERVICE HANDLER
    @Override
    public void onCreate()
    {
        //super.onCreate();
    createNotificationChannel();
        handlerThread=new HandlerThread("ServiceStartArguments", android.os.Process.THREAD_PRIORITY_BACKGROUND);//thread for service to be independnet of proess main thread
        handlerThread.start();
        serviceLooper=handlerThread.getLooper();
        serviceHandler=new ServiceHandler(serviceLooper);


    }


    @Override
    public int onStartCommand(Intent intent,int flags,int startId)
    {
        intentMsg=intent.getStringExtra("todo");
        if(intentMsg.equals("stop"))
            isRecording=false;
        else
            isRecording=true;
       // super.onStartCommand(intent,flags,startId);
        switch (intentMsg)
        {
            case "stop":
                recording.recordingStatus.set(false);
                isRecording=false;
                stopSelf();
                break;
            case "start":
                isRecording=true;
                Intent notificationIntent=new Intent(this,Pradarsh.class);//for reacting to notification press opening pradashactivity also need pending intnet.
                PendingIntent pendingIntent=PendingIntent.getActivity(this,1422,notificationIntent,0);
                notification=new NotificationCompat.Builder(this,CHANNEL_ID).setContentTitle("RECORDING SERVICE").setContentText("recording").setSmallIcon(R.drawable.ic_launcher_background)
                        .setContentIntent(pendingIntent).build();
                startForeground(startId,notification);
               Message msg=serviceHandler.obtainMessage();
               msg.arg1=startId;
                serviceHandler.sendMessage(msg);
                break;
            case "data":
                int resultCode=intent.getIntExtra("resultCode",0);
                MediaProjectionManager mediaProjectionManager=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
              mediaProjection=(MediaProjection)mediaProjectionManager.getMediaProjection(resultCode,(Intent)intent.getParcelableExtra("data"));
                if(mediaProjection!=null)
                {Log.e("mediaProjction","recieved successfully");}
                Message message=serviceHandler.obtainMessage();
                message.arg1=1422;
                serviceHandler.sendMessage(message);
                break;


        }

        Log.e("foregorund","service fresh start");

        return START_STICKY;
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.e("foregorund","service stopped");


    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
       // throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }
    public void createNotificationChannel()
    {


        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
        {
            NotificationChannel serviceChannel=new NotificationChannel(CHANNEL_ID,"serviceNotificationChannel", NotificationManager.IMPORTANCE_HIGH );
            NotificationManager notificationManager=getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(serviceChannel);
        }


    }
    public void prepareEncoder(File ouputDir)/////////////////////////////////////PREPARE ENCODER
    {

        bufferInfo=new MediaCodec.BufferInfo();
        MediaFormat mediaFormat=MediaFormat.createVideoFormat(MIME_TYPE,1080,720);////////////////////////need more work;
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE,2000000);//////////////////////make value dynamic;
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE,FRAME_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,IFRAME_INTERVAL);


        try{
        encoder=MediaCodec.createEncoderByType(MIME_TYPE);
        encoder.configure(mediaFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);

        recordingSurface=encoder.createInputSurface();
      //  encoder.setInputSurface(recordingSurface);
        encoder.start();
        //now creating muxer

            muxer=new MediaMuxer(ouputDir+"/vid.mp4",MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            Log.e("MUXER","CReated" );


        }catch(IOException ioe){ioe.printStackTrace();
            throw new RuntimeException("MediaMuxer creation failed", ioe);}
        trackIndex=-1;
        muxerStarted=false;



    }////////////////////////////////////PREPARE ENCODER
    public void drainEncoder(boolean endOfStream)                                            ////////////////DRAIN ENCODER
    { final int TIMEOUT_USEC = 10000;
        Log.d(TAG, "drainEncoder(" + endOfStream + ")");
        if (endOfStream) {
             Log.d(TAG, "sending EOS to encoder");
            encoder.signalEndOfInputStream();
        }
        // Retrieve the set of OutputBuffers
    //    ByteBuffer[] encoderOutputBuffers = encoder.getOutputBuffers();
        // 获取编码数据数组
        while (true) {
            // Returns the index of an output buffer that has been successfully decoded.
            // or one of the INFO_* constants.
            int encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                Log.d(TAG, "try again later");
                // no output available yet
                if (!endOfStream) {
                    break; // out of while
                } else {
                     Log.d(TAG, "no output available, spinning to await EOS");
                }
            } /*else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                Log.d(TAG, "output buffers changed");
              //  encoderOutputBuffers = encoder.getOutputBuffers();}*/
             else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (muxerStarted) {
                    throw new RuntimeException("format changed twice");
                }
                MediaFormat newFormat = encoder.getOutputFormat();
                Log.d(TAG, "encoder output format changed: " + newFormat);
                // now that we have the Magic Goodies, start the muxer
                trackIndex = muxer.addTrack(newFormat);
                muxer.start();
                muxerStarted = true;
            } else if (encoderStatus < 0) {
                    Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                            encoderStatus);
                // let's ignore it
            } else {
                ByteBuffer encodedData = encoder.getOutputBuffer(encoderStatus);
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                            " was null");
                }
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status, Ignore it.
                    Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG: " + bufferInfo.flags);
                    bufferInfo.size = 0;
                }
                if (bufferInfo.size != 0) {
                    if (!muxerStarted) {
                        throw new RuntimeException("muxer hasn't started");
                    }
                    // adjust the ByteBuffer values to match BufferInfo
                    //encodedData.position(bufferInfo.offset); // 设置偏移量
                   // encodedData.limit(bufferInfo.offset + bufferInfo.size); // 设置limit
                    bufferInfo.presentationTimeUs = presentationTimeUs;
                    presentationTimeUs += 1000000L / FRAME_RATE;
                    // 使用Muxer写入视频数据
                    muxer.writeSampleData(trackIndex, encodedData, bufferInfo);
                    Log.d(TAG, "sent " + bufferInfo.offset + "/" + bufferInfo.size + " bytes to muxer");
                }
                // release释放索引位置对应的buffer
                encoder.releaseOutputBuffer(encoderStatus, false);
                // endOfStream -> break;
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "reached end of stream unexpectedly");
                    } else {
                         Log.d(TAG, "end of stream reached");
                    }
                    break; // out of while
                }
            }
        }

    }///////////////////////////////////////DRAIN ENCODER
    public void releaseEncoder()
    {
        if(encoder!=null)
        {
            encoder.stop();
            encoder.release();
            encoder=null;
        }
        if(recordingSurface!=null)
        {
            recordingSurface.release();
            recordingSurface=null;
        }
        if(muxer!=null)
        {
            muxer.stop();
            muxer.release();
            muxer=null;
        }
    }

    public void internalAudioCapture()
    { AudioRecording.BUFFERSIZE=AudioRecord.getMinBufferSize(48000,2,AudioFormat.ENCODING_PCM_16BIT);
        AudioRecording.BUFFERSIZE=48000*5;
        for(int i=0;i<10;i++)
        {
            Log.e("the buffer size","is "+AudioRecording.BUFFERSIZE);
        }
        AudioFormat audioFormat=new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(48000).setChannelMask(AudioFormat.CHANNEL_IN_STEREO).build();
        AudioPlaybackCaptureConfiguration audioPlaybackCaptureConfiguration=new AudioPlaybackCaptureConfiguration.Builder(mediaProjection).addMatchingUsage(AudioAttributes.USAGE_MEDIA).build();
        audioRecord=new AudioRecord.Builder().setAudioPlaybackCaptureConfig(audioPlaybackCaptureConfiguration).setAudioFormat(audioFormat).setBufferSizeInBytes(AudioRecording.BUFFERSIZE).build();
        recording=new AudioRecording();
        recording.audioRecord=audioRecord;
        recording.recordingStatus.set(true);
       // recording.setPriority(Thread.MAX_PRIORITY);
        audioRecord.startRecording();
        recording.start();

    }
}
