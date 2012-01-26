package org.alex73.android.arsc2;

import org.alex73.android.Assert;
import org.alex73.android.arsc2.reader.ChunkMapper;

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
        return parent.getTypeNames().getString(getId() - 1);
    }

    public Type2(Package2 parent, ChunkMapper rd) {
        Assert.assertTrue("Type chunk", rd.header.chunkType == 0x202);

        this.rd = rd;
        this.parent = parent;

        Assert.assertTrue("Wrong type id", getId() > 0 && getId() < 255);

        // some data left
        rd.skip(rd.left());
    }
}
