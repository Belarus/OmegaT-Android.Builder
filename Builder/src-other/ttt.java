import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.alex73.android.arsc2.ResourceProcessor;
import org.alex73.android.arsc2.Translation;
import org.alex73.android.arsc2.reader.ChunkReader2;
import org.alex73.android.bel.zip.ApkUpdater;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class ttt {

    public static void main(String[] args) throws Exception {
        InputStream intr = new FileInputStream("../Installer/res/raw/translation.bin");
        Translation tr = new Translation(new GZIPInputStream(intr));
        intr.close();

        ZipFile zip = new ZipFile("f:/binaries/Android/framework-res.apk");
        ZipEntry en = zip.getEntry("resources.arsc");

        InputStream in = zip.getInputStream(en);
        ChunkReader2 rsReader = new ChunkReader2(in);
        ResourceProcessor rs = new ResourceProcessor(rsReader);

        rs.process("Settings", tr);

        byte[] out = rs.save();
        FileUtils.writeByteArrayToFile(new File("C:/Temp/resources.arsc"), out);
        new ApkUpdater().replace(new File("f:/binaries/Android/framework-res.apk"), new File(
                "C:/Temp/framework-res.apk"), new byte[] { 0 }, out);
        if (true) {
            // all();
        }

        System.out.println("done");
    }

    public static void all() throws Exception {
        File[] zips = new File("f:/binaries/Android/").listFiles(new FileFilter() {
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
                    System.out.println("  " + ze.getName());

                    ZipInputStream inApk = new ZipInputStream(in);
                    ZipEntry zeApk;
                    while ((zeApk = inApk.getNextEntry()) != null) {
                        if ("resources.arsc".equals(zeApk.getName())) {
                            process(inApk);
                        }
                    }
                }
            }
        }
    }

    protected static void process(InputStream in) throws IOException {
        byte[] resources = IOUtils.toByteArray(in);

        ResourceProcessor rs = new ResourceProcessor();

        ChunkReader2 rsReader = new ChunkReader2(new ByteArrayInputStream(resources));
        rs.markUsed(rsReader);

        rsReader = new ChunkReader2(new ByteArrayInputStream(resources));
        rs.updateStrings(rsReader);
    }
}
