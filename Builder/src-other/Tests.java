import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.alex73.android.Assert;
import org.alex73.android.StAXDecoderReader;
import org.alex73.android.StyledString;
import org.alex73.android.arsc2.ResourceProcessor;
import org.alex73.android.arsc2.Segmenter;
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

        StringTable2.OPTIMIZE = true;
        testTranslations();
        testRecreate();

        String orig = "Test 1 asdjghjkasdhgjk hasd gjk.  Ajhkjashdjkgh jkhjkhjk.\n\n\nZzzzzz.";
        List<String> s = Segmenter.segment(orig);
        String dest = Segmenter.glue(s.toArray(new String[s.size()]));
        Assert.assertTrue("", orig.equals(dest));
    }

    public static void testStAXDecoderReader() {
        testStAXDecoderReader(" a", "a");
        testStAXDecoderReader(" a ", "a");
        testStAXDecoderReader("a ", "a");
        testStAXDecoderReader("\"zzz", "zzz");
        testStAXDecoderReader("List \"OK\"", "List OK");
        testStAXDecoderReader("List \\\"OK\\\"", "List \"OK\"");
        testStAXDecoderReader("a\\u0020", "a ");
        testStAXDecoderReader("a\\n", "a\n");
        testStAXDecoderReader("a\\n\\n", "a\n\n");
    }

    private static void testStAXDecoderReader(String src, String result) {
        StyledString str = new StyledString();
        str.tags = new StyledString.Tag[0];
        str.raw = src;
        str = StAXDecoderReader.postProcessString(str);
        Assert.assertTrue("Result: '" + str.raw + "', Expected: '" + result + "'", result.equals(str.raw));
    }

    public static void testTranslations() throws Exception {
        InputStream intr = new GZIPInputStream(new FileInputStream("../Installer/res/raw/translation.bin"));
        Translation tr = new Translation(intr);
        intr.close();
        Assert.assertTrue("", tr.defaults.size() > 1000);
        Assert.assertTrue("", tr.multiples.size() > 100);
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
        ResourceProcessor rs = new ResourceProcessor(rsReader);
        return rs.save();
    }

    public static byte[] recreatePackage(byte[] data, int packageIndex) {
        ChunkReader2 rsReader = new ChunkReader2(new ByteArrayInputStream(data));
        ResourceProcessor rs = new ResourceProcessor(rsReader);
        return rs.packages[packageIndex].write();
    }

    public static byte[] recreateStrings(byte[] data) {
        ChunkReader2 rsReader = new ChunkReader2(new ByteArrayInputStream(data));
        ResourceProcessor rs = new ResourceProcessor(rsReader);
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
