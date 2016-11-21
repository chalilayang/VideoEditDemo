package com.chalilayang.mediaextractordemo.Utils;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;

import java.io.IOException;

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

    public static void cropVideo(String path, long start, long end, int type) {
        switch (type) {
            case METHOD_BY_MEDIA:
                VideoDecoder decoder = new VideoDecoder();
                decoder.decodeVideo2(path, start, end - start);
                break;
            case METHOD_BY_MP4PARSER:
                try {
                    Mp4Parser.startTrim(path, start, end);
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

}
