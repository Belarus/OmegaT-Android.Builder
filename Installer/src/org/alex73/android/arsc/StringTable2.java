package org.alex73.android.arsc;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

public class StringTable2 extends BaseChunk2 {
    private static final int UTF8_FLAG = 0x00000100;

    private static final Charset UTF_16LE = Charset.forName("UTF-16LE");
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private BytesReader.IntArray read_stringOffsets;
    private BytesAccess read_strings;
    private BytesReader.IntArray read_styleOffsets;
    private BytesReader.IntArray read_styles;

    private int flags;
   

    public static class Tag {
        public int tagIndex;
        public int start;
        public int end;
    }

    public StringTable2(ChunkHeader2 header, BytesReader rd) {
        super(header, rd);
        Checks.expect(ChunkHeader2.TYPE_STRINGTABLE, header.chunkType, "Invalid StringTable");
        read();
    }

    public void read() {

        int stringCount = rd.readInt();
        int styleOffsetCount = rd.readInt();
        flags = rd.readInt();
        int stringsOffset = rd.readInt();
        int stylesOffset = rd.readInt();

        read_stringOffsets = rd.readIntArray(stringCount);
        read_styleOffsets = rd.readIntArray(styleOffsetCount);

        int size = ((stylesOffset == 0) ? header.chunkSize : stylesOffset) - stringsOffset;
        if ((size % 4) != 0) {
            throw new RuntimeException("String data size is not multiple of 4 (" + size + ").");
        }
        read_strings = rd.readBytesAccess(size);

        if (stylesOffset != 0) {
            int size2 = (header.chunkSize - stylesOffset);
            if ((size % 4) != 0) {
                throw new RuntimeException("Style data size is not multiple of 4 (" + size + ").");
            }
            read_styles = rd.readIntArray(size2 / 4);
        }

    }

    static final int[] getVarint(BytesAccess array, int offset) {
        int val = array.getByte(offset);
        boolean more = (val & 0x80) != 0;
        val &= 0x7f;

        if (!more) {
            return new int[] { val, 1 };
        } else {
            return new int[] { val << 8 | array.getByte(offset + 1) & 0xff, 2 };
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
            return new String(read_strings.data, read_strings.beginOffset + offset, length,
                    isUTF8() ? "UTF-8" : "UTF-16LE");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Returns raw string (without any styling information) at specified index.
     */
    public String getString(int index) {
        int offset = read_stringOffsets.get(index);
        int length;

        if (isUTF8()) {
            offset += getVarint(read_strings, offset)[1];
            int[] varint = getVarint(read_strings, offset);
            offset += varint[1];
            length = varint[0];
        } else {
            length = read_strings.getShort(offset) * 2;
            offset += 2;
        }
        return decodeString(offset, length);
    }
/*
    public Tag[] getStyle(int index) {
        if (index >= styles.size()) {
            return null;
        }
        return styles.get(index);
    }*/
}
