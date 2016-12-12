package com.chalilayang.mediaextractordemo;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.chalilayang.mediaextractordemo.Utils.audio.MP3RadioStreamPlayer;
import com.chalilayang.mediaextractordemo.entities.VideoData;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class DecodeActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final String TAG = "DecodeActivity";
    private VideoDecodeThread videoPlayer = null;
    private AudioDecodeThread audioPlayer = null;
    private VideoData videoToPlay;
    private MP3RadioStreamPlayer player;
    private void play(String file)
    {
        if(player != null)
        {
            player.stop();
            player.release();
            player = null;

        }

        player = new MP3RadioStreamPlayer();
        player.setUrlString(file);

        try {
            player.play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static String getVideoPath(Context context, Uri uri) {
        Uri videopathURI = uri;
        if (uri.getScheme().toString().compareTo("content") == 0) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                videopathURI = Uri.parse(cursor.getString(column_index));
                return videopathURI.getPath();
            }
        } else if (uri.getScheme().compareTo("file") == 0) {
            return videopathURI.getPath();
        }

        return videopathURI.toString();
    }
    private void stop()
    {
        player.stop();
    }
    private void initData() {
        Intent intent = getIntent();
        if (intent.getAction() != null &&
                Intent.ACTION_VIEW.equals(intent.getAction())) {
            String path = getVideoPath(getApplicationContext(), intent.getData());
            String name = path.substring(path.lastIndexOf(File.separator));
            videoToPlay = new VideoData(path, name);
        }
        Bundle b = getIntent().getExtras();
        if (b != null) {
            String name = b.getString(MainActivity.KEY_FILE_NAME);
            String path = b.getString(MainActivity.KEY_FILE_PATH);
            videoToPlay = new VideoData(path, name);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
        SurfaceView sv = new SurfaceView(this);
        sv.getHolder().addCallback(this);
        setContentView(sv);
    }

    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (videoPlayer == null) {
            videoPlayer = new VideoDecodeThread(holder.getSurface());
            videoPlayer.start();
        }
//        play(videoToPlay.filePath);
        if (audioPlayer == null) {
            audioPlayer = new AudioDecodeThread();
            audioPlayer.start();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (videoPlayer != null) {
            videoPlayer.interrupt();
        }
//        stop();
        if (audioPlayer != null) {
            audioPlayer.stopPlay();
            audioPlayer.interrupt();
        }
    }

    private class VideoDecodeThread extends Thread {
        private MediaExtractor extractor;
        private MediaCodec decoder;
        private Surface surface;

        public VideoDecodeThread(Surface surface) {
            this.surface = surface;
        }

        @Override
        public void run() {
            try {
                extractor = new MediaExtractor();
                try {
                    extractor.setDataSource(videoToPlay.filePath);
                    for (int i = 0; i < extractor.getTrackCount(); i++) {
                        MediaFormat format = extractor.getTrackFormat(i);
                        String mime = format.getString(MediaFormat.KEY_MIME);
                        if (mime.startsWith("video/")) {
                            extractor.selectTrack(i);
                            decoder = MediaCodec.createDecoderByType(mime);
                            decoder.configure(format, surface, null, 0);
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (decoder == null) {
                    Log.e("DecodeActivity", "Can't find video info!");
                    return;
                }

                decoder.start();

                ByteBuffer[] inputBuffers = decoder.getInputBuffers();
                ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                boolean isEOS = false;
                long startMs = System.currentTimeMillis();

                while (!Thread.interrupted()) {
                    if (!isEOS) {
                        int inIndex = decoder.dequeueInputBuffer(10000);
                        if (inIndex >= 0) {
                            ByteBuffer buffer = inputBuffers[inIndex];
                            int sampleSize = extractor.readSampleData(buffer, 0);
                            if (sampleSize < 0) {
                                Log.d("DecodeActivity", "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                                decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec
                                        .BUFFER_FLAG_END_OF_STREAM);
                                isEOS = true;
                            } else {
                                decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor
                                        .getSampleTime(), 0);
                                extractor.advance();
                            }
                        }
                    }

                    int outIndex = decoder.dequeueOutputBuffer(info, 10000);
                    switch (outIndex) {
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
                            outputBuffers = decoder.getOutputBuffers();
                            break;
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            Log.d("DecodeActivity", "New format " + decoder.getOutputFormat());
                            break;
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
                            break;
                        default:
                            ByteBuffer buffer = outputBuffers[outIndex];
                            Log.v("DecodeActivity", "We can't use this buffer but render it due " +
                                    "to the API limit, " + buffer);

                            while (info.presentationTimeUs / 1000 > System.currentTimeMillis() -
                                    startMs) {
                                try {
                                    sleep(10);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    break;
                                }
                            }
                            decoder.releaseOutputBuffer(outIndex, true);
                            break;
                    }

                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                        break;
                    }
                }

                decoder.stop();
                decoder.release();
                extractor.release();
            } catch (MediaCodec.CryptoException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class AudioDecodeThread extends Thread {
        private MediaExtractor audioExtractor;
        private MediaCodec audioDecoder;
        private AudioTrack audioTrack;

        public void stopPlay() {
            audioTrack.stop();
            audioTrack.release();
        }
        @Override
        public void run() {
            ByteBuffer[] codecInputBuffers;
            ByteBuffer[] codecOutputBuffers;

            audioExtractor = new MediaExtractor();
            try {
                audioExtractor.setDataSource(videoToPlay.filePath);
            } catch (Exception e) {
                return;
            }
            MediaFormat format = null;
            String mime = null;
            for (int i = 0; i < audioExtractor.getTrackCount(); i++) {
                format = audioExtractor.getTrackFormat(i);
                mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio/")) {
                    audioExtractor.selectTrack(i);
                    break;
                }
            }

            try {
                audioDecoder = MediaCodec.createDecoderByType(mime);
            } catch (IOException e) {
                e.printStackTrace();
            }
            audioDecoder.configure(format, null, null, 0);
            audioDecoder.start();
            codecInputBuffers = audioDecoder.getInputBuffers();
            codecOutputBuffers = audioDecoder.getOutputBuffers();

            int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            Log.i(TAG, "run: channelCount " + channelCount);
            audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    AudioTrack.getMinBufferSize (
                            sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT
                    ),
                    AudioTrack.MODE_STREAM
            );

            audioTrack.play();
            final long kTimeOutUs = 10000;
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean sawInputEOS = false;
            boolean sawOutputEOS = false;
            int noOutputCounter = 0;
            int noOutputCounterLimit = 50;

            while (!sawOutputEOS && noOutputCounter < noOutputCounterLimit) {
                if (!sawInputEOS) {
                    int inputBufIndex = audioDecoder.dequeueInputBuffer(kTimeOutUs);
                    if (inputBufIndex >= 0) {
                        ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                        int sampleSize = audioExtractor.readSampleData(dstBuf, 0);
                        long presentationTimeUs = 0;
                        if (sampleSize < 0) {
                            sawInputEOS = true;
                            sampleSize = 0;
                        } else {
                            presentationTimeUs = audioExtractor.getSampleTime();
                        }
                        audioDecoder.queueInputBuffer(
                                inputBufIndex,
                                0,
                                sampleSize,
                                presentationTimeUs,
                                sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                        if (!sawInputEOS) {
                            audioExtractor.advance();
                        }
                    }
                }

                int res = audioDecoder.dequeueOutputBuffer(info, kTimeOutUs);
                if (res >= 0) {
                    if (info.size > 0) {
                        noOutputCounter = 0;
                    }
                    int outputBufIndex = res;
                    ByteBuffer buf = codecOutputBuffers[outputBufIndex];

                    final byte[] chunk = new byte[info.size];
                    buf.get(chunk);
                    buf.clear();
                    if(chunk.length > 0){
                        audioTrack.write(chunk,0,chunk.length);
                    }
                    audioDecoder.releaseOutputBuffer(outputBufIndex, false);
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true;
                    }
                } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    codecOutputBuffers = audioDecoder.getOutputBuffers();
                } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat oformat = audioDecoder.getOutputFormat();
                    int cc = oformat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    Log.i(TAG, "INFO_OUTPUT_FORMAT_CHANGED cc " + cc);
                }
            }
        }
    }
}
