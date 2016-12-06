package com.chalilayang.mediaextractordemo;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.chalilayang.mediaextractordemo.Utils.audio.MP3RadioStreamPlayer;
import com.chalilayang.mediaextractordemo.entities.VideoData;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DecodeActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final String SAMPLE = Environment.getExternalStorageDirectory() + "/video.mp4";
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

    private void stop()
    {
        player.stop();
    }
    private void initData() {
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
        private MediaExtractor extractor;
        private MediaCodec decoder;
        private AudioTrack audioTrack;

        @Override
        public void run() {
            ByteBuffer[] codecInputBuffers;
            ByteBuffer[] codecOutputBuffers;

            extractor = new MediaExtractor();
            try {
                extractor.setDataSource(videoToPlay.filePath);
            } catch (Exception e) {
                return;
            }
            MediaFormat format = null;
            String mime = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                format = extractor.getTrackFormat(i);
                mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio/")) {
                    extractor.selectTrack(i);
                    break;
                }
            }

            // the actual decoder
            try {
                decoder = MediaCodec.createDecoderByType(mime);
            } catch (IOException e) {
                e.printStackTrace();
            }
            decoder.configure(format, null, null, 0);
            decoder.start();
            codecInputBuffers = decoder.getInputBuffers();
            codecOutputBuffers = decoder.getOutputBuffers();

            // get the sample rate to configure AudioTrack
            int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);

            // create our AudioTrack instance
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

            // start playing, we will feed you later
            audioTrack.play();

            // start decoding
            final long kTimeOutUs = 10000;
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean sawInputEOS = false;
            boolean sawOutputEOS = false;
            int noOutputCounter = 0;
            int noOutputCounterLimit = 50;


            while (!sawOutputEOS && noOutputCounter < noOutputCounterLimit) {
                if (!sawInputEOS) {
                    int inputBufIndex = decoder.dequeueInputBuffer(kTimeOutUs);
                    if (inputBufIndex >= 0) {
                        ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];

                        int sampleSize =
                                extractor.readSampleData(dstBuf, 0 /* offset */);

                        long presentationTimeUs = 0;

                        if (sampleSize < 0) {
                            sawInputEOS = true;
                            sampleSize = 0;
                        } else {
                            presentationTimeUs = extractor.getSampleTime();
                        }

                        decoder.queueInputBuffer(
                                inputBufIndex,
                                0 /* offset */,
                                sampleSize,
                                presentationTimeUs,
                                sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);



                        if (!sawInputEOS) {
                            extractor.advance();
                        }
                    }
                }

                int res = decoder.dequeueOutputBuffer(info, kTimeOutUs);

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
                    decoder.releaseOutputBuffer(outputBufIndex, false /* render */);
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true;
                    }
                } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    codecOutputBuffers = decoder.getOutputBuffers();
                } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat oformat = decoder.getOutputFormat();
                }
            }

            if(sawOutputEOS) {
                audioTrack.play();
            }
        }
    }
}
