package com.chalilayang.mediaextractordemo.entities;

import java.io.Serializable;

/**
 * Created by chalilayang on 2016/11/29.
 */

public class VideoCodecModel implements Serializable {

    private static final long serialVersionUID = -1307249622002520298L;

    public String srcPath;

    public String dstPath;

    public VideoCodecModel(String srcPath, String dstPath) {
        this.srcPath = srcPath;
        this.dstPath = dstPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VideoCodecModel)) return false;

        VideoCodecModel that = (VideoCodecModel) o;
        if (!srcPath.equals(that.srcPath)) return false;
        return dstPath.equals(that.dstPath);
    }
}
