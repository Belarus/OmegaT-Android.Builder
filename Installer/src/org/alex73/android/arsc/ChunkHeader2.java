package org.alex73.android.arsc;

public class ChunkHeader2 {
    public static final short TYPE_NONE = -1;
    public static final short TYPE_TABLE = 0x0002;
    public static final short TYPE_PACKAGE = 0x0200;
    public static final short TYPE_TYPE = 0x0202;
    public static final short TYPE_CONFIG = 0x0201;

    public static final int TYPE_STRINGTABLE = 0x001C0001;
    public static final int TYPE_XML = 0x00080003;
    public static final int TYPE_RESOURCEIDS = 0x00080180;

    public static final int TYPE_XML_START_NAMESPACE = 0x00100100;
    public static final int TYPE_XML_START_TAG = 0x00100102;

    public final int chunkType;
    public final int chunkSize;

    public ChunkHeader2(BytesReader rd) {
        chunkType = rd.readInt();
        chunkSize = rd.readInt();
    }

    public String toString() {
        return "chunk type=" + Integer.toHexString(chunkType) + " size=" + chunkSize;
    }

    /*
     * public void write(ChunkWriter2 wr) { wr.writeShort(chunkType); wr.writeShort(chunkType2);
     * wr.writeInt(wr.size()); }
     */
}
