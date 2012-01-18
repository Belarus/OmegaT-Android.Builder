package org.alex73.android.arsc;

public class BaseChunk2 {
    protected final ChunkHeader2 header;
    protected final BytesReader rd;

    public BaseChunk2(ChunkHeader2 header, BytesReader rd) {
        this.header = header;
        this.rd = new BytesReader(rd.data, rd.beginOffset + rd.pos, header.chunkSize - 8);
        rd.skip(header.chunkSize - 8);
    }
}
