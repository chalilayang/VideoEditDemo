package com.chalilayang.mediaextractordemo.Utils;

import java.io.File;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.content.Context;


public class ObjectDiskLruCache extends MyDiskLruCache {
	
	private static final String CACHE_NAME = "object";
	
	private static ObjectDiskLruCache sMyDiskLruCache;
	
	private ObjectDiskLruCache(Context context, MyCacheParams cacheParams) {
		super(context, cacheParams);
	}

	public static synchronized ObjectDiskLruCache getInstance(Context context, MyCacheParams cacheParams) {
        if (sMyDiskLruCache == null) {
            sMyDiskLruCache = new ObjectDiskLruCache(context, cacheParams);
        } else if (sMyDiskLruCache.isClosed()) {
            sMyDiskLruCache.open(context);
        }
		return sMyDiskLruCache;
	}
	
	public static synchronized ObjectDiskLruCache getInstance() {
	    return sMyDiskLruCache;
	}

	@Override
	public Object get(String key) {
		ObjectInputStream objin = null;
		try {
			InputStream inputStream = getFromDiskCache(key);
			if (inputStream != null) {
				objin = new ObjectInputStream(inputStream);
				return objin.readObject();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (objin != null) {
					objin.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public void put(String key, Object obj) {
		ObjectOutputStream objout =  null; 
		try {
			DiskLruCache.Editor editor = mCache.edit(key);
			if (editor != null) {
				objout = new  ObjectOutputStream(editor.newOutputStream(0));
				objout.writeObject(obj);
				editor.commit();
				mCache.flush();
			}
			
		} catch (Exception e) {
			
		} finally {
			try {
				objout.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected File getCacheDirectory(Context context) {
		return getCacheDirectory(context, CACHE_NAME);
	}
	
}
