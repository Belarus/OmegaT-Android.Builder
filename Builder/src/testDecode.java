import java.io.File;

import org.apache.commons.io.FileUtils;

import brut.androlib.ApkDecoder;

public class testDecode {

    /**
     * Распакоўваем apk праз звычайны ApkDecoder.
     */
    public static void main(String[] args) throws Exception {

        File outDir = new File("C:/temp/a/apidemos/");
        FileUtils.deleteDirectory(outDir);
        ApkDecoder decoder = new ApkDecoder();
        decoder.setDebugMode(true);
        decoder.setFrameworkTag("2.2_HuaweiU8150");
        decoder.setDecodeSources(ApkDecoder.DECODE_SOURCES_NONE);
        decoder.setOutDir(outDir);
        decoder.setApkFile(new File("c:\\Temp\\a\\apidemos.apk"));
        decoder.decode();
    }

}
