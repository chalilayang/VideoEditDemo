/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chalilayang.mediaextractordemo.Utils;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;

import com.google.common.base.Charsets;

/**
 * Class containing some static utility methods.
 */
public class Utils {
    public static final int IO_BUFFER_SIZE = 8 * 1024;

    private Utils() {
    };

    /**
     * Workaround for bug pre-Froyo, see here for more info:
     * http://android-developers.blogspot.com/2011/09/androids-http-clients.html
     */
    public static void disableConnectionReuseIfNecessary() {
        // HTTP connection reuse which was buggy pre-froyo
        if (hasHttpConnectionBug()) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    /**
     * Get the size in bytes of a bitmap.
     * @param bitmap
     * @return size in bytes
     */
    public static int getBitmapSize(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            return bitmap.getByteCount();
        }
        // Pre HC-MR1
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    /**
     * Check if external storage is built-in or removable.
     * @return True if external storage is removable (like an SD card), false
     *         otherwise.
     */
    public static boolean isExternalStorageRemovable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return Environment.isExternalStorageRemovable();
        }
        return true;
    }

    /**
     * Get the external app cache directory.
     * @param context The context to use
     * @return The external cache dir
     */
    public static File getExternalCacheDir(Context context) {
        if (hasExternalCacheDir()) {
            return context.getExternalCacheDir();
        }

        // Before Froyo we need to construct the external cache dir ourselves
        try {
			final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
			return new File(Environment.getExternalStorageDirectory().getPath() + cacheDir);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return null;
    }

    public static File getExternalDiskCacheDir(Context context, String uniqueName) {

        try {
			if (isExternalStorageExist() && Utils.getExternalCacheDir(context) != null) {
			    final String cachePath = Utils.getExternalCacheDir(context).getPath();
			    return new File(cachePath + File.separator + uniqueName);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return null;
    }

    public static File getInternalDiskCacheDir(Context context, String uniqueName) {
    	try {
			if(context.getCacheDir() != null) {
			    final String cachePath = context.getCacheDir().getPath();
			    return new File(cachePath + File.separator + uniqueName);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return null;
    }

    public static boolean isExternalStorageExist() {
        return !Utils.isExternalStorageRemovable()
                || Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * Get the memory class of this device (approx. per-app memory limit)
     * @param context
     * @return
     */
    public static int getMemoryClass(Context context) {
        return ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
    }

    /**
     * Check if OS version has a http URLConnection bug. See here for more
     * information:
     * http://android-developers.blogspot.com/2011/09/androids-http-clients.html
     * @return
     */
    public static boolean hasHttpConnectionBug() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO;
    }

    /**
     * Check if OS version has built-in external cache dir method.
     * @return
     */
    public static boolean hasExternalCacheDir() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    /**
     * 将URL转换为缓存所需要的key值
     * @param uri
     * @return String
     */
    public static String uriToKey(String uri) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] md5bytes = messageDigest.digest(Strings.getBytes(uri, Charsets.UTF_8));
            return Strings.bytesToHexString(md5bytes, false);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    private static final String BITMAP_REFLECT_URL_SUFFIX = "_reflect";
    
    public static String getReflectUrl(String url) {
    	return url + BITMAP_REFLECT_URL_SUFFIX;
    }
    
    public static String getBitmapKey(String url) {
    	return getBitmapKey(url, false);
    }
    /**
     * 针对图片Cache的key, 使用URL中除了HOST部分转化为KEY 1、直接使用原始URL会导致不同CDN的图片重复下载
     * 2、直接使用文件名会导致不同Host下文件名相同的图片只保存一份
     */
    public static String getBitmapKey(String url, boolean hasRef) {
        if (TextUtils.isEmpty(url))
            return "";
        if (hasRef){
        	url = url + BITMAP_REFLECT_URL_SUFFIX;
        }
        if (url.startsWith("http:")) {
            String tmpUrl = url.replace("//", "");
            int index = tmpUrl.indexOf("/");
            if (index != -1) {
                tmpUrl = tmpUrl.substring(index);
                return uriToKey(tmpUrl);
            }
            return "";
        } else {
            return uriToKey(url);
        }
    }

    /**
     * Check how much usable space is available at a given path.
     * @param path The path to check
     * @return The space available in bytes
     */
    public static long getAvailableSize(File path) {
        if (!path.exists()) {
            path.mkdirs();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return path.getUsableSpace();
        }
        final StatFs stats = new StatFs(path.getPath());
        return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
    }

}
