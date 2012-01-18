package org.alex73.android.arsc;

public class BytesAccess {
    final byte[] data;
    final int beginOffset;
    final int length;

    public BytesAccess(byte[] data, int begin, int length) {
        this.data = data;
        this.beginOffset = begin;
        this.length = length;
    }

    public int getByte(int offset) {
        checkOffset(offset);
        return data[beginOffset + offset];
    }

    public short getShort(int offset) {
        int p = beginOffset + offset;
        checkOffset(offset);
        checkOffset(offset + 1);
        int ch2 = data[p++] & 0xff;
        int ch1 = data[p++] & 0xff;
        return (short) ((ch1 << 8) + (ch2 << 0));
    }

    public int getInt(int offset) {
        int p = beginOffset + offset;
        checkOffset(offset);
        checkOffset(offset + 3);
        int ch4 = data[p++] & 0xff;
        int ch3 = data[p++] & 0xff;
        int ch2 = data[p++] & 0xff;
        int ch1 = data[p++] & 0xff;
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    public BytesAccess subArray(int offset, int size) {
        checkOffset(offset);
        checkOffset(offset + size - 1);
        return new BytesAccess(data, beginOffset + offset, size);
    }

    protected void checkOffset(int offset) {
        if (offset < 0 || offset > length) {
            throw new ArrayIndexOutOfBoundsException(offset);
        }
    }

    public int length() {
        return length;
    }
}
