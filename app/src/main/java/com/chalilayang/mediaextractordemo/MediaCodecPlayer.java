package com.chalilayang.mediaextractordemo;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.chalilayang.mediaextractordemo.entities.VideoData;
import com.google.common.collect.Interner;

import java.nio.ByteBuffer;

import static java.lang.Thread.sleep;

public class MediaCodecPlayer extends AppCompatActivity
        implements SeekBar.OnSeekBarChangeListener,
        SurfaceHolder.Callback {
    private static final String TAG = "MediaCodecPlayer";
    private static final int MSG_PARSED = 21;

    private static final int MSG_DECODE = 27;
    private static final int MAX = 10000;//Integer.MAX_VALUE;
    public DisplayMetrics mDisplayMetrics;
    private SurfaceView surfaceView;
    private SeekBar seekBar;
    private int screenWidthPx;
    private int screenHeightPx;
    private HandlerThread handlerThread;
    private DecodeHandler seekHandler;
    private VideoData videoToPlay;

    private MediaExtractor mediaExtractor;
    private MediaCodec mediaCodec;

    private long videoDuration; // in microseconds
    private long audioDuration; // in microseconds
    private int videoHint;
    private int video_width;
    private int video_height;
    private int sampleRate;
    private int audioChannelCount;
    private int frameRate;
    private int trackNumber;
    private boolean videoHasParsed = false;

    private boolean surfaceCreated = false;
    private Handler handler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            switch (msg.what) {
                case MSG_PARSED:
                    seekBar.setMax(MAX);
                    break;
            }
        }
    };
    AsyncTask<VideoData, Void, Void> parseTask = new AsyncTask<VideoData, Void, Void>() {
        @Override
        protected Void doInBackground(VideoData... params) {
            for (VideoData video : params
                    ) {
                parseVideo(video);
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.i(TAG, "onPreExecute: 开始解析");
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            videoHasParsed = true;
            handler.obtainMessage(MSG_PARSED).sendToTarget();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_codec_player);
        initMetrics();
        initView();
        initData();
    }

    private void initData() {
        Bundle b = getIntent().getExtras();
        if (b != null) {
            String name = b.getString(MainActivity.KEY_FILE_NAME);
            String path = b.getString(MainActivity.KEY_FILE_PATH);
            videoToPlay = new VideoData(path, name);
        }
    }

    private void initMetrics() {
        mDisplayMetrics = getApplicationContext().getResources().getDisplayMetrics();
        screenWidthPx = mDisplayMetrics.widthPixels;
        screenHeightPx = mDisplayMetrics.heightPixels;
        Log.i("displayMetrics", mDisplayMetrics.toString());
    }

    private void initView() {
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.hide();
        }
        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        surfaceView.getLayoutParams().height = screenHeightPx / 2;
        surfaceView.getHolder().addCallback(this);

        seekBar = (SeekBar) findViewById(R.id.seek_bar);
        seekBar.setOnSeekBarChangeListener(this);
        ((RelativeLayout.LayoutParams) seekBar.getLayoutParams()).topMargin = screenHeightPx / 8;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (handlerThread == null) {
            handlerThread = new HandlerThread("decode thread", Thread.NORM_PRIORITY);
            handlerThread.start();
            seekHandler = new DecodeHandler(handlerThread.getLooper());
        }
    }

    @Override
    protected void onPause() {
        super.onResume();
        seekHandler.removeCallbacksAndMessages(null);
        handlerThread.quit();
        handlerThread = null;
        seekHandler = null;
        parseTask.cancel(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaExtractor != null) {
            mediaExtractor.release();
        }
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
        }
    }

    private void parseVideo(VideoData videoData) {
        mediaExtractor = new MediaExtractor();
        try {
            String path = videoData.filePath;
            mediaExtractor.setDataSource(path);
        } catch (Exception e) {
            Log.e(TAG, "error path" + e.getMessage());
        }

        MediaFormat mediaFormat = null;
        final int trackCount = this.trackNumber = mediaExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            try {
                mediaFormat = mediaExtractor.getTrackFormat(i);
                final String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    video_width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
                    video_height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
                    videoDuration = mediaFormat.getLong(MediaFormat.KEY_DURATION);
                    if (mediaFormat.containsKey(MediaFormat.KEY_ROTATION)) {
                        videoHint = mediaFormat.getInteger(MediaFormat.KEY_ROTATION);
                    }
                    if (mediaFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        frameRate = mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
                    }
                    mediaCodec = MediaCodec.createDecoderByType(mime);
                    mediaCodec.configure(mediaFormat, surfaceView.getHolder().getSurface(), null,
                            0);
                    mediaCodec.start();
                } else if (mime.startsWith("audio/")) {
                    this.sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    this.audioChannelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    audioDuration = mediaFormat.getLong(MediaFormat.KEY_DURATION);
                }
                Log.d(TAG, "file mime is " + mime);
            } catch (Exception e) {
                Log.e(TAG, " read error " + e.getMessage());
            }
        }
        videoHasParsed = true;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (surfaceCreated && videoHasParsed && fromUser) {
            seekHandler.removeMessages(MSG_DECODE);
            Message msg = seekHandler.obtainMessage(MSG_DECODE);
            msg.arg1 = progress;
            seekHandler.sendMessage(msg);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceCreated = true;
        if (!videoHasParsed) {
            parseTask.execute(videoToPlay);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceCreated = false;
    }

    private class DecodeHandler extends Handler {
        public DecodeHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_DECODE:
                    double rate = msg.arg1 * 1.0 / MAX;
                    long time = (long) (videoDuration * rate * 1.0);
                    decode(time);
                    break;
            }
        }
    }

    private void decode(long time_ns) {
        MediaFormat mediaFormat = null;
        final int trackCount = this.trackNumber = mediaExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            try {
                mediaFormat = mediaExtractor.getTrackFormat(i);
                final String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    mediaExtractor.selectTrack(i);
                } else {
                    mediaExtractor.unselectTrack(i);
                }
            } catch (Exception e) {
                Log.e(TAG, " read error " + e.getMessage());
            }
        }
        mediaExtractor.seekTo(time_ns, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int inIndex = mediaCodec.dequeueInputBuffer(10000);
        if (inIndex >= 0) {
            ByteBuffer buffer = inputBuffers[inIndex];
            int sampleSize = mediaExtractor.readSampleData(buffer, 0);
            if (sampleSize < 0) {
                mediaCodec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec
                        .BUFFER_FLAG_END_OF_STREAM);
            } else {
                mediaCodec.queueInputBuffer(inIndex, 0, sampleSize, mediaExtractor
                        .getSampleTime(), 0);
            }
        }

        int outIndex = mediaCodec.dequeueOutputBuffer(info, 10000);
        switch (outIndex) {
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
                break;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                Log.d("DecodeActivity", "New format " + mediaCodec.getOutputFormat());
                break;
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
                break;
            default:
                mediaCodec.releaseOutputBuffer(outIndex, true);
                break;
        }
    }
}
