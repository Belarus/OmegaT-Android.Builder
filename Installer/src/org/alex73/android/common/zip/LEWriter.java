package org.alex73.android.common.zip;

import java.io.IOException;

public interface LEWriter {
    void writeShort(int v) throws IOException;

    void writeInt(int v) throws IOException;

    void writeFully(byte[] data) throws IOException;

    void seek(int count) throws IOException;

    int pos();
}
