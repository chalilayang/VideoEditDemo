package com.chalilayang.mediaextractordemo.Utils;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;

/**
 * Created by chalilayang on 2016/11/15.
 */
@TargetApi(18)
public class VideoDecoder {
    private final static String TAG = "VideoDecoder";
    private MediaCodec mediaDecoder;
    private MediaExtractor mediaExtractor;
    private MediaFormat mediaFormat;
    private MediaMuxer mediaMuxer;
    private String mime = null;

    /**
     * remove the audioTrack of mediafile
     *
     * @param srcFilePath
     * @param dstFilePath
     * @return
     */
    public boolean removeAudio(String srcFilePath,
                               String dstFilePath) {
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
            Log.i(TAG, "removeAudio: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        MediaMuxer muxer;
        try {
            muxer = new MediaMuxer(dstFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            Log.i(TAG, "removeAudio: " + e.getMessage());
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
                break;
            } else {
                audioTrackIndex = i;
            }
        }

        boolean sawEOS = false;
        int bufferSize = 256 * 1024;
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
                if (trackIndex == videoTrackIndex) {
                    Log.i(TAG, "videoTrack " + " flag :" + newFlags + " time :"
                            + bufferInfo.presentationTimeUs);
                } else {
                    Log.i(TAG, "audioTrack " + " time :"
                            + bufferInfo.presentationTimeUs);
                }
            }
        }
        muxer.stop();
        muxer.release();
        return true;
    }

    public static class Segment {
        final long startTime_ns;
        final long endTime_ns;
        public Segment(long start, long end) {
            this.startTime_ns = start;
            this.endTime_ns = end;
        }
    }

    /**
     * merge a number of segments of one mp4 video into one video file
     * @param srcFilePath source file path
     * @param destFilePath destination file path
     * @param segments list of entities describe the start-end time point of segments ti be merged
     * @return
     */
    public boolean mergeSegments(String srcFilePath,
                                 String destFilePath,
                                 List<Segment> segments) {
        boolean result = false;
        if (TextUtils.isEmpty(srcFilePath) || !srcFilePath.toLowerCase().endsWith("mp4")) {
            return false;
        }
        if (TextUtils.isEmpty(destFilePath) || !destFilePath.toLowerCase().endsWith("mp4")) {
            return false;
        }
        if (segments == null || segments.size() <= 0) {
            return false;
        }
        try {
            File srcFile = new File(srcFilePath);
            if (!srcFile.exists()) {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        MediaExtractor mediaExtractor = new MediaExtractor();
        MediaMuxer mediaMuxer = null;
        try {
            mediaMuxer = new MediaMuxer(destFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (Exception e) {
            e.printStackTrace();
            if (mediaExtractor != null) {
                mediaMuxer.release();
            }
            return false;
        }

        int videoTrackIndexInMuxer = -1;
        int audioTrackIndexInMuxer = -1;

        int videoTrackIndex = -1;
        int audioTrackIndex = -1;

        long videoDuration = -1;
        long audioDuration = -1;

        boolean hasAddVideoTrack = false;
        boolean hasAddAudioTrack = false;

        long baseTime = 0;
        long lastBaseTime = 0;

        try {
            if (mediaExtractor != null) {
                mediaExtractor.release();
                mediaExtractor = new MediaExtractor();
            }
            mediaExtractor.setDataSource(srcFilePath);
        } catch (IOException e) {
            Log.i(TAG, "mergeVideos: mediaExtractor.setDataSource " + e
                    .getMessage());
            e.printStackTrace();
        }
        int trackCount = mediaExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            mediaExtractor.selectTrack(i);
            MediaFormat format = mediaExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                videoTrackIndex = i;
                videoDuration = format.getLong(MediaFormat.KEY_DURATION);
                if (!hasAddVideoTrack) {
                    videoTrackIndexInMuxer = mediaMuxer.addTrack(format);
                    hasAddVideoTrack = true;
                }
                continue;
            } else if (mime.startsWith("audio/")) {
                audioTrackIndex = i;
                audioDuration = format.getLong(MediaFormat.KEY_DURATION);
                if (!hasAddAudioTrack) {
                    audioTrackIndexInMuxer = mediaMuxer.addTrack(format);
                    hasAddAudioTrack = true;
                }
                continue;
            }
        }
        boolean sawEOS = false;
        int bufferSize = 1024 * 1024;
        ByteBuffer dstBuf = ByteBuffer.allocate(bufferSize);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        mediaMuxer.start();
        for (int index = 0, count = segments.size(); index < count; index ++) {
            long start = segments.get(index).startTime_ns;
            long end = segments.get(index).endTime_ns;
            if (start >= end
                    || start < 0
                    || end > videoDuration) {
                break;
            }
            mediaExtractor.seekTo(start, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            boolean hasReachTargetPoint = false;
            baseTime = lastBaseTime;
            while (!sawEOS) {
                bufferInfo.offset = 0;
                bufferInfo.size = mediaExtractor.readSampleData(dstBuf, 0);
                if (bufferInfo.size < 0) {
                    sawEOS = true;
                    bufferInfo.size = 0;
                } else {
                    long presentTime = mediaExtractor.getSampleTime();
                    int newFlags = mediaExtractor.getSampleFlags();
                    if (newFlags == 0) {
                        bufferInfo.flags = 0;
                    } else {
                        bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
                    }
                    int trackIndex = mediaExtractor.getSampleTrackIndex();
                    if (trackIndex == videoTrackIndex) {
                        if (hasReachTargetPoint) {
                            break;
                        } else if (presentTime > end) {
                            hasReachTargetPoint = true;
                        } else {
                            lastBaseTime =
                                    bufferInfo.presentationTimeUs =
                                            baseTime + presentTime;
                            mediaMuxer.writeSampleData(videoTrackIndexInMuxer, dstBuf,
                                    bufferInfo);
                        }

                    } else if (trackIndex == audioTrackIndex) {
                        if (hasReachTargetPoint) {
                            break;
                        } else if (presentTime > end) {
                            hasReachTargetPoint = true;
                        } else {
                            lastBaseTime =
                                    bufferInfo.presentationTimeUs =
                                            baseTime + presentTime;
                            mediaMuxer.writeSampleData(audioTrackIndexInMuxer, dstBuf,
                                    bufferInfo);
                        }
                    }
                    mediaExtractor.advance();
                }
            }
        }

        return result;
    }

    /**
     * merge more than one mp4 files into single one mp4 file
     *
     * @param srcPaths absolute path list of mp4 files to be merged
     * @param destPath absolute path of output-file
     * @return
     */
    public boolean mergeVideos(List<String> srcPaths,
                               String destPath) {
        boolean result = true;
        if (srcPaths == null
                || destPath == null
                || srcPaths.size() <= 0
                || TextUtils.isEmpty(destPath)
                ) {
            return false;
        }
        MediaExtractor mediaExtractor = new MediaExtractor();
        MediaMuxer mediaMuxer = null;
        try {
            mediaMuxer = new MediaMuxer(destPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (Exception e) {
            e.printStackTrace();
            mediaMuxer.release();
            return false;
        }

        int videoTrackIndexInMuxer = -1;
        int audioTrackIndexInMuxer = -1;

        int videoTrackIndex = -1;
        int audioTrackIndex = -1;

        boolean hasStarted = false;
        boolean hasAddVideoTrack = false;
        boolean hasAddAudioTrack = false;

        long baseTime = 0;
        long lastBaseTime = 0;
        for (int index = 0, fileCount = srcPaths.size(); index < fileCount; index++) {
            String filePathStr = srcPaths.get(index);
            if (TextUtils.isEmpty(filePathStr)) {
                continue;
            }
            File filePath = new File(srcPaths.get(index));
            if (!filePath.exists()
                    || !filePath.isFile()
                    || !filePathStr.toLowerCase().endsWith(".mp4")) {
                continue;
            }
            try {
                if (mediaExtractor != null) {
                    mediaExtractor.release();
                    mediaExtractor = new MediaExtractor();
                }
                mediaExtractor.setDataSource(filePathStr);
            } catch (IOException e) {
                Log.i(TAG, "mergeVideos: mediaExtractor.setDataSource index " + index + " " + e
                        .getMessage());
                e.printStackTrace();
                continue;
            }

            int trackCount = mediaExtractor.getTrackCount();
            for (int i = 0; i < trackCount; i++) {
                mediaExtractor.selectTrack(i);
                MediaFormat format = mediaExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i;
                    if (!hasAddVideoTrack) {
                        videoTrackIndexInMuxer = mediaMuxer.addTrack(format);
                        hasAddVideoTrack = true;
                    }
                    continue;
                } else if (mime.startsWith("audio/")) {
                    audioTrackIndex = i;
                    if (!hasAddAudioTrack) {
                        audioTrackIndexInMuxer = mediaMuxer.addTrack(format);
                        hasAddAudioTrack = true;
                    }
                    continue;
                }
            }

            boolean sawEOS = false;
            int bufferSize = 1024 * 1024;
            ByteBuffer dstBuf = ByteBuffer.allocate(bufferSize);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            if (!hasStarted) {
                mediaMuxer.start();
                hasStarted = true;
            }
            baseTime = lastBaseTime;
            while (!sawEOS) {
                bufferInfo.offset = 0;
                bufferInfo.size = mediaExtractor.readSampleData(dstBuf, 0);
                if (bufferInfo.size < 0) {
                    sawEOS = true;
                    bufferInfo.size = 0;
                } else {
                    lastBaseTime =
                            bufferInfo.presentationTimeUs =
                                    baseTime + mediaExtractor.getSampleTime();
                    int newFlags = mediaExtractor.getSampleFlags();
                    if (newFlags == 0) {
                        bufferInfo.flags = 0;
                    } else {
                        bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
                    }
                    int trackIndex = mediaExtractor.getSampleTrackIndex();
                    if (trackIndex == videoTrackIndex) {
                        mediaMuxer.writeSampleData(videoTrackIndexInMuxer, dstBuf,
                                bufferInfo);
                    } else if (trackIndex == audioTrackIndex) {
                        mediaMuxer.writeSampleData(audioTrackIndexInMuxer, dstBuf,
                                bufferInfo);
                    }
                    mediaExtractor.advance();
                }
            }
            result = true;
        }
        mediaMuxer.stop();
        mediaMuxer.release();
        mediaExtractor.release();
        mediaExtractor = null;
        return result;
    }

    public boolean cropVideo(String srcFilePath,
                             String destFilePath,
                             long clipStartPoint,
                             long clipEndPoint) {
        if (TextUtils.isEmpty(srcFilePath) || TextUtils.isEmpty(destFilePath)) {
            return false;
        }
        File srcfile = new File(srcFilePath);
        if (!srcfile.exists() || !srcfile.isFile()) {
            return false;
        }
        final int MAX_INPUT_SIZE = 1024 * 1204;
        int videoTrackIndex = -1;
        int audioTrackIndex = -1;
        int videoMaxInputSize = 0;
        int audioMaxInputSize = 0;
        int sourceVTrack = 0;
        int sourceATrack = 0;
        long videoDuration, audioDuration;

        mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(srcFilePath);
            mediaMuxer = new MediaMuxer(destFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (Exception e) {
            Log.e(TAG, "error path" + e.getMessage());
        }
        final int trackCount = mediaExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            try {
                mediaFormat = mediaExtractor.getTrackFormat(i);
                mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    sourceVTrack = i;
                    int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
                    int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
                    videoMaxInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                    videoDuration = mediaFormat.getLong(MediaFormat.KEY_DURATION);
                    if (clipStartPoint >= videoDuration) {
                        Log.e(TAG, "clipStartPoint is error!");
                        return false;
                    }
                    if ((clipEndPoint <= 0) || (clipEndPoint > videoDuration)) {
                        Log.e(TAG, "clipEndPoint is error!");
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
        ByteBuffer inputBuffer = ByteBuffer.allocate(videoMaxInputSize > 0 ? videoMaxInputSize :
                MAX_INPUT_SIZE);
        mediaMuxer.start();
        mediaExtractor.selectTrack(sourceVTrack);
        MediaCodec.BufferInfo videoInfo = new MediaCodec.BufferInfo();
        videoInfo.presentationTimeUs = 0;
        long basePresentation = -1;
        mediaExtractor.seekTo(clipStartPoint, MediaExtractor.SEEK_TO_NEXT_SYNC);
        boolean hasMoreData = true;
        while (hasMoreData) {
            int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
            if (sampleSize < 0) {
                mediaExtractor.unselectTrack(sourceVTrack);
                break;
            }
            int trackIndex = mediaExtractor.getSampleTrackIndex();
            long presentationTimeUs = mediaExtractor.getSampleTime();
            if (basePresentation < 0) {
                basePresentation = presentationTimeUs;
            }
            int sampleFlag = mediaExtractor.getSampleFlags();
            Log.i(TAG, "trackIndex is " + trackIndex
                    + ";presentationTimeUs is " + presentationTimeUs
                    + ";sampleFlag is " + sampleFlag
                    + ";sampleSize is " + sampleSize);
            if ((clipEndPoint != 0) && (presentationTimeUs > clipEndPoint)) {
                Log.i(TAG, "cropVideo: clipEndPoint " + clipEndPoint);
                mediaExtractor.unselectTrack(sourceVTrack);
                break;
            }
            videoInfo.offset = 0;
            videoInfo.size = sampleSize;
            if ((sampleFlag & MediaExtractor.SAMPLE_FLAG_SYNC)
                    == MediaExtractor.SAMPLE_FLAG_SYNC) {
                videoInfo.flags = MediaExtractor.SAMPLE_FLAG_SYNC;
                if ((sampleFlag & MediaExtractor.SAMPLE_FLAG_ENCRYPTED)
                        == MediaExtractor.SAMPLE_FLAG_ENCRYPTED) {
                    videoInfo.flags = MediaExtractor.SAMPLE_FLAG_SYNC | MediaExtractor
                            .SAMPLE_FLAG_ENCRYPTED;
                }
            } else {
                videoInfo.flags = 0;
                if ((sampleFlag & MediaExtractor.SAMPLE_FLAG_ENCRYPTED)
                        == MediaExtractor.SAMPLE_FLAG_ENCRYPTED) {
                    videoInfo.flags = MediaExtractor
                            .SAMPLE_FLAG_ENCRYPTED;
                }
            }

            Log.i(TAG, "修改后的sampleFlag is " + videoInfo.flags);
            mediaMuxer.writeSampleData(videoTrackIndex, inputBuffer, videoInfo);
            videoInfo.presentationTimeUs = presentationTimeUs - basePresentation;
            Log.i(TAG, "修改后的presentationTimeUs is " + videoInfo.presentationTimeUs);
            hasMoreData = mediaExtractor.advance();
        }
        mediaExtractor.selectTrack(sourceATrack);
        MediaCodec.BufferInfo audioInfo = new MediaCodec.BufferInfo();
        audioInfo.presentationTimeUs = 0;
        basePresentation = -1;
        mediaExtractor.seekTo(clipStartPoint, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        hasMoreData = true;
        while (hasMoreData) {
            int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
            if (sampleSize < 0) {
                mediaExtractor.unselectTrack(sourceATrack);
                break;
            }
            int trackIndex = mediaExtractor.getSampleTrackIndex();
            long presentationTimeUs = mediaExtractor.getSampleTime();
            if (basePresentation < 0) {
                basePresentation = presentationTimeUs;
            }
            Log.d(TAG, "trackIndex is " + trackIndex
                    + ";presentationTimeUs is " + presentationTimeUs);
            if ((clipEndPoint != 0) && (presentationTimeUs > clipEndPoint)) {
                mediaExtractor.unselectTrack(sourceATrack);
                break;
            }
            audioInfo.offset = 0;
            audioInfo.size = sampleSize;
            mediaMuxer.writeSampleData(audioTrackIndex, inputBuffer, audioInfo);
            audioInfo.presentationTimeUs = presentationTimeUs - basePresentation;
            hasMoreData = mediaExtractor.advance();
        }
        mediaMuxer.stop();
        mediaMuxer.release();
        mediaExtractor.release();
        mediaExtractor = null;
        return true;
    }
}
