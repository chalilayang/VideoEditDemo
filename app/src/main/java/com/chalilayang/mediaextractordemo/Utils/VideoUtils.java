package com.chalilayang.mediaextractordemo.Utils;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;

/**
 * Created by chalilayang on 2016/11/16.
 */

public class VideoUtils {
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
}
