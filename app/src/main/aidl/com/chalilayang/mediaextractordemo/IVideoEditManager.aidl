// IVideoEditManager.aidl
package com.chalilayang.mediaextractordemo;

import com.chalilayang.mediaextractordemo.Video;
import com.chalilayang.mediaextractordemo.IVideoEditListener;
interface IVideoEditManager {
    void editVideo(in Video video);
    void registerListener(IVideoEditListener listener);
    void unregisterListener(IVideoEditListener listener);
}
