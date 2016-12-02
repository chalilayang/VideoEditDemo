// IBinderPool.aidl
package com.chalilayang.mediaextractordemo;

// Declare any non-default types here with import statements

interface IBinderPool {
    IBinder queryBinder(int binderCode);
    int getPid();
}
