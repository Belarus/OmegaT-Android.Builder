package org.alex73.android;

import org.alex73.android.arsc.ManifestInfo;

public class Context {

    public static boolean ALLOW_MERGE_DUPLICATES;

    public static void setByManifest(ManifestInfo mi) {
        ALLOW_MERGE_DUPLICATES = true;
        // if ("com.android.settings".equals(mi.getPackageName()) &&
        // mi.getVersion().startsWith("4.1")) {
        if (mi.getPackageName().startsWith("com.android") && mi.getVersion().startsWith("4.1")) {
            // Settings 4.1.2
            //
            ALLOW_MERGE_DUPLICATES = false;
        }
        if (mi.getPackageName().startsWith("android") && mi.getVersion().startsWith("4.1")) {
            // Settings 4.1.2
            //
            ALLOW_MERGE_DUPLICATES = false;
        }
    }
}
