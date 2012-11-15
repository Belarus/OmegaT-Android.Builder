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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
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

    Properties mv1;

    private String systemMountDevice, systemMountOptions;

    public LocalStorage() throws Exception {
        BufferedReader rd = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/mounts"),
                "UTF-8"));
        String s;
        while ((s = rd.readLine()) != null) {
            String[] a = s.split("\\s+");
            if (a[1].equals("/system")) {
                systemMountDevice = a[0];
                systemMountOptions = a[3];
            }
        }
        rd.close();

        if (systemMountDevice == null) {
            throw new Exception("/system partition not found for remount");
        }

        String[] opts = systemMountOptions.split(",");
        systemMountOptions = "";
        boolean found = false;
        for (String o : opts) {
            if (o.equals("ro") || o.equals("rw")) {
                found = true;
            } else {
                systemMountOptions += ',' + o;
            }
        }

        if (!found) {
            throw new Exception("/system partition mounted with unknow noptions");
        }
    }

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

    public List<FilePerm> getFilesPermissions(List<FileInfo> files) throws Exception {
        List<String> filesList = new ArrayList<String>();
        for (FileInfo fi : files) {
            filesList.add(fi.localFile.getPath());
        }
        Collections.sort(filesList);

        StringBuilder cmd = new StringBuilder("ls -l ");
        for (String f : filesList) {
            cmd.append('"').append(f).append('"').append(' ');
        }
        final List<String> ls = new ArrayList<String>();
        new ExecProcess("su") {
            @Override
            protected void processOutputLine(String line) {
                ls.add(line);
            }
        }.exec(cmd.toString());

        return calcPermissions(filesList, ls, false);
    }

    public List<FilePerm> getDirsPermissions(List<FileInfo> files) throws Exception {
        List<String> filesList = new ArrayList<String>();
        for (FileInfo fi : files) {
            String dir = fi.localFile.getParent();
            if (!filesList.contains(dir)) {
                filesList.add(dir);
            }
        }
        Collections.sort(filesList);

        StringBuilder cmd = new StringBuilder("ls -l -d ");
        for (String f : filesList) {
            cmd.append('"').append(f).append('"').append(' ');
        }
        final List<String> ls = new ArrayList<String>();
        new ExecProcess("su") {
            @Override
            protected void processOutputLine(String line) {
                ls.add(line);
            }
        }.exec(cmd.toString());

        return calcPermissions(filesList, ls, true);
    }

    static final Pattern RE_LS_FILE = Pattern
            .compile("\\-([rwx\\-]{9})\\s+([a-z0-9_]+)\\s+([a-z0-9_]+)\\s+([0-9]+)\\s+([0-9]{4}\\-[0-9]{2}\\-[0-9]{2})\\s+([0-9]{2}\\:[0-9]{2})\\s+(\\S+)");
    static final Pattern RE_LS_DIR = Pattern
            .compile("d([rwx\\-]{9})\\s+([a-z0-9_]+)\\s+([a-z0-9_]+)\\s+([0-9]{4}\\-[0-9]{2}\\-[0-9]{2})\\s+([0-9]{2}\\:[0-9]{2})\\s+(\\S+)");

    private List<FilePerm> calcPermissions(List<String> filesList, List<String> lsout, boolean isDirs)
            throws Exception {
        if (filesList.size() != lsout.size()) {
            throw new Exception("Wrong ls size");
        }

        List<FilePerm> result = new ArrayList<FilePerm>();
        for (int i = 0; i < filesList.size(); i++) {
            FilePerm p = new FilePerm();
            p.file = filesList.get(i);
            Matcher m = (isDirs ? RE_LS_DIR : RE_LS_FILE).matcher(lsout.get(i));
            if (!m.matches()) {
                throw new Exception("Wrong ls line: " + lsout.get(i));
            }
            p.owner = m.group(2);
            p.group = m.group(3);
            p.perm = calcPermByLs(m.group(1));
            if (isDirs) {
                p.fileName = m.group(6);
            } else {
                p.fileSize = Integer.parseInt(m.group(4));
                p.fileName = m.group(7);
            }
            int pp = p.file.lastIndexOf('/');
            if (!p.fileName.equals(p.file.substring(pp + 1))) {
                throw new Exception("Wrong file name in ls line: " + lsout.get(i));
            }
            result.add(p);
        }
        return result;
    }

    static final Map<String, Character> ls2p;
    static {
        ls2p = new HashMap<String, Character>();
        ls2p.put("---", '0');
        ls2p.put("--x", '1');
        ls2p.put("-w-", '2');
        ls2p.put("-wx", '3');
        ls2p.put("r--", '4');
        ls2p.put("r-x", '5');
        ls2p.put("rw-", '6');
        ls2p.put("rwx", '7');
    }

    private String calcPermByLs(String ls) throws Exception {
        if (ls.length() != 9) {
            throw new Exception("Wrong ls perm: " + ls);
        }
        String result = "";
        Character r;
        for (int i = 0; i < 9; i += 3) {
            r = ls2p.get(ls.substring(i, i + 3));
            if (r == null) {
                throw new Exception("Wrong ls perm: " + ls);
            }
            result += r;
        }
        return result;
    }

    public void backupList() throws Exception {
        final List<String> output = new ArrayList<String>();

        output.add("## mount");
        new ExecProcess("mount") {
            @Override
            protected void processOutputLine(String line) {
                output.add(line);
            }
        }.exec();

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
        outFile.getParentFile().mkdirs();
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

    public void remountRW(List<LocalStorage.FilePerm> origDirsPerms,
            List<LocalStorage.FilePerm> origFilesPerms) throws Exception {
        String sucmd = "";
        sucmd += "mount -o remount,rw" + systemMountOptions + " " + systemMountDevice + " /system\n";
        for (FilePerm p : origDirsPerms) {
            sucmd += "chmod 777 '" + p.file + "'\n";
        }

        try {
            new ExecProcess("su").exec(sucmd);
        } catch (Exception ex) {
            throw new Exception("Error execute su: " + ex.getMessage());
        }
    }

    public void remountRO(List<LocalStorage.FilePerm> origDirsPerms,
            List<LocalStorage.FilePerm> origFilesPerms) throws Exception {
        String sucmd = "";

        for (FilePerm p : origDirsPerms) {
            sucmd += "chmod " + p.perm + " '" + p.file + "'\n";
            sucmd += "chown " + p.owner + "." + p.group + " '" + p.file + "'\n";
        }
        for (FilePerm p : origFilesPerms) {
            sucmd += "chmod " + p.perm + " '" + p.file + "'\n";
            sucmd += "chown " + p.owner + "." + p.group + " '" + p.file + "'\n";
        }
        sucmd += "mount -o remount,ro" + systemMountOptions + " " + systemMountDevice + " /system\n";

        exec10su(sucmd);
    }

    private void exec10su(String cmd) throws Exception {
        int tries = 0;
        while (true) {
            try {
                new ExecProcess("su").exec(cmd);
                break;
            } catch (Exception ex) {
                tries++;
                if (tries < 10) {
                    Thread.sleep(2000);
                    continue;
                }
                throw new Exception("Error execute su: " + ex.getMessage());
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

    public static class FilePerm {
        public String file;
        public long perm;
        public long owner, group;
    }
}
