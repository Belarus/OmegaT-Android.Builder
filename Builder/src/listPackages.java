import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.alex73.android.arsc.ManifestInfo;
import org.alex73.android.bel.Utils;
import org.apache.commons.io.IOUtils;

public class listPackages {
    public static void main(String[] args) throws Exception {

        Map<String, String> packages = new TreeMap<String, String>();
        File[] zips = new File("d:\\My\\binaries\\Android").listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".zip");
            }
        });

        for (File zipFile : zips) {
            // System.out.println(zipFile);
            ZipInputStream in = new ZipInputStream(new FileInputStream(zipFile));

            ZipEntry ze;
            while ((ze = in.getNextEntry()) != null) {

                if (ze.getName().endsWith(".apk")) {
                    byte[] apk = IOUtils.toByteArray(in);

                    byte[] m2 = extractFile(apk, "AndroidManifest.xml");
                    byte[] r = extractFile(apk, "resources.arsc");
                    if (r == null)
                        continue;
                    ManifestInfo mi = new ManifestInfo(m2);
                    packages.put(mi.getPackageName(), ze.getName());
                    System.out.println(Utils.sha1(r) + '_'
                            + ze.getName().substring(ze.getName().lastIndexOf('/') + 1) + "="
                            + Utils.sha1(apk));
                }
                System.gc();
            }
        }
        for (String p : packages.keySet()) {
            System.out.println(p + " = " + packages.get(p));
        }
        for (String p : packages.keySet()) {
            System.out.println(p);
        }

    }

    protected static byte[] extractFile(byte[] zip, String name) throws Exception {
        ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip));
        ZipEntry ze;
        while ((ze = in.getNextEntry()) != null) {
            if (name.equals(ze.getName())) {
                return IOUtils.toByteArray(in);
            }
        }
        return null;
    }
}
