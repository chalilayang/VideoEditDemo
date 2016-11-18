package com.chalilayang.mediaextractordemo.Utils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Random;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

public class StorageEngine {

    public static final String TAG = "StorageEngine";

    public static final String SAVED_DIR_NAME = "sohuvideo";

    public static final String PROPERTIES_DOWNLOAD_SETTING = "/assets/download_setting.properties";
    public static final String PROPERTIES_DOWNLOAD_SETTING_KEY = "save_pos";
    static Random random;
    /**
     * 是否使用外接存储
     */
    private static StorageType SAVE_POSITION = StorageType.INTERNAL_SD;

    public static StorageType getSaveType() {
        return SAVE_POSITION;
    }

    public static synchronized String getMarkMD5() {

        String data = stringToMD5(Long.toString(System.currentTimeMillis()));

        data += getRandomNO();

        return data;

    }

    public static int getRandomNO() {
        if (random == null) {
            random = new Random(86);
        }

        return random.nextInt(100);
    }

    /**
     * 将字符串转成MD5值
     *
     * @param string
     * @return
     */
    public static String stringToMD5(String string) {
        byte[] hash;

        try {
            hash = MessageDigest.getInstance("MD5").digest(
                    string.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }

        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10)
                hex.append("0");
            hex.append(Integer.toHexString(b & 0xFF));
        }

        return hex.toString();
    }

    /**
     * 获取文件保存路径
     *
     * @return
     */
    public static synchronized File getDownloadFile(Context context, String filename) {
        File file = null;
        if (!TextUtils.isEmpty(filename)) {
            if (SDCardUtil.checkDownloadAvailable(context)) {
                File fileFolder = getDownloadFolder(context);
                if (fileFolder != null) {
                    file = new File(fileFolder, filename);
                }
            } else {
                Log.d(TAG, "StorageEngine.getDownloadFile()  sdcard doesnot exist");
            }

        } else {
            Log.d(TAG, "StorageEngine.getDownloadFile()  forderName or fileName is empty!");
        }
        return file;
    }

    /**
     * 获取到下载目录
     *
     * @return
     */
    public static File getDownloadFolder(Context context) {
        File result = null;
        if (SDCardUtil.checkDownloadAvailable(context)) {
            File file1 = new File(getDownloadRootPath(context));
            result = new File(file1, SAVED_DIR_NAME);
            if (!result.exists()) {
                result.mkdirs();
            }
        } else {
            Log.d(TAG, "StorageEngine.getDownloadFile()  sdcard doesnot exist");
        }
        return result;
    }

    /**
     * 获取下载根目录
     *
     * @return
     */
    public static String getDownloadRootPath(Context context) {
        if (SAVE_POSITION != StorageType.INTERNAL_SD) {
            List<SDCardInfo> list = SDCardUtil.getExternalSDCardInfos(context);
            if (list != null && list.size() > 0) {
                switch (SAVE_POSITION) {
                    case EXTERNAL_SD:
                        for (SDCardInfo sdCardInfo : list) {
                            String path = sdCardInfo.getMountPoint();
                            if (path != null && path.toLowerCase().contains("external")) {
                                return path;
                            } else {
                                continue;
                            }
                        }
                        break;
                    case USB:
                        for (SDCardInfo sdCardInfo : list) {
                            String path = sdCardInfo.getMountPoint();
                            if (path != null && path.toLowerCase().contains("usb")) {
                                return path;
                            } else {
                                continue;
                            }
                        }
                        break;
                    default:
                        break;
                }
                return null;
            } else {
                return null;
            }
        } else {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
    }

    /**
     * 获取到下载日志目录
     *
     * @return
     */
    public static File getDownloadLogFolder() {
        File result = null;
        File file1 = Environment.getExternalStorageDirectory();
        result = new File(file1, SAVED_DIR_NAME + File.separator + "log");
        if (!result.exists()) {
            result.mkdirs();
        }
        return result;
    }

    /**
     * 注册SD卡挂载监听
     *
     * @param context
     * @param broadcastRec
     */
    public static void registeSDCardMountReceiver(Context context, BroadcastReceiver broadcastRec) {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.setPriority(1000);
        intentFilter.addAction(Intent.ACTION_MEDIA_CHECKING);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        intentFilter.addDataScheme("file");
        context.registerReceiver(broadcastRec, intentFilter);
    }

    public static void unregisterSDCardMountReceiver(Context context, BroadcastReceiver
            broadcastRec) {
        context.unregisterReceiver(broadcastRec);
    }

    public static enum StorageType {
        INTERNAL_SD, EXTERNAL_SD, USB
    }
}
