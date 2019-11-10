package com.shamanland.adosbox2;

import android.content.Context;
import android.os.Environment;

import com.fishstix.dosboxfree.DBMain;

import java.io.File;

/**
 * Created by root on 1/8/18.
 */

public class DosHDD {
    public static boolean unzipping = false;

    public static boolean hddExists() {
        String parentDir = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Android/data/" + DBMain.PACKAGE_NAME + "/files/";
        String hddDir = parentDir + "doshdd/WINDOWS/SYSTEM/WIN32S/WSOCK32.DLL"; // last file that is extracted
        File hddDirFile = new File(hddDir);

        return hddDirFile.exists();
    }

    public static boolean createHdd(Context ctx) {
        String parentDir = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Android/data/" + DBMain.PACKAGE_NAME + "/files/";
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Android/data/" + DBMain.PACKAGE_NAME);
        if (!file.exists())
            file.mkdirs();
        unzipping = true;
        boolean ret = Decompress.unzipFromAssets(ctx, "doshdd.zip", parentDir);
        unzipping = false;
        return ret;
    }

    public static String hddLocation() {
        return Environment.getExternalStorageDirectory().getAbsolutePath()+"/Android/data/" + DBMain.PACKAGE_NAME + "/files/doshdd/";
    }
}
