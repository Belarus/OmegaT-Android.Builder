package org.alex73.android.bel;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.alex73.android.arsc.ManifestInfo;
import org.alex73.android.bel.zip.ApkUpdater;

public class LocalStorage {
    final static File FRAMEWORK_DIR = new File("/system/framework/");

    final static File APP_DIR = new File("/system/app/");

    final static File USER_DIR = new File("/data/app/");

    final static String DIR_STORE = "/sdcard/i18n-bel/";

    final static Pattern RE_APKNAME = Pattern.compile("(.+)\\.apk$");

    final static String RES_FILE = "resources.arsc";

    final static String MANIFEST_FILE = "AndroidManifest.xml";

    final static String PATCHED_INFO = "i18n.bel";

    final static String UTF8 = "UTF-8";

    protected boolean stopped;

    Properties mv1;

    public List<File> getLocalFiles() throws Exception {
        List<File> files = new ArrayList<File>();

        appendApks(APP_DIR, files, FILTER_APK);
        appendApks(FRAMEWORK_DIR, files, FILTER_APK);
        appendApks(USER_DIR, files, FILTER_APK);

        return files;
    }

    public List<File> getLocalFilesNew() throws Exception {
        List<File> files = new ArrayList<File>();

        appendApks(APP_DIR, files, FILTER_APK_NEW);
        appendApks(FRAMEWORK_DIR, files, FILTER_APK_NEW);
        appendApks(USER_DIR, files, FILTER_APK_NEW);

        return files;
    }

    private void appendApks(File dir, List<File> out, FileFilter filter) {
        File[] files = dir.listFiles(filter);
        if (files != null) {
            for (File f : files) {
                out.add(f);
            }
        }
    }

    static Pattern RE_VERSION1 = Pattern.compile("([0-9a-f]{40})_([0-9a-f]{40})");
    static Pattern RE_VERSION2 = Pattern
            .compile("v2: origApkSha1=([0-9a-f]{40}) transARSCSha1=([0-9a-f]{40})");

    byte[] buffer = new byte[1024];

    public void getManifestInfo(FileInfo fi) throws Exception {
        if (stopped) {
            return;
        }

        ZipFile apk = new ZipFile(fi.localFile);
        try {
            ZipEntry manifestEntry = apk.getEntry(MANIFEST_FILE);
            if (manifestEntry == null) {
                // manifest not found in this apk
                return;
            }

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
            fi.packageName = m.getPackageName();
            fi.versionName = m.getVersion();
        } finally {
            apk.close();
        }
    }

    public void getVersionInfo(FileInfo fi) throws Exception {
        ZipFile apk = new ZipFile(fi.localFile);
        try {
            ZipEntry resourceEntry = apk.getEntry(RES_FILE);
            if (resourceEntry == null) {
                // resource not found in this apk
                return;
            }

            if (stopped) {
                return;
            }

            fi.localSize = (int) fi.localFile.length();

            // already patched - read stored version
            ZipEntry patchedEntry = apk.getEntry(PATCHED_INFO);
            if (patchedEntry != null) {
                String v = readString(apk, patchedEntry);
                Matcher m;
                if ((m = RE_VERSION1.matcher(v)).matches()) {
                    if (mv1 == null) {
                        mv1 = new Properties();
                        InputStream inv1 = this.getClass().getResourceAsStream("v1.properties");
                        try {
                            mv1.load(inv1);
                        } finally {
                            inv1.close();
                        }
                    }
                    fi.originID = mv1.getProperty(m.group(1) + '_' + fi.localFile.getName());
                    fi.translatedID = m.group(2);
                } else if ((m = RE_VERSION2.matcher(v)).matches()) {
                    fi.originID = m.group(1);
                    fi.translatedID = m.group(2);
                }
            }
            if (fi.originID == null) {
                fi.originID = Utils.sha1(fi.localFile);
                fi.translatedID = null;
            }
        } finally {
            apk.close();
        }
    }

    public byte[] getResources(File f) throws Exception {
        ZipFile apk = new ZipFile(f);
        try {
            ZipEntry resourceEntry = apk.getEntry(RES_FILE);
            if (resourceEntry == null) {
                // resource not found in this apk
                return null;
            }

            if (stopped) {
                return null;
            }

            InputStream in = apk.getInputStream(resourceEntry);
            try {
                return Utils.read(in);
            } finally {
                in.close();
            }
        } finally {
            apk.close();
        }
    }

    protected static final Pattern RE_NAME = Pattern.compile(".+_([A-Za-z0-9\\-]+)\\.arsc(\\.gz)?");

    public String getVersion(String name) {
        Matcher m = RE_NAME.matcher(name);
        if (!m.matches()) {
            throw new RuntimeException("Unknown version for " + name);
        }
        return m.group(1);
    }

    private String readString(ZipFile zip, ZipEntry entry) throws Exception {
        StringWriter wr = new StringWriter(256);
        Reader rd = new InputStreamReader(zip.getInputStream(entry), UTF8);
        try {
            int c;
            char[] buffer = new char[256];
            while ((c = rd.read(buffer)) > 0) {
                wr.write(buffer, 0, c);
            }
        } finally {
            rd.close();
        }

        return wr.toString();
    }

    public boolean existFile(String filename) throws Exception {
        File outFile = new File(DIR_STORE + filename);

        return outFile.exists();
    }

