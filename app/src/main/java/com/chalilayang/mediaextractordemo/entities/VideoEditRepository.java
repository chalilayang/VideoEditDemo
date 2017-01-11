package com.chalilayang.mediaextractordemo.entities;

import com.chalilayang.mediaextractordemo.Utils.BitmapMemoryCache;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chalilayang on 2016/12/12.
 */

public class VideoEditRepository {
    private static final int CACHE_SIZE = 1024 * 1024;
    private BitmapMemoryCache bitmapMemoryCache;
    private VideoData videoData;

    private List<VideoSegment> segmentList = new ArrayList<VideoSegment>(6);
    private static VideoEditRepository INSTANCE = null;

    public static VideoEditRepository getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new VideoEditRepository();
        }
        return INSTANCE;
    }

    public void insertSegment(List<VideoSegment> segments, int index) {
        if (index >= 0 && index <= segmentList.size()) {
            segmentList.addAll(index, segments);
        }
    }

    public void addSegment(VideoSegment segment) {
        segmentList.add(segment);
    }

    public List<VideoSegment> getSegmentList() {
        List<VideoSegment> segments = new ArrayList<>(segmentList.size());
        segments.addAll(segmentList);
        return segments;
    }
}
