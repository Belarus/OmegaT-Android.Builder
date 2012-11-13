package org.alex73.android.common;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

public class UTFUtils {
    private static final CharsetDecoder UTF8_DECODER = Charset.forName("UTF-8").newDecoder();
    private static final CharsetEncoder UTF8_ENCODER = Charset.forName("UTF-8").newEncoder();

    public static String utf8decoder(byte[] data, int offset, int length) {
        try {
            return UTF8_DECODER.decode(ByteBuffer.wrap(data, offset, length)).toString();
        } catch (CharacterCodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static byte[] utf8encode(String str) {
        try {
            return UTF8_ENCODER.encode(CharBuffer.wrap(str)).array();
        } catch (CharacterCodingException ex) {
            throw new RuntimeException(ex);
        }
    }
}
