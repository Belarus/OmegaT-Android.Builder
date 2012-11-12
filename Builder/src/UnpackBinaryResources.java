import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.bind.JAXBContext;

import org.alex73.android.Context;
import org.alex73.android.IDecoder;
import org.alex73.android.IEncoder;
import org.alex73.android.StAXDecoder;
import org.alex73.android.StAXEncoder;
import org.alex73.android.StyledString;
import org.alex73.android.arsc2.ManifestInfo;
import org.alex73.android.arsc2.ResourceProcessor;
import org.alex73.android.arsc2.StringTable2;
import org.alex73.android.arsc2.reader.ChunkMapper;
import org.alex73.android.arsc2.reader.ChunkReader2;
import org.alex73.android.common.FileInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;

import android.control.App;
import android.control.Translation;

/**
 * Imports binary resources. arg0 is binaries .zips dir
 */
public class UnpackBinaryResources {
    static String projectPath = "../../Android.OmegaT/Android/";
    static File[] zips;

    static List<FileInfo> fileNames = new ArrayList<FileInfo>();

    static Translation translationInfo;

    public static void main(String[] args) throws Exception {
        StringTable2.DESKTOP_MODE = true;

        translationInfo = readTranslationInfo();

        // process binary zips
        zips = new File(args[0]).listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".zip");
            }
        });
        for (File zipFile : zips) {
            System.out.println(zipFile);
            ZipInputStream in = new ZipInputStream(new FileInputStream(zipFile));

            ZipEntry ze;
            while ((ze = in.getNextEntry()) != null) {
                if (ze.getName().contains("GameHub") && zipFile.getName().contains("s2")) {
                    // invalid entry size (expected 2255224840 but got 50345
                    // bytes)
                    continue;
                }
                if (ze.getName().endsWith(".apk")) {
                    byte[] apk = IOUtils.toByteArray(in);
                    byte[] manifest = extractFile(apk, "AndroidManifest.xml");
                    ManifestInfo mi = new ManifestInfo(manifest);
                    System.out.println("  " + ze.getName() + "  p:" + mi.getPackageName() + " v:" + mi.getVersion());
                    Context.setByManifest(mi);
                    String dirName = getDirName(mi.getPackageName(), mi.getVersion());
                    if (dirName != null) {
                        byte[] arsc = extractFile(apk, "resources.arsc");
                        if (arsc != null) {
                            processARSC(arsc, dirName, createSuffix(mi));
                        }
                    }
                }
                System.gc();
            }
        }
    }

    static byte[] globalStringTableBytes;

    protected static void processARSC(byte[] arsc, String dirName, String suffixVersion) throws Exception {
        ChunkReader2 rsReader = new ChunkReader2(new ByteArrayInputStream(arsc));
        ResourceProcessor rs = new ResourceProcessor(rsReader, new ResourceProcessor.Callback() {
            public void onGlobalStringTable(ChunkMapper stringChunk) {
                globalStringTableBytes = stringChunk.getBytes();
            }
        });

        File out = new File(projectPath + "/source/" + dirName);
        out.mkdirs();

        checkAllStrings(rs.globalStringTable);

        // checks
        byte[] written = rs.globalStringTable.write();
        byte[] readed = globalStringTableBytes;

        if (!Arrays.equals(readed, written)) {
            FileUtils.writeByteArrayToFile(new File("/tmp/st-orig"), readed);
            FileUtils.writeByteArrayToFile(new File("/tmp/st-new"), written);
            throw new Exception(
                    "StringTables are differ: /tmp/st-orig, /tmp/st-new\nhd -v < /tmp/st-orig > /tmp/st-orig.txt\nhd -v < /tmp/st-new > /tmp/st-new.txt");
        }

        System.out.println("    Store resources in " + out.getAbsolutePath() + " version=" + suffixVersion);
        new StAXEncoder().dump(rs, out, suffixVersion);

        // зыходныя рэсурсы захаваныя наноў - для параўняньня
        byte[] rsWriterOriginal = rs.save();

        if (!Arrays.equals(arsc, rsWriterOriginal)) {
            FileUtils.writeByteArrayToFile(new File("/tmp/st-orig"), arsc);
            FileUtils.writeByteArrayToFile(new File("/tmp/st-new"), rsWriterOriginal);
            throw new Exception(
                    "Resources are differ: /tmp/st-orig, /tmp/st-new\nhd -v < /tmp/st-orig > /tmp/st-orig.txt\nhd -v < /tmp/st-new > /tmp/st-new.txt");
        }
    }

    protected static void checkAllStrings(final StringTable2 table) throws Exception {
        IEncoder encoder = new StAXEncoder();
        IDecoder decoder = new StAXDecoder();

        for (int i = 0; i < table.getStringCount(); i++) {
            StyledString ss1 = table.getStrings().get(i).getStyledString();
            if (hasInvalidChars(ss1.raw)) {
                continue;
            }

            String x = encoder.marshall(ss1);
            StyledString ss2 = decoder.unmarshall(x);

            Assert.assertEquals(ss1.raw, ss2.raw);
            Assert.assertArrayEquals(ss1.tags, ss2.tags);
        }
    }

    static final String ALLOWED_VER = "0123456789.";

    static String createSuffix(ManifestInfo mi) {
        return mi.getVersion();
    }

    static Translation readTranslationInfo() throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(Translation.class);
        Translation result = (Translation) ctx.createUnmarshaller().unmarshal(
                new File(projectPath + "../translation.xml"));

        Set<String> uniqueApps = new HashSet<String>();
        Set<String> uniquePackages = new HashSet<String>();
        for (App app : result.getApp()) {
            if (uniqueApps.contains(app.getDirName())) {
                throw new RuntimeException("Non-unique app in translation.xml: " + app.getDirName());
            }
            if (uniquePackages.contains(app.getPackageName())) {
                throw new RuntimeException("Non-unique package in translation.xml: " + app.getPackageName());
            }
            uniqueApps.add(app.getDirName());
            uniquePackages.add(app.getPackageName());
        }
        return result;
    }

    static String getDirName(String packageName, String versionName) {
        String dirName = null;
        for (App app : translationInfo.getApp()) {
            if (!packageName.equals(app.getPackageName())) {
                continue;
            }
            if (dirName != null) {
                throw new RuntimeException("Duplicate dir for package " + packageName);
            }
            dirName = app.getDirName();
        }
        return dirName;
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

    public static boolean hasInvalidChars(CharSequence str) {
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c < 0x20) {
                if (c != 0x09 && c != 0x0A && c != 0x0D) {
                    return true;
                }
            } else if (c >= 0x20 && c <= 0xD7FF) {
            } else if (c >= 0xE000 && c <= 0xFFFD) {
            } else if (c >= 0x10000 && c <= 0x10FFFF) {
            } else {
                return true;
            }
        }
        return false;
    }
}
