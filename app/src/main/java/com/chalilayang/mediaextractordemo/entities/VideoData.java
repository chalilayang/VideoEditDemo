package com.chalilayang.mediaextractordemo.entities;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by chalilayang on 2016/11/16.
 */

public class VideoData implements Parcelable, Serializable {
    public final String fileName;
    public final String filePath;
    public VideoData(String file_path, String fileName) {
        this.filePath = file_path;
        this.fileName = fileName;
    }
    public VideoData(Parcel data) {
        this.filePath = data.readString();
        this.fileName = data.readString();
    }
    public static final Parcelable.Creator<VideoData> CREATOR = new Creator<VideoData>() {
        @Override
        public VideoData createFromParcel(Parcel source) {
            return new VideoData(source);
        }

        @Override
        public VideoData[] newArray(int size) {
            return new VideoData[0];
        }
    };
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.fileName);
        dest.writeString(this.filePath);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VideoData)) {
            return false;
        }

        VideoData that = (VideoData) o;
        if (!fileName.equals(that.fileName)) {
            return false;
        }
        return filePath.equals(that.filePath);
    }
}
