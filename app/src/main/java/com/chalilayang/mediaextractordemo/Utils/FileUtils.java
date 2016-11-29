package com.chalilayang.mediaextractordemo.Utils;

import android.text.TextUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by chalilayang on 2016/11/16.
 */

public class FileUtils {
    public static String generateNameByDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH:mm:ss");
        return sdf.format(new Date(System.currentTimeMillis()));
    }

    public static String parseFileName(String path) {
        if (TextUtils.isEmpty(path)) {
            return "";
        }
        int index = path.lastIndexOf(File.separatorChar);
        if (index < 0 && index < path.length()-1) {
            return path;
        } else {
            return path.substring(index + 1);
        }
    }
}
