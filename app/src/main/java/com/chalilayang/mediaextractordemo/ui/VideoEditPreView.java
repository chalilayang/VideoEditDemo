package com.chalilayang.mediaextractordemo.ui;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by chalilayang on 2016/12/6.
 */

public class VideoEditPreView extends SurfaceView implements SurfaceHolder.Callback {
    private static final int MAX = 10000;//Integer.MAX_VALUE;
    private static final int STATE_ERROR              = -1;
    private static final int STATE_IDLE               = 0;
    private static final int STATE_PREPARING          = 1;
    private static final int STATE_PREPARED           = 2;
    private static final int STATE_PLAYING            = 3;
    private static final int STATE_PAUSED             = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;

    private String videoFilePath;


    private int mVideoWidth;
    private int mVideoHeight;

    private MediaFormat videoFormat;
    private MediaFormat audioFormat;
    private long mVideoDuration;
    private long mAudioDuration;
    private int videoTrackIndex = -1;
    private int audioTrackIndex = -1;

    private MediaExtractor mediaExtractor;
    private MediaCodec mediaFrameDecoder;

    private int mSurfaceWidth;
    private int mSurfaceHeight;

    private int mCurrentState = STATE_IDLE;
    private int mTargetState  = STATE_IDLE;

    private HandlerThread mHandlerThread;
    InnerHandler threadHandler;

    private SurfaceHolder surfaceHolder;

    public VideoEditPreView(Context context) {
        super(context);
        initVideoView();
    }

    public VideoEditPreView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView();
    }

    public VideoEditPreView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoView();
    }
    private void initVideoView() {
        mVideoWidth = 0;
        mVideoHeight = 0;
        getHolder().addCallback(this);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        mHandlerThread = new HandlerThread(this.getClass().getSimpleName());
        mHandlerThread.start();
        threadHandler = new InnerHandler(mHandlerThread.getLooper());
    }

    /**
     * Sets video path.
     *
     * @param path the path of the video.
     */
    public void setVideoPath(String path) {
        videoFilePath = path;
        openVideo();
        requestLayout();
        invalidate();
    }

    private void openVideo() {
        mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(videoFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(
                    "setDataSource failed :videoFilePath " + videoFilePath);
        }
        int trackCount = mediaExtractor.getTrackCount();
        for(int index = 0; index < trackCount; index ++) {
            MediaFormat format = mediaExtractor.getTrackFormat(index);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                this.videoTrackIndex = index;
                this.videoFormat = format;
                mVideoWidth = format.getInteger(MediaFormat.KEY_WIDTH);
                mVideoHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
                mVideoDuration = format.getLong(MediaFormat.KEY_DURATION);
            } else if (mime.startsWith("audio/")) {
                this.audioTrackIndex = index;
                this.audioFormat = format;
                mAudioDuration = format.getLong(MediaFormat.KEY_DURATION);
            }
        }
        mediaExtractor.release();
        mediaExtractor = null;
    }

    public void seek(long time_ns) {
        if (threadHandler != null) {
            threadHandler.removeMessages(InnerHandler.MSG_DECODE_FRAME);
            Message message = threadHandler.obtainMessage(InnerHandler.MSG_DECODE_FRAME);
            threadHandler.sendMessage(message);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
        int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
        if (mVideoWidth > 0 && mVideoHeight > 0) {

            int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
            int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
            int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

            if (widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.EXACTLY) {
                // the size is fixed
                width = widthSpecSize;
                height = heightSpecSize;

                // for compatibility, we adjust size based on aspect ratio
                if ( mVideoWidth * height  < width * mVideoHeight ) {
                    //Log.i("@@@", "image too wide, correcting");
                    width = height * mVideoWidth / mVideoHeight;
                } else if ( mVideoWidth * height  > width * mVideoHeight ) {
                    //Log.i("@@@", "image too tall, correcting");
                    height = width * mVideoHeight / mVideoWidth;
                }
            } else if (widthSpecMode == MeasureSpec.EXACTLY) {
                // only the width is fixed, adjust the height to match aspect ratio if possible
                width = widthSpecSize;
                height = width * mVideoHeight / mVideoWidth;
                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    height = heightSpecSize;
                }
            } else if (heightSpecMode == MeasureSpec.EXACTLY) {
                // only the height is fixed, adjust the width to match aspect ratio if possible
                height = heightSpecSize;
                width = height * mVideoWidth / mVideoHeight;
                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    width = widthSpecSize;
                }
            } else {
                // neither the width nor the height are fixed, try to use actual video size
                width = mVideoWidth;
                height = mVideoHeight;
                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // too tall, decrease both width and height
                    height = heightSpecSize;
                    width = height * mVideoWidth / mVideoHeight;
                }
                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // too wide, decrease both width and height
                    width = widthSpecSize;
                    height = width * mVideoHeight / mVideoWidth;
                }
            }
        } else {
            // no size yet, just adopt the given spec sizes
        }
        setMeasuredDimension(width, height);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceHolder = holder;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        boolean isValidState =  (mTargetState == STATE_PLAYING);
        boolean hasValidSize = (mVideoWidth == width && mVideoHeight == height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceHolder = null;
        release(true);
    }

    private void release(boolean cleartargetstate) {
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
        if (threadHandler != null) {
            threadHandler.removeCallbacksAndMessages(null);
            threadHandler = null;
        }
        if (mediaFrameDecoder != null) {
            mediaFrameDecoder.stop();
            mediaFrameDecoder.release();
            mediaFrameDecoder = null;
        }
        if (mediaExtractor != null) {
            mediaExtractor.release();
            mediaExtractor = null;
        }
    }

    private class InnerHandler extends Handler {
        private static final int MSG_DECODE_FRAME = 229;
        public InnerHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_DECODE_FRAME:
                    double rate = msg.arg1 * 1.0 / MAX;
                    long time = (long) (mVideoDuration * rate * 1.0);
                    decodeFrame(time);
                    break;
            }
        }
    }

    /**
     *
     * @param time_ns
     */
    private void decodeFrame(long time_ns) {
        if (mediaFrameDecoder == null) {
            String mime = videoFormat.getString(MediaFormat.KEY_MIME);
            try {
                mediaFrameDecoder = MediaCodec.createDecoderByType(mime);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mediaFrameDecoder.configure(this.videoFormat, getHolder().getSurface(), null,
                    0);
            mediaFrameDecoder.start();
        }
        mediaExtractor.seekTo(time_ns, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        ByteBuffer[] inputBuffers = mediaFrameDecoder.getInputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int inIndex = mediaFrameDecoder.dequeueInputBuffer(10000);
        if (inIndex >= 0) {
            ByteBuffer buffer = inputBuffers[inIndex];
            int sampleSize = mediaExtractor.readSampleData(buffer, 0);
            if (sampleSize < 0) {
                mediaFrameDecoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec
                        .BUFFER_FLAG_END_OF_STREAM);
            } else {
                mediaFrameDecoder.queueInputBuffer(inIndex, 0, sampleSize, mediaExtractor
                        .getSampleTime(), 0);
            }
        }

        int outIndex = mediaFrameDecoder.dequeueOutputBuffer(info, 10000);
        switch (outIndex) {
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
                break;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                Log.d("DecodeActivity", "New format " + mediaFrameDecoder.getOutputFormat());
                break;
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
                break;
            default:
                mediaFrameDecoder.releaseOutputBuffer(outIndex, true);
                break;
        }
    }
}
