package org.alex73.android.common;

import java.io.File;
import java.io.IOException;

import org.alex73.android.bel.MyLog;

public class JniWrapper {

    public static native void getPermissions(FilePerm fi);

    private static native long getSpaceNearFile(String path);

    public static long getSpaceNearFile(File file) throws IOException {
        long r = getSpaceNearFile(file.getPath());
        if (r < 0) {
            MyLog.log("## getSpaceNearFile " + file.getPath() + ": ERROR");
            throw new IOException("Error get space near " + file);
        } else {
            MyLog.log("## getSpaceNearFile " + file.getPath() + ": " + r);
        }
        return r;
    }

    static {
        System.loadLibrary("stat");
    }
}
