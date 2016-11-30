package com.chalilayang.mediaextractordemo.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.ImageView;

import com.chalilayang.mediaextractordemo.entities.VideoData;

/**
 * Created by chalilayang on 2016/11/17.
 */

public class VideoThumbnailLoader {
    private static final String TAG = "VideoThumbnailLoader";
    private static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 4;
    private static volatile VideoThumbnailLoader ins;
    private BitmapMemoryCache mMCache;//一级缓存,内存缓存
    private Context context;

    private VideoThumbnailLoader(Context context) {
        this.context = context;
        mMCache = new BitmapMemoryCache(context, DEFAULT_DISK_CACHE_SIZE) {
            @Override
            protected int sizeOf(Object obj) {
                if (obj instanceof Bitmap) {
                    Bitmap bp = (Bitmap) obj;
                    return bp.getByteCount();
                }
                return 0;
            }
        };
    }

    public synchronized void notifyBitmap(String filepath) {
        String key = getMemoryKey(filepath);
        Bitmap bitmap = mMCache.get(key);//先去内存缓存取

        if (bitmap == null || bitmap.isRecycled()) {
            bitmap = BitmapFactory.decodeFile(filepath);
            if (null == bitmap) {
                bitmap = getVideoThumbnail(filepath, WIDTH, HEIGHT, MediaStore.Video
                        .Thumbnails.MICRO_KIND);
            }
            if (null != bitmap) {
                mMCache.put(key, bitmap);//存入内存缓存
            }
        }
    }

    public static VideoThumbnailLoader getInstance(Context context) {
        if (ins == null) {
            synchronized (VideoThumbnailLoader.class) {
                if (ins == null) {
                    ins = new VideoThumbnailLoader(context);
                }
            }
        }
        return ins;
    }

    public void display(VideoData mEntity, ImageView iv, int width, int height,
                        ThumbnailListener thumbnailListener) {

        new ThumbnailLoadTask(mEntity.filePath, iv, width, height, thumbnailListener)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);//使用AsyncTask自带的线程池

    }

    /**
     * @param videoPath 视频路径
     * @param width
     * @param height
     * @param kind      eg:MediaStore.Video.Thumbnails.MICRO_KIND   MINI_KIND: 512 x
     *                  384，MICRO_KIND: 96 x 96
     * @return
     */
    private Bitmap getVideoThumbnail(String videoPath, int width, int height,
                                     int kind) {
        // 获取视频的缩略图
        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, kind);
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height, ThumbnailUtils
                .OPTIONS_RECYCLE_INPUT);
        return bitmap;
    }

    /**
     * imageloader 的内存缓存的 key 以_ 结尾  截取key比较的时候如果没有加_ 会报错崩溃,所以自己自定义
     *
     * @param filePath 文件地址
     * @return
     */
    private String getMemoryKey(String filePath) {

        String key;
        int index = filePath.lastIndexOf("/");
        key = filePath.substring(index + 1, filePath.length()) + "_";
        return key;
    }

    private static final int WIDTH = 400;
    private static final int HEIGHT = 300;
    public Bitmap get(String filepath) {
        String key = getMemoryKey(filepath);
        Bitmap bitmap = mMCache.get(key);//先去内存缓存取

        if (bitmap == null || bitmap.isRecycled()) {
            bitmap = BitmapFactory.decodeFile(filepath);
            if (null == bitmap) {
                bitmap = getVideoThumbnail(filepath, WIDTH, HEIGHT, MediaStore.Video
                        .Thumbnails.MICRO_KIND);
            }
            if (null != bitmap) {
                mMCache.put(key, bitmap);//存入内存缓存
            }
        }
        return bitmap;
    }

    public interface ThumbnailListener {
        void onThumbnailLoadCompleted(String url, ImageView iv, Bitmap bitmap);
    }

    private class ThumbnailLoadTask extends AsyncTask<Void, Void, Bitmap> {

        private String path;
        private ImageView iv;
        private ThumbnailListener thumbnailListener;
        private int width;
        private int height;

        public ThumbnailLoadTask(String filepath, ImageView iv, int width, int height,
                                 ThumbnailListener thumbnailListener) {
            this.path = filepath;
            this.iv = iv;
            this.width = width;
            this.height = height;
            this.thumbnailListener = thumbnailListener;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            /**
             * 注意,由于我们使用了缓存,所以在加载缩略图之前,我们需要去缓存里读取,如果缓存里有,我们则直接获取,如果没有,则去加载.并且加载完成之后记得放入缓存.
             */
            Bitmap bitmap = null;
            if (!TextUtils.isEmpty(path)) {
                String key = getMemoryKey(path);
                bitmap = mMCache.get(key);//先去内存缓存取

                if (bitmap == null || bitmap.isRecycled()) {

                    bitmap = BitmapFactory.decodeFile(path);
                    if (null == bitmap) {
                        bitmap = getVideoThumbnail(path, width, height, MediaStore.Video
                                .Thumbnails.MICRO_KIND);
                    }
                    if (null != bitmap) {
                        mMCache.put(key, bitmap);//存入内存缓存
                    }

                }
            }
            return bitmap;
        }


        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            thumbnailListener.onThumbnailLoadCompleted(path, iv, bitmap);//回调
        }
    }
}
