package com.chalilayang.mediaextractordemo.services;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.chalilayang.mediaextractordemo.entities.VideoCodecModel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by chalilayang on 2016/11/29.
 */

public class CodecService extends Service {

    public static final int REQUEST_CODEC = 0x183;
    public static final int REQUEST_CODEC_CANCEL = 0x184;
    private static final int MSG_COMPLETE = 48;
    private final static String TAG = "CodecService";
    private MediaExtractor extractor;
    private MediaMuxer muxer;
    private MediaFormat format;
    private int videoMaxInputSize = 0, videoRotation = 0;
    private long videoDuration;
    private boolean decodeOver = false, encoding = false, mCancel, mDelete;
    private int videoTrackIndex = -1;
    private MediaCodec mediaDecode, mediaEncode;
    private ByteBuffer[] decodeInputBuffers, decodeOutputBuffers;
    private ArrayList<Frame> timeDataContainer;
    private MediaCodec.BufferInfo decodeBufferInfo;
    private int srcWidth, srcHeight, dstWidth, dstHeight;
    private SimpleDateFormat videoTimeFormat;
    private int mProgress, mMax;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private DecodeRunnable decodeRunnable;
    private EncodeRunnable encodeRunnable;
    private CodecBinder mBinder = new CodecBinder();
    private VideoCodecModel mVideo;
    private List<VideoCodecModel> videos = new ArrayList<>();
    private OnProgressChangeListener mListener;
    android.os.Handler handler = new android.os.Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_COMPLETE:
                    onComplete();
                    break;
                default:
                    break;
            }
        }
    };

    public static byte[] getNV21(int width, int height, Bitmap scaled) {
        int[] argb = new int[width * height];
        scaled.getPixels(argb, 0, width, 0, 0, width, height);
        byte[] yuv = new byte[width * height * 3 / 2];
        encodeYUV420SP(yuv, argb, width, height);
        scaled.recycle();
        return yuv;
    }

    /**
     * 将bitmap里得到的argb数据转成yuv420sp格式
     * <p>
     * 这个yuv420sp数据就可以直接传给MediaCodec,通过AvcEncoder间接进行编码
     *
     * @param yuv420sp 用来存放yuv420sp数据
     * @param argb     传入argb数据
     * @param width    图片width
     * @param height   图片height
     */
    public static void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;

        int yIndex = 0;
        int uvIndex = frameSize;

        int R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff) >> 0;

                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                    yuv420sp[uvIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                }
                index++;
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        videoTimeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        timeDataContainer = new ArrayList<>();
        paint.setColor(Color.WHITE);
        paint.setTextSize(20);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        decodeOver = true;
        encoding = false;
    }

    private void init(String srcPath, String dstpath) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(srcPath);
        try {
            srcWidth = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever
                    .METADATA_KEY_VIDEO_WIDTH));
            srcHeight = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever
                    .METADATA_KEY_VIDEO_HEIGHT));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        try {
            extractor = new MediaExtractor();
            extractor.setDataSource(srcPath);

            String mime = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i;
                    this.format = format;
                } else if (mime.startsWith("audio/")) {
                    continue;
                } else {
                    continue;
                }
            }

            extractor.selectTrack(videoTrackIndex);

            srcWidth = format.getInteger(MediaFormat.KEY_WIDTH);
            dstHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
            videoMaxInputSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
            videoDuration = format.getLong(MediaFormat.KEY_DURATION);
            //videoRotation = format.getInteger(MediaFormat.KEY_ROTATION);

            videoRotation = 90;//低版本不支持获取旋转,手动写入了
            if (videoRotation == 90) {
                dstWidth = srcHeight;
                dstHeight = srcWidth;
            } else if (videoRotation == 0) {
                dstWidth = srcWidth;
                dstHeight = srcHeight;
            }

            mMax = (int) (videoDuration / 1000);
            //int bit = this.format.getInteger(MediaFormat.KEY_BIT_RATE);

            Log.d(TAG, "videoWidth=" + srcWidth + ",videoHeight=" + srcHeight + "," +
                    "videoMaxInputSize=" + videoMaxInputSize + ",videoDuration=" + videoDuration
                    + ",videoRotation=" + videoRotation);

            muxer = new MediaMuxer(dstpath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            videoTrackIndex = muxer.addTrack(format);
            MediaCodec.BufferInfo videoInfo = new MediaCodec.BufferInfo();
            videoInfo.presentationTimeUs = 0;

            initMediaDecode(mime);
            initMediaEncode(mime);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void extract() {
        int inputIndex = mediaDecode.dequeueInputBuffer(-1);

        if (inputIndex < 0) {
            Log.d(TAG, "=========== code over =======");
            return;
        }

        ByteBuffer inputBuffer = decodeInputBuffers[inputIndex];
        inputBuffer.clear();

        int length = extractor.readSampleData(inputBuffer, 0);
        if (length < 0) {
            Log.d(TAG, "extract Over");
            decodeOver = true;
            return;
        } else {
            MediaCodec.BufferInfo videoInfo = new MediaCodec.BufferInfo();
            videoInfo.offset = 0;
            videoInfo.size = length;

            int sampleFlag = extractor.getSampleFlags();
            if ((sampleFlag & MediaExtractor.SAMPLE_FLAG_SYNC)
                    == MediaExtractor.SAMPLE_FLAG_SYNC) {
                videoInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
            } else {
                videoInfo.flags = 0;
            }
            videoInfo.presentationTimeUs = extractor.getSampleTime();
            decode(videoInfo, inputIndex);
            extractor.advance();
        }
    }

    private void handleFrameData(byte[] data, MediaCodec.BufferInfo info) {
        //YUV420sp转RGB数据 5-60ms
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, srcWidth, srcHeight, null);
        yuvImage.compressToJpeg(new Rect(0, 0, srcWidth, srcHeight), 100, out);
        byte[] imageBytes = out.toByteArray();

        Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        Bitmap bitmap = rotateImageView(videoRotation, image);
        image.recycle();

        Canvas canvas = new Canvas(bitmap);
        canvas.drawText(videoTimeFormat.format(mVideo.videoCreateTime + info.presentationTimeUs /
                1000), 10, 30, paint);

        mProgress = (int) (info.presentationTimeUs / 1000);
        if (mListener != null) {
            mListener.onProgress(mProgress, mMax);
        }

        synchronized (MediaCodec.class) {
            timeDataContainer.add(new Frame(info, bitmap));
        }
    }

    private Frame getFrameData() {
        synchronized (MediaCodec.class) {
            if (timeDataContainer.isEmpty()) {
                return null;
            }
            Frame frame = timeDataContainer.remove(0);
            frame.data = getNV21(dstWidth, dstHeight, frame.bitmap);
            return frame;
        }
    }
    public Bitmap rotateImageView(int angle, Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix,
                true);
    }

    private void initMediaDecode(String mime) {
        try {
            mediaDecode = MediaCodec.createDecoderByType(mime);
            mediaDecode.configure(format, null, null, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mediaDecode == null) {
            Log.e(TAG, "create mediaDecode failed");
            return;
        }
        mediaDecode.start();
        decodeInputBuffers = mediaDecode.getInputBuffers();
        decodeOutputBuffers = mediaDecode.getOutputBuffers();
        decodeBufferInfo = new MediaCodec.BufferInfo();

    }

    private void initMediaEncode(String mime) {
        try {
            MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,
                    dstWidth, dstHeight);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 1024 * 512);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 27);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities
                    .COLOR_FormatYUV420Flexible);
            //  format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities
            // .COLOR_FormatYUV420Planar);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            mediaEncode = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            mediaEncode.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mediaEncode == null) {
            Log.e(TAG, "create mediaEncode failed");
            return;
        }
        mediaEncode.start();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void decode(MediaCodec.BufferInfo videoInfo, int inputIndex) {
        mediaDecode.queueInputBuffer(inputIndex, 0, videoInfo.size, videoInfo.presentationTimeUs,
                videoInfo.flags);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputIndex = mediaDecode.dequeueOutputBuffer(bufferInfo, 50000);

        switch (outputIndex) {
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                decodeOutputBuffers = mediaDecode.getOutputBuffers();
                break;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                Log.d(TAG, "New format " + mediaDecode.getOutputFormat());
                break;
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                Log.d(TAG, "dequeueOutputBuffer timed out!");
                break;
            default:
                ByteBuffer outputBuffer;
                byte[] frame;
                while (outputIndex >= 0) {
                    outputBuffer = decodeOutputBuffers[outputIndex];
                    frame = new byte[bufferInfo.size];
                    outputBuffer.get(frame);
                    outputBuffer.clear();
                    handleFrameData(frame, videoInfo);
                    mediaDecode.releaseOutputBuffer(outputIndex, false);
                    outputIndex = mediaDecode.dequeueOutputBuffer(decodeBufferInfo, 50000);
                }
                break;
        }
    }

    private void encode() {
        byte[] chunkTime;
        Frame frame = getFrameData();
        if (frame == null) {
            return;
        }
        chunkTime = frame.data;
        int inputIndex = mediaEncode.dequeueInputBuffer(-1);

        if (inputIndex < 0) {
            Log.d(TAG, "dequeueInputBuffer return inputIndex " + inputIndex + ",then break");
            mediaEncode.signalEndOfInputStream();
        }

        ByteBuffer inputBuffer = mediaEncode.getInputBuffers()[inputIndex];
        inputBuffer.clear();
        inputBuffer.put(chunkTime);
        inputBuffer.limit(frame.videoInfo.size);
        mediaEncode.queueInputBuffer(inputIndex, 0, chunkTime.length, frame.videoInfo
                .presentationTimeUs, frame.videoInfo.flags);

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputIndex = mediaEncode.dequeueOutputBuffer(bufferInfo, 50000);

        switch (outputIndex) {
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                break;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                MediaFormat outputFormat = mediaEncode.getOutputFormat();
                outputFormat.setInteger(MediaFormat.KEY_ROTATION, videoRotation);
                Log.d(TAG, "mediaEncode find New format " + outputFormat);
                videoTrackIndex = muxer.addTrack(outputFormat);
                muxer.start();
                break;
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                Log.d(TAG, "dequeueOutputBuffer timed out!");
                break;
            default:
                ByteBuffer outputBuffer;
                while (outputIndex >= 0) {
                    outputBuffer = mediaEncode.getOutputBuffers()[outputIndex];
                    muxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo);
                    mediaEncode.releaseOutputBuffer(outputIndex, false);
                    outputIndex = mediaEncode.dequeueOutputBuffer(bufferInfo, 50000);
                }
                break;
        }
    }

    private void release() {
        extractor.release();
        mediaDecode.release();
        mediaEncode.release();
        muxer.stop();
        muxer.release();
    }

    public void onComplete() {
        if (mDelete) {
            mDelete = false;
            new File(mVideo.srcPath).delete();
            Log.d(TAG, "delete file " + mVideo.srcPath);
        } else {
            mVideo.finish = mCancel ? 0 : 100;

        }
        if (mCancel) {
            mCancel = false;
            new File(mVideo.dstPath).delete();
            Log.d("px", "delete file " + mVideo.dstPath);
        } else {
            new File(mVideo.srcPath).delete();
            Log.d("px", "delete file " + mVideo.srcPath);
        }
        if (mListener != null) {
            mListener.onCodecFinish(mVideo);
        }
        if (!videos.isEmpty()) {
            VideoCodecModel video = videos.remove(0);
            start(video);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        if (intent == null) {
            return START_NOT_STICKY;
        }
        int action = intent.getIntExtra("action", 0);
        if (action == REQUEST_CODEC) {
            VideoCodecModel video = (VideoCodecModel) intent.getSerializableExtra("video");
            if (!encoding) {
                start(video);
            } else {
                videos.add(video);
            }
        } else if (action == REQUEST_CODEC_CANCEL) {
            VideoCodecModel video = (VideoCodecModel) intent.getSerializableExtra("video");
            mDelete = intent.getBooleanExtra("delete", false);
            Log.d(TAG, "----- onStartCommand action " + action + " is delete?" + mDelete);
            mBinder.cancel(video);
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    private void start(VideoCodecModel video) {
        if (video == null) {
            return;
        }
        if (!new File(video.srcPath).exists()) {
            Toast.makeText(this, "该视频缓存文件可能已经被删除", Toast.LENGTH_SHORT).show();
            video.finish = -100;
            return;
        }
        mVideo = video;
        if (mListener != null) {
            mListener.onCodecStart(mVideo);
        }
        mVideo.finish = 50;

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                init(mVideo.srcPath, mVideo.dstPath);
                decodeRunnable = new DecodeRunnable();
                decodeRunnable.start();
                encodeRunnable = new EncodeRunnable();
                encodeRunnable.start();
            }
        };
        AsyncTask.THREAD_POOL_EXECUTOR.execute(runnable);
    }
    public interface OnProgressChangeListener {

        void onProgress(int progress, int max);

        void onCodecStart(VideoCodecModel video);

        void onCodecFinish(VideoCodecModel video);
    }

    private class DecodeRunnable extends Thread {
        @Override
        public void run() {
            decodeOver = false;
            while (!decodeOver) {
                try {
                    extract();
                } catch (Exception e) {
                    Log.e("px", e.toString());
                }
                synchronized (encodeRunnable) {
                    encodeRunnable.notify();
                }
            }
        }
    }

    private class EncodeRunnable extends Thread {
        @Override
        public void run() {
            encoding = true;
            while (encoding) {
                if (timeDataContainer.isEmpty()) {
                    if (decodeOver) {
                        break;
                    }
                    try {
                        synchronized (encodeRunnable) {
                            wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    encode();
                }
            }
            release();
            encoding = false;
            handler.sendEmptyMessage(MSG_COMPLETE);
        }
    }

    class Frame {
        MediaCodec.BufferInfo videoInfo;
        byte[] data;
        Bitmap bitmap;

        public Frame(MediaCodec.BufferInfo videoInfo, Bitmap bitmap) {
            this.videoInfo = videoInfo;
            this.bitmap = bitmap;
        }
    }

    public class CodecBinder extends Binder {
        public boolean start(VideoCodecModel video) {
            if (!encoding) {
                CodecService.this.start(video);
            } else {
                videos.add(video);
            }
            return !encoding;
        }

        public void setOnProgressChangeListener(OnProgressChangeListener l) {
            mListener = l;
        }

        public VideoCodecModel getCurrentVideo() {
            return mVideo;
        }

        public void cancel(VideoCodecModel video) {
            if (mVideo.equals(video)) {
                decodeOver = true;
                encoding = false;
                mCancel = true;
            } else {
                boolean flag = videos.remove(video);
                if (flag) {
                    Log.d("px", "cancel render task sucess");
                } else {
                    Log.d("px", "cancel render task fail，seems this video not in renderring queen");
                }
                if (mDelete) {
                    mDelete = false;
                    new File(video.srcPath).delete();
                }
            }
        }

        public List<VideoCodecModel> getVideoList() {
            return videos;
        }

        public void removeListener() {
            mListener = null;
        }
    }
}
