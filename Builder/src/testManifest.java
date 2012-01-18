import java.io.File;

import org.alex73.android.arsc.ManifestInfo;
import org.apache.commons.io.FileUtils;

public class testManifest {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        byte[] manifest = FileUtils.readFileToByteArray(new File("c:\\Temp\\a\\AndroidManifest.xml"));

        UnpackResources.ManifestInfo m = UnpackResources.extractVersion(manifest);
        System.out.println("=" + m);

        m = UnpackResources.extractVersion(manifest);
        System.out.println("=" + m);

        // CHUNK_AXML_FILE = 0x00080003,
        // size
        ManifestInfo ma = new ManifestInfo(manifest);
        System.out.println("package=" + ma.getPackageName() + " ver=" + ma.getVersion());
    }
}
