package org.alex73.android.arsc;

import java.util.Arrays;

import org.alex73.android.Assert;

public class ChunkReader {
    private final byte[] in;
    private int begin, end, pos;
    public final ChunkHeader header;
    private final ChunkReader parent;
    private boolean childOpened;

    public ChunkReader(ChunkReader rd) {
        this.parent = rd;
        this.parent.childOpened = true;
        this.in = rd.in;
        this.begin = rd.pos;
        this.pos = this.begin;
        this.end = begin + 8;
        this.header = new ChunkHeader(this);
        this.end = this.begin + this.header.chunkSize;
        if (this.end > in.length) {
            throw new RuntimeException("Chunk larger than data");
        }
    }

    public ChunkReader(byte[] rd) {
        this.parent = null;
        this.in = rd;
        this.begin = 0;
        this.pos = 0;
        this.end = begin + 8;
        this.header = new ChunkHeader(this);
        this.end = this.begin + this.header.chunkSize;
        if (this.end != in.length) {
            throw new RuntimeException("Chunk has other size than data");
        }
    }

    public byte[] getBytes() {
        return Arrays.copyOfRange(in, begin, end);
    }

    private void checkChild() {
        if (childOpened) {
            throw new RuntimeException("Child not closed yet");
        }
    }

    public int left() {
        checkChild();
        return end - pos;
    }

    public void close() {
        checkChild();
        if (pos != end) {
            throw new RuntimeException("Pos=" + pos + ", end=" + end);
        }
        if (parent != null) {
            parent.childOpened = false;
            parent.pos += end - begin;
        }
    }

    public byte readByte() {
        checkChild();
        return in[pos++];
    }

    public int readInt() {
        checkChild();
        int ch4 = in[pos++] & 0xff;
        int ch3 = in[pos++] & 0xff;
        int ch2 = in[pos++] & 0xff;
        int ch1 = in[pos++] & 0xff;
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    public short readShort() {
        checkChild();
        int ch2 = in[pos++] & 0xff;
        int ch1 = in[pos++] & 0xff;
        return (short) ((ch1 << 8) + (ch2 << 0));
    }

    public int[] readIntArray(int length) {
        checkChild();
        int array[] = new int[length];
        for (int i = 0; i < length; i++)
            array[i] = readInt();

        return array;
    }

    public String readNulEndedString(int length, boolean fixed) {
        checkChild();
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
                Assert.assertEquals("", 0, readShort());
            }
        }
        return string.toString();
    }

    public void readFully(byte b[]) {
        checkChild();
        System.arraycopy(in, pos, b, 0, b.length);
        pos += b.length;
    }
}
