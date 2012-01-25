package org.alex73.android.arsc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.alex73.android.Assert;

public class ChunkWriter {
    private final ChunkHeader header;
    private byte[] buffer;
    private int bufferPos;
    private int bufferSize;
    private boolean closed;
    private List<LaterInt> latersInt = new ArrayList<LaterInt>();
    private List<LaterShort> latersShort = new ArrayList<LaterShort>();

    public ChunkWriter(ChunkReader rd) {
        this.header = rd.header;
        buffer = new byte[1024];
        header.write(this);
    }

    public byte[] getBytes() {
        Assert.assertTrue("", closed);
        return Arrays.copyOfRange(buffer, 0, bufferSize);
    }

    public byte[] getPart(int start, int end) {
        return Arrays.copyOfRange(buffer, start, end);
    }

    public void close() {
        bufferPos = 0;
        header.write(this);

        for (LaterInt lat : latersInt) {
            bufferPos = lat.pos;
            writeInt(lat.value);
        }
        for (LaterShort lat : latersShort) {
            bufferPos = lat.pos;
            writeShort(lat.value);
        }

        closed = true;
    }

    private void haveSpace(int count) {
        if (closed) {
            throw new RuntimeException("EOF");
        }
        if (bufferPos + count <= buffer.length) {
            return;
        }

        int newcount = Math.max(bufferPos + count, buffer.length * 2);
        buffer = Arrays.copyOf(buffer, newcount);
    }

    public void write(int b) {
        haveSpace(1);
        buffer[bufferPos] = (byte) b;
        bufferPos++;
        bufferSize = Math.max(bufferPos, bufferSize);
    }

    public void writeInt(int v) {
        write((v >>> 0) & 0xFF);
        write((v >>> 8) & 0xFF);
        write((v >>> 16) & 0xFF);
        write((v >>> 24) & 0xFF);
    }

    public void writeShort(short v) {
        write((v >>> 0) & 0xFF);
        write((v >>> 8) & 0xFF);
    }

    public void writeIntArray(int[] v) {
        haveSpace(4 * v.length);
        for (int i : v) {
            writeInt(i);
        }
    }

    public void write(byte[] v) {
        haveSpace(v.length);
        System.arraycopy(v, 0, buffer, bufferPos, v.length);
        bufferPos += v.length;
        bufferSize = Math.max(bufferPos, bufferSize);
    }

    public void writeNulEndedString(int fixedLength, String text) {
        Assert.assertTrue("",text.length() < fixedLength);

        for (char c : text.toCharArray()) {
            writeShort((short) c);
        }
        for (int i = text.length(); i < fixedLength; i++) {
            writeShort((short) 0);
        }
    }

    public void align(int div, int filler) {
        while (bufferPos % div > 0) {
            write(filler);
        }
    }

    public int pos() {
        return bufferPos;
    }

    public int size() {
        return bufferSize;
    }

    public final class LaterInt {
        private int pos;
        protected int value;

        public LaterInt() {
            pos = ChunkWriter.this.pos();
            latersInt.add(this);
            writeInt(0);
        }

        public void setValue(int v) {
            value = v;
        }

        public int getValue() {
            return value;
        }
    }

    public final class LaterShort {
        private int pos;
        protected short value;

        public LaterShort() {
            pos = ChunkWriter.this.pos();
            latersShort.add(this);
            writeShort((short) 0);
        }

        public void setValue(short v) {
            value = v;
        }

        public short getValue() {
            return value;
        }
    }
}
