package org.alex73.android.arsc2;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;

import org.alex73.android.Assert;
import org.alex73.android.StyledString;
import org.alex73.android.arsc2.reader.ChunkHeader2;
import org.alex73.android.arsc2.reader.ChunkMapper;
import org.alex73.android.arsc2.writer.ChunkWriter2;
import org.alex73.android.arsc2.writer.ChunkWriter2.LaterInt;

public class StringTable2 {
    private static final int UTF8_FLAG = 0x00000100;

    private static final Charset UTF_16LE = Charset.forName("UTF-16LE");
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private static final CharsetDecoder UTF16LE_DECODER = UTF_16LE.newDecoder();
    private static final CharsetDecoder UTF8_DECODER = UTF_8.newDecoder();

    private static StyledString.Tag[] NOTAGS = new StyledString.Tag[0];

    private byte[] read_strings;
    private int[] read_styles;

    // offsets for strings
    private int[] read_stringOffsets;
    // offsets for styles
    private int[] read_styleOffsets;

    private int flags;

    public static class Tag {
        public int tagIndex;
        public int start;
        public int end;
    }

    public void close() {
        read_stringOffsets = null;
        read_strings = null;
        read_styleOffsets = null;
        read_styles = null;
    }

    public void read(ChunkMapper rd) {
        Assert.assertTrue("StringTable chunk", rd.header.chunkType == ChunkHeader2.TYPE_STRINGS
                && rd.header.chunkType2 == ChunkHeader2.TYPE2_STRINGS);

        int stringCount = rd.readInt();
        int styleOffsetCount = rd.readInt();
        flags = rd.readInt();
        int stringsOffset = rd.readInt();
        int stylesOffset = rd.readInt();

        read_stringOffsets = rd.readIntArray(stringCount);
        read_styleOffsets = rd.readIntArray(styleOffsetCount);

        int size = ((stylesOffset == 0) ? rd.header.chunkSize : stylesOffset) - stringsOffset;
        Assert.assertTrue("String data size is not multiple of 4 (" + size + ").", (size % 4) == 0);

        read_strings = new byte[size];
        rd.readFully(read_strings);

        if (stylesOffset != 0) {
            int size2 = (rd.header.chunkSize - stylesOffset);
            Assert.assertTrue("Style data size is not multiple of 4 (" + size + ").", (size % 4) == 0);
            read_styles = rd.readIntArray(size2 / 4);
        }

        // rd.close();

    }

    public byte[] write() {
        ChunkWriter2 wr = new ChunkWriter2(ChunkHeader2.TYPE_STRINGS, ChunkHeader2.TYPE2_STRINGS);

        wr.writeInt(read_stringOffsets.length);
        wr.writeInt(read_styleOffsets.length);
        wr.writeInt(flags);
        LaterInt laterStringsOffset = wr.new LaterInt();
        LaterInt laterStylesOffset = wr.new LaterInt();

        wr.writeIntArray(read_stringOffsets);
        wr.writeIntArray(read_styleOffsets);

        laterStringsOffset.setValue(wr.pos());
        wr.write(read_strings);

        if (read_styles != null) {
            laterStylesOffset.setValue(wr.pos());
            wr.writeIntArray(read_styles);
        } else {
            laterStylesOffset.setValue(0);
        }

        wr.close();
        return wr.getBytes();
    }

    public int getStringCount() {
        return read_stringOffsets.length;
    }

    /**
     * Returns raw string (without any styling information) at specified index.
     */
    public String getString(int index) {
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

    private Tag[] getStyle(int index) {
        int[] si = getStyleInfo(index);
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

        return st;
    }

    public StyledString getStyledString(int index) {
        StyledString result = new StyledString();
        result.raw = getString(index);
        Tag[] tags = getStyle(index);
        if (tags != null) {
            result.tags = new StyledString.Tag[tags.length];
            for (int i = 0; i < tags.length; i++) {
                result.tags[i] = new StyledString.Tag();
                result.tags[i].start = tags[i].start;
                result.tags[i].end = tags[i].end;
                result.tags[i].tagName = getString(tags[i].tagIndex);
            }
        } else {
            result.tags = NOTAGS;
        }
        return result;
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
        Assert.assertEquals("Styles count wrong", 0, count % 3);

        return Arrays.copyOfRange(read_styles, offset, offset + count);
    }

    private boolean isUTF8() {
        return (flags & UTF8_FLAG) != 0;
    }

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

    private String decodeString(int offset, int length) {
        try {
            return (isUTF8() ? UTF8_DECODER : UTF16LE_DECODER).decode(
                    ByteBuffer.wrap(read_strings, offset, length)).toString();
        } catch (CharacterCodingException ex) {
            throw new RuntimeException(ex);
        }
    }
}
