package org.alex73.android.arsc2.reader;

public class ChunkHeader2 {
    public static final short TYPE_NONE = -1;
    public static final short TYPE_TABLE = 0x0002;
    public static final short TYPE_DATA = 0x0003;
    public static final short TYPE_XML_EVENT = 0x0010;
    public static final short TYPE_STRINGS = 0x0001;
    public static final short TYPE_PACKAGE = 0x0200;
    public static final short TYPE_TYPE = 0x0202;
    public static final short TYPE_CONFIG = 0x0201;
    public static final short TYPE_XML_EVENT_START_TAG = 0x0102;

    public static final short TYPE2_TABLE = 0x000C;
    public static final short TYPE2_STRINGS = 0x001C;
    public static final short TYPE2_PACKAGE = 0x011C;
    public static final short TYPE2_XML = 0x0008;
    public static final short TYPE2_RESOURCEIDS = 0x0180;
    public static final short TYPE2_XML_EVENT_START_TAG = 0x0010;
    
    public static final int TYPE3_STRINGTABLE = 0x001C0001;
    public static final int TYPE3_XML = 0x00080003;
    public static final int TYPE3_RESOURCEIDS = 0x00080180;

    public static final int TYPE3_XML_START_NAMESPACE = 0x00100100;
    public static final int TYPE3_XML_START_TAG = 0x00100102;
    
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