package com.chalilayang.mediaextractordemo.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;


public class SDCardUtil {

    public static final int KB = 1024;
    public static final int MB = 1024 * KB;
    public static final int GB = 1024 * MB;

    /**
     * 内置
     */
    public static String SDCARD_INTERNAL = "internal";


    /**
     * 外置
     */
    public static String SDCARD_EXTERNAL = "external";
    public static String USB_EXTERNAL = "usb_storage";


    /**
     * API14以下通过读取Linux的vold.fstab文件来获取SDCard信息
     *
     * @return
     */
    public static HashMap<String, SDCardInfo> getSDCardInfoBelow14() {
        HashMap<String, SDCardInfo> sdCardInfos = new HashMap<String, SDCardInfo>();
        BufferedReader bufferedReader = null;
        List<String> dev_mountStrs = null;
        try {
            // API14以下通过读取Linux的vold.fstab文件来获取SDCard信息
            bufferedReader = new BufferedReader(new FileReader(Environment.getRootDirectory().getAbsoluteFile()
                    + File.separator + "etc" + File.separator + "vold.fstab"));
            dev_mountStrs = new ArrayList<String>();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith("dev_mount")) {
                    dev_mountStrs.add(line);
                }
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; dev_mountStrs != null && i < dev_mountStrs.size(); i++) {
            SDCardInfo sdCardInfo = new SDCardInfo();
            String[] infoStr = dev_mountStrs.get(i).split(" ");
            sdCardInfo.setLabel(infoStr[1]);
            sdCardInfo.setMountPoint(infoStr[2]);
            if (sdCardInfo.getMountPoint().equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
                sdCardInfo.setMounted(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED));
                sdCardInfos.put(SDCARD_INTERNAL, sdCardInfo);
            } else if (sdCardInfo.getMountPoint().startsWith("/mnt")
                    && !sdCardInfo.getMountPoint().equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
                File file = new File(sdCardInfo.getMountPoint() + File.separator + "temp");
                if (file.exists()) {
                    sdCardInfo.setMounted(true);
                } else {
                    if (file.mkdir()) {
                        file.delete();
                        sdCardInfo.setMounted(true);
                    } else {
                        sdCardInfo.setMounted(false);
                    }
                }
                sdCardInfos.put(SDCARD_EXTERNAL, sdCardInfo);
            }
        }
        return sdCardInfos;
    }


    // 
    /**
     * @Description:API14以上包括14通过此方法获取设备SD卡信息
     * @param context
     * @return
     */
    public static HashMap<String, SDCardInfo> getSDCardInfo(Context context) {
        HashMap<String, SDCardInfo> sdCardInfos = new HashMap<String, SDCardInfo>();
        String[] storagePathList = null;
        try {
            StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            Method getVolumePaths = storageManager.getClass().getMethod("getVolumePaths");
            storagePathList = (String[]) getVolumePaths.invoke(storageManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (storagePathList != null && storagePathList.length > 0) {
            String mSDCardPath = storagePathList[0];
            SDCardInfo internalDevInfo = new SDCardInfo();
            internalDevInfo.setMountPoint(mSDCardPath);
            internalDevInfo.setMounted(checkSDCardMount(context, mSDCardPath));
            sdCardInfos.put(SDCARD_INTERNAL, internalDevInfo);
            if (storagePathList.length >= 2) {
                for (int index = 1, count = storagePathList.length; index <  count; index ++ ) {
                    String externalDevPath = storagePathList[index];
                    if (checkSDCardMount(context, externalDevPath)) {
                        SDCardInfo externalDevInfo = new SDCardInfo();
                        externalDevInfo.setMountPoint(storagePathList[index]);
                        externalDevInfo.setMounted(checkSDCardMount(context, externalDevPath));
                        if (externalDevInfo.getMountPoint().toLowerCase().contains("external")) {
                            sdCardInfos.put(SDCARD_EXTERNAL, externalDevInfo);
                        } else if (externalDevInfo.getMountPoint().toLowerCase().contains("usb")) {
                            sdCardInfos.put(USB_EXTERNAL, externalDevInfo);
                        }                        
                        break;
                    }
                }                
            }
        }
        return sdCardInfos;
    }
    
    /**
     * @Description: 获取内部存储卡信息列表
     * @param context
     * @return
     */
    public static List<SDCardInfo> getInternalSDCardInfos(Context context) {
        List<SDCardInfo> sdCardInfos = new ArrayList<SDCardInfo>();
        String[] storagePathList = null;
        try {
            StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            Method getVolumePaths = storageManager.getClass().getMethod("getVolumePaths");
            storagePathList = (String[]) getVolumePaths.invoke(storageManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (storagePathList != null && storagePathList.length > 0) {
            String internalDevPath = storagePathList[0];
            if (checkSDCardMount(context, internalDevPath)) {
                SDCardInfo externalDevInfo = new SDCardInfo();
                externalDevInfo.setMountPoint(storagePathList[0]);
                externalDevInfo.setMounted(true);
                sdCardInfos.add(externalDevInfo);
            }
        }
        return sdCardInfos;
    }
    
    /**
     * @Description: 获取已挂载成功外接存储信息列表,usb,sdcard
     * @param context
     * @return
     */
    public static List<SDCardInfo> getExternalSDCardInfos(Context context) {
        List<SDCardInfo> sdCardInfos = new ArrayList<SDCardInfo>();
        String[] storagePathList = null;
        try {
            StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            Method getVolumePaths = storageManager.getClass().getMethod("getVolumePaths");
            storagePathList = (String[]) getVolumePaths.invoke(storageManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (storagePathList != null && storagePathList.length > 1) {
            for (int index = 1, count = storagePathList.length; index <  count; index ++ ) {
                String externalDevPath = storagePathList[index];
                if (checkSDCardMount(context, externalDevPath)) {
                    SDCardInfo externalDevInfo = new SDCardInfo();
                    externalDevInfo.setMountPoint(storagePathList[index]);
                    externalDevInfo.setMounted(true);
                    sdCardInfos.add(externalDevInfo);
                }
            }
        }
        return sdCardInfos;
    }


    /**
     * @Description:判断SDCard是否挂载上,返回值为true证明挂载上了，否则未挂载
     * @param context 上下文
     * @param mountPoint 挂载点
     */
    protected static boolean checkSDCardMount(Context context, String mountPoint) {
        if (mountPoint == null) {
            return false;
        }
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        try {
            Method getVolumeState = storageManager.getClass().getMethod("getVolumeState", String.class);
            String state = (String) getVolumeState.invoke(storageManager, mountPoint);
            return Environment.MEDIA_MOUNTED.equals(state);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
//    /**
//     * 检测存储目录是否挂载
//     * @return
//     */
//    public static boolean checkDownloadAvailable(Context context) {
//        boolean result = false;
//        String file = StorageEngine.getDownloadRootPath(context);
//        if (file != null) {
//            result = checkSDCardMount(context, file);
//        }
//        return result;
//    }
    
    /**
     * 检测存储目录空间是否满
     * @return
     */
    public static boolean checkAvailableSizeForDownload(Context context) {
        boolean result = false;
        String file = StorageEngine.getDownloadRootPath(context);
        if (file != null) {
            result = (getAvailableSize(file) > 0);
        }
        return result;
    }
    
    /**
     * @Description:获取路径path所在磁盘分区剩余空间
     * @param path
     * @return
     */
    public static long getAvailableSize(String path) {
        if (path == null || path.isEmpty()) {
            return 0;
        }
        long size = 0;
        if (!path.endsWith("/")) {
            path += "/";
        }
        StatFs mStatFs = new StatFs(path);
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2) {  
            size = mStatFs.getAvailableBytes();  
        }else {  
            long availableBlocks = mStatFs.getAvailableBlocksLong();  
            long blockSize = mStatFs.getBlockSizeLong();  
            size = availableBlocks * blockSize;  
        }      
        return size; 
    }
    
    /**
     * @Description: 获取路径path所在磁盘分区总容量
     * @param path
     * @return
     */
    public static long getTotalSize(String path) {
        if (path == null || path.isEmpty()) {
            return 0;
        }
        long size = 0;
        if (!path.endsWith("/")) {
            path += "/";
        }
        StatFs mStatFs = new StatFs(path);
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2) {  
            size = mStatFs.getTotalBytes(); 
        }else {  
            long availableBlocks = mStatFs.getBlockCountLong();
            long blockSize = mStatFs.getBlockSizeLong();  
            size = availableBlocks * blockSize;  
        }      
        return size; 
    }
    
    /**
     * @Description: 格式化后的磁盘分区剩余空间
     * @param path
     * @return
     */
    public static String getAvailableSizeStr(String path) {
        return formatSize(getAvailableSize(path));
    }
    
    /**
     * @Description: 格式化后的磁盘分区容量
     * @param path
     * @return
     */
    public static String getTotalSizeStr(String path) {
        return formatSize(getTotalSize(path));
    }
    
    /**
     * @Description: 格式化字节数量
     * @param size
     * @return
     */
    public static String formatSize(float size) {
        StringBuilder sb = new StringBuilder();
        float tmpSize = size;
        if (tmpSize < 1) {
            tmpSize = 0;
        }
        if (tmpSize < KB) {
            int tmpdd = Math.round(tmpSize * 10);
            float speed = (float) (tmpdd / 10.0);
            sb.append(speed).append("B");            
        } else if (tmpSize < MB) {
            tmpSize = (float) (tmpSize * 1.0 / KB);
            int tmpdd = Math.round(tmpSize * 10);
            float speed = (float) (tmpdd / 10.0);
            sb.append(speed).append("KB");
        } else if (size < GB) {
            tmpSize = (float) (tmpSize * 1.0 / MB);
            int tmpdd = Math.round(tmpSize * 10);
            float speed = (float) (tmpdd / 10.0);
            sb.append(speed).append("MB");
        } else {
            tmpSize = (float) (tmpSize * 1.0 / GB);
            int tmpdd = Math.round(tmpSize * 10);
            float speed = (float) (tmpdd / 10.0);
            sb.append(speed).append("GB");
        }
        return sb.toString();
    }
    
    /**
     * @Description: 格式化网络速度
     * @param size
     * @return
     */
    public static String formatSpeed(float size) {
        StringBuilder sb = new StringBuilder();
        float tmp = (float) -1.0;
        float tmpSize = size;
        if (tmpSize < 1) {
            tmpSize = 0;
        }
        if (tmpSize < KB) {
            sb.append(Math.round(tmpSize)).append("B/s");
        } else if (tmpSize < MB) {
            tmp = (float) (tmpSize * 1.0 / KB);
            sb.append(Math.round(tmp)).append("KB/s");
        } else if (size < GB) {            
            tmpSize = (float) (tmpSize * 1.0 / MB);
            int tmpdd = Math.round(tmpSize * 10);
            float speed = (float) (tmpdd / 10.0);            
            sb.append(speed).append("MB/s");
        } else {
            tmpSize = (float) (tmpSize * 1.0 / GB);
            int tmpdd = Math.round(tmpSize * 10);
            float speed = (float) (tmpdd / 10.0);            
            sb.append(speed).append("GB/s");
        }
        return sb.toString();
    }
}