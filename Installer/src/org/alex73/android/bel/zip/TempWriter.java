package org.alex73.android.bel.zip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class TempWriter implements LEWriter {
    private ByteArrayOutputStream out = new ByteArrayOutputStream();

    public void writeShort(int v) throws IOException {
        out.write((v >>> 0) & 0xFF);
        out.write((v >>> 8) & 0xFF);
    }

    public void writeInt(int v) throws IOException {
        out.write((v >>> 0) & 0xFF);
        out.write((v >>> 8) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>> 24) & 0xFF);
    }

    public void writeFully(byte[] data) throws IOException {
        out.write(data);
    }

    public int size() {
        return out.size();
    }

    public byte[] toByteArray() {
        return out.toByteArray();
    }

    public void seek(int count) throws IOException {
        throw new RuntimeException("Not implemented");
    }

    public int pos() {
        return out.size();
    }
}
