package org.alex73.android.common;

import java.io.File;
import java.io.IOException;

import org.alex73.android.bel.MyLog;

public class JniWrapper {

    public static native void getPermissions(FilePerm fi);

    private static native long getSpaceNearFile(String path);

    private static native int chmod(String path, int mode);

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

    public static void chmod(File file, int mode) throws IOException {
        int r = chmod(file.getPath(), mode);
        if (r < 0) {
            throw new IOException("Error chmod " + file);
        }
    }

    static {
        System.loadLibrary("stat");
    }
}
