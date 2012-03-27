import java.io.File;
import java.io.IOException;

import org.alex73.android.arsc.ManifestInfo;
import org.alex73.android.bel.Utils;
import org.alex73.android.common.FileInfo;
import org.alex73.android.common.zip.ApkUpdater;
import org.apache.commons.io.FileUtils;

public class PackResources {
    public static void main(String[] args) throws Exception {
        PackBinaryResources.projectPath = "out/";

        for (String s : args) {
            System.out.println(s);

            File f = new File(s);
            byte[] apk = FileUtils.readFileToByteArray(f);
            byte[] arsc = BuildAll.extractFile(apk, "resources.arsc");
            if (arsc == null) {
                throw new RuntimeException("There is no arsc in " + f.getAbsolutePath());
            }
            byte[] manifest = BuildAll.extractFile(apk, "AndroidManifest.xml");
            ManifestInfo mi = new ManifestInfo(manifest);

            byte[] outResources = PackBinaryResources.processARSC(arsc, "",
                    UnpackBinaryResources.createSuffix(mi));

            File apkOut = new File(f.getParent() + "/new/", f.getName());
            System.out.println("New apk saved into " + apkOut.getAbsolutePath());
            replaceResources(apk, apkOut, outResources);
        }
        System.out.println("done");
    }

    protected static void replaceResources(byte[] apk, File out, byte[] newResources) throws Exception {
        out.getParentFile().mkdirs();
        FileUtils.writeByteArrayToFile(out, apk);
        File o = new File("out/z");
        FileUtils.writeByteArrayToFile(o, apk);

        FileInfo fi = new FileInfo(null);
        fi.originID = Utils.sha1(apk);
        fi.translatedID = Utils.sha1(newResources);

        new ApkUpdater().replace(o, out, fi.store().getBytes("UTF-8"), newResources);
        if (o.exists()) {
            if (!o.delete()) {
                throw new IOException("Error delete " + o);
            }
        }
    }
}
