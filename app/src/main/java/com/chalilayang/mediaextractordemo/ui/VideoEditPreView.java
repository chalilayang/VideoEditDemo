package com.chalilayang.mediaextractordemo.ui;

import android.animation.TimeAnimator;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.chalilayang.mediaextractordemo.Utils.common.media.MediaCodecWrapper;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by chalilayang on 2016/12/6.
 */

public class VideoEditPreView extends TextureView implements TextureView.SurfaceTextureListener {
    private static final String TAG = "VideoEditPreView";
    public static final int MAX = 10000;//Integer.MAX_VALUE;
    private static final int STATE_SEEKING = 1;
    private static final int STATE_PAUSE = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PLAY_COMPLETE = 4;

    private boolean hasGetFirstFrame = false;
    private String videoFilePath;

    private onPlayBackPositionUpdateListener playBackListener;

    private int mVideoWidth;
    private int mVideoHeight;

    private MediaFormat videoFormat;
    private MediaFormat audioFormat;
    private long mVideoDuration;
    private long mAudioDuration;
    private int videoRotation = 0;
    private int videoTrackIndex = -1;
    private int audioTrackIndex = -1;

    private MediaExtractor mediaExtractor;
    private MediaCodec mediaFrameDecoder;
    private MediaCodecWrapper mCodecWrapper;
    private TimeAnimator mTimeAnimator = new TimeAnimator();
    private int mSurfaceWidth;
    private int mSurfaceHeight;

    private Surface surface;

    private int mCurrentState = STATE_SEEKING;

