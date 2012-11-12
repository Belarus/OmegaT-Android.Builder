import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.alex73.android.arsc2.ManifestInfo;
import org.apache.commons.io.IOUtils;

/*
 * Паказвае назвы пакетаў і вэрсіі бінарных файлаў apk. 
 */
public class ListBinaries {
    public static void main(String[] args) throws Exception {
        File[] zips = new File(args[0]).listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".zip");
            }
        });
        Arrays.sort(zips);

        for (File zipFile : zips) {
            System.out.println("============== " + zipFile + " ==============");
            ZipInputStream in = new ZipInputStream(new FileInputStream(zipFile));

            ZipEntry ze;
            while ((ze = in.getNextEntry()) != null) {
                if (ze.getName().endsWith(".apk")) {
                    if (ze.getName().contains("GameHub") && zipFile.getName().contains("s2")) {
                        // invalid entry size (expected 2255224840 but got 50345
                        // bytes)
                        continue;
                    }
                    byte[] apk = IOUtils.toByteArray(in);

                    byte[] manifest = UnpackBinaryResources.extractFile(apk, "AndroidManifest.xml");
                    ManifestInfo mi = new ManifestInfo(manifest);
                    System.out.println("  " + ze.getName() + "  p:" + mi.getPackageName() + " v:" + mi.getVersion());
                }
                System.gc();
            }
        }
    }
}
