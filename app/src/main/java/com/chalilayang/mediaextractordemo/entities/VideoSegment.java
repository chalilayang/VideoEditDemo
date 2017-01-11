package com.chalilayang.mediaextractordemo.entities;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by chalilayang on 2016/12/13.
 */

public class VideoSegment implements Parcelable {
    private VideoData video;
    private long startTime_ns;
    private long endTime_ns;
    public VideoSegment(VideoData videoData, long start, long end) {
        this.video = new VideoData(videoData.filePath, videoData.fileName);
        this.startTime_ns = start;
        this.endTime_ns = end;
    }
    public VideoSegment(Parcel parcel) {
        String filename = parcel.readString();
        String filepath = parcel.readString();
        this.video = new VideoData(filepath, filename);
        this.startTime_ns = parcel.readLong();
        this.endTime_ns = parcel.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.video.fileName);
        dest.writeString(this.video.filePath);
        dest.writeLong(startTime_ns);
        dest.writeLong(endTime_ns);
    }
    public static final Parcelable.Creator<VideoSegment> CREATOR = new Creator<VideoSegment>() {
        @Override
        public VideoSegment createFromParcel(Parcel source) {
            return new VideoSegment(source);
        }

        @Override
        public VideoSegment[] newArray(int size) {
            return new VideoSegment[0];
        }
    };

    public long getStartTime_ns() {
        return this.startTime_ns;
    }
    public long getEndTime_ns() {
        return this.endTime_ns;
    }
    public String getFileName() {
        return this.video.fileName;
    }
    public String getFilePath() {
        return this.getFilePath();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VideoData)) {
            return false;
        }

        VideoSegment that = (VideoSegment) o;
        if (!video.equals(that.video)) {
            return false;
        }
        if (startTime_ns != that.startTime_ns) {
            return false;
        }
        if (endTime_ns != that.endTime_ns) {
            return false;
        }
        return true;
    }
}
