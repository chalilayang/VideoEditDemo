package com.chalilayang.mediaextractordemo.Utils;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.support.annotation.IntDef;
import android.text.TextUtils;

import com.chalilayang.mediaextractordemo.entities.SrtEntity;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Created by chalilayang on 2016/11/16.
 */

public class VideoUtils {

    public static final int METHOD_BY_MEDIA = 12;
    public static final int METHOD_BY_MP4PARSER = 13;

    /**
     * @param videoPath 视频路径
     * @param width     图片宽度
     * @param height    图片高度
     * @param kind      eg:MediaStore.Video.Thumbnails.MICRO_KIND   MINI_KIND: 512 x
     *                  384，MICRO_KIND: 96 x 96
     * @return
     */
    public static Bitmap getVideoThumbnail(String videoPath, int width, int height, int kind) {
        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, kind);
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height, ThumbnailUtils
                .OPTIONS_RECYCLE_INPUT);
        return bitmap;
    }

    public static void removeAudioTrack(String url, String des) {
        VideoDecoder decoder = new VideoDecoder();
        decoder.removeAudio(url, des);
    }

    public static void cropVideo(String path, String despath, long start, long end,
                                 @CropVideo_Method_Type int type) {
        switch (type) {
            case METHOD_BY_MEDIA:
                VideoDecoder decoder = new VideoDecoder();
                decoder.cropVideo(path, despath, start, end);
                break;
            case METHOD_BY_MP4PARSER:
                try {
                    Mp4Parser.startTrim(path, despath, start, end);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    public static boolean cloneVideo(String path) {
        String dst = path.substring(0, path.lastIndexOf(".")) + "_clone.mp4";
        MediaMuxerUtils mediaMuxerUtils = new MediaMuxerUtils();
        try {
            mediaMuxerUtils.cloneMediaUsingMuxer(path, dst, 2, 90);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * add text track
     *
     * @author chalilayang
     * @time 2016/11/24 12:45
     */
    public static boolean addTextTrack(String path, List<SrtEntity> entities) {
        String dst = path.substring(0, path.lastIndexOf(".")) + "_srt.mp4";
        Mp4Parser.addTextTrack(path, dst, entities);
        return true;
    }

    public static boolean mergeVideos(List<String> fileList, String desPath) {
        if (fileList != null) {
            int size = fileList.size();
            if (size <= 0) {
                return false;
            } else {
                String[] files = new String[size];
                fileList.toArray(files);
                for(int index = 0; index < size; index ++) {
                    String path = files[index];
                    if (TextUtils.isEmpty(path) || !(new File(path)).exists()) {
                        return false;
                    }
                }
//                try {
//                    Mp4Parser.appendVideo(files, desPath);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    return false;
//                }
                VideoDecoder decoder = new VideoDecoder();
                decoder.mergeVideos(files[0], files[1], desPath);
                return true;
            }
        } else {
            return false;
        }
    }

    @IntDef(
            value = {
                    METHOD_BY_MEDIA,
                    METHOD_BY_MP4PARSER,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface CropVideo_Method_Type {
    }

}
