import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.alex73.android.IDecoder;
import org.alex73.android.IEncoder;
import org.alex73.android.StAXDecoder;
import org.alex73.android.StAXEncoder;
import org.alex73.android.StyledString;
import org.alex73.android.arsc.ChunkReader;
import org.alex73.android.arsc.ChunkWriter;
import org.alex73.android.arsc.ManifestInfo;
import org.alex73.android.arsc.Resources;
import org.alex73.android.arsc.StringTable;
import org.alex73.android.bel.Utils;
import org.alex73.android.bel.zip.ApkUpdater;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.omegat.core.Core;
import org.omegat.core.data.ProjectProperties;
import org.omegat.core.data.RealProject;
import org.omegat.filters2.master.PluginUtils;
import org.omegat.util.ProjectFileStorage;
import org.omegat.util.RuntimePreferences;

public class BuildAll {
    static String projectPath = "../../Android.OmegaT/Android/";
    static String configPath = "../../Android.OmegaT/Android.settings/";
    static String OUT_DIR = "../out/";

    static File[] zips;

    static List<FileInfo> fileNames = new ArrayList<FileInfo>();

    static boolean write;

    public static void main(String[] args) throws Exception {
        zips = new File(args[0]).listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".zip");
            }
        });

        FileUtils.deleteDirectory(new File(OUT_DIR));
        File[] projectSource = new File(projectPath, "source").listFiles();
        for (File f : projectSource) {
            if (f.isDirectory()) {
                FileUtils.deleteDirectory(f);
            }
        }
        File[] projectTarget = new File(projectPath, "target").listFiles();
        for (File f : projectTarget) {
            if (f.isDirectory()) {
                FileUtils.deleteDirectory(f);
            }
        }
        write = false;
        process();
        translate();
        write = true;
        process();

        Writer wr = new OutputStreamWriter(new FileOutputStream(new File(OUT_DIR, "list2.txt")));
        for (FileInfo f : fileNames) {
            wr.write(f.packageName + '|' + f.versionName + '|' + f.originVersion + '|' + f.translatedVersion
                    + '|' + f.filename + "\n");
        }
        wr.close();
    }

    protected static void process() throws Exception {
        Set<String> packages = new HashSet<String>(FileUtils.readLines(new File("../Site/translated2.txt")));
        for (File zipFile : zips) {
            System.out.println(zipFile);
            ZipInputStream in = new ZipInputStream(new FileInputStream(zipFile));

            ZipEntry ze;
            while ((ze = in.getNextEntry()) != null) {

                if (ze.getName().endsWith(".apk")) {
                    System.out.println("  " + ze.getName());
                    byte[] apk = IOUtils.toByteArray(in);
                    byte[] arsc = extractFile(apk, "resources.arsc");
                    byte[] manifest = extractFile(apk, "AndroidManifest.xml");
                    if (arsc != null) {
                        ManifestInfo mi = new ManifestInfo(manifest);
                        if (packages.contains(mi.getPackageName())) {
                            String suffixVersion = zipFile.getName().replaceAll(".zip$", "");
                            String appName = ze.getName().replaceAll(".+/(.+?)\\.apk$", "$1");
                            processARSC(mi, Utils.sha1(apk), arsc, appName, mi.getVersion());
                        } else {
                            System.out.println("   package " + mi.getPackageName() + " not need");
                        }
                    }
                }
                System.gc();
            }
        }
    }

    protected static byte[] processARSC(ManifestInfo man, String originVersion, byte[] arsc, String appName,
            String suffixVersion) throws Exception {
        ChunkReader rsReader = new ChunkReader(arsc);
        Resources rs = new Resources(rsReader);

        File out = new File(projectPath + "source/" + appName);
        out.mkdirs();

        // checks
        ChunkWriter wr = rs.getStringTable().write();
        byte[] readed = rs.getStringTable().getOriginalBytes();
        byte[] written = wr.getBytes();
        Assert.assertArrayEquals(readed, written);

        if (!write) {
            checkAllStrings(rs.getStringTable());

            System.out.println("    Store resources in " + out.getAbsolutePath());
            new StAXEncoder().dump(rs, out, suffixVersion);

            // зыходныя рэсурсы захаваныя наноў - для параўняньня
            ChunkWriter rsWriterOriginal = rs.write();

            Assert.assertArrayEquals(arsc, rsWriterOriginal.getBytes());

            return null;
        } else {
            File outTranslated = new File(projectPath + "target/" + appName);
            System.out.println("    Load resources from " + out.getAbsolutePath());
            new StAXDecoder().reads(rs, outTranslated, suffixVersion);
            ChunkWriter rsWriter = rs.write();
            byte[] outResources = rsWriter.getBytes();

            FileInfo fi = new FileInfo();
            fi.translatedVersion = Utils.sha1(outResources);
            fi.packageName = man.getPackageName();
            fi.versionName = man.getVersion();
            fi.originVersion = originVersion;
            fi.filename = man.getPackageName() + '_' + man.getVersion() + '_' + fi.translatedVersion+".arsc";

            save(fi.filename, arsc, outResources);

            fileNames.add(fi);

            return outResources;

            // File apkOut = new File(OUT_DIR, "files/" + phoneName + '/' + ze.getName());
            // replaceResources(apk, apkOut, outResources);
        }
    }

    protected static void replaceResources(byte[] apk, File out, byte[] newResources) throws Exception {
        out.getParentFile().mkdirs();
        FileUtils.writeByteArrayToFile(out, apk);
        File o = new File(OUT_DIR + "z");
        FileUtils.writeByteArrayToFile(o, apk);

        String origV = Utils.sha1(new ByteArrayInputStream(BuildAll.extractFile(apk, "resources.arsc")));
        String transV = Utils.sha1(new ByteArrayInputStream(newResources));

        String versionMark = origV + "_" + transV;

        new ApkUpdater().replace(o, out, versionMark.getBytes("UTF-8"), newResources);
        if (o.exists()) {
            if (!o.delete()) {
                throw new IOException("Error delete " + o);
            }
        }
    }

    protected static void checkAllStrings(final StringTable table) throws Exception {
        IEncoder encoder = new StAXEncoder();
        IDecoder decoder = new StAXDecoder();

        for (int i = 0; i < table.getStringCount(); i++) {
            StyledString ss1 = table.getStyledString(i);
            if (ss1.hasInvalidChars()) {
                continue;
            }

            String x = encoder.marshall(ss1);
            StyledString ss2 = decoder.unmarshall(x);

            Assert.assertEquals(ss1.raw, ss2.raw);
            Assert.assertArrayEquals(ss1.tags, ss2.tags);
        }
    }

    protected static void translate() throws Exception {
        System.out.println("Initializing OmegaT");
        Map<String, String> pa = new TreeMap<String, String>();
        pa.put("ITokenizer", "org.omegat.plugins.tokenizer.SnowballEnglishTokenizer");
        pa.put("alternate-filename-from", "_.+.xml$");
        pa.put("alternate-filename-to", "_VERSION.xml");

        RuntimePreferences.setConfigDir(configPath);

        PluginUtils.loadPlugins(pa);

        Core.initializeConsole(pa);

        ProjectProperties projectProperties = ProjectFileStorage.loadProjectProperties(new File(projectPath));
        if (!projectProperties.verifyProject()) {
            throw new Exception("The project cannot be verified");
        }

        RealProject p = new RealProject(projectProperties);
        p.loadProject();
        Core.setProject(p);

        p.compileProject(".*");

        p.closeProject();
        System.out.println("Translation finished");
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

    protected static void save(String fn, byte[] origData, byte[] newData) throws Exception {
        File outRes = new File(OUT_DIR + fn);
        outRes.getParentFile().mkdirs();
        GZIPOutputStream gz = new GZIPOutputStream(new FileOutputStream(outRes), newData.length);
        try {
            gz.write(newData);
        } finally {
            gz.close();
        }
    }

    public static class FileInfo {
        String packageName;
        String versionName;
        String originVersion;
        String translatedVersion;
        String filename;
    }
}
