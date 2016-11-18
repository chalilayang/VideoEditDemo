package com.chalilayang.mediaextractordemo.Utils;

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
}
