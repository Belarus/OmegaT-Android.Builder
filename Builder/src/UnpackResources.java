import java.io.File;

import org.alex73.android.arsc.ManifestInfo;
import org.alex73.android.bel.Utils;
import org.apache.commons.io.FileUtils;

public class UnpackResources {
    public static void main(String[] args) throws Exception {
        UnpackBinaryResources.projectPath = "out/";

        BuildAll.write = false;
        for (String s : args) {
            System.out.println(s);

            File f = new File(s);
            byte[] apk = FileUtils.readFileToByteArray(f);
            byte[] arsc = BuildAll.extractFile(apk, "resources.arsc");
            if (arsc != null) {
                byte[] manifest = BuildAll.extractFile(apk, "AndroidManifest.xml");
                ManifestInfo mi = new ManifestInfo(manifest);             
                UnpackBinaryResources.processARSC(arsc, "", mi.getVersion() + '-' + Utils.sha1(arsc));
            }
        }
        System.out.println("done");
    }
}