    private long currentPlayPosition_ns = 0;
    private long startPlayPosition_ns = 0;
    private HandlerThread mHandlerThread;
    InnerHandler threadHandler;

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
        Log.i(TAG, "initVideoView: start");
        hasGetFirstFrame = false;
        mVideoWidth = 0;
        mVideoHeight = 0;
        setSurfaceTextureListener(this);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        mHandlerThread = new HandlerThread(this.getClass().getSimpleName());
        mHandlerThread.start();
        threadHandler = new InnerHandler(mHandlerThread.getLooper());
        Log.i(TAG, "initVideoView: end");
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
        postDelayed(new Runnable() {
            @Override
            public void run() {
                seek(0);
            }
        }, 100);
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
                if (format.containsKey(MediaFormat.KEY_ROTATION)) {
                    videoRotation = format.getInteger(MediaFormat.KEY_ROTATION);
                }
                setRotation(90);
            } else if (mime.startsWith("audio/")) {
                this.audioTrackIndex = index;
                this.audioFormat = format;
                mAudioDuration = format.getLong(MediaFormat.KEY_DURATION);
            }
        }
    }

    public void setOnPlayBackPositionListener(onPlayBackPositionUpdateListener lis) {
        if (lis != null) {
            this.playBackListener = lis;
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
                width = widthSpecSize;
                height = heightSpecSize;
                Log.i(TAG, "onMeasure: width " + width + " height " + height);
                Log.i(TAG, "onMeasure: mVideoWidth " + mVideoWidth + " mVideoHeight " + mVideoHeight);
                if ( mVideoWidth * height  < width * mVideoHeight ) {
                    width = height * mVideoWidth / mVideoHeight;
                } else if ( mVideoWidth * height  > width * mVideoHeight ) {
                    height = width * mVideoHeight / mVideoWidth;
                }
            }
        }
        setMeasuredDimension(width, height);
        Log.i(TAG, "onMeasure: measured width " + width + " height " + height);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        this.surface = new Surface(surface);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        mSurfaceWidth = width;
        mSurfaceHeight = height;
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        release(true);
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

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
        private static final int MSG_PLAY = 230;
        private static final int MSG_STOP = 231;
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
                    currentPlayPosition_ns = time;
                    decodeFrame(time);
                    break;
                case MSG_PLAY:
                    startPlayback();
                    break;
                case MSG_STOP:
                    stopPlayBack();
                    break;
            }
        }
    }

    public boolean isPlaying() {
        return mCurrentState == STATE_PLAYING;
    }
    public void seek(int progress) {
        Log.i(TAG, "seek: ");
        if (mCurrentState != STATE_SEEKING) {
            pause();
            mCurrentState = STATE_SEEKING;
        }
        if (threadHandler != null) {
            threadHandler.removeMessages(InnerHandler.MSG_DECODE_FRAME);
            Message message = threadHandler.obtainMessage(InnerHandler.MSG_DECODE_FRAME);
            message.arg1 = progress;
            threadHandler.sendMessage(message);
        }
    }

    public void play() {
        Log.i(TAG, "play: ");
        if (mCurrentState != STATE_PLAYING) {
            if (mCurrentState == STATE_PLAY_COMPLETE) {
                currentPlayPosition_ns = 0;
            }
            mCurrentState = STATE_PLAYING;
            if (threadHandler != null) {
                threadHandler.removeMessages(InnerHandler.MSG_STOP);
                Message msg = threadHandler.obtainMessage(InnerHandler.MSG_PLAY);
                threadHandler.sendMessage(msg);
            }
        }
    }
    public void pause() {
        Log.i(TAG, "pause: ");
        if (mCurrentState == STATE_PLAYING || mCurrentState == STATE_SEEKING) {
            mCurrentState = STATE_PAUSE;
            if (threadHandler != null) {
                threadHandler.removeMessages(InnerHandler.MSG_PLAY);
                Message msg = threadHandler.obtainMessage(InnerHandler.MSG_STOP);
                threadHandler.sendMessage(msg);
            }
        }
    }

    private void startPlayback() {
        if (mediaFrameDecoder != null) {
            mediaFrameDecoder.stop();
            mediaFrameDecoder.release();
            mediaFrameDecoder = null;
        }
        try {
            int nTracks = mediaExtractor.getTrackCount();
            for (int i = 0; i < nTracks; ++i) {
                mediaExtractor.unselectTrack(i);
            }
            mCodecWrapper = MediaCodecWrapper.fromVideoFormat(videoFormat, surface);
            if (mCodecWrapper != null) {
                mediaExtractor.selectTrack(videoTrackIndex);
            }
            if (mTimeAnimator == null) {
                Log.i(TAG, "startPlayback: mTimeAnimator == null");
                mTimeAnimator = new TimeAnimator();
            }
            mTimeAnimator.setCurrentPlayTime(0);
            mTimeAnimator.setTimeListener(new TimeAnimator.TimeListener() {
                @Override
                public void onTimeUpdate(final TimeAnimator animation,
                                         final long totalTime,
                                         final long deltaTime) {
                    if (mCurrentState != STATE_PLAYING) {
                        return;
                    }
                    boolean isEos = ((mediaExtractor.getSampleFlags() & MediaCodec
                            .BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    if (!isEos) {
                        boolean result = mCodecWrapper.writeSample(mediaExtractor, false,
                                mediaExtractor.getSampleTime(), mediaExtractor.getSampleFlags());
                        if (result) {
                            mediaExtractor.advance();
                        }
                    }
                    MediaCodec.BufferInfo out_bufferInfo = new MediaCodec.BufferInfo();
                    mCodecWrapper.peekSample(out_bufferInfo);

                    if (out_bufferInfo.size <= 0 && isEos) {
                        Log.i(TAG, "onTimeUpdate: isEos ");
                        mTimeAnimator.end();
                        mCodecWrapper.stopAndRelease();
                        mCurrentState = STATE_PLAY_COMPLETE;
                    } else {
                        long elapseTime = out_bufferInfo.presentationTimeUs - startPlayPosition_ns;
                        if (elapseTime / 1000 < totalTime) {
                            currentPlayPosition_ns = out_bufferInfo.presentationTimeUs;
                            if (currentPlayPosition_ns >= startPlayPosition_ns) {
                                mCodecWrapper.popSample(true);
                                Log.i(TAG, "onTimeUpdate: " + currentPlayPosition_ns);
                                if (playBackListener != null) {
                                    playBackListener.onUpdatePosition(currentPlayPosition_ns,
                                            mVideoDuration
                                    );
                                }
                            } else {
                                mCodecWrapper.popSample(false);
                            }
                        }
                    }
                }
            });
            Log.i(TAG, "startPlayback: startPlayPosition_ns " + startPlayPosition_ns);
            startPlayPosition_ns = currentPlayPosition_ns;
            mediaExtractor.seekTo(startPlayPosition_ns, MediaExtractor.SEEK_TO_NEXT_SYNC);
            mTimeAnimator.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopPlayBack() {
        if (mTimeAnimator != null) {
            mTimeAnimator.end();
            mTimeAnimator.cancel();
            mTimeAnimator = null;
        }
        if (mCodecWrapper != null) {
            mCodecWrapper.stopAndRelease();
            mCodecWrapper = null;
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
            mediaFrameDecoder.configure(this.videoFormat, this.surface, null, 0);
            mediaFrameDecoder.start();
        }
        mediaExtractor.seekTo(time_ns, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        mediaExtractor.unselectTrack(audioTrackIndex);
        mediaExtractor.selectTrack(videoTrackIndex);
        ByteBuffer[] inputBuffers = mediaFrameDecoder.getInputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int retryCount = hasGetFirstFrame ? 1:5;
        long timeout_ns = hasGetFirstFrame?20000:500000;
        for (int index = 0; index < retryCount; index ++) {
            int inIndex = mediaFrameDecoder.dequeueInputBuffer(timeout_ns);
            if (inIndex >= 0) {
                ByteBuffer buffer = inputBuffers[inIndex];
                int sampleSize = mediaExtractor.readSampleData(buffer, 0);
                if (sampleSize < 0) {
                    mediaFrameDecoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec
                            .BUFFER_FLAG_END_OF_STREAM);
                    break;
                } else {
                    mediaFrameDecoder.queueInputBuffer(inIndex, 0, sampleSize, mediaExtractor
                            .getSampleTime(), 0);
                }
            }
        }

        boolean timeout = false;
        while (!timeout) {
            int outIndex = mediaFrameDecoder.dequeueOutputBuffer(info, timeout_ns);
            switch (outIndex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    Log.i(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    Log.i(TAG, "New format " + mediaFrameDecoder.getOutputFormat());
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    Log.i(TAG, "dequeueOutputBuffer timed out!");
                    timeout = true;
                    break;
                default:
                    Log.i(TAG, "releaseOutputBuffer " + outIndex);
                    mediaFrameDecoder.releaseOutputBuffer(outIndex, true);
                    break;
            }
        }
        if (!hasGetFirstFrame) {
            hasGetFirstFrame = true;
        }
    }

    public interface onPlayBackPositionUpdateListener {
        void onUpdatePosition(long presentTime, long duration);
    }
}
