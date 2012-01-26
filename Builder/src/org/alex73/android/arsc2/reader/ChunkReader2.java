package org.alex73.android.arsc2.reader;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.alex73.android.Assert;

public class ChunkReader2 {
    private int left;
    public final ChunkHeader2 header;
    private final BufferedInputStream in;

    public ChunkReader2(InputStream in) {
        this.in = new BufferedInputStream(in);
        this.header = new ChunkHeader2(this);
        left = header.chunkSize - 8;
    }

    public ChunkReader2(ChunkReader2 parent) {
        this.in = parent.in;
        this.header = new ChunkHeader2(this);
        left = header.chunkSize - 8;
    }

    public int left() {
        return left;
    }

    public ChunkHeader2 prereadHeader() {
        try {
            in.mark(8);
            ChunkHeader2 header = new ChunkHeader2(this);
            in.reset();
            return header;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public ChunkMapper readChunk() {
        short chunkType = readShort();
        short chunkType2 = readShort();
        int chunkSize = readInt();
        if (chunkType == -1 && chunkType2 == -1 && chunkSize == -1) {
            return null;
        }

        byte[] bytes = new byte[chunkSize];

        // write header back
        bytes[0] = (byte) ((chunkType >>> 0) & 0xFF);
        bytes[1] = (byte) ((chunkType >>> 8) & 0xFF);
        bytes[2] = (byte) ((chunkType2 >>> 0) & 0xFF);
        bytes[3] = (byte) ((chunkType2 >>> 8) & 0xFF);
        bytes[4] = (byte) ((chunkSize >>> 0) & 0xFF);
        bytes[5] = (byte) ((chunkSize >>> 8) & 0xFF);
        bytes[6] = (byte) ((chunkSize >>> 16) & 0xFF);
        bytes[7] = (byte) ((chunkSize >>> 24) & 0xFF);

        readFully(bytes, 8, bytes.length - 8);

        return new ChunkMapper(bytes, 0);
    }

    public byte readByte() {
        try {
            left--;
            return (byte) in.read();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public int readInt() {
        try {
            left -= 4;
            int ch4 = in.read() & 0xff;
            int ch3 = in.read() & 0xff;
            int ch2 = in.read() & 0xff;
            int ch1 = in.read() & 0xff;
            return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public short readShort() {
        try {
            left -= 2;
            int ch2 = in.read() & 0xff;
            int ch1 = in.read() & 0xff;
            return (short) ((ch1 << 8) + (ch2 << 0));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public int[] readIntArray(int length) {
        int array[] = new int[length];
        for (int i = 0; i < length; i++) {
            array[i] = readInt();
        }

        return array;
    }

    public String readNulEndedString(int length, boolean fixed) {
        StringBuilder string = new StringBuilder(length);
        do {
            if (length-- == 0)
                break;
            short ch = readShort();
            if (ch == 0)
                break;
            string.append((char) ch);
        } while (true);
        if (fixed) {
            while (length-- > 0) {
                Assert.assertEquals("String finished earlier", 0, readShort());
            }
        }
        return string.toString();
    }

    public void readFully(byte b[]) {
        readFully(b, 0, b.length);
    }

    protected void readFully(byte b[], int offset, int length) {
        try {
            for (int p = 0; p < length;) {
                int len = in.read(b, p + offset, length - p);
                if (len < 0) {
                    throw new EOFException();
                }
                p += len;
            }
            left -= length;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
