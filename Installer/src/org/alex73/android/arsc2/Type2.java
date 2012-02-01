package org.alex73.android.arsc2;

import org.alex73.android.Assert;
import org.alex73.android.arsc2.reader.ChunkHeader2;
import org.alex73.android.arsc2.reader.ChunkMapper;
import org.alex73.android.arsc2.writer.ChunkWriter2;

public class Type2 {

    private final ChunkMapper rd;

    private final Package2 parent;

    public Package2 getParentPackage() {
        return parent;
    }

    public int getId() {
        rd.setPos(8);
        return rd.readInt();
    }

    public String getName() {
        return parent.getTypeNames().getStrings().get(getId() - 1).getRawString();
    }

    public Type2(Package2 parent, ChunkMapper in) {
        Assert.assertTrue("Type chunk", in.header.chunkType == ChunkHeader2.TYPE_TYPE);

        this.rd = in.clone();
        this.parent = parent;

        Assert.assertTrue("Wrong type id", getId() > 0 && getId() < 255);

        // some data left
        rd.skip(rd.left());
    }

    public void writeTo(ChunkWriter2 wr) {
        rd.writeTo(wr);
    }
}
