package com.geekyengineer.framegrabber_screenrecorder;

import android.media.AudioDeviceCallback;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.nio.ByteBuffer;

public class AudioEncoding extends Thread{
    static final long TIMEOUT=1000;//waiting for dequeOUTPUTBUFFER;
    int dequeStatus,inputBufferId,outputBufferId;
MediaCodec.BufferInfo bufferInfo;
MediaFormat audioFormat;
static MediaCodec audioEncoder;
ByteBuffer inputBuffer,outputBuffer;
File outputFile;
PipedInputStream pipedInputStream;
     FileOutputStream outputStream;
  static final int ADTS_SIZE = 7;
    private static final int CHANNEL_COUNT = 2;
    private static final int AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
    Handler encodeHandler;

    byte[] inputData;
int channelCount=2,sampleRate=41800;
public static final String MIME_TYPE="audio/mpeg";//for mp3

  /*  @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        encoderSetup();
        encodeHandler=new Handler(getLooper()){
            @Override
            public void handleMessage(Message msg)
            {
                super.handleMessage(msg);
                ByteBuffer byteBuffer=(ByteBuffer)msg.obj;
                encode(byteBuffer);



            }

        };


    }*/


@Override
public void run()
{
    ByteBuffer byteBuffer;
    encoderSetup();
    try{
    AudioRecording.pipedOutputStream.connect(pipedInputStream);}catch (Exception e){e.printStackTrace();}
    //encode(byteBuffer);
}

    public void encoderSetup()
    {    pipedInputStream=new PipedInputStream();
        outputFile=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),"internalAudio.aac");


        try{
          outputStream=new FileOutputStream(outputFile);

            audioFormat=new MediaFormat();
            audioFormat.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
            audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, 48000);
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
            audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
            audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, AudioRecording.BUFFERSIZE*2);
            audioEncoder=MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            audioEncoder.configure(audioFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
           // audioEncoder.start();
            //remember start with draining encoder
            bufferInfo=new MediaCodec.BufferInfo();
            audioEncoder.start();




        }catch (Exception e){e.printStackTrace();}
    }
    public void enqueEncoder()
    {
        inputBufferId=audioEncoder.dequeueInputBuffer(TIMEOUT);
        if(inputBufferId>=0) {
            inputBuffer = audioEncoder.getInputBuffer(inputBufferId);//get empty inputBuffer From encoder
            if(inputBuffer!=null)
            inputBuffer.put(inputData);//fill the input buffer with data;
            audioEncoder.queueInputBuffer(inputBufferId, 0, inputData.length, 0, 0);
        }
        else
            Log.e("no Buffers available ","now audioEncoder enque encoder");


    }


