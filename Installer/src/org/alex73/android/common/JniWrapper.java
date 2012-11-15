package org.alex73.android.common;

import org.alex73.android.bel.LocalStorage;

public class JniWrapper {

    public static native void getPermissions(LocalStorage.FilePerm fi);

    static {
        System.loadLibrary("stat");
    }
}
