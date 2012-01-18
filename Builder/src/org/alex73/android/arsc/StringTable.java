package org.alex73.android.arsc;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alex73.android.StyledString;
import org.alex73.android.arsc.ChunkWriter.LaterInt;

import junit.framework.Assert;

public class StringTable extends BaseChunked {
    private static final int UTF8_FLAG = 0x00000100;

    private static final Charset UTF_16LE = Charset.forName("UTF-16LE");
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private static final CharsetDecoder UTF16LE_DECODER = UTF_16LE.newDecoder();
    private static final CharsetDecoder UTF8_DECODER = UTF_8.newDecoder();

    private int[] read_stringOffsets;
    private byte[] read_strings;
    private int[] read_styleOffsets;
    private int[] read_styles;

    private int flags;
    private List<String> strings;
    private List<Tag[]> styles;

    public static class Tag {
        public int tagIndex;
        public int start;
        public int end;
    }

    public StringTable(ChunkReader rd) {
        super(rd);
    }

    public void read() throws Exception {
        if (rd.header.chunkType != 0x01 || rd.header.chunkType2 != 0x1C) {
            throw new Exception("StringTable chunk");
        }

        int stringCount = rd.readInt();
        int styleOffsetCount = rd.readInt();
        flags = rd.readInt();
        int stringsOffset = rd.readInt();
        int stylesOffset = rd.readInt();

        read_stringOffsets = rd.readIntArray(stringCount);
        read_styleOffsets = rd.readIntArray(styleOffsetCount);

        int size = ((stylesOffset == 0) ? rd.header.chunkSize : stylesOffset) - stringsOffset;
        if ((size % 4) != 0) {
            throw new Exception("String data size is not multiple of 4 (" + size + ").");
        }
        read_strings = new byte[size];
        rd.readFully(read_strings);

        if (stylesOffset != 0) {
            int size2 = (rd.header.chunkSize - stylesOffset);
            if ((size % 4) != 0) {
                throw new Exception("Style data size is not multiple of 4 (" + size + ").");
            }
            read_styles = rd.readIntArray(size2 / 4);
        }

        rd.close();

        // construct internal structures
        strings = new ArrayList<String>(read_stringOffsets.length);
        for (int i = 0; i < read_stringOffsets.length; i++) {
            strings.add(getReadString(i));
        }

        styles = new ArrayList<Tag[]>(read_styleOffsets.length);
        for (int i = 0; i < read_styleOffsets.length; i++) {
            int[] si = getStyleInfo(i);
            Tag[] st;
            if (si != null && si.length > 0) {
                st = new Tag[si.length / 3];
                for (int j = 0; j < st.length; j++) {
                    st[j] = new Tag();
                    st[j].tagIndex = si[j * 3];
                    st[j].start = si[j * 3 + 1];
                    st[j].end = si[j * 3 + 2];
                }
            } else {
                st = null;
            }
            styles.add(st);
        }

        Assert.assertEquals(stringCount, strings.size());
        Assert.assertEquals(styleOffsetCount, styles.size());

        read_stringOffsets = null;
        read_strings = null;
        read_styleOffsets = null;
        read_styles = null;
    }

    public ChunkWriter write() {
        ChunkWriter wr = new ChunkWriter(rd);

        ChunkWriter.LaterInt[] offsetsString = new ChunkWriter.LaterInt[strings.size()];
        ChunkWriter.LaterInt[] offsetsStyles = new ChunkWriter.LaterInt[styles.size()];

        wr.writeInt(strings.size());
        wr.writeInt(styles.size());
        wr.writeInt(flags);

        ChunkWriter.LaterInt stringsOffset = wr.new LaterInt();
        ChunkWriter.LaterInt stylesOffset = wr.new LaterInt();

        for (int i = 0; i < offsetsString.length; i++) {
            offsetsString[i] = wr.new LaterInt();
        }
        for (int i = 0; i < offsetsStyles.length; i++) {
            offsetsStyles[i] = wr.new LaterInt();
        }

        Map<String, Integer> stringOffsetMap = new HashMap<String, Integer>();
        stringsOffset.setValue(wr.pos());
        for (int i = 0; i < strings.size(); i++) {
            String s = strings.get(i);
            Integer existOffset = stringOffsetMap.get(s);
            if (existOffset != null) {
                // ужо было
                offsetsString[i].setValue(existOffset);
            } else {
                // яшчэ не было - запісваем
                offsetsString[i].setValue(wr.pos() - stringsOffset.getValue());
                stringOffsetMap.put(s, offsetsString[i].getValue());
                if (isUTF8()) {
                    // UTF-8: right, two length, with zero ended
                    byte[] sbytes = s.getBytes(UTF_8);
                    char[] len = new char[2];
                    wr.write(constructVarint(s.length()));
                    wr.write(constructVarint(sbytes.length));
                    wr.write(sbytes);
                    wr.write(0);
                } else {
                    // UTF-16
                    wr.writeShort((short) s.length());
                    wr.write(s.getBytes(UTF_16LE));
                    wr.writeShort((short) 0);
                }
            }
        }
        wr.align(4, 0);

        if (!styles.isEmpty()) {
            stylesOffset.setValue(wr.pos());
            for (int i = 0; i < styles.size(); i++) {
                offsetsStyles[i].setValue(wr.pos() - stylesOffset.getValue());
                Tag[] st = styles.get(i);
                if (st != null) {
                    for (Tag t : st) {
                        wr.writeInt(t.tagIndex);
                        wr.writeInt(t.start);
                        wr.writeInt(t.end);
                    }
                }
                wr.writeInt(-1);
            }
            wr.writeInt(-1);
            wr.writeInt(-1);
        }

        wr.close();

        return wr;
    }

