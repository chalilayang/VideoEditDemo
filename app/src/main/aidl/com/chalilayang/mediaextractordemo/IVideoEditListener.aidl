// IVideoEditListener.aidl
package com.chalilayang.mediaextractordemo;

import com.chalilayang.mediaextractordemo.Video;
interface IVideoEditListener {
    void onProgress(int progress, int max);
    void onCodecStart(in Video video);
    void onCodecFinish(in Video video);
}
