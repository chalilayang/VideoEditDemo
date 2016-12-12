package com.chalilayang.mediaextractordemo.Utils.common.audioplayer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.text.TextUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by chalilayang on 2016/12/9.
 */

public class PreviewAudioPlayer {
    private MediaExtractor audioExtractor;
    private MediaCodec audioDecoder;
    private AudioTrack audioTrack;
    private int audioTrackIndex = -1;
    private int sampleRate = -1;
    private int channelCount = -1;
    private long audioDuration = -1;
    private MediaFormat audioFormat = null;
    private String filePath = null;
    private String mime = null;
    private volatile boolean stop = false;
    private volatile boolean isPlaying = false;
    private AudioPlayRunnable playRunnable;
    private Thread playThread;

    /**
     * 正常播放结束标识
     */
    private volatile boolean isPlayOver;

    public boolean openAudio(String filepath) {
        boolean result = true;
        if (TextUtils.isEmpty(filepath)) {
            result = false;
            return result;
        }
        if (audioExtractor != null) {
            audioExtractor.release();
        }
        audioExtractor = new MediaExtractor();
        try {
            audioExtractor.setDataSource(filepath);
            final int trackCount = audioExtractor.getTrackCount();
            for (int index = 0; index < trackCount; index++) {
                MediaFormat format = audioExtractor.getTrackFormat(index);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio/")) {
                    audioExtractor.selectTrack(index);
                    audioTrackIndex = index;
                    audioFormat = format;
                    this.mime = mime;
                    break;
                }
            }
            audioDuration = audioFormat.getLong(MediaFormat.KEY_DURATION);
            sampleRate = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            channelCount = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        } catch (IOException e) {
            result = false;
            e.printStackTrace();
        }
        this.filePath = filepath;
        return result;
    }

    public void play() {
        if (isPlaying) {
            return;
        }
        if (audioTrack == null && !createAudioTrack()) {
            return;
        }
        if (audioDecoder == null && !createMediaCodec()) {
            return;
        }
        if (playRunnable == null) {
            playRunnable = new AudioPlayRunnable();
        }
        startPlayThread();
    }

    public void startPlayThread() {
        if (playThread != null) {
            playThread.interrupt();
            playThread = null;
        }
        playThread = new Thread(playRunnable);
        playThread.start();
    }

    public void stopPlayThread() {
        if (playThread != null) {
            stop = true;
            isPlaying = false;
            playThread.interrupt();
        }
    }

    public void pausePlay() {
        if (audioTrack != null) {
            audioTrack.pause();
        }
    }

    public void resumePlay() {
        if (audioTrack != null) {
            audioTrack.play();
        }
    }

    private boolean createMediaCodec() {
        boolean result = true;
        if (this.audioTrackIndex != -1) {
            try {
                audioExtractor.selectTrack(this.audioTrackIndex);
                audioDecoder = MediaCodec.createDecoderByType(mime);
                audioDecoder.configure(audioFormat, null, null, 0);
                audioDecoder.start();
            } catch (IOException e) {
                result = false;
                e.printStackTrace();
            }
        } else {
            result = false;
        }
        return result;
    }

    private boolean createAudioTrack() {
        boolean result = true;
        if (this.filePath == null) {
            return false;
        }
        if (audioTrack != null) {
            audioTrack.release();
        }
        int minBufferSize = AudioTrack.getMinBufferSize(this.sampleRate,
                AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        int bufferSize = 4 * minBufferSize;
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        if (channelCount >= 2) {
            channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
        }
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                this.sampleRate,
                channelConfig,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM);
        audioTrack.play();
        return result;
    }

    public void releaseResource() {
        stopPlayThread();
        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;
        }
        if (audioDecoder != null) {
            audioDecoder.release();
            audioDecoder = null;
        }
        if (audioExtractor != null) {
            audioExtractor.release();
            audioExtractor = null;
        }
    }

    private class AudioPlayRunnable implements Runnable {

        @Override
        public void run() {
            isPlaying = true;
            ByteBuffer[] inputBuffersAudio = null;
            ByteBuffer[] outputBuffersAudio = null;
            MediaCodec.BufferInfo infoAudio = null;

            audioDecoder.flush();
            audioTrack.play();
            while (!Thread.interrupted() && !isPlayOver && !stop) {
                inputBuffersAudio = audioDecoder.getInputBuffers();
                outputBuffersAudio = audioDecoder.getOutputBuffers();
                infoAudio = new MediaCodec.BufferInfo();

                int inIndex = -1;
                try {
                    inIndex = audioDecoder.dequeueInputBuffer(10000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (inIndex >= 0) {
                    ByteBuffer buffer = inputBuffersAudio[inIndex];
                    int sampleSize = audioExtractor.readSampleData(buffer, 0);
                    if (sampleSize < 0) {
                        audioDecoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec
                                .BUFFER_FLAG_END_OF_STREAM);
                        buffer.clear();
                        isPlayOver = true;
                    } else {
                        audioDecoder.queueInputBuffer(inIndex, 0, sampleSize, audioExtractor
                                .getSampleTime(), 0);
                        buffer.clear();
                        audioExtractor.advance();
                    }
                }

                int outIndex = -1;
                try {
                    outIndex = audioDecoder.dequeueOutputBuffer(infoAudio, 10000);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                switch (outIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        outputBuffersAudio = audioDecoder.getOutputBuffers();
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        MediaFormat format = audioDecoder.getOutputFormat();
                        if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            audioTrack.setPlaybackRate(format.getInteger(MediaFormat
                                    .KEY_SAMPLE_RATE));
                        }
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        break;
                    default:
                        if (outIndex >= 0) {
                            ByteBuffer buffer = outputBuffersAudio[outIndex];
                            byte[] chunk = new byte[infoAudio.size];
                            buffer.get(chunk);
                            buffer.clear();
                            if (chunk.length > 0) {
                                audioTrack.write(chunk, 0, chunk.length);
                            }
                            audioDecoder.releaseOutputBuffer(outIndex, false);
                        }
                        break;
                }
                if (isPlayOver) {
                    break;
                }
            }
        }
    }
}
