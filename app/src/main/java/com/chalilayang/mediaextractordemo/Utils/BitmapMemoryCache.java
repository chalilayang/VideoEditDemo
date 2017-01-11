package com.chalilayang.mediaextractordemo.Utils;

import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

public class BitmapMemoryCache {

	private LruCache<String, Bitmap> mMemoryCache;
	
	/**
	 * @param memoryCacheSize Cache容量(以字节为单位)
	 */
	public BitmapMemoryCache(Context context, int memoryCacheSize) {
		int maxMemCacheSize = Utils.getMemoryClass(context) * 1024 * 1024 / 2;
		memoryCacheSize = memoryCacheSize > maxMemCacheSize ? maxMemCacheSize : memoryCacheSize;
		mMemoryCache = new LruCache<String, Bitmap>(memoryCacheSize) {
	        /**
	         * Measure item size in bytes rather than units which is more practical for a obj
	         * cache
	         */
	         @Override
	         protected int sizeOf(String key, Bitmap obj) {
	             return BitmapMemoryCache.this.sizeOf(obj);
	         }
	    };
	}
	
	public void put(String key, Bitmap obj) {
		if (mMemoryCache.get(key) == null) {
			mMemoryCache.put(key, obj);
		}
	}
	
	 /**
     * Get from memory cache.
     *
     * @param key Unique identifier for which item to get
     * @return The obj if found in cache, null otherwise
     */
    public Bitmap get(String key) {
    	return mMemoryCache.get(key);
    }
    
    public void clear() {
    	mMemoryCache.evictAll();
	}
    
    public void remove(String key){
    	mMemoryCache.remove(key);
    }
    
    public synchronized final Map<String, Bitmap> snapshot() {
    	return mMemoryCache.snapshot();
    }
    /**
     * 返回该对象的大小(字节数)，用以计算Cache中所有对象占用的空间
     * 当所占空间超过设置大小时，根据LRU算法清除部分对象
     */
    protected int sizeOf(Bitmap obj) {
		return obj != null? obj.getByteCount():1;
	}
}
