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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        return sdf.format(new Date(System.currentTimeMillis()));
    }

    public static String parseFileName(String path) {
        if (TextUtils.isEmpty(path)) {
            return "";
        }
        int index = path.lastIndexOf(File.separatorChar);
        if (index < 0) {
            return path;
        } else {
            return path.substring(index);
        }
    }
}
