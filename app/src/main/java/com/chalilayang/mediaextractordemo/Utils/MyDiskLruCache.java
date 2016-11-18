package com.chalilayang.mediaextractordemo.Utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;


/**
 * 所有Cache的基类，使用DiskLruCache管理Disk上缓存的文件
 * 主要包括ObjectDiskLruCache、BitmapLruCache、StringDiskLruCache三种
 * 用户可以继承该类进行扩展，实现自定义Cache
 */
public abstract class MyDiskLruCache {
	
	protected static final int VERSION = 201205;
	protected static final int VALUE_COUNT = 1;
	
	private static final long DEFAULT_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 5; // five day
	private static final boolean DEFAULT_EXPIRE_TIME_ENABLED = false;

    // Default disk cache size
    private static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 20; // 20MB

    // Constants to easily toggle various caches
    private static final boolean DEFAULT_CLEAR_DISK_CACHE_ON_START = false;
    
    private static final boolean DEFAULT_INTERNAL_DISK_ENABLED = true;
	    
	protected DiskLruCache mCache;

	protected MyCacheParams mCacheParams;
	
	protected MyDiskLruCache() {}
	
	protected MyDiskLruCache(Context context, MyCacheParams cacheParams) {
	    mCacheParams = cacheParams;
		open(context);
	}
	
	protected abstract File getCacheDirectory(Context context);
	
	public abstract Object get(String key);
	
	public abstract void put(String key, Object obj);
	
	public File getCacheDirectory() {
		if (mCache != null) {
			return mCache.getDirectory();
		}
		return null;
	}
	
    public File getCacheFile(String key) {
        return new File(getCacheDirectory(), key + ".0");
    }
	
	public synchronized boolean remove(String key) throws IOException {
		mCache.remove(key);
		mCache.flush();
		return true;
	}
	
	/**
	 * 清除缓存，对外公共的方法
	 */
	public void clearCache() {
		try {
			deleteFile(getCacheDirectory());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isClosed() {
		return mCache == null || mCache.isClosed();
	}
	
    protected void open(Context context) {
        try {
            if (mCache == null || mCache.isClosed()) {
                File cacheDir = getCacheDirectory(context);
                if (cacheDir == null) {
                    return;
                }
                long availableSize = Utils.getAvailableSize(cacheDir.getParentFile());
                if (mCacheParams.getDiskCacheSize() * 3 > availableSize) {
                    mCacheParams.setDiskCacheSize(availableSize / 3);
                }
                
            	if (mCacheParams.clearDiskCacheOnStart) {
					deleteFile(cacheDir);
				}
            	
            	if (mCacheParams.expireTimeEnabled) {
            		deleteExpiredFile(cacheDir);
            	}
            	
                mCache = DiskLruCache.open(cacheDir, VERSION, VALUE_COUNT, mCacheParams.getDiskCacheSize());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
	
	public void reopen(Context context) {
	    close();
	    open(context);
	}
	
	public void close() {
        if (mCache != null && !mCache.isClosed()) {
            try {
                mCache.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCache = null;
        }
	}
	
	/**
	 * Get InputStream from disk cache.
	 * 
	 * @param data
	 *            Unique identifier for which item to get
	 * @return The InputStream if found in cache, null otherwise
	 */
	protected InputStream getFromDiskCache(String data) {
	    if(mCache == null) {
	        return null;
	    }
		try {
			DiskLruCache.Snapshot snapshot = mCache.get(data);
			if (snapshot != null) {
				return snapshot.getInputStream(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
    private static void deleteFile(File dir) throws IOException {
    	File[] files = getFilesFromDir(dir);
        if (files != null) {
        	for (File file : files) {
                if (file.isDirectory()) {
                	deleteFile(file);
                }
                if (!file.delete()) {
                    throw new IOException("failed to delete file: " + file);
                }
            }
        }
    }
    
    private void deleteExpiredFile(File dir) throws IOException {
    	File[] files = getFilesFromDir(dir);
        if (files != null) {
        	long curTime = System.currentTimeMillis();
        	for (File file : files) {
                if (file.isDirectory()) {
                	deleteExpiredFile(file);
                }
                boolean isDel = (file.lastModified() + mCacheParams.expireTime) < curTime;
                if (isDel && !file.delete()) {
                    throw new IOException("failed to delete file: " + file);
                }
            }
        }
    }
    
    public long getCacheSize(){
        return calculateCacheSize(getCacheDirectory());
    }
    
    /**
     * 计算cache大小（此方法默认dir目录下没有子目录，因此没有做文件递归处理）
     * @param dir cache目录
     * @return 
     */
    private long calculateCacheSize(File dir){
        
        long size = 0;
        if(dir == null || !dir.exists()) return size;      
        
        try {
            File flist[] = dir.listFiles();
            for (int i = 0; i < flist.length; i++)
            {
                if (flist[i].isFile())
                {
                    size = size + flist[i].length();
                }
            }
        }catch (OutOfMemoryError error){
            
        }catch (Exception e) {
        
        }
        
        return size;
    }
    
    public void clearExpiredCache() {
    	try {
			deleteExpiredFile(getCacheDirectory());
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

	private static File[] getFilesFromDir(File dir) {
		if (dir == null || !dir.exists() || !dir.isDirectory()) {
    		return null;
    	}
    	
        File[] files = dir.listFiles();
		return files;
	}
    
	protected File getCacheDirectory(Context context, String name) {
		File file = null;
		if (Utils.isExternalStorageExist()){
			file = Utils.getExternalDiskCacheDir(context, name);
			try {
				deleteFile(Utils.getInternalDiskCacheDir(context, name));
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if(mCacheParams.internalDiskEnabled){
			file = Utils.getInternalDiskCacheDir(context, name);
		}
		return file;
	}
	
	/**
	 * 将一个InputStream保存的Disk上
	 */
	public void putInputStream(String key, InputStream is) {
		OutputStream out = null;
        try {
        	DiskLruCache.Editor editor = mCache.edit(key);
			if (editor != null) {
				 out = new BufferedOutputStream(editor.newOutputStream(0), Utils.IO_BUFFER_SIZE);
				 byte b[] = new byte[1024];
				 int i = -1;
				 while ((i = is.read(b)) != -1) {
					 out.write(b, 0, i);
				 }
				 out.flush();
				 is.close();
		         editor.commit();
			     mCache.flush();
			}
        } catch (Exception e) {
        	e.printStackTrace();
		} finally {
            if (out != null) {
                try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
            }
        }
	}
	
    public static class MyCacheParams {
        private long diskCacheSize = DEFAULT_DISK_CACHE_SIZE;
        public boolean clearDiskCacheOnStart = DEFAULT_CLEAR_DISK_CACHE_ON_START;
        public long expireTime = DEFAULT_EXPIRE_TIME;
        public boolean expireTimeEnabled = DEFAULT_EXPIRE_TIME_ENABLED;
        public boolean internalDiskEnabled = DEFAULT_INTERNAL_DISK_ENABLED;
        
        public long getDiskCacheSize() {
			return diskCacheSize;
		}
        
        public void setDiskCacheSize(long diskCacheSize) {
			this.diskCacheSize = diskCacheSize;
		}
    }
}
