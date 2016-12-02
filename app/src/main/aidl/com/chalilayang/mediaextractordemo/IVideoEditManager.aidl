// IVideoEditManager.aidl
package com.chalilayang.mediaextractordemo;

import com.chalilayang.mediaextractordemo.entities.VideoData;
import com.chalilayang.mediaextractordemo.IVideoEditListener;
interface IVideoEditManager {
    void editVideo(in VideoData video);
    void registerListener(IVideoEditListener listener);
    void unregisterListener(IVideoEditListener listener);
    int getPid();
}
