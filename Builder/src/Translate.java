import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.alex73.android.StyledString;
import org.alex73.android.arsc2.ResourceProcessor;
import org.alex73.android.arsc2.Translation;
import org.alex73.android.arsc2.reader.ChunkReader2;
import org.alex73.android.common.FileInfo;
import org.alex73.android.common.zip.ApkUpdater;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class Translate {
    static TranslationDebug tr;

    public static void main(String[] args) throws Exception {
        tr = new TranslationDebug(new GZIPInputStream(new FileInputStream(
                "../Installer/res/raw/translation.bin")));
        for (String f : args) {
            if (f.endsWith(".apk")) {
                processApk(new File(f));
            } else if (f.endsWith(".zip")) {
                processZip(new File(f));
            }
        }
    }

    static void processZip(File f) throws Exception {
        TranslateSignatures sig = new TranslateSignatures();
        File temp = new File("temp.data");

        ZipFile zip = new ZipFile(f);
        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(f.getParent() + "/belarusian-"
                + f.getName()));
        for (Enumeration<? extends ZipEntry> it = zip.entries(); it.hasMoreElements();) {
            ZipEntry en = it.nextElement();
            if (en.getName().equals("META-INF/CERT.SF") || en.getName().equals("META-INF/CERT.RSA")
                    || en.getName().equals("META-INF/MANIFEST.MF")) {
                continue;
            }
            InputStream in = zip.getInputStream(en);
            byte[] data = IOUtils.toByteArray(in);
            in.close();
            if (en.getName().endsWith(".apk")) {
                FileUtils.writeByteArrayToFile(temp, data);
                File o = processApk(temp);
                if (o != null) {
                    data = FileUtils.readFileToByteArray(o);
                }
            } else if (en.getName().endsWith("/build.prop")) {
                FileUtils.writeByteArrayToFile(temp, data);
                processProp(temp);
                data = FileUtils.readFileToByteArray(temp);
            }
            zipOut.putNextEntry(new ZipEntry(en.getName()));
            IOUtils.write(data, zipOut);

            sig.putDigestToManifest(en, data);
        }

        sig.writeTo(zipOut);
        zip.close();
        zipOut.close();
    }

    static File processApk(File f) throws Exception {
        ResourceProcessor rs;
        FileInfo fi = new FileInfo(f);
        fi.readManifestInfo();
        if (tr.isPackageTranslated(fi.packageName)) {
            ZipFile zip = new ZipFile(fi.localFile);
            ZipEntry en = zip.getEntry("resources.arsc");

            InputStream in = zip.getInputStream(en);
            ChunkReader2 rsReader = new ChunkReader2(in);
            rs = new ResourceProcessor(rsReader);
            in.close();
            zip.close();

            rs.process(fi.packageName, tr);

            byte[] translatedResources = rs.save();

            File fo = new File(fi.localFile.getAbsolutePath() + ".out");
            fo.delete();
            new ApkUpdater().replace(fi.localFile, fo, translatedResources);
            return fo;
        } else {
            return null;
        }
    }

    static void processProp(File f) throws Exception {
        List<String> lines = FileUtils.readLines(f, "UTF-8");
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("ro.product.locale.language=")) {
                lines.set(i, "ro.product.locale.language=be");
            }
            if (lines.get(i).startsWith("ro.product.locale.region=")) {
                lines.set(i, "ro.product.locale.region=BY");
            }
        }
        FileUtils.writeLines(f, "UTF-8", lines, "\n");
    }

    static class TranslationDebug extends Translation {
        public TranslationDebug(InputStream in) throws IOException {
            super(in);
        }

        @Override
        public void notFoundInExact(String packageName, String id, StyledString source) {
            System.out.println("package: " + packageName);
            System.out.println("id     : " + id);
            try {
                source.dump(System.out);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            System.out.println("==============================");
        }
    }
}
