package com.chalilayang.mediaextractordemo.aidl;

import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import com.chalilayang.mediaextractordemo.IVideoEditListener;
import com.chalilayang.mediaextractordemo.IVideoEditManager;
import com.chalilayang.mediaextractordemo.entities.VideoData;

/**
 * Created by chalilayang on 2016/9/2.
 */
public class VideoEditManagerImpl extends IVideoEditManager.Stub {
    private static final String TAG = "VideoEditManagerImpl";
    private RemoteCallbackList<IVideoEditListener> mLisnteners
            = new RemoteCallbackList<>();

    public VideoEditManagerImpl() {
        super();
    }

    @Override
    public void registerListener(IVideoEditListener listener) throws RemoteException {
        mLisnteners.register(listener);
        Log.i(TAG, "registerListener: success");
    }

    @Override
    public void unregisterListener(IVideoEditListener listener) throws RemoteException {
        mLisnteners.unregister(listener);
        Log.i(TAG, "unregisterListener: success");
    }

    public int count = 0;
    @Override
    public void editVideo(VideoData video) throws RemoteException {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while(true) {
//                    final int N = mLisnteners.beginBroadcast();
//                    for (int index = 0; index < N; index ++) {
//                        IVideoEditListener listener = mLisnteners.getBroadcastItem(index);
//                        if (listener != null) {
//                            try {
//                                listener.onProgress(count++, 10000);
//                            } catch (RemoteException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
//                    mLisnteners.finishBroadcast();
//                    try {
//                        Thread.sleep(5000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }).start();
    }
}
