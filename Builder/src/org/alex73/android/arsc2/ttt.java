package org.alex73.android.arsc2;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.alex73.android.arsc2.reader.ChunkReader2;

public class ttt {

    public static void main(String[] args) throws Exception {
        InputStream intr = new FileInputStream("../Installer/res/raw/translation.bin");
        Translation tr = new Translation(intr);
        intr.close();

        ZipFile zip = new ZipFile("f:/binaries/Android/Gmail.apk");
        ZipEntry en = zip.getEntry("resources.arsc");
        InputStream in = zip.getInputStream(en);

        ChunkReader2 rsReader = new ChunkReader2(in);
        Resources2 rs = new Resources2();
        rs.processResources(rsReader);

        if (true) {
            all();
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
        ChunkReader2 rsReader = new ChunkReader2(in);
        Resources2 rs = new Resources2();
        rs.processResources(rsReader);
    }
}
