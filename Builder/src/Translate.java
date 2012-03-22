import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.alex73.android.StyledString;
import org.alex73.android.arsc2.ResourceProcessor;
import org.alex73.android.arsc2.Translation;
import org.alex73.android.arsc2.reader.ChunkReader2;
import org.alex73.android.bel.FileInfo;
import org.alex73.android.bel.LocalStorage;

public class Translate {
    static TranslationDebug tr;

    public static void main(String[] args) throws Exception {
        tr = new TranslationDebug(new GZIPInputStream(new FileInputStream(
                "../Installer/res/raw/translation.bin")));
        for (String f : args) {
            process(new File(f));
        }
    }

    static void process(File f) throws Exception {
        ResourceProcessor rs;
        FileInfo fi = new FileInfo(f);
        new LocalStorage().getManifestInfo(fi);
        ZipFile zip = new ZipFile(fi.localFile);
        try {
            ZipEntry en = zip.getEntry("resources.arsc");

            InputStream in = zip.getInputStream(en);
            try {
                ChunkReader2 rsReader = new ChunkReader2(in);
                rs = new ResourceProcessor(rsReader);

            } finally {
                in.close();
            }
        } finally {
            zip.close();
        }

        rs.process(fi.packageName, tr);

        byte[] translatedResources = rs.save();

        // new LocalStorage().patchFile(fi.localFile, translatedResources);
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