    // int stringsOffset;
    // int stylesOffset;
    // int[] offsetsString;
    // int[] offsetsStyles;

    private static final int getShort(byte[] array, int offset) {
        return (array[offset + 1] & 0xff) << 8 | array[offset] & 0xff;
    }

    static final int[] getVarint(byte[] array, int offset) {
        int val = array[offset];
        boolean more = (val & 0x80) != 0;
        val &= 0x7f;

        if (!more) {
            return new int[] { val, 1 };
        } else {
            return new int[] { val << 8 | array[offset + 1] & 0xff, 2 };
        }
    }

    static byte[] constructVarint(int v) {
        Assert.assertTrue(v < 32768);
        if (v < 0x80) {
            return new byte[] { (byte) v };
        } else {
            int v1 = (v >>> 8) & 0xFF;
            int v2 = v & 0xFF;
            return new byte[] { (byte) (v1 | 0x80), (byte) v2 };
        }
    }

    private boolean isUTF8() {
        return (flags & UTF8_FLAG) != 0;
    }

    private String decodeString(int offset, int length) {
        try {
            return (isUTF8() ? UTF8_DECODER : UTF16LE_DECODER).decode(
                    ByteBuffer.wrap(read_strings, offset, length)).toString();
        } catch (CharacterCodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public StyledString getStyledString(int index) {
        StyledString result = new StyledString();
        result.raw = strings.get(index);
        if (index < styles.size() && styles.get(index) != null) {
            Tag[] tags = styles.get(index);
            result.tags = new StyledString.Tag[tags.length];
            for (int i = 0; i < tags.length; i++) {
                result.tags[i] = new StyledString.Tag();
                result.tags[i].start = tags[i].start;
                result.tags[i].end = tags[i].end;
                result.tags[i].tagName = strings.get(tags[i].tagIndex);
            }
        } else {
            result.tags = new StyledString.Tag[0];
        }
        return result;
    }

    public int addStyledString(StyledString str) {
        strings.add(str.raw);
        if (str.tags.length > 0) {
            Tag[] tags = new Tag[str.tags.length];
            for (int i = 0; i < str.tags.length; i++) {
                tags[i] = new Tag();
                tags[i].start = str.tags[i].start;
                tags[i].end = str.tags[i].end;
                tags[i].tagIndex = strings.indexOf(str.tags[i].tagName);
                Assert.assertTrue(tags[i].tagIndex >= 0);
            }
            while (styles.size() < strings.size() - 1) {
                styles.add(null);
            }
            styles.add(tags);
        }
        return strings.size() - 1;
    }

    public String getString(int index) {
        return strings.get(index);
    }

    public int getStringIndex(String raw) {
        return strings.indexOf(raw);
    }
    
    public int getStringCount() {
        return strings.size();
    }

    /**
     * Returns raw string (without any styling information) at specified index.
     */
    private String getReadString(int index) {
        int offset = read_stringOffsets[index];
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
        return decodeString(offset, length);
    }

    public Tag[] getStyle(int index) {
        if (index >= styles.size()) {
            return null;
        }
        return styles.get(index);
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
    private int[] getStyleInfo(int index) {
        if (index >= read_styleOffsets.length) {
            return null;
        }

        int offset = read_styleOffsets[index] / 4;

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
        Assert.assertEquals(0, count % 3);

        return Arrays.copyOfRange(read_styles, offset, offset + count);
    }
}
