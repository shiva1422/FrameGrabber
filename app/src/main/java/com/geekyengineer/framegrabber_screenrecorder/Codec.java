package com.geekyengineer.framegrabber_screenrecorder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class Codec implements Runnable {
    static AtomicBoolean encoderStatus;
    static boolean endOfInput;
    MediaCodec audioEncoder;
    MediaCodec.BufferInfo bufferInfo;
    int obufId,ibufId,bytesRead;
    long TIMEOUT=10000,presentationUs=0;
    private static final String TAG="CODEC OBJECT";
    private final int ADTS_SIZE=7;
    private static final int CHANNEL_COUNT = 2,SAMPLE_RATE=48000;
    private static final int AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
    File outputFile;
    FileOutputStream outputStream;
    PipedInputStream pipedInputStream;
    int inputDatasize;
    boolean inputEos=false;
    byte[] midBuf;
    MediaFormat audioFormat;
    AtomicBoolean recording=new AtomicBoolean();

    //

 public void run() {
        try{
        codecSetup();
        while (recording.get())
        {
           // Thread.sleep(200);
           // enqueInput();
            dequeOuput(false);
        }

            while(pipedInputStream.available()>=0)
            {
              //  enqueInput();
                dequeOuput(false);
            }
        }catch (Exception e){e.printStackTrace();}
        inputEos=true;
        dequeOuput(true);
        releaseAudioEncoder();
    }

    public  void codecSetup()
    {
        try {

            outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "internalAudio.aac");
            outputStream=new FileOutputStream(outputFile);
            audioFormat=new MediaFormat();
            audioFormat.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
            audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, AAC_PROFILE);
            audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, 48000);
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128*1024);
            audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
            audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, AudioRecording.BUFFERSIZE);
            audioEncoder=MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            audioEncoder.configure(audioFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
            // audioEncoder.start();
            //remember start with draining encoder
            bufferInfo=new MediaCodec.BufferInfo();
            audioEncoder.start();

        }catch (Exception e)
        {e.printStackTrace();}

    }
    //
    //
    public void enqueInput(ByteBuffer inputData){



                ibufId = audioEncoder.dequeueInputBuffer(TIMEOUT);
            if (ibufId >= 0) {
                ByteBuffer inputBuffer = audioEncoder.getInputBuffer(ibufId);
                inputBuffer.clear();
                if (inputBuffer != null) {


                            inputBuffer.put(inputData);
                         //  presentationUs+=AudioRecording.BUFFERSIZE*1000000/SAMPLE_RATE;
                    }
                    //enqueInputBuffer
                    audioEncoder.queueInputBuffer(ibufId, 0, AudioRecording.BUFFERSIZE,presentationUs, 0);//inputEos ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                    Log.e(TAG,"enqued the input input Buffer");


                }


        }




    //
    //
    public void dequeOuput(boolean eos){

      //  if(eos) { audioEncoder.signalEndOfInputStream();}
while(true) {
    obufId = audioEncoder.dequeueOutputBuffer(bufferInfo, TIMEOUT);
    //
    if (obufId == MediaCodec.INFO_TRY_AGAIN_LATER) {
        Log.d(TAG, "try again later");
        // no output available yet
        if (!eos) {
             break; // out of while
        } else {
            Log.d(TAG, "no output available, but continuing same until EOS ");
        }

    }
    //
    else if (obufId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
        // should happen before receiving buffers, and should only happen once

        MediaFormat newFormat = audioEncoder.getOutputFormat();
        Log.d(TAG, "encoder output format changed: " + newFormat);
        // now that we have the Magic Goodies, start the muxer
    }
    //
    else if (obufId < 0) {
        Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " + obufId);
    }
    //
    else {
        ByteBuffer encodedData = audioEncoder.getOutputBuffer(obufId);
        if (encodedData == null) {
            throw new RuntimeException("encoderOutputBuffer " + obufId +
                    " was null");
        }

        if (bufferInfo.size != 0) {
            int outBitsSize = bufferInfo.size;
            int outPacketSize = outBitsSize + ADTS_SIZE;
          //  bufferInfo.presentationTimeUs+=AudioRecording.BUFFERSIZE*1000000/48000;
            drainOutputBuffer(outputStream, bufferInfo, outBitsSize, outPacketSize, encodedData);
        }
        encodedData.clear();
        audioEncoder.releaseOutputBuffer(obufId, false);
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            if (!eos) {
                Log.w(TAG, "reached end of stream unexpectedly");
            } else {
                Log.d(TAG, "end of stream reached");
            }
             break;
        }
    }

}


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
                Log.e(TAG,"written"+outPacketSize +"bytes to aac file");
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
/*
*/
