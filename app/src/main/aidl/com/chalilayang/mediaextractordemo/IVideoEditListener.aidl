// IVideoEditListener.aidl
package com.chalilayang.mediaextractordemo;

import com.chalilayang.mediaextractordemo.entities.VideoData;
interface IVideoEditListener {
    void onProgress(int progress, int max);
    void onCodecStart(in VideoData video);
    void onCodecFinish(in VideoData video);
}
