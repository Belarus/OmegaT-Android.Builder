package org.alex73.android.arsc2.reader;

public class ChunkHeader2 {
    public static final short TYPE_NONE = -1;
    public static final short TYPE_TABLE = 0x0002;
    public static final short TYPE_STRINGS = 0x0001;
    public static final short TYPE_PACKAGE = 0x0200;
    public static final short TYPE_TYPE = 0x0202;
    public static final short TYPE_CONFIG = 0x0201;

    public static final short TYPE2_STRINGS = 0x001C;

    public final short chunkType;
    public final short chunkType2;
    public final int chunkSize;

    public ChunkHeader2(ChunkReader2 rd) {
        chunkType = rd.readShort();
        chunkType2 = rd.readShort();
        chunkSize = rd.readInt();
    }

    public ChunkHeader2(ChunkMapper rd) {
        chunkType = rd.readShort();
        chunkType2 = rd.readShort();
        chunkSize = rd.readInt();
    }

    // public void write(ChunkWriter2 wr) {
    // wr.writeShort(chunkType);
    // wr.writeShort(chunkType2);
    // wr.writeInt(wr.size());
    // }
}