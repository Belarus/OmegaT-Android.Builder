package org.alex73.android.arsc;

public class ChunkResourceIds extends BaseChunk2 {
    BytesReader.IntArray m_resourceIDs;

    public ChunkResourceIds(ChunkHeader2 header, BytesReader rd) {
        super(header, rd);
        Checks.expect(ChunkHeader2.TYPE_RESOURCEIDS, header.chunkType);
        if (header.chunkSize < 8 || (header.chunkSize % 4) != 0) {
            throw new RuntimeException("Invalid resource ids size (" + header.chunkSize + ").");
        }
        m_resourceIDs = rd.readIntArray(header.chunkSize / 4 - 2);
    }
}
