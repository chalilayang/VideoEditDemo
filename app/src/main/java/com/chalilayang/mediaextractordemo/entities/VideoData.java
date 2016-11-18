package com.chalilayang.mediaextractordemo.entities;

/**
 * Created by chalilayang on 2016/11/16.
 */

public class VideoData {
    public final String fileName;
    public final String filePath;
    public VideoData(String file_path, String fileName) {
        this.filePath = file_path;
        this.fileName = fileName;
    }
}
