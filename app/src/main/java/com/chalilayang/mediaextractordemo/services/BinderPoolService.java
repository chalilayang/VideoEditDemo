package com.chalilayang.mediaextractordemo.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.chalilayang.mediaextractordemo.aidl.BinderPoolImpl;

public class BinderPoolService extends Service {
    private static final String TAG = "BinderPoolService";
    private Binder mBinder = new BinderPoolImpl();
    @Override
    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }
}
