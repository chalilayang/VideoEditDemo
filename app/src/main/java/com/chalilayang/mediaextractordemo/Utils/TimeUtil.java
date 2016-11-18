/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.chalilayang.mediaextractordemo.Utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static com.chalilayang.mediaextractordemo.Utils.TimeConstants.MS_PER_DAY;
import static com.chalilayang.mediaextractordemo.Utils.TimeConstants.MS_PER_HOUR;
import static com.chalilayang.mediaextractordemo.Utils.TimeConstants.MS_PER_MINUTE;
import static com.chalilayang.mediaextractordemo.Utils.TimeConstants.MS_PER_SECOND;

/**
 * Time utilities
 */
public class TimeUtil {

    private static final long[] TIME_UNITS = {MS_PER_DAY, MS_PER_HOUR, MS_PER_MINUTE,
            MS_PER_SECOND};
    private static final String[] TIME_UNITS_ABBR = {"d", "h", "m", "s", "ms"};

    /**
     * The API deals with seconds since the PST epoch, while pretty much everything else deals
     * with milliseconds since the UTC epoch.
     *
     * @param utcTime time in milliseconds since the epoch in UTC time
     * @return time in seconds since the epoch in PST time zone
     */
    public static long utcToApiTime(long utcTime) {
        long apiTimeMs = utcTime + TimeZone.getTimeZone("PST").getRawOffset();
        return apiTimeMs / MS_PER_SECOND;
    }

    /**
     * The API deals with seconds since the PST epoch, while pretty much everything else deals
     * with milliseconds since the UTC epoch.
     *
     * @param apiTime time in seconds since the epoch in PST time zone
     * @return time in milliseconds since the epoch in UTC time
     */
    public static long apiToUtcTime(long apiTime) {
        long apiTimeMs = apiTime * MS_PER_SECOND;
        return apiTimeMs - TimeZone.getTimeZone("PST").getRawOffset();
    }

    /**
     * Converts minutes to milliseconds.
     * It is implemented because current min API is 8,
     * but TimeUnit.MINUTES.toMillis was added to API 9.
     *
     * @param minutes value to be converted to milliseconds
     * @return milliseconds converted from given minutes
     */
    public static long minutesToMilliseconds(int minutes) {
        return minutes * MS_PER_MINUTE;
    }

    /**
     * Converts given time stamp to human readable string based on liveshare_picker_fragmentnow
     *
     * @param whenMillis time stamp to be converted based on now
     * @param nowMillis  current time stamp
     * @return human readable string indicating given time based on now
     */
    public static String whenFromNow(long whenMillis, long nowMillis) {
        if (whenMillis == nowMillis) {
            return "now";
        }
        StringBuilder sb = new StringBuilder();
        String lastWord = (whenMillis > nowMillis) ? "later" : "ago";
        long diff = Math.abs(whenMillis - nowMillis);
        sb.append(toHumanReadableTime(diff)).append(' ').append(lastWord);
        return sb.toString();
    }

    /**
     * Converts given time stamp to human readable string
     *
     * @param milliseconds time stamp to be converted
     * @return human readable time
     */
    public static String toHumanReadableTime(long milliseconds) {
        StringBuilder sb = new StringBuilder();
        int len = TIME_UNITS.length;
        int i = 0;
        for (; i < len; i++) {
            long quotient = milliseconds / TIME_UNITS[i];
            if (quotient > 0) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(quotient).append(TIME_UNITS_ABBR[i]);
                milliseconds = milliseconds % TIME_UNITS[i];
            }
        }
        if (sb.length() > 0) {
            if (milliseconds > 0) {
                sb.append(' ').append(milliseconds).append(TIME_UNITS_ABBR[i]);
            }
        } else {
            sb.append(milliseconds).append(TIME_UNITS_ABBR[i]);
        }
        return sb.toString();
    }

    public static String getFormatDateTime(int msec) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date(msec));
    }

    public static String getFormatTime(long msec) {
        long hour = msec / (60 * 60 * 1000);
        long minute = (msec - hour * 60 * 60 * 1000) / (60 * 1000);
        long second = (msec - hour * 60 * 60 * 1000 - minute * 60 * 1000) / 1000;
        if (second >= 60) {
            second = second % 60;
            minute += second / 60;
        }
        if (minute >= 60) {
            minute = minute % 60;
            hour += minute / 60;
        }
        StringBuilder sb = new StringBuilder();
        if (hour < 10) {
            sb.append('0');
        }
        sb.append(String.valueOf(hour)).append(':');
        if (minute < 10) {
            sb.append('0');
        }
        sb.append(String.valueOf(minute)).append(':');
        if (second < 10) {
            sb.append('0');
        }
        sb.append(String.valueOf(second));
        return sb.toString();
    }
}
