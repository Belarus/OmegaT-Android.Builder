package org.alex73.android.common.zip;

import java.io.IOException;
import java.io.InputStream;

public interface LEReader {
    int readInt() throws IOException;

    short readShort() throws IOException;

    byte[] readFully(int sz) throws IOException;

//    void skip(int sz) throws IOException;

    int pos();

    InputStream getInputStream();

    int getPushbackBufferSize();

    void pushback(byte[] buffer, int offset, int len) throws IOException;
}
