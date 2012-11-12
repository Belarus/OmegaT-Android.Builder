package org.alex73.android.arsc2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alex73.android.Assert;
import org.alex73.android.StyledString;
import org.alex73.android.arsc2.reader.ChunkHeader2;
import org.alex73.android.arsc2.reader.ChunkMapper;
import org.alex73.android.arsc2.writer.ChunkWriter2;

public class StringTable2 {
    private static final int UTF8_FLAG = 0x00000100;

    private static final String UTF_16LE = "UTF-16LE";
    private static final String UTF_8 = "UTF-8";

    private static final CharsetDecoder UTF16LE_DECODER = Charset.forName(UTF_16LE).newDecoder();
    private static final CharsetDecoder UTF8_DECODER = Charset.forName(UTF_8).newDecoder();

    private int flags;

    private List<StringInstance> strings;

    public static boolean DESKTOP_MODE = false;
    private boolean mergeDuplicates = false;
    private Map<ByteArray, Integer> duplicatesIndex;

    public class StringInstance {
        public byte[] text;
        public int[] tags;

        public String getRawString() {
            int offset = 0;
            int length;
            if (isUTF8()) {
                offset += getVarint(text, offset)[1];
                int[] varint = getVarint(text, offset);
                offset += varint[1];
                length = varint[0];
                return decodeString(text, offset, length);
            } else {
                length = getShort(text, offset) * 2;
                offset += 2;
                return decodeString(text, offset, length);
            }
        }

        public Tag[] getTags() {
            Tag[] st;
            if (tags != null) {
                st = new Tag[tags.length / 3];
                for (int j = 0; j < st.length; j++) {
                    st[j] = new Tag(j * 3);
                }
            } else {
                st = null;
            }

            return st;
        }

        public StyledString getStyledString() {
            StyledString orig = new StyledString();
            orig.raw = new LightString(getRawString());
            Tag[] tags = getTags();
            if (tags != null) {
                orig.tags = new StyledString.Tag[tags.length];
                for (int i = 0; i < orig.tags.length; i++) {
                    orig.tags[i] = new StyledString.Tag();
                    orig.tags[i].start = tags[i].start();
                    orig.tags[i].end = tags[i].end();
                    orig.tags[i].tagName = new LightString(tags[i].tagName());
                }
                orig.sortTags();
            } else {
                orig.tags = StyledString.NO_TAGS;
            }
            return orig;
        }

        public class Tag {
            public int tagIndex() {
                return tags[offset];
            }

            public void setTagIndex(int v) {
                tags[offset] = v;
            }

            public int start() {
                return tags[offset + 1];
            }

            public int end() {
                return tags[offset + 2];
            }

            public String tagName() {
                return strings.get(tagIndex()).getRawString();
            }

            private final int offset;

            Tag(int offset) {
                this.offset = offset;
            }
        }
    }

    public int getFlags() {
        return flags;
    }

    public void read(ChunkMapper rd) {
        Assert.assertTrue("StringTable chunk", rd.header.chunkType == ChunkHeader2.TYPE_STRINGS
                && rd.header.chunkType2 == ChunkHeader2.TYPE2_STRINGS);

        int stringCount = rd.readInt();
        int styleOffsetCount = rd.readInt();
        flags = rd.readInt();
        int stringsOffset = rd.readInt();
        int stylesOffset = rd.readInt();

        int[] read_stringOffsets = rd.readIntArray(stringCount);
        int[] read_styleOffsets = rd.readIntArray(styleOffsetCount);

        int size = ((stylesOffset == 0) ? rd.header.chunkSize : stylesOffset) - stringsOffset;
        Assert.assertTrue("String data size is not multiple of 4 (" + size + ").", (size % 4) == 0);

        byte[] read_strings = new byte[size];
        rd.readFully(read_strings);

        strings = new ArrayList<StringInstance>(stringCount);
        for (int i = 0; i < stringCount; i++) {
            strings.add(new StringInstance());
            strings.get(i).text = getStringBytes(read_stringOffsets[i], read_strings);
        }
        read_strings = null;

        if (stylesOffset != 0) {
            int size2 = (rd.header.chunkSize - stylesOffset);
            Assert.assertTrue("Style data size is not multiple of 4 (" + size + ").", (size % 4) == 0);
            int[] read_styles = rd.readIntArray(size2 / 4);
            for (int i = 0; i < styleOffsetCount; i++) {
                strings.get(i).tags = getStyleInfo(read_styleOffsets[i], read_styles);
            }
        }

        if (DESKTOP_MODE) {
            mergeDuplicates = findDuplicates(read_stringOffsets);
            duplicatesIndex = new HashMap<StringTable2.ByteArray, Integer>();
        }
    }

