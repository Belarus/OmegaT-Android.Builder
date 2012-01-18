import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.alex73.android.arsc.ManifestInfo;
import org.apache.commons.io.IOUtils;

/**
 * Dumps list of binary resources, with package name and version.
 */
public class DumpBinaryResourcesNames {
    public static void main(String[] args) throws Exception {

        // process binary zips
        File[] zips = new File(args[0]).listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".zip");
            }
        });
        for (File zipFile : zips) {
            System.out.println(zipFile);
            ZipInputStream in = new ZipInputStream(new FileInputStream(zipFile));

            ZipEntry ze;
            while ((ze = in.getNextEntry()) != null) {
                if (ze.getName().endsWith(".apk")) {
                    byte[] apk = IOUtils.toByteArray(in);
                    byte[] manifest = BuildAll.extractFile(apk, "AndroidManifest.xml");
                    ManifestInfo mi = new ManifestInfo(manifest);
                    System.out.println("  " + ze.getName() + " -> " + mi.getPackageName() + " / "
                            + mi.getVersion());
                }
            }
        }
    }
}
