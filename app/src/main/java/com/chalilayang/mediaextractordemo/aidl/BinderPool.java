package com.chalilayang.mediaextractordemo.aidl;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.chalilayang.mediaextractordemo.IBinderPool;
import com.chalilayang.mediaextractordemo.services.BinderPoolService;

import java.util.concurrent.CountDownLatch;

/**
 * Created by chalilayang on 2016/9/2.
 */
public class BinderPool {
    private static final String TAG = "BinderPool";
    private Context mContext;
    private static BinderPool sInstance;

    private CountDownLatch mCountDownLatch;
    private IBinderPool mBinderPool;
    private BinderPool(Context context) {
        this.mContext = context.getApplicationContext();
        connectBinderPoolService();
    }
    public static BinderPool getInstance(Context context) {
        if (sInstance == null) {
            synchronized (BinderPool.class) {
                if (sInstance == null) {
                    sInstance = new BinderPool(context);
                }
            }
        }
        return sInstance;
    }
    public synchronized void connectBinderPoolService() {
        mCountDownLatch = new CountDownLatch(1);
        Intent intent = new Intent(this.mContext, BinderPoolService.class);
        this.mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        try {
            mCountDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinderPool = BinderPoolImpl.asInterface(service);
            try {
                mBinderPool.asBinder().linkToDeath(mDeathRecipient, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mCountDownLatch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            Log.i(TAG, "binderDied: binder died");
            mBinderPool.asBinder().unlinkToDeath(mDeathRecipient, 0);
            mBinderPool = null;
            connectBinderPoolService();
        }
    };

    public IBinder queryBinder(int code) {
        IBinder binder = null;
        try {
            if (mBinderPool != null) {
                binder = mBinderPool.queryBinder(code);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return binder;
    }

    public static synchronized void unbindService() {
        if (sInstance != null && sInstance.mConnection != null) {
            sInstance.mContext.unbindService(sInstance.mConnection);
            sInstance = null;
            sInstance.mConnection = null;
        }
    }
}