    private boolean findDuplicates(int[] read_stringOffsets) {
        Set<Integer> offsets = new HashSet<Integer>();
        for (int i = 0; i < read_stringOffsets.length; i++) {
            int offset = read_stringOffsets[i];
            if (offsets.contains(offset)) {
                return true;
            }
            offsets.add(offset);
        }
        return false;
    }

    public int readCount(ChunkMapper rd) {
        Assert.assertTrue("StringTable chunk", rd.header.chunkType == ChunkHeader2.TYPE_STRINGS
                && rd.header.chunkType2 == ChunkHeader2.TYPE2_STRINGS);

        int stringCount = rd.readInt();
        return stringCount;
    }

    public int addString(StyledString str) {
        StringInstance instance = new StringInstance();
        instance.text = putStringBytes(str.raw);
        strings.add(instance);
        return strings.size() - 1;
    }

    public int getStringContentIndex(byte[] data) {
        Integer index = duplicatesIndex.get(new ByteArray(data));
        return index == null ? -1 : index;
    }

    public void setStringContentIndex(byte[] data, int index) {
        duplicatesIndex.put(new ByteArray(data), index);
    }

    public void clearStringContentIndex() {
        duplicatesIndex.clear();
    }

    public byte[] write() {
        ChunkWriter2 wr = new ChunkWriter2(ChunkHeader2.TYPE_STRINGS, ChunkHeader2.TYPE2_STRINGS);

        if (mergeDuplicates) {
            clearStringContentIndex();
        }
        // calc strings offsets
        int[] write_stringOffsets = new int[strings.size()];
        int off = 0;
        for (int i = 0; i < strings.size(); i++) {
            if (mergeDuplicates) {
                int wrIndex = getStringContentIndex(strings.get(i).text);
                if (wrIndex >= 0) {
                    // duplicate
                    write_stringOffsets[i] = write_stringOffsets[wrIndex];
                    continue;
                } else {
                    setStringContentIndex(strings.get(i).text, i);
                }
            }
            write_stringOffsets[i] = off;
            off += strings.get(i).text.length;
            if (isUTF8()) {
                off++;
            } else {
                off += 2;
            }
        }
        off = (off + 3) / 4;// align

        // which style is last ?
        int lastStyle = -1;
        for (int i = strings.size() - 1; i >= 0; i--) {
            if (strings.get(i).tags != null) {
                lastStyle = i;
                break;
            }
        }
        // calculate styles offsets
        int[] write_styleOffsets = new int[lastStyle + 1];
        off = 0;
        for (int i = 0; i <= lastStyle; i++) {
            write_styleOffsets[i] = off;
            int[] tags = strings.get(i).tags;
            if (tags != null) {
                off += tags.length * 4;
            }
            off += 4;
        }

        // write header
        wr.writeInt(write_stringOffsets.length);
        wr.writeInt(write_styleOffsets.length);
        wr.writeInt(flags);
        ChunkWriter2.LaterInt laterStringsOffset = wr.new LaterInt();
        ChunkWriter2.LaterInt laterStylesOffset = wr.new LaterInt();

        wr.writeIntArray(write_stringOffsets);
        wr.writeIntArray(write_styleOffsets);

        if (mergeDuplicates) {
            clearStringContentIndex();
        }
        // write strings
        laterStringsOffset.setValue(wr.pos());
        for (int i = 0; i < write_stringOffsets.length; i++) {
            if (mergeDuplicates) {
                int wrIndex = getStringContentIndex(strings.get(i).text);
                if (wrIndex >= 0) {
                    // duplicate
                    continue;
                } else {
                    setStringContentIndex(strings.get(i).text, i);
                }
            }
            wr.write(strings.get(i).text);
            if (isUTF8()) {
                wr.write(0);
            } else {
                wr.writeShort((short) 0);
            }
        }
        wr.align(4, 0);

        if (lastStyle >= 0) {
            // write styles
            laterStylesOffset.setValue(wr.pos());
            for (int i = 0; i < write_styleOffsets.length; i++) {
                int[] tags = strings.get(i).tags;
                if (tags != null) {
                    for (int j = 0; j < tags.length; j++) {
                        wr.writeInt(tags[j]);
                    }
                }
                wr.writeInt(-1);
            }
            wr.writeInt(-1);
            wr.writeInt(-1);
        } else {
            laterStylesOffset.setValue(0);
        }

        wr.close();
        return wr.getBytes();
    }

