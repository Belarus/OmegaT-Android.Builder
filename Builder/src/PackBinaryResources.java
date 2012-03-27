import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.alex73.android.StAXDecoder;
import org.alex73.android.arsc.BaseChunked;
import org.alex73.android.arsc.ChunkReader;
import org.alex73.android.arsc.ChunkWriter;
import org.alex73.android.arsc.Config;
import org.alex73.android.arsc.ManifestInfo;
import org.alex73.android.arsc.Package;
import org.alex73.android.arsc.Resources;
import org.alex73.android.bel.Utils;
import org.alex73.android.common.FileInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;

import android.control.App;

public class PackBinaryResources {
    static String projectPath = "../../Android.OmegaT/Android/";
    static File[] zips;
    static File OUT_DIR = new File("../out/");

    public static void main(String[] args) throws Exception {
        FileUtils.deleteDirectory(OUT_DIR);
        OUT_DIR.mkdirs();

        UnpackBinaryResources.readTranslationInfo();

        // process binary zips
        zips = new File(args[0]).listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".zip");
            }
        });
        List<FileInfo> fileInfos = new ArrayList<FileInfo>();
        for (File zipFile : zips) {
            System.out.println(zipFile);
            ZipInputStream in = new ZipInputStream(new FileInputStream(zipFile));

            ZipEntry ze;
            while ((ze = in.getNextEntry()) != null) {

                if (ze.getName().endsWith(".apk")) {
                    System.out.println("  " + ze.getName());
                    byte[] apk = IOUtils.toByteArray(in);
                    byte[] manifest = BuildAll.extractFile(apk, "AndroidManifest.xml");
                    ManifestInfo mi = new ManifestInfo(manifest);
                    String dirName = UnpackBinaryResources.getDirName(mi.getPackageName(), mi.getVersion());
                    if (dirName != null) {
                        byte[] arsc = BuildAll.extractFile(apk, "resources.arsc");

                        byte[] translatedARSC = processARSC(arsc, dirName,
                                UnpackBinaryResources.createSuffix(mi));

                        FileInfo fi = new FileInfo(null);
                        fi.packageName = mi.getPackageName();
                        fi.versionName = mi.getVersion();
                        fi.translatedID = Utils.sha1(translatedARSC);
                        fi.originID = Utils.sha1(apk);
                        fi.remoteFilename = fi.packageName + '_' + fi.translatedID + ".arsc.gz";
                        fileInfos.add(fi);

                        File outRes = new File(OUT_DIR, fi.remoteFilename);
                        FileUtils.writeByteArrayToFile(outRes, Utils.gzip(translatedARSC));
                    }
                }
                System.gc();
            }
        }

        Writer wr = new OutputStreamWriter(new FileOutputStream(new File(OUT_DIR, "list2.txt")));
        for (FileInfo f : fileInfos) {
            wr.write(f.packageName + '|' + f.originID + '|' + f.translatedID + '|' + f.remoteFilename + "\n");
        }
        wr.close();

        wr = new OutputStreamWriter(new FileOutputStream(new File(OUT_DIR, "translated2.txt")));
        for (App a : UnpackBinaryResources.translationInfo.getApp()) {
            if (a.getPackageName() != null) {
                wr.write(a.getPackageName() + "\n");
            }
        }
        wr.close();
    }

    protected static byte[] processARSC(byte[] arsc, String dirName, String suffixVersion) throws Exception {
        ChunkReader rsReader = new ChunkReader(arsc);
        Resources rs = new Resources(rsReader);

        File out = new File(projectPath + "/target/" + dirName);

        // checks
        ChunkWriter wr = rs.getStringTable().write();
        byte[] readed = rs.getStringTable().getOriginalBytes();
        byte[] written = wr.getBytes();
        Assert.assertArrayEquals(readed, written);

        File outTranslated = new File(projectPath + "target/" + dirName);
        System.out.println("    Load resources from " + out.getAbsolutePath());
        new StAXDecoder().reads(rs, outTranslated, suffixVersion);
        removeOtherLanguages(rs);
        ChunkWriter rsWriter = rs.write();
        byte[] outResources = rsWriter.getBytes();

        return outResources;
    }

    protected static void removeOtherLanguages(Resources rs) {
        for (Package p : rs.getPackages()) {
            List<BaseChunked> list = p.getContent();
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) instanceof Config) {
                    Config c = (Config) list.get(i);

                    switch (c.getFlags().getLanguage()) {
                    case "":
                    case "en":
                    case "ru":
                    case "be":
                        break;
                    default:
                        list.remove(i);
                        i--;
                    }
                }
            }
        }
    }
}
