package org.alex73.android.common;

import java.io.File;
import java.io.IOException;

public class JniWrapper {

    public static native void getPermissions(FilePerm fi);

    private static native long getSpaceNearFile(String path);

    public static long getSpaceNearFile(File file) throws IOException {
        long r = getSpaceNearFile(file.getPath());
        if (r < 0) {
            throw new IOException("Error get space near " + file);
        }
        return r;
    }

    static {
        System.loadLibrary("stat");
    }
}
