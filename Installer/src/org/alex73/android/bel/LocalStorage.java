package org.alex73.android.bel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.alex73.android.common.FileInfo;
import org.alex73.android.common.zip.ApkUpdater;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.os.Environment;
import android.os.StatFs;
import android.util.Xml;

public class LocalStorage {
    final static File FRAMEWORK_DIR = new File("/system/framework/");

    final static File APP_DIR = new File("/system/app/");

    final static File USER_DIR = new File("/data/app/");

    final static String BACKUP_PARTITION = Environment.getExternalStorageDirectory().getPath();
    final static String BACKUP_DIR = BACKUP_PARTITION + "/i18n-bel/backup/";

    final static Pattern RE_APKNAME = Pattern.compile("(.+)\\.apk$");

    final static String RES_FILE = "resources.arsc";

    final static String PATCHED_INFO = "i18n.bel";

    final static String UTF8 = "UTF-8";

    final static String DATETIME_PATTERN = "-yyyyMMdd-HHmmss";

    protected boolean stopped;

    Properties mv1;

    public List<FileInfo> getLocalFiles() throws Exception {
        final List<FileInfo> files = new ArrayList<FileInfo>();
        InputStream in = new FileInputStream(new File("/data/system/packages.xml"));
        try {
            Xml.parse(in, Xml.Encoding.UTF_8, new DefaultHandler() {
                public void startElement(String uri, String localName, String qName, Attributes attributes)
                        throws SAXException {
                    if ("package".equals(localName)) {
                        String name = attributes.getValue("name");
                        String codePath = attributes.getValue("codePath");
                        if (name != null && codePath != null) {
                            FileInfo fi = new FileInfo(new File(codePath));
                            fi.packageName = name;
                            files.add(fi);
                        }
                    }
                }

            });
        } finally {
            in.close();
        }

        return files;
    }

    public List<File> getLocalFilesNew(List<FileInfo> files) throws Exception {
        List<File> result = new ArrayList<File>();

        Set<File> dirs = new TreeSet<File>();
        dirs.add(APP_DIR);
        dirs.add(FRAMEWORK_DIR);
        dirs.add(USER_DIR);
        for (FileInfo fi : files) {
            dirs.add(fi.localFile.getParentFile());
        }
        for (File d : dirs) {
            appendApks(d, result, FILTER_APK_NEW);
        }

        return result;
    }

    private void appendApks(File dir, List<File> out, FileFilter filter) {
        File[] files = dir.listFiles(filter);
        if (files != null) {
            for (File f : files) {
                out.add(f);
            }
        }
    }

    public void patchFile(File f, byte[] translatedResources) throws Exception {
        File fo = new File(f.getAbsolutePath() + ".new");
        createReadableFile(fo);
        new ApkUpdater().replace(f, fo, translatedResources);

        fo.setLastModified(f.lastModified());
        if (!fo.renameTo(f)) {
            throw new Exception("Error renaming");
        }
    }

    public StatFs getFreeSpaceForBackups() throws Exception {
        return new StatFs(BACKUP_PARTITION);
    }

    public void backupList() throws Exception {
        final List<String> output = new ArrayList<String>();
        output.add("## ls -l " + APP_DIR.getPath());
        new ExecProcess("su") {
            @Override
            protected void processOutputLine(String line) {
                output.add(line);
            }
        }.exec("ls -l " + APP_DIR.getPath());

        output.add("## ls -l " + USER_DIR.getPath());
        new ExecProcess("su") {
            @Override
            protected void processOutputLine(String line) {
                output.add(line);
            }
        }.exec("ls -l " + USER_DIR.getPath());

        File outFile = new File(BACKUP_DIR + "ls" + new SimpleDateFormat(DATETIME_PATTERN).format(new Date())
                + ".txt");
        Utils.writeLines(outFile, output);
    }

    public void backupApk(File f) throws Exception {
        // File outFile = new File(BACKUP_DIR + f.getName() + new
        // SimpleDateFormat(DATETIME_PATTERN).format(new Date())
        // + ".bak");
        File outFile = new File(BACKUP_DIR + f.getName());
        outFile.getParentFile().mkdirs();

        FileOutputStream out = new FileOutputStream(outFile);
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
    }

    public boolean isFileTranslated(File f) throws Exception {
        ZipFile zip = new ZipFile(f);
        try {
            ZipEntry en = zip.getEntry(ApkUpdater.MARK_NAME);
            return en != null;
        } finally {
            zip.close();
        }
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
        rd.close();

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
