package com.chalilayang.mediaextractordemo.Utils;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.text.TextUtils;
import android.util.Log;
import android.util.TimeUtils;

import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * Created by chalilayang on 2016/11/15.
 */

public class VideoDecoder {
    private final static String TAG = "VideoDecoder";
    private MediaCodec mediaDecoder;
    private MediaExtractor mediaExtractor;
    private MediaFormat mediaFormat;
    private MediaMuxer mediaMuxer;
    private String mime = null;

    private static MediaCodec.BufferInfo getBufferInfo(MediaCodec.BufferInfo videoInfo,
                                                       int newOffset, int newSize, long
                                                               newTimeUs, int newFlags) {
        if (videoInfo != null) {
            boolean is_end = (newFlags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == 0;
            if (is_end) {
                videoInfo.set(newOffset, newSize, newTimeUs, MediaCodec.BUFFER_FLAG_KEY_FRAME |
                        MediaCodec
                                .BUFFER_FLAG_END_OF_STREAM);
            } else {
                videoInfo.set(newOffset, newSize, newTimeUs, MediaCodec.BUFFER_FLAG_KEY_FRAME);
            }
        }
        return videoInfo;
    }

    public boolean decodeVideo3(String srcFilePath,
                                String dstFilePath,
                                long clipPoint,
                                long clipDuration) {
        if (TextUtils.isEmpty(srcFilePath)) {
            return false;
        }
        File srcfile = new File(srcFilePath);
        if (!srcfile.exists() || !srcfile.isFile()) {
            return false;
        }

        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(srcFilePath);
        } catch (IOException e) {
            Log.i(TAG, "decodeVideo3: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        MediaMuxer muxer;
        try {
            muxer = new MediaMuxer(dstFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            Log.i(TAG, "decodeVideo3: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        int trackCount = extractor.getTrackCount();
        HashMap<Integer, Integer> indexMap = new HashMap<Integer, Integer>(trackCount);

        int audioTrackIndex = -1;
        int videoTrackIndex = -1;
        for (int i = 0; i < trackCount; i++) {
            extractor.selectTrack(i);
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                videoTrackIndex = i;
                int dstIndex = muxer.addTrack(format);
                indexMap.put(i, dstIndex);
            } else {
                audioTrackIndex = i;
//                int dstIndex = muxer.addTrack(format);
//                indexMap.put(i, dstIndex);
            }
        }

        boolean sawEOS = false;
        int bufferSize = 256 * 1024;
        int frameCount = 0;
        int offset = 100;
        ByteBuffer dstBuf = ByteBuffer.allocate(bufferSize);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        muxer.start();
        extractor.unselectTrack(audioTrackIndex);
        extractor.selectTrack(videoTrackIndex);
        while (!sawEOS) {
            bufferInfo.offset = offset;
            bufferInfo.size = extractor.readSampleData(dstBuf, offset);
            if (bufferInfo.size < 0) {
                sawEOS = true;
                bufferInfo.size = 0;
            } else {
                bufferInfo.presentationTimeUs = extractor.getSampleTime();
                int newFlags = extractor.getSampleFlags();
                bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
                int trackIndex = extractor.getSampleTrackIndex();
                muxer.writeSampleData(indexMap.get(trackIndex), dstBuf,
                        bufferInfo);
                extractor.advance();
                frameCount++;
                if (trackIndex == videoTrackIndex) {
                    Log.i(TAG, "videoTrack " + "frameCount is " + frameCount + " flag :" + newFlags + " time :"
                            + bufferInfo.presentationTimeUs);
                } else {
                    Log.i(TAG, "audioTrack " + "frameCount is " + frameCount + " time :"
                            + bufferInfo.presentationTimeUs);
                }
            }
        }
//        muxer.
        muxer.stop();
        muxer.release();
        return true;
    }

    public boolean decodeVideo2(String url, long clipPoint, long clipDuration) {
        int videoTrackIndex = -1;
        int audioTrackIndex = -1;
        int videoMaxInputSize = 0;
        int audioMaxInputSize = 0;
        int sourceVTrack = 0;
        int sourceATrack = 0;
        long videoDuration, audioDuration;

        mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(url);
            mediaMuxer = new MediaMuxer(url.substring(0, url.lastIndexOf(".")) + "_output.mp4",
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (Exception e) {
            Log.e(TAG, "error path" + e.getMessage());
        }
        for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
            try {
                mediaFormat = mediaExtractor.getTrackFormat(i);
                mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    sourceVTrack = i;
                    int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
                    int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
                    videoMaxInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                    videoDuration = mediaFormat.getLong(MediaFormat.KEY_DURATION);
                    if (clipPoint >= videoDuration) {
                        Log.e(TAG, "clip point is error!");
                        return false;
                    }
                    if ((clipDuration != 0) && ((clipDuration + clipPoint) >= videoDuration)) {
                        Log.e(TAG, "clip duration is error!");
                        return false;
                    }
                    Log.i(TAG, "width and height is " + width + " " + height
                            + ";maxInputSize is " + videoMaxInputSize
                            + ";duration is " + videoDuration
                    );
                    videoTrackIndex = mediaMuxer.addTrack(mediaFormat);
                } else if (mime.startsWith("audio/")) {
                    sourceATrack = i;
                    int sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    int channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    audioMaxInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                    audioDuration = mediaFormat.getLong(MediaFormat.KEY_DURATION);
                    Log.i(TAG, "sampleRate is " + sampleRate
                            + ";channelCount is " + channelCount
                            + ";audioMaxInputSize is " + audioMaxInputSize
                            + ";audioDuration is " + audioDuration
                    );
                    audioTrackIndex = mediaMuxer.addTrack(mediaFormat);
                }
                Log.d(TAG, "file mime is " + mime);
            } catch (Exception e) {
                Log.e(TAG, " read error " + e.getMessage());
            }
        }
        ByteBuffer inputBuffer = ByteBuffer.allocate(videoMaxInputSize);
        mediaMuxer.start();
        mediaExtractor.selectTrack(sourceVTrack);
        MediaCodec.BufferInfo videoInfo = new MediaCodec.BufferInfo();
        videoInfo.presentationTimeUs = 0;
        long videoSampleTime;
        {
            mediaExtractor.readSampleData(inputBuffer, 0);
            if (mediaExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                mediaExtractor.advance();
            }
            mediaExtractor.readSampleData(inputBuffer, 0);
            long firstVideoPTS = mediaExtractor.getSampleTime();
            mediaExtractor.advance();
            mediaExtractor.readSampleData(inputBuffer, 0);
            long SecondVideoPTS = mediaExtractor.getSampleTime();
            videoSampleTime = Math.abs(SecondVideoPTS - firstVideoPTS);
            Log.d(TAG, "videoSampleTime is " + videoSampleTime);
        }
        mediaExtractor.seekTo(clipPoint, MediaExtractor.SEEK_TO_NEXT_SYNC);
        while (true) {
            int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
            if (sampleSize < 0) {
                mediaExtractor.unselectTrack(sourceVTrack);
                break;
            }
            int trackIndex = mediaExtractor.getSampleTrackIndex();
            long presentationTimeUs = mediaExtractor.getSampleTime();
            int sampleFlag = mediaExtractor.getSampleFlags();
            Log.i(TAG, "trackIndex is " + trackIndex
                    + ";presentationTimeUs is " + presentationTimeUs
                    + ";sampleFlag is " + sampleFlag
                    + ";sampleSize is " + sampleSize);
            if ((clipDuration != 0) && (presentationTimeUs > (clipPoint + clipDuration))) {
                mediaExtractor.unselectTrack(sourceVTrack);
                break;
            }
            mediaExtractor.advance();
            videoInfo.offset = 0;
            videoInfo.size = sampleSize;
            if (videoInfo != null) {
                boolean is_end = (sampleFlag & MediaCodec.BUFFER_FLAG_END_OF_STREAM) ==
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                if (is_end) {
                    videoInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME | MediaCodec
                            .BUFFER_FLAG_END_OF_STREAM;
                } else {
                    videoInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
                }
            }
            Log.i(TAG, "修改后的sampleFlag is " + sampleFlag);
            mediaMuxer.writeSampleData(videoTrackIndex, inputBuffer, videoInfo);
            videoInfo.presentationTimeUs += videoSampleTime;//presentationTimeUs;
        }
        mediaExtractor.selectTrack(sourceATrack);
        MediaCodec.BufferInfo audioInfo = new MediaCodec.BufferInfo();
        audioInfo.presentationTimeUs = 0;
        long audioSampleTime;
        {
            mediaExtractor.readSampleData(inputBuffer, 0);
            if (mediaExtractor.getSampleTime() == 0)
                mediaExtractor.advance();
            mediaExtractor.readSampleData(inputBuffer, 0);
            long firstAudioPTS = mediaExtractor.getSampleTime();
            mediaExtractor.advance();
            mediaExtractor.readSampleData(inputBuffer, 0);
            long SecondAudioPTS = mediaExtractor.getSampleTime();
            audioSampleTime = Math.abs(SecondAudioPTS - firstAudioPTS);
            Log.d(TAG, "AudioSampleTime is " + audioSampleTime);
        }
        mediaExtractor.seekTo(clipPoint, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        while (true) {
            int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
            if (sampleSize < 0) {
                mediaExtractor.unselectTrack(sourceATrack);
                break;
            }
            int trackIndex = mediaExtractor.getSampleTrackIndex();
            long presentationTimeUs = mediaExtractor.getSampleTime();
            Log.d(TAG, "trackIndex is " + trackIndex
                    + ";presentationTimeUs is " + presentationTimeUs);
            if ((clipDuration != 0) && (presentationTimeUs > (clipPoint + clipDuration))) {
                mediaExtractor.unselectTrack(sourceATrack);
                break;
            }
            mediaExtractor.advance();
            audioInfo.offset = 0;
            audioInfo.size = sampleSize;
            mediaMuxer.writeSampleData(audioTrackIndex, inputBuffer, audioInfo);
            audioInfo.presentationTimeUs += audioSampleTime;//presentationTimeUs;
        }
        mediaMuxer.stop();
        mediaMuxer.release();
        mediaExtractor.release();
        mediaExtractor = null;
        return true;
    }

    public boolean decodeVideo(String url, long clipPoint, long clipDuration) {
        int videoTrackIndex = -1;
        int audioTrackIndex = -1;
        int videoMaxInputSize = 1024 * 1024 * 10;
        int sourceVTrack = 0;
        int sourceATrack = 0;
        mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(url);
            mediaMuxer = new MediaMuxer(url.substring(0, url.lastIndexOf(".")) + "_output.mp4",
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (Exception e) {
            Log.e(TAG, "error path" + e.getMessage());
        }
        for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
            try {
                mediaFormat = mediaExtractor.getTrackFormat(i);
                mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    sourceVTrack = i;
                    videoMaxInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                    videoTrackIndex = mediaMuxer.addTrack(mediaFormat);
                } else if (mime.startsWith("audio/")) {
                    sourceATrack = i;
                    audioTrackIndex = mediaMuxer.addTrack(mediaFormat);
                }
                Log.i(TAG, "file mime is " + mime);
            } catch (Exception e) {
                Log.e(TAG, " read error " + e.getMessage());
            }
        }
        ByteBuffer inputBuffer = ByteBuffer.allocate(videoMaxInputSize);
        mediaMuxer.start();

        mediaExtractor.selectTrack(sourceVTrack);
        MediaCodec.BufferInfo videoInfo = new MediaCodec.BufferInfo();

        long presentationTimeUs = 0;
        videoInfo.presentationTimeUs = 0;
        mediaExtractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        while (true) {
            int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
            if (sampleSize < 0) {
                mediaExtractor.unselectTrack(sourceVTrack);
                break;
            }
            long sampleTimeUs = mediaExtractor.getSampleTime();
            int sampleFlag = mediaExtractor.getSampleFlags();
            presentationTimeUs = sampleTimeUs;
            videoInfo = getBufferInfo(videoInfo, 0, sampleSize, presentationTimeUs, sampleFlag);
            mediaMuxer.writeSampleData(videoTrackIndex, inputBuffer, videoInfo);
            mediaExtractor.advance();
        }

        mediaExtractor.selectTrack(sourceATrack);
        MediaCodec.BufferInfo audioInfo = new MediaCodec.BufferInfo();
        audioInfo.presentationTimeUs = 0;
        mediaExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        while (true) {
            int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
            if (sampleSize < 0) {
                mediaExtractor.unselectTrack(sourceATrack);
                break;
            }
            long sampleTimeUs = mediaExtractor.getSampleTime();
            audioInfo.offset = 0;
            audioInfo.size = sampleSize;
            audioInfo.presentationTimeUs = sampleTimeUs;
            mediaMuxer.writeSampleData(audioTrackIndex, inputBuffer, audioInfo);
            mediaExtractor.advance();
        }
        mediaMuxer.stop();
        mediaMuxer.release();
        mediaExtractor.release();
        mediaExtractor = null;
        return true;
    }

    public boolean process(String url, String destPath) throws IOException {
        mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(url);
        int mVideoTrackIndex = -1;
        for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
            MediaFormat format = mediaExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                mediaExtractor.selectTrack(i);
                mediaMuxer = new MediaMuxer(destPath,
                        MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                mVideoTrackIndex = mediaMuxer.addTrack(format);
                break;
            }
        }
        mediaMuxer.start();
        if (mediaMuxer == null) {
            return false;
        }
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        ByteBuffer buffer = ByteBuffer.allocate(500 * 1024);
        long sampInterval = 0;
        long lastSample = 0;
        int count = 0;
        mediaExtractor.seekTo(33000, MediaExtractor.SEEK_TO_NEXT_SYNC);
        while (true) {
            int sampleSize = mediaExtractor.readSampleData(buffer, 0);
            if (sampleSize < 0) {
                break;
            }
            info.offset = 0;
            info.size = sampleSize;
            int sampleflag = mediaExtractor.getSampleFlags();
            if (sampleflag == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                info.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
            } else {
                info.flags = 0;
            }

            if (mediaExtractor.getSampleTime() > 0) {
                lastSample = mediaExtractor.getSampleTime();
                info.presentationTimeUs = lastSample;
                if (sampInterval == 0) {
                    sampInterval = lastSample;
                } else {
                    sampInterval = lastSample - sampInterval;
                }
                Log.i(TAG, info.flags + "  sampleTimeUs " +
                        info.presentationTimeUs + " size:" + info.size + "  count: " + count++);
                mediaMuxer.writeSampleData(mVideoTrackIndex, buffer, info);
            }
            if (!mediaExtractor.advance()) {
                break;
            }
        }
        mediaExtractor.release();
        mediaMuxer.stop();
        mediaMuxer.release();
        return true;
    }
}
