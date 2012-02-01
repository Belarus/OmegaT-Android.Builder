package org.alex73.android.arsc2.reader;

import java.util.Arrays;

import org.alex73.android.Assert;
import org.alex73.android.arsc2.writer.ChunkWriter2;

public class ChunkMapper {
    private byte[] in;
    private int start, end, pos;
    public final ChunkHeader2 header;

    public ChunkMapper(byte[] underlineData, int start) {
        this.in = underlineData;
        this.start = start;
        this.pos = start;
        this.end = start + 8;
        this.header = new ChunkHeader2(this);
        this.end = start + header.chunkSize;
        Assert.assertTrue("Chunk larger than data", end <= underlineData.length);
    }

    public ChunkMapper clone() {
        ChunkMapper result = new ChunkMapper(getBytes(), 0);
        pos = end;
        return result;
    }

    public void close() {
        in = null;
    }

    public byte[] getBytes() {
        byte[] result = new byte[end - start];
        System.arraycopy(in, start, result, 0, result.length);
        return result;
    }

    public void writeTo(ChunkWriter2 wr) {
        wr.write(in, start, end - start);
    }

    public int left() {
        return end - pos;
    }

    public void finish() {
        Assert.assertTrue("Left=" + left(), left() == 0);
    }

    public int getPos() {
        return pos - start;
    }

    public void setPos(int pos) {
        checkPos(pos + start);
        this.pos = pos + start;
    }

    public void skip(int c) {
        checkPos(pos + c);
        pos += c;
    }

    private void checkPos(int newPos) {
        Assert.assertTrue("Outside chunk", pos >= start && pos <= end);
    }

    public byte readByte() {
        checkPos(pos + 1);
        return in[pos++];
    }

    public void writeByte(byte v) {
        checkPos(pos + 1);
        in[pos++] = v;
    }

    public void writeInt(int v) {
        checkPos(pos + 4);
        in[pos++] = (byte) ((v >>> 0) & 0xFF);
        in[pos++] = (byte) ((v >>> 8) & 0xFF);
        in[pos++] = (byte) ((v >>> 16) & 0xFF);
        in[pos++] = (byte) ((v >>> 24) & 0xFF);
    }

    public int readInt() {
        checkPos(pos + 4);
        int ch4 = in[pos++] & 0xff;
        int ch3 = in[pos++] & 0xff;
        int ch2 = in[pos++] & 0xff;
        int ch1 = in[pos++] & 0xff;
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    public short readShort() {
        checkPos(pos + 2);
        int ch2 = in[pos++] & 0xff;
        int ch1 = in[pos++] & 0xff;
        return (short) ((ch1 << 8) + (ch2 << 0));
    }

    public int[] readIntArray(int length) {
        int array[] = new int[length];
        for (int i = 0; i < length; i++)
            array[i] = readInt();

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
                Assert.assertEquals("", 0, readShort());
            }
        }
        return string.toString();
    }

    public void readFully(byte b[]) {
        checkPos(pos + b.length);
        System.arraycopy(in, pos, b, 0, b.length);
        pos += b.length;
    }

    public ChunkMapper readChunk() {
        int chunkStartPos = pos;

        ChunkHeader2 header = new ChunkHeader2(this);
        if (header.chunkType == -1 && header.chunkType2 == -1 && header.chunkSize == -1) {
            return null;
        }

        Assert.assertTrue("Chunk larger than data", chunkStartPos + header.chunkSize <= end);
        pos = chunkStartPos;
        ChunkMapper result = new ChunkMapper(in, chunkStartPos);
        pos = chunkStartPos + header.chunkSize;

        return result;
    }
}
