import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import brut.androlib.Androlib;
import brut.androlib.ApkDecoder;

public class AndroidExtract {
    static final String SOURCE_XML = "../../Android.OmegaT/Android/source/";
    static final String TARGET_XML = "../../Android.OmegaT/Android/target/";
    static final String OUT_DIR = "../out/";
    static final String APK_DIR = "../Files/apk/";

    static final Pattern RE_RESLANG = Pattern.compile("[A-Za-z]{2}");

    static File tempDir;
    static int extracted = 0;

    public static void main(String[] args) throws Exception {
        setupLogging(false);

        FileUtils.copyFile(new File("bin/aapt-8.exe"), new File("aapt.exe"));

        tempDir = new File(File.createTempFile("AndroidExtract", "").getAbsolutePath() + ".dir");

        FileUtils.deleteDirectory(new File(OUT_DIR));

        File[] zips = new File(APK_DIR).listFiles();
        for (File z : zips) {
            ZipFile zip = new ZipFile(z);
            String system = z.getName().replace(".zip", "");
            System.out.println("================ " + z.getName());
            for (Enumeration<? extends ZipEntry> en = zip.entries(); en.hasMoreElements();) {
                ZipEntry entry = en.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".apk")) {
                    continue;
                }
                File f = extractFromZip(zip, entry);

                if ("system/app/Email.apk".equals(entry.getName()) && "2.2_HuaweiU8150".equals(system)) {
                    continue;
                }
                if ("system/app/AIME.apk".equals(entry.getName()) && "LG_P500_Optimus_One".equals(system)) {
                    continue;
                }
                if ("system/app/Calendar.apk".equals(entry.getName()) && "LG_P500_Optimus_One".equals(system)) {
                    continue;
                }
                if ("system/framework/lge-res.apk".equals(entry.getName()) && "LG_P500_Optimus_One".equals(system)) {
                    continue;
                }

                // if ("system/app/GoogleServicesFramework.apk".equals(entry.getName())) {
                // Спраўджваецца подпіс, і не дазваляе дадаць рахунак Google калі подпіс несапраўдны
                // continue;
                // }

                System.out.println(entry.getName());
                extractFile(system, entry.getName(), f);
                compileFile(system, entry.getName(), f, new File(OUT_DIR + system + "/" + entry.getName()));
                FileUtils.deleteDirectory(new File(tempDir, "extract"));
                new File(tempDir, "compiled.apk").delete();
                new File(tempDir, "resources.arsc").delete();
            }
            zip.close();
        }
    }

    protected static final Pattern RE_APK = Pattern.compile(".+/(.+?)\\.apk");

    protected static void compileFile(String system, String fn, File inApk, File outApk) throws Exception {
        File translatedDir = new File(TARGET_XML + system + "/" + fn);
        if (!translatedDir.exists()) {
            return;
        }

        File outDir = new File(tempDir, "extract");
        FileUtils.copyDirectory(translatedDir, outDir);

        File compiledApk = new File(tempDir, "compiled.apk");
        new Androlib().build(outDir, compiledApk, false, false);

        FileUtils.copyFile(inApk, outApk);

        String packMark = isResourcesPacked(inApk) ? "" : "0";

        byte[] originalResources = extractCompiledResources(inApk);
        byte[] translatedResources = extractCompiledResources(compiledApk);
        FileUtils.writeByteArrayToFile(new File(tempDir, "resources.arsc"), translatedResources);
        exec("jar uvf" + packMark + " " + outApk.getAbsolutePath() + " -C " + tempDir.getAbsolutePath()
                + " resources.arsc");

        Matcher m = RE_APK.matcher(fn);
        if (!m.matches()) {
            throw new Exception("Invalid file name: " + fn);
        }
        File outRes = new File(OUT_DIR + "resources/" + m.group(1) + "_" + sha1(originalResources) + "_"
                + sha1(translatedResources) + ".arsc.gz");
        outRes.getParentFile().mkdirs();
        GZIPOutputStream gz = new GZIPOutputStream(new FileOutputStream(outRes), translatedResources.length);
        try {
            gz.write(translatedResources);
        } finally {
            gz.close();
        }
    }

    protected static void exec(String e) throws Exception {
        int res = Runtime.getRuntime().exec(e).waitFor();
        if (res != 0) {
            throw new Exception("Error execute : " + e);
        }
    }

    protected static File extractFromZip(ZipFile zip, ZipEntry entry) throws Exception {
        extracted++;
        File outFile = new File(tempDir, "fromzip-" + extracted + ".apk");
        outFile.getParentFile().mkdirs();

        InputStream in = zip.getInputStream(entry);
        OutputStream out = new FileOutputStream(outFile);
        IOUtils.copy(in, out);
        in.close();
        out.close();
        return outFile;
    }

    protected static void extractFile(String system, String fn, File apk) throws Exception {
        File outDir = new File(tempDir, "extract");

        ApkDecoder decoder = new ApkDecoder();
        decoder.setDebugMode(true);
        decoder.setFrameworkTag(system);
        decoder.setDecodeSources(ApkDecoder.DECODE_SOURCES_NONE);
        decoder.setOutDir(outDir);
        decoder.setApkFile(apk);
        decoder.decode();

        File fStrings = new File(outDir, "res/values/strings.xml");
        if (fStrings.exists()) {
            FileUtils.copyFile(fStrings, new File(SOURCE_XML + system + "/" + fn
                    + "/res/values-be/strings.xml"));
        }
        File fArrays = new File(outDir, "res/values/arrays.xml");
        if (fArrays.exists()) {
            FileUtils.copyFile(fArrays,
                    new File(SOURCE_XML + system + "/" + fn + "/res/values-be/arrays.xml"));
        }
    }

    static byte[] extractCompiledResources(File apkFile) throws Exception {
        ZipFile resZip = new ZipFile(apkFile);
        try {
            InputStream in = resZip.getInputStream(resZip.getEntry("resources.arsc"));
            try {
                return IOUtils.toByteArray(in);
            } finally {
                in.close();
            }
        } finally {
            resZip.close();
        }
    }

    static boolean isResourcesPacked(File apkFile) throws Exception {
        ZipFile resZip = new ZipFile(apkFile);
        try {
            ZipEntry e = resZip.getEntry("resources.arsc");
            return e.getCompressedSize() != e.getSize();
        } finally {
            resZip.close();
        }
    }

    private static void setupLogging(boolean verbose) {
        Logger logger = Logger.getLogger("");
        Handler handler = new ConsoleHandler();
        logger.removeHandler(logger.getHandlers()[0]);
        logger.addHandler(handler);

        if (verbose) {
            handler.setLevel(Level.ALL);
            logger.setLevel(Level.ALL);
        } else {
            handler.setLevel(Level.WARNING);
            logger.setLevel(Level.WARNING);
            handler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    if (record.getMessage().equals("Debug mode not available.")) {
                        // on decode sources
                        return "";
                    }
                    return record.getLevel().toString().charAt(0) + ": " + record.getMessage()
                            + System.getProperty("line.separator");
                }
            });
        }
    }

    public static Map<String, File> listFiles(File dir) {
        int prefix = dir.getAbsolutePath().length() + 1;
        Map<String, File> result = new TreeMap<String, File>();
        for (File f : (Collection<File>) FileUtils.listFiles(dir, null, true)) {
            String fn = f.getAbsolutePath().substring(prefix).replace('\\', '/');
            result.put(fn, f);
        }
        return result;
    }

    protected static void cleanTemp() throws Exception {
        FileUtils.deleteDirectory(tempDir);
        tempDir.mkdirs();
    }

    static void removeUnusedLangs(File resDir) throws Exception {
        File[] res = resDir.listFiles();
        for (File d : res) {
            for (String p : d.getName().split("-")) {
                if (RE_RESLANG.matcher(p).matches()) {
                    if (!"be".equalsIgnoreCase(p) && !"ru".equalsIgnoreCase(p)) {
                        FileUtils.deleteDirectory(d);
                    }
                }
            }
        }
    }

    static String sha1(byte[] data) throws Exception {
        MessageDigest digester = MessageDigest.getInstance("SHA-1");
        digester.update(data);

        // convert digest to string
        byte[] digest = digester.digest();
        StringBuilder s = new StringBuilder(40);
        for (int b : digest) {
            String hex = Integer.toHexString(b);
            if (hex.length() == 1) {
                s.append('0');
            }
            if (hex.length() > 2) {
                hex = hex.substring(hex.length() - 2);
            }
            s.append(hex);
        }
        return s.toString();
    }
}