    /*
     * public void storeFile(String filename, byte[] data) throws Exception { new File(DIR_STORE).mkdirs();
     * 
     * File outFile = new File(DIR_STORE + filename); File newFile = new File(DIR_STORE + filename + ".new");
     * OutputStream out = new FileOutputStream(newFile); try { out.write(data); } finally { out.close(); }
     * outFile.delete(); newFile.renameTo(outFile); }
     */

    public File storeFileBegin(String filename) throws Exception {
        new File(DIR_STORE).mkdirs();

        File newFile = new File(DIR_STORE + filename + ".new");

        return newFile;
    }

    public void storeFileEnd(String filename) throws Exception {
        File outFile = new File(DIR_STORE + filename);
        File newFile = new File(DIR_STORE + filename + ".new");
        newFile.setLastModified(outFile.lastModified());
        outFile.delete();
        newFile.renameTo(outFile);
    }

    public byte[] loadFile(String filename) throws Exception {
        byte[] result;

        InputStream in = new FileInputStream(DIR_STORE + filename);
        try {
            result = Utils.read(in);
        } finally {
            in.close();
        }
        return result;
    }

    public void patchFile(File f, FileInfo version, byte[] translatedResources) throws Exception {
        // new ApkUpdater().append(f, version.store().getBytes(UTF8), translatedResources);
        File fo = new File(f.getAbsolutePath() + ".new");
        createReadableFile(fo);
        new ApkUpdater().replace(f, fo, version.store().getBytes(UTF8), translatedResources);

        fo.setLastModified(f.lastModified());
        if (!f.delete()) {
            throw new Exception("Error delete");
        }
        if (!fo.renameTo(f)) {
            throw new Exception("Error renaming");
        }
    }

    public void patchFile(File f, byte[] translatedResources) throws Exception {
        File fo = new File(f.getAbsolutePath() + ".new");
        createReadableFile(fo);
        new ApkUpdater().replace(f, fo, new byte[0], translatedResources);

        fo.setLastModified(f.lastModified());
        if (!f.delete()) {
            throw new Exception("Error delete");
        }
        if (!fo.renameTo(f)) {
            throw new Exception("Error renaming");
        }
    }

    protected void backupApk(File f) throws Exception {
        File outFile = new File(DIR_STORE + "/backup/" + f.getName());
        if (outFile.exists()) {
            return;
        }
        outFile.getParentFile().mkdirs();

        File outFileNew = new File(DIR_STORE + "/backup/new-" + f.getName());
        FileOutputStream out = new FileOutputStream(outFileNew);
        try {
            FileInputStream in = new FileInputStream(f);
            try {
                Utils.copy(in, out);
            } finally {
                in.close();
            }
        } finally {
            out.close();
        }
        outFileNew.renameTo(outFile);
    }

    /**
     * Check if 'su' can be executed, i.e. if we have root priviledges.
     */
    public boolean checkSu() {
        try {
            new ExecProcess("su").exec("ls");
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public void remountSystem(final boolean allowWrite) throws Exception {
        final StringBuilder systemDevice = new StringBuilder();

        BufferedReader rd = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/mounts"),
                "UTF-8"), 8192);
        String s;
        while ((s = rd.readLine()) != null) {
            String[] a = s.split("\\s+");
            if (a[1].equals("/system")) {
                systemDevice.append(a[0]);
            }
        }

        if (systemDevice.length() == 0) {
            throw new Exception("/system partition not found for remount");
        }

        String sucmd = "";
        if (allowWrite) {
            sucmd += "mount -o remount,rw " + systemDevice + " /system\n";
            sucmd += "chmod 777 /system/app/\n";
            sucmd += "chmod 777 /system/framework/\n";
            sucmd += "chmod 777 /data /data/app/\n";
        } else {
            sucmd += "chown root /system/app/*.apk\n";
            sucmd += "chmod 755 /system/app\n";
            sucmd += "chown root /system/framework/*.apk\n";
            sucmd += "chmod 755 /system/framework\n";
            sucmd += "mount -o remount,ro " + systemDevice + " /system\n";
            sucmd += "chmod 771 /data /data/app/\n";
            sucmd += "chmod 644 /data/app/*.apk\n";
        }
        int tries = 0;
        while (true) {
            try {
                new ExecProcess("su").exec(sucmd);
                break;
            } catch (Exception ex) {
                if (allowWrite) {
                    throw new Exception("Error execute su: " + ex.getMessage());
                } else {
                    tries++;
                    if (tries < 10) {
                        Thread.sleep(2000);
                        continue;
                    }
                    throw new Exception("Error execute su: " + ex.getMessage());
                }
            }
        }
    }

    protected void createReadableFile(File f) throws Exception {
        FileOutputStream o = new FileOutputStream(f);
        o.close();

        try {
            new ExecProcess("chmod", "644", f.getAbsolutePath()).exec();
        } catch (Exception ex) {
            throw new Exception("Error execute chmod: " + ex.getMessage());
        }
    }

    static FileFilter FILTER_APK = new FileFilter() {
        public boolean accept(File pathname) {
            return pathname.isFile() && pathname.getName().endsWith(".apk");
        }
    };
    static FileFilter FILTER_APK_NEW = new FileFilter() {
        public boolean accept(File pathname) {
            return pathname.isFile() && pathname.getName().endsWith(".apk.new");
        }
    };
}
