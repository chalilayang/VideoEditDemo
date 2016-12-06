package com.chalilayang.mediaextractordemo.Utils.common.audioplayer;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;

import java.io.IOException;


/**
 * AudioTrack 播放音频文件
 * Created by tianbin on 16/5/17.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class AudioPlayer implements DecodeAudioRunnable.OnPlayListener {

    private MediaExtractor mExtractor;
    private MediaCodec mAudioDecoder;
    private AudioTrack mAudioTrack;
    private int audioTrackIndex = -1;
    private int sampleRate = -1;
    private MediaFormat format = null;
    private String mime = null;
    private DecodeAudioRunnable mDecodeAudioRunnable;

    public void play(Context context, Uri uri) throws IOException {
        if (!isPlaying()) {
            setDataSource(context, uri);
            play();
        }
    }

    private void setDataSource(Context context, Uri uri) throws IOException {
        createExtractor(context, uri);
        createMediaCodec();
        createAudioTrack();
    }

    private void play() {
        mAudioTrack.play();
        mAudioDecoder.start();
        startPlayRunnable();
    }


    private void createExtractor(Context context, Uri uri) throws IOException {
        mExtractor = new MediaExtractor();
        mExtractor.setDataSource(context, uri, null);
        int trackCount = mExtractor.getTrackCount();
        for (int index = 0; index < trackCount; index ++) {
            MediaFormat tmp = mExtractor.getTrackFormat(index);
            String mime = tmp.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                this.sampleRate = tmp.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                this.audioTrackIndex = index;
                this.format = tmp;
                this.mime = mime;
                break;
            }
        }
    }

    private void createMediaCodec() throws IOException {
        if (this.audioTrackIndex != -1) {
            mExtractor.selectTrack(this.audioTrackIndex);
            mAudioDecoder = MediaCodec.createDecoderByType(mime);
            mAudioDecoder.configure(getTrackFormat(), null, null, 0);
        }
    }

    private void createAudioTrack() {
        int minBufferSize = AudioTrack.getMinBufferSize(this.sampleRate,
                AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        int bufferSize = 4 * minBufferSize;
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                this.sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM);
    }

    private void startPlayRunnable() {
        mDecodeAudioRunnable = new DecodeAudioRunnable(mExtractor, mAudioDecoder, mAudioTrack, format);
        mDecodeAudioRunnable.setOnPlayListener(this);
        mDecodeAudioRunnable.start();
    }

    /**
     * 是否正在播放
     *
     * @return
     */
    public boolean isPlaying() {
        return mDecodeAudioRunnable != null && mDecodeAudioRunnable.isRunning();
    }



    private MediaFormat getTrackFormat() {
        return this.format;
    }

    @Override
    public void onComplete() {
        release();
    }

    private void release() {
        if(mAudioTrack != null){
            mAudioTrack.release();
            mAudioTrack = null;
        }
        if(mAudioDecoder != null){
            mAudioDecoder.release();
            mAudioDecoder = null;
        }
        if(mExtractor != null){
            mExtractor.release();
            mExtractor = null;
        }
    }
}
