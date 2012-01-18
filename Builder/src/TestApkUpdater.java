import java.io.File;

import org.alex73.android.bel.zip.ApkUpdater;
import org.apache.commons.io.FileUtils;

public class TestApkUpdater {
    public static void main(String[] args) throws Exception {
        byte[] mark = "123".getBytes();
        byte[] newResources = FileUtils.readFileToByteArray(new File("c:/temp/a/resources.arsc"));
        new File("c:/temp/a/a-out.apk").delete();
        new ApkUpdater().replace(new File("c:/temp/a/a.apk"), new File("c:/temp/a/a-out.apk"), mark,
                newResources);
    }
}
