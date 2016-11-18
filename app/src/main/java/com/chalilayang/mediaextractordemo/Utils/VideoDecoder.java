package com.chalilayang.mediaextractordemo.Utils;

import android.media.MediaCodec;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
            switch (newFlags) {
                case MediaCodec.BUFFER_FLAG_KEY_FRAME:
                    videoInfo.set(newOffset, newSize, newTimeUs, MediaCodec.BUFFER_FLAG_KEY_FRAME);
                    break;
                case MediaCodec.BUFFER_FLAG_CODEC_CONFIG:
                    videoInfo.set(newOffset, newSize, newTimeUs, MediaCodec
                            .BUFFER_FLAG_CODEC_CONFIG);
                    break;
                case MediaCodec.BUFFER_FLAG_END_OF_STREAM:
                    videoInfo.set(newOffset, newSize, newTimeUs, MediaCodec
                            .BUFFER_FLAG_END_OF_STREAM);
                    break;
                default:
                    break;
            }
        }
        return videoInfo;
    }

    public boolean decodeVideo(String url, long clipPoint, long clipDuration) {
        int videoTrackIndex = -1;
        int audioTrackIndex = -1;
        int videoMaxInputSize = 0;
        int audioMaxInputSize = 0;
        int sourceVTrack = 0;
        int sourceATrack = 0;
        long videoDuration, audioDuration;
        //创建分离器
        mediaExtractor = new MediaExtractor();
        try {
            //设置文件路径
            mediaExtractor.setDataSource(url);
            //创建合成器
            mediaMuxer = new MediaMuxer(url.substring(0, url.lastIndexOf(".")) + "_output.mp4",
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (Exception e) {
            Log.e(TAG, "error path" + e.getMessage());
        }
        //获取每个轨道的信息
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
                    //检测剪辑点和剪辑时长是否正确
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
                    //向合成器添加视频轨
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
                    //添加音轨
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
        long videoSampleTime;
        //获取源视频相邻帧之间的时间间隔。(1)
        {
            mediaExtractor.readSampleData(inputBuffer, 0);
            //skip first I frame
            if (mediaExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                mediaExtractor.advance();
            }
            mediaExtractor.readSampleData(inputBuffer, 0);
            long firstVideoPTS = mediaExtractor.getSampleTime();
            mediaExtractor.advance();
            mediaExtractor.readSampleData(inputBuffer, 0);
            long SecondVideoPTS = mediaExtractor.getSampleTime();
            videoSampleTime = Math.abs(SecondVideoPTS - firstVideoPTS);
            Log.i(TAG, "videoSampleTime is " + videoSampleTime);
        }
        //选择起点
        mediaExtractor.seekTo(clipPoint, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        while (true) {
            int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
            if (sampleSize < 0) {
                //这里一定要释放选择的轨道，不然另一个轨道就无法选中了
                mediaExtractor.unselectTrack(sourceVTrack);
                break;
            }
            int trackIndex = mediaExtractor.getSampleTrackIndex();
            //获取时间戳
            long sampleTimeUs = mediaExtractor.getSampleTime();
            //获取帧类型，只能识别是否为I帧
            int sampleFlag = mediaExtractor.getSampleFlags();
            Log.i(TAG, "trackIndex is " + trackIndex
                    + ";sampleTimeUs is " + sampleTimeUs
                    + ";sampleFlag is " + sampleFlag
                    + ";sampleSize is " + sampleSize);
            //剪辑时间到了就跳出
            if ((clipDuration != 0) && (sampleTimeUs > (clipPoint + clipDuration))) {
                mediaExtractor.unselectTrack(sourceVTrack);
                break;
            }
            if (videoInfo != null) {
                switch (sampleFlag) {
                    case MediaCodec.BUFFER_FLAG_KEY_FRAME:
                        videoInfo.set(0, sampleSize, presentationTimeUs, MediaCodec.BUFFER_FLAG_KEY_FRAME);
                        break;
                    case MediaCodec.BUFFER_FLAG_CODEC_CONFIG:
                        videoInfo.set(0, sampleSize, presentationTimeUs, MediaCodec
                                .BUFFER_FLAG_CODEC_CONFIG);
                        break;
                    case MediaCodec.BUFFER_FLAG_END_OF_STREAM:
                        videoInfo.set(0, sampleSize, presentationTimeUs, MediaCodec
                                .BUFFER_FLAG_END_OF_STREAM);
                        break;
                    default:
                        break;
                }
            }
//            videoInfo = getBufferInfo(videoInfo, 0, sampleSize, presentationTimeUs, sampleFlag);
            mediaMuxer.writeSampleData(videoTrackIndex, inputBuffer, videoInfo);

            mediaExtractor.advance();
            presentationTimeUs += videoSampleTime;
        }

        //音频部分
        mediaExtractor.selectTrack(sourceATrack);
        MediaCodec.BufferInfo audioInfo = new MediaCodec.BufferInfo();
        audioInfo.presentationTimeUs = 0;
        long audioSampleTime;
        //获取音频帧时长
        {
            mediaExtractor.readSampleData(inputBuffer, 0);
            //skip first sample
            if (mediaExtractor.getSampleTime() == 0) {
                mediaExtractor.advance();
            }
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
            long sampleTimeUs = mediaExtractor.getSampleTime();
            Log.d(TAG, "trackIndex is " + trackIndex
                    + ";sampleTimeUs is " + sampleTimeUs);
            if ((clipDuration != 0) && (sampleTimeUs > (clipPoint + clipDuration))) {
                mediaExtractor.unselectTrack(sourceATrack);
                break;
            }
            mediaExtractor.advance();
            audioInfo.offset = 0;
            audioInfo.size = sampleSize;
            mediaMuxer.writeSampleData(audioTrackIndex, inputBuffer, audioInfo);
            audioInfo.presentationTimeUs += audioSampleTime;//presentationTimeUs;
        }
        //全部写完后释放MediaMuxer和MediaExtractor
        mediaMuxer.stop();
        mediaMuxer.release();
        mediaExtractor.release();
        mediaExtractor = null;
        return true;
    }


}
