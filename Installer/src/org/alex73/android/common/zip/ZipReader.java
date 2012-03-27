package org.alex73.android.common.zip;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class ZipReader {
    private final static int PUSHBACK_BUFFER_SIZE = 8192;
    private final RandomAccessFile inFile;
    private final OwnBufferedStream in;

    public ZipReader(RandomAccessFile in) {
        this.inFile = in;
        this.in = new OwnBufferedStream();
    }

    public LEReader getReader() {
        return reader;
    }

    public LEWriter getWriter() {
        return writer;
    }

    public void close() throws IOException {
        inFile.close();
    }

    protected LEWriter writer = new LEWriter() {
        int pos;

        public void writeShort(int v) throws IOException {
            inFile.write((v >>> 0) & 0xFF);
            inFile.write((v >>> 8) & 0xFF);
            pos += 2;
        }

        public void writeInt(int v) throws IOException {
            inFile.write((v >>> 0) & 0xFF);
            inFile.write((v >>> 8) & 0xFF);
            inFile.write((v >>> 16) & 0xFF);
            inFile.write((v >>> 24) & 0xFF);
            pos += 4;
        }

        public void writeFully(byte[] data) throws IOException {
            inFile.write(data);
            pos += data.length;
        }

        public void seek(int p) throws IOException {
            inFile.seek(p);
            pos = p;
        }

        public int pos() {
            return pos;
        }
    };

    protected LEReader reader = new LEReader() {

        public InputStream getInputStream() {
            return in;
        }

        public int getPushbackBufferSize() {
            return PUSHBACK_BUFFER_SIZE;
        }

        public void pushback(byte[] buffer, int offset, int len) throws IOException {
            in.pushback(buffer, offset, len);
        }

        public int pos() {
            return in.pos;
        }

        public int readInt() throws IOException {
            int ch4 = in.read();
            int ch3 = in.read();
            int ch2 = in.read();
            int ch1 = in.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0)
                throw new EOFException();
            return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
        }

        public short readShort() throws IOException {
            int ch2 = in.read();
            int ch1 = in.read();
            if ((ch1 | ch2) < 0)
                throw new EOFException();
            return (short) ((ch1 << 8) + (ch2 << 0));
        }

        public void skip(int sz) throws IOException {

            int off = 0;
            while (off < sz) {
                int len = (int) in.skip(sz - off);
                if (len < 0) {
                    throw new EOFException();
                }
                off += len;
            }
        }

        public byte[] readFully(int sz) throws IOException {
            byte[] result = new byte[sz];

            int off = 0;
            while (off < sz) {
                int len = (int) in.read(result, off, sz - off);
                if (len < 0) {
                    throw new EOFException();
                }
                off += len;
            }

            return result;
        }
    };

    protected class OwnBufferedStream extends InputStream {
        private final byte[] buffer = new byte[PUSHBACK_BUFFER_SIZE];
        private int bufferStart, bufferEnd;

        private int pos;

        @Override
        public int read() throws IOException {
            if (bufferStart == bufferEnd) {
                fill();
            }
            int result = buffer[bufferStart] & 0xFF;
            bufferStart++;
            pos++;
            return result;
        }

        @Override
        public int read(byte[] b, int offset, int length) throws IOException {
            if (bufferStart == bufferEnd) {
                fill();
            }
            int len = Math.min(length, bufferEnd - bufferStart);
            System.arraycopy(buffer, bufferStart, b, offset, len);
            bufferStart += len;
            pos += len;
            return len;
        }

        @Override
        public long skip(long n) throws IOException {
            int skipInBuffer = Math.min((int) n, bufferEnd - bufferStart);
            int skipInFile = (int) n - skipInBuffer;
            bufferStart += skipInBuffer;
            if (skipInFile > 0) {
                inFile.skipBytes(skipInFile);
            }
            pos += n;
            return n;
        }

        protected void fill() throws IOException {
            bufferStart = 0;
            bufferEnd = inFile.read(buffer);
            if (bufferEnd < 0) {
                throw new EOFException();
            }
        }

        public void pushback(byte[] b, int o, int l) throws IOException {
            if (l > bufferStart) {
                throw new IOException("There is no space for pushback");
            }
            bufferStart -= l;
            pos -= l;
            System.arraycopy(b, o, buffer, bufferStart, l);
        }
    }
}
