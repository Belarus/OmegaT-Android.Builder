package org.alex73.android.arsc2.writer;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.alex73.android.Assert;

public class ChunkWriter2 extends OutputStream {
    private byte[] buffer;
    private int bufferPos;
    private int bufferSize;
    private boolean closed;
    private List<LaterInt> latersInt = new ArrayList<LaterInt>();
    private List<LaterShort> latersShort = new ArrayList<LaterShort>();
    private LaterInt laterSize;

    public ChunkWriter2(int chunkType, int chunkType2) {
        buffer = new byte[1024];
        writeShort((short) chunkType);
        writeShort((short) chunkType2);
        laterSize = new LaterInt();
    }

    public byte[] getBytes() {
        Assert.assertTrue("Not closed yet", closed);

        byte[] result = new byte[bufferSize];
        System.arraycopy(buffer, 0, result, 0, result.length);
        return result;
    }

    public byte[] getPart(int start, int end) {
        byte[] result = new byte[end - start];
        System.arraycopy(buffer, start, result, 0, result.length);
        return result;
    }

    public void close() {
        laterSize.setValue(bufferSize);

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
        byte[] newBuffer = new byte[newcount];
        System.arraycopy(buffer, 0, newBuffer, 0, bufferPos);
        buffer = newBuffer;
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

    public void writeIntArray(List<Integer> v, int count) {
        haveSpace(4 * count);
        for (int i = 0; i < count; i++) {
            writeInt(v.get(i));
        }
    }

    public void writeIntArray(List<Integer> v) {
        writeIntArray(v, v.size());
    }

    @Override
    public void write(byte[] v) {
        write(v, 0, v.length);
    }

    public void writeNulEndedString(int fixedLength, String text) {
        Assert.assertTrue("String lesser", text.length() < fixedLength);

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

    @Override
    public void write(byte[] b, int off, int len) {
        haveSpace(len);
        System.arraycopy(b, off, buffer, bufferPos, len);
        bufferPos += len;
        bufferSize = Math.max(bufferPos, bufferSize);
    }

    public final class LaterInt {
        private int pos;
        private int value;

        public LaterInt() {
            pos = ChunkWriter2.this.pos();
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
        private short value;

        public LaterShort() {
            pos = ChunkWriter2.this.pos();
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
