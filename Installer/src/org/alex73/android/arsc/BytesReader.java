package org.alex73.android.arsc;

public class BytesReader extends BytesAccess {
    protected int pos;

    public BytesReader(byte[] data, int begin, int length) {
        super(data, begin, length);
        pos = 0;
    }

    public void skip(int count) {
        pos += count;
        checkOffset(pos - 1);
    }

    public int readInt() {
        int result = getInt(pos);
        pos += 4;
        return result;
    }

    public short readShort() {
        short result = getShort(pos);
        pos += 2;
        return result;
    }

    public IntArray readIntArray(int intCounts) {
        checkOffset(pos);
        IntArray result = new IntArray(subArray(pos, intCounts * 4));
        pos += intCounts * 4;
        checkOffset(pos - 1);

        return result;
    }

    public int[] readIntArr(int intCounts) {
        int[] result = new int[intCounts];
        for (int i = 0; i < intCounts; i++) {
            result[i] = readInt();
        }

        return result;
    }

    public BytesAccess readBytesAccess(int size) {
        checkOffset(pos);
        BytesAccess result = subArray(pos, size);
        pos += size;
        checkOffset(pos - 1);

        return result;
    }

    public int pos() {
        return pos;
    }

    public boolean isEOF() {
        return pos >= length;
    }

    public class IntArray {
        private final BytesAccess bytes;

        public IntArray(BytesAccess bytes) {
            this.bytes = bytes;
        }

        public int get(int i) {
            return bytes.getInt(i * 4);
        }

        public int size() {
            return bytes.length() / 4;
        }

        public String toString() {
            return "sz=" + size();
        }
    }
}
