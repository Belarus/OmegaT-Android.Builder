import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.alex73.android.Assert;
import org.alex73.android.StAXDecoderReader2;
import org.alex73.android.StyledString;
import org.alex73.android.arsc2.ResourceProcessor;
import org.alex73.android.arsc2.StringTable2;
import org.alex73.android.arsc2.Translation;
import org.alex73.android.arsc2.reader.ChunkHeader2;
import org.alex73.android.arsc2.reader.ChunkReader2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class Tests {

    public static void main(String[] args) throws Exception {
        // testVar();
        testStAXDecoderReader();

        StringTable2.DESKTOP_MODE = true;
        testTranslations();
    }

    public static void testStAXDecoderReader() {
//        testStAXDecoderReader(" a", "a");
//        testStAXDecoderReader(" a ", "a");
//        testStAXDecoderReader("a ", "a");
//        testStAXDecoderReader("\"zzz", "zzz");
//        testStAXDecoderReader("List \"OK\"", "List OK");
//        testStAXDecoderReader("List \\\"OK\\\"", "List \"OK\"");
//        testStAXDecoderReader("a\\u0020", "a ");
//        testStAXDecoderReader("a\\n", "a\n");
//        testStAXDecoderReader("a\\n\\n", "a\n\n");

        StyledString s1 = new StyledString();
        StyledString s2 = new StyledString();
        s1.raw = " zz   tt ";
        s1.tags = new StyledString.Tag[1];
        s1.tags[0] = new StyledString.Tag();
        s1.tags[0].tagName = "b";
        s1.tags[0].start = 6;
        s1.tags[0].end = 6;
        s2.raw = "zz tt";
        s2.tags = new StyledString.Tag[1];
        s2.tags[0] = new StyledString.Tag();
        s2.tags[0].tagName = "b";
        s2.tags[0].start = 3;
        s2.tags[0].end = 3;
        s1.removeSpaces();
        Assert.assertTrue("", s1.equals(s2));
    }

    private static void testStAXDecoderReader(String src, String result) {
        StyledString str = new StyledString();
        str.tags = new StyledString.Tag[0];
        str.raw = src;
        str = StAXDecoderReader2.postProcessString(new StringBuilder(src), new ArrayList<StyledString.Tag>());
        Assert.assertTrue("Result: '" + str.raw + "', Expected: '" + result + "'", result.equals(str.raw));
    }

    public static void testTranslations() throws Exception {
        InputStream intr = new GZIPInputStream(new FileInputStream("../Installer/res/raw/translation.bin"));
        Translation tr = new Translation(intr);
        intr.close();
        Assert.assertTrue("", tr.defaults.size() > 1000);
    }

    public static void testRecreate() throws Exception {
        ZipFile zip = new ZipFile("f:/binaries/Android/Gmail.apk");
        ZipEntry en = zip.getEntry("resources.arsc");

        byte[] data = IOUtils.toByteArray(zip.getInputStream(en));

        readChunks(data);

        byte[] r = recreateStrings(data);
        if (!Arrays.equals(chunks[0], r)) {
            FileUtils.writeByteArrayToFile(new File("C:/Temp/b1"), chunks[0]);
            FileUtils.writeByteArrayToFile(new File("C:/Temp/b2"), r);
            Assert.fail("");
        }

        r = recreatePackage(data, 0);
        if (!Arrays.equals(chunks[1], r)) {
            FileUtils.writeByteArrayToFile(new File("C:/Temp/b1"), chunks[1]);
            FileUtils.writeByteArrayToFile(new File("C:/Temp/b2"), r);
            Assert.fail("");
        }

        r = recreateFull(data);
        if (!Arrays.equals(data, r)) {
            FileUtils.writeByteArrayToFile(new File("C:/Temp/b1"), data);
            FileUtils.writeByteArrayToFile(new File("C:/Temp/b2"), r);
            Assert.fail("");
        }
    }

    static byte[][] chunks;

    public static void readChunks(byte[] data) {
        ChunkReader2 rd = new ChunkReader2(new ByteArrayInputStream(data));
        Assert.assertTrue("Main chunk should be TABLE", rd.header.chunkType == ChunkHeader2.TYPE_TABLE
                && rd.header.chunkType2 == ChunkHeader2.TYPE2_TABLE);

        int packageCount = rd.readInt();

        chunks = new byte[packageCount + 1][];

        chunks[0] = rd.readChunk().getBytes();

        for (int i = 0; i < packageCount; i++) {
            chunks[i + 1] = rd.readChunk().getBytes();
        }
        Assert.assertNull("", rd.readChunk());
    }

    public static byte[] recreateFull(byte[] data) {
        ChunkReader2 rsReader = new ChunkReader2(new ByteArrayInputStream(data));
        ResourceProcessor rs = new ResourceProcessor(rsReader, null);
        return rs.save().getBytes();
    }

    public static byte[] recreatePackage(byte[] data, int packageIndex) {
        ChunkReader2 rsReader = new ChunkReader2(new ByteArrayInputStream(data));
        ResourceProcessor rs = new ResourceProcessor(rsReader, null);
        return rs.packages[packageIndex].write();
    }

    public static byte[] recreateStrings(byte[] data) {
        ChunkReader2 rsReader = new ChunkReader2(new ByteArrayInputStream(data));
        ResourceProcessor rs = new ResourceProcessor(rsReader, null);
        return rs.globalStringTable.write();
    }

    public static void testVar() {
        for (int i = 0; i < 32768; i++) {
            byte[] c = StringTable2.constructVarint(i);
            int[] cc = StringTable2.getVarint(c, 0);
            Assert.assertEquals("", i, cc[0]);
        }
    }
}
