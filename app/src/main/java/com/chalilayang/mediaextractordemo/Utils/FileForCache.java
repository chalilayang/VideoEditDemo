package com.chalilayang.mediaextractordemo.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import android.os.Environment;
import android.util.Log;

public class FileForCache {
    File oSavedFile;
    long nPos;
    RandomAccessFile outFile = null;

    public FileForCache(String path, String sName, long nPos) throws IOException {

        File dir = new File(path);

        try {
            String command = "chmod 777 " + dir.getParentFile().getAbsolutePath();
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            Log.i("yangyong", "chmod fail!!!!"  + dir.getParentFile().getAbsolutePath());
            e.printStackTrace();
        }

        boolean mkdirsuccess = false;
        if (!dir.exists()) {
            mkdirsuccess = dir.mkdirs();
        } else {
            mkdirsuccess = true;
        }

        if (mkdirsuccess) {
            oSavedFile = new File(path + File.separator + sName);
            if (oSavedFile.exists() && nPos == 0) {
                oSavedFile.delete();
                oSavedFile.createNewFile();
            } else if (!oSavedFile.exists()) {
                oSavedFile.createNewFile();
            }
            
            this.nPos = nPos;
            outFile = new RandomAccessFile(oSavedFile.getAbsoluteFile(), "rw");
            outFile.seek(nPos);
        } else {
            FileNotFoundException e = new FileNotFoundException();
            throw e;
        }        
    }

    public boolean checkSpaceAvailble(long filesize) throws IOException {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        if (oSavedFile.getAbsolutePath().startsWith(path)) {
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//                throw new IOException(CacheInfo.STATE_ERROR_NO_SPACE,
//                        path + " MEDIA_MOUNTED failed");
            }
        }
        long availale_space = SDCardUtil.getAvailableSize(path);
        return (availale_space - filesize) > 0;
    }

    public int write_flush(byte[] b, int nLen) throws IOException {
        outFile.write(b, 0, nLen);
        nPos += nLen;
        return nLen;
    }

    public long getFileSize() {
        if (oSavedFile != null) {
            return oSavedFile.length();
        } else {
            return 0;
        }
    }

    public void closeFile() {
        if (outFile != null) {
            try {
                outFile.close();
                outFile = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