public void dequeEncoder(Boolean endOfStream)
{
//if(endOfStream)
//{audioEncoder.signalEndOfInputStream();
//Log.e("signalled end of stream","audioEncoder");}
outputBufferId=audioEncoder.dequeueOutputBuffer(bufferInfo,TIMEOUT);
if(outputBufferId>=0)
{
if(bufferInfo.flags!=0)
    audioEncoder.releaseOutputBuffer(outputBufferId,false);
else {
    try {
        if(outputBuffer!=null)
        outputStream.write(outputBuffer.array(), 0, outputBuffer.position());
    } catch (Exception e) {
        e.printStackTrace();
    }
    audioEncoder.releaseOutputBuffer(outputBufferId, false);
}

}


}
public void encode(ByteBuffer byteBuffer)
{
    boolean inputEos=false;
    boolean ouputEos=false;
    int noOutputCounter=0;
    try{
        while(!ouputEos&&noOutputCounter<50)
        {
            noOutputCounter++;
            if(!inputEos)
            {
                inputBufferId=audioEncoder.dequeueInputBuffer(TIMEOUT);
                if(inputBufferId>=0)
                {
                    inputBuffer=audioEncoder.getInputBuffer(inputBufferId);
                            if(inputBuffer!=null)
                            {
                                int inputBufferSize=inputBuffer.capacity();
                                ByteBuffer byteBuffer1=ByteBuffer.allocateDirect(inputBufferSize);
                                int bytestRead=19;//fake remove this and take bytesReadfromBelow
                               // int bytesRead = inputStream.read(buffer, 0, bufferSize);//read data from audioRecord into someBuffer first but can directly read to inputBuffer
                                    long presentationUs=System.nanoTime();
                                    if(bytestRead<0)
                                    {
                                        Log.e("eno of input stream","found");
                                        inputEos=true;
                                        bytestRead=0;
                                    }
                                    else
                                    {//put data into inputBuffer
                                      //  ByteArrayOutputStream outStream = new ByteArrayOutputStream(bufferSize);
                                       // outStream.write(buffer, 0, bytesRead);
                                       // inputBuffer.put(outStream.toByteArray());
                                        inputBuffer.put(inputData);
                                    }
                                    //enqueInputBuffer
                                audioEncoder.queueInputBuffer(inputBufferId,0,bytestRead,presentationUs,inputEos?MediaCodec.BUFFER_FLAG_END_OF_STREAM:0);

                            }
                }
            }

        }
        //// see if codec has encoded data in a new output buffer
        outputBufferId=audioEncoder.dequeueOutputBuffer(bufferInfo,TIMEOUT);
        if(outputBufferId>=0)
        {
            if(bufferInfo.size>0)
            {
                noOutputCounter=0;
            }
            // prepare output buffer including ADTS header
            int outBitsSize=bufferInfo.size;
            int outPacketSize=outBitsSize+ADTS_SIZE;
            outputBuffer=audioEncoder.getOutputBuffer(outputBufferId);
            if(outputBuffer!=null)
            {
                // add encoded data to file
                drainOutputBuffer(outputStream, bufferInfo, outBitsSize, outPacketSize, outputBuffer);
            }
            audioEncoder.releaseOutputBuffer(outputBufferId,false);
            if(isEndOfStream(bufferInfo))
            {
                Log.e("END of OUTPUT","stream found");
                ouputEos=true;
            }
        }

    }catch (Exception e){e.printStackTrace();}

}
    /**
     * extracts the packet from the outputBuffer, adds an ADTS header and appends the encoded data to
     * the outputStream (i.e. the encoded aac file).
     */
    private void drainOutputBuffer(FileOutputStream outputStream, MediaCodec.BufferInfo info, int outBitsSize, int outPacketSize, ByteBuffer outputBuffer) {
        // set position and limit of outputBuffer
        outputBuffer.position(info.offset);
        outputBuffer.limit(info.offset + outBitsSize);

        try {
            // prepare byte array containing encoded data
            byte[] data = new byte[outPacketSize];

            // add ADTS header to data packet
            addADTStoPacket(data, outPacketSize);

            // place encoded audio + ADTS header into data array
            outputBuffer.get(data, ADTS_SIZE, outBitsSize);

            // update outputBuffer position
            outputBuffer.position(info.offset);

            // only write real audio data (exclude codec info and EOS info)
            if (!isCodecInfo(info) && !isEndOfStream(info)) {
                outputStream.write(data, 0, outPacketSize);
            }

        } catch (IOException e) {
            Log.e("draingin", "failed writing bit stream data to file");
            e.printStackTrace();

        }

        outputBuffer.clear();
    }
    private void addADTStoPacket(byte[] packet, int packetLength) {
        int profile = AAC_PROFILE;
        int chanCfg = CHANNEL_COUNT;

        // 0: 96000 Hz
        // 1: 88200 Hz
        // 2: 64000 Hz
        // 3: 48000 Hz
        // 4: 44100 Hz
        // 5: 32000 Hz
        int freqIdx = 3;

        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLength >> 11));
        packet[4] = (byte) ((packetLength & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLength & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }
    private boolean isEndOfStream(MediaCodec.BufferInfo info) {
        return (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
    }
    private boolean isCodecInfo(MediaCodec.BufferInfo info) {
        return (info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0;
    }
public void releaseAudioEncoder()
{audioEncoder.stop();
audioEncoder.release();audioEncoder=null;}
}
