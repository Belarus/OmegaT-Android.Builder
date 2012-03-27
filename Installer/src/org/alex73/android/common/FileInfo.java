package org.alex73.android.common;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.alex73.android.arsc.ManifestInfo;

public class FileInfo {
    final static String MANIFEST_FILE = "AndroidManifest.xml";

    public enum STATUS {
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
        return "v2: origApkSha1=" + originID + " transARSCSha1="
                + (translatedID != null ? translatedID : "0");
    }

    public void readManifestInfo() throws Exception {
        ZipFile apk = new ZipFile(localFile);
        try {
            ZipEntry manifestEntry = apk.getEntry(MANIFEST_FILE);
            if (manifestEntry == null) {
                // manifest not found in this apk
                return;
            }

            byte[] buffer = new byte[8192];
            ByteArrayOutputStream buf = new ByteArrayOutputStream(16 * 1024);
            InputStream in = apk.getInputStream(manifestEntry);
            try {
                int c;
                while ((c = in.read(buffer)) > 0) {
                    buf.write(buffer, 0, c);
                }
            } finally {
                in.close();
            }

            ManifestInfo m = new ManifestInfo(buf.toByteArray());
            packageName = m.getPackageName();
            versionName = m.getVersion();
        } finally {
            apk.close();
        }
    }
}
