package com.chalilayang.mediaextractordemo.entities;

/**
 * Created by chalilayang on 2016/11/24.
 */

public class SrtEntity {
    public final long start;
    public final long end;
    public final String text;
    public SrtEntity(long s, long e, String t) {
        this.start = s;
        this.end = e;
        this.text = t;
    }
}