    public List<StringInstance> getStrings() {
        return strings;
    }

    public int getStringCount() {
        return strings.size();
    }

    byte[] getStringBytes(int startOffset, byte[] read_strings) {
        int offset = startOffset;

        int length;
        if (isUTF8()) {
            offset += getVarint(read_strings, offset)[1];
            int[] varint = getVarint(read_strings, offset);
            offset += varint[1];
            length = varint[0];
        } else {
            length = getShort(read_strings, offset) * 2;
            offset += 2;
        }

        byte[] result = new byte[offset + length - startOffset];
        System.arraycopy(read_strings, startOffset, result, 0, result.length);
        return result;
    }

    private byte[] putStringBytes(LightString s) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            if (isUTF8()) {
                // UTF-8: right, two length, with zero ended
                byte[] sbytes = s.toString().getBytes(UTF_8);
                out.write(constructVarint(s.length()));
                out.write(constructVarint(sbytes.length));
                out.write(sbytes);
            } else {
                // UTF-16
                writeShort((short) s.length(), out);
                out.write(s.toString().getBytes(UTF_16LE));
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return out.toByteArray();
    }

    /**
     * Returns style information - array of int triplets, where in each triplet:
     * 
     * * first int is index of tag name ('b','i', etc.)
     * 
     * * second int is tag start index in string
     * 
     * * third int is tag end index in string
     */
    int[] getStyleInfo(int startOffset, int[] read_styles) {
        int offset = startOffset / 4;

        int count = 0;
        for (int i = offset; i < read_styles.length; i++) {
            if (read_styles[i] == -1) { // ланцужок з offset да '-1'
                break;
            }
            count += 1;
        }
        if (count == 0) {
            return null;
        }
        Assert.assertEquals("Styles count wrong", 0, count % 3);

        int[] result = new int[count];
        System.arraycopy(read_styles, offset, result, 0, result.length);
        return result;
    }

    private boolean isUTF8() {
        return (flags & UTF8_FLAG) != 0;
    }

    private static final int getShort(byte[] array, int offset) {
        return (array[offset + 1] & 0xff) << 8 | array[offset] & 0xff;
    }

    private void writeShort(short v, OutputStream out) throws IOException {
        out.write((v >>> 0) & 0xFF);
        out.write((v >>> 8) & 0xFF);
    }

    public static final int[] getVarint(byte[] array, int offset) {
        int val = array[offset];
        boolean more = (val & 0x80) != 0;
        val &= 0x7f;

        if (!more) {
            return new int[] { val, 1 };
        } else {
            return new int[] { val << 8 | array[offset + 1] & 0xff, 2 };
        }
    }

    public static byte[] constructVarint(int v) {
        Assert.assertTrue("", v < 32768);
        if (v < 0x80) {
            return new byte[] { (byte) v };
        } else {
            int v1 = (v >>> 8) & 0xFF;
            int v2 = v & 0xFF;
            return new byte[] { (byte) (v1 | 0x80), (byte) v2 };
        }
    }

    private String decodeString(byte[] text, int offset, int length) {
        try {
            return (isUTF8() ? UTF8_DECODER : UTF16LE_DECODER).decode(ByteBuffer.wrap(text, offset, length)).toString();
        } catch (CharacterCodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    static class ByteArray {
        byte[] data;
        int hash;

        public ByteArray(byte[] data) {
            this.data = data;
        }

        public boolean equals(Object o) {
            return Arrays.equals(data, ((ByteArray) o).data);
        }

        public int hashCode() {
            int h = hash;
            if (h == 0 && data.length > 0) {
                for (int i = 0; i < data.length; i++) {
                    h = 31 * h + data[i];
                }
                hash = h;
            }
            return h;
        }
    }
}
