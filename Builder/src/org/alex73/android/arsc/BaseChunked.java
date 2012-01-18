package org.alex73.android.arsc;

public abstract class BaseChunked {
    protected final ChunkReader rd;

    public BaseChunked(ChunkReader rd) {
        this.rd = rd;
    }

    public byte[] getOriginalBytes() {
        return rd.getBytes();
    }

    protected abstract ChunkWriter write();
}
