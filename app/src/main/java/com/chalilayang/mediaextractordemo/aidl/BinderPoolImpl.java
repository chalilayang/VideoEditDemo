package com.chalilayang.mediaextractordemo.aidl;

import android.os.IBinder;
import android.os.RemoteException;

import com.chalilayang.mediaextractordemo.IBinderPool;

/**
 * Created by chalilayang on 2016/9/2.
 */
public class BinderPoolImpl extends IBinderPool.Stub {
    public static final int BINDER_ID_BASE = 1;
    public static final int BINDER_ID_VIDEO_EDIT = BINDER_ID_BASE + 1;
    public BinderPoolImpl() {
        super();
    }
    @Override
    public IBinder queryBinder(int binderCode) throws RemoteException {
        switch (binderCode) {
            case BINDER_ID_VIDEO_EDIT:
                return new VideoEditManagerImpl();
        }
        return new VideoEditManagerImpl();
    }
}
