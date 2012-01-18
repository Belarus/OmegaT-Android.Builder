package org.alex73.android.bel;

import java.io.File;
import java.io.IOException;

public class FileInfo {
    enum STATUS {
        UPDATE, LATEST, NONTRANSLATED, NONEED
    };

    public final File localFile;
    public String packageName, versionName;
    public String originID;
    public String translatedID;
    public String remoteFilename;
    public int localSize;
    public int transferSize;
    public STATUS remoteStatus;
    public boolean localStatusOrigin;

    public FileInfo(File f) {
        localFile = f;
    }

    public String store() throws IOException {
        return "v2: origApkSha1=" + originID + " transARSCSha1=" + (translatedID != null ? translatedID : "0");
    }
}
