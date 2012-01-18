import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;

/**
 * @author Alex Buloichik (alex73mail@gmail.com
 */
public class ExtractResourcesFromApk {
    static List<String> notfound = new ArrayList<String>();
    static ZipOutputStream zipOut;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Exec: <source dir> <target dir>");
            System.exit(1);
        }

        File sourceDir = new File(args[0]);
        zipOut = new ZipOutputStream(new FileOutputStream(args[1]));

        Collection<File> files = FileUtils.listFiles(sourceDir, FileFileFilter.FILE,
                DirectoryFileFilter.DIRECTORY);
        for (File f : files) {
            String fn = relativeFileName(sourceDir, f);
            fn = fn.replaceAll("\\.apk$", ".arsc");
            System.out.println(f);
            extract(f, fn);
        }
        if (!notfound.isEmpty()) {
            for (String f : notfound) {
                System.out.println("Not found resources in files: ");
                System.out.println("    " + f);
            }
        }

        zipOut.close();
    }

    protected static String relativeFileName(File dir, File file) throws Exception {
        String d = dir.getAbsolutePath();
        String f = file.getAbsolutePath();
        if (!f.startsWith(d)) {
            throw new Exception("File '" + f + "' not in dir '" + d + "'");
        }
        String r = f.substring(d.length());
        if (r.startsWith("\\") || r.startsWith("/")) {
            r = r.substring(1);
        }
        return r;
    }

    protected static void extract(File apkFile, String fn) throws Exception {
        ZipFile zip = new ZipFile(apkFile);
        ZipEntry en = zip.getEntry("resources.arsc");
        if (en == null) {
            notfound.add(apkFile.getPath());
            return;
        }
        zipOut.putNextEntry(new ZipEntry(fn));
        InputStream in = zip.getInputStream(en);
        IOUtils.copy(in, zipOut);
        in.close();
        zip.close();
        zipOut.closeEntry();
    }
}
