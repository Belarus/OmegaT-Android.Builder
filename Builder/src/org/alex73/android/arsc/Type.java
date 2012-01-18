package org.alex73.android.arsc;

import java.util.Arrays;

import junit.framework.Assert;

public class Type extends BaseChunked {
    boolean[] mMissingResSpecs;

    int id;

    private Package parent;

    public Package getParentPackage() {
        return parent;
    }

    public int getId() {
        return id;
    }

    public Type(Package parent, ChunkReader rd) throws Exception {
        super(rd);

        if (rd.header.chunkType != 0x202) {
            throw new Exception("Type chunk");
        }
        this.parent = parent;

        id = rd.readInt();
        Assert.assertTrue(id > 0 && id < 255);

        int entryCount = rd.readInt();

        mMissingResSpecs = new boolean[entryCount];
        Arrays.fill(mMissingResSpecs, true);

        for (int i = 0; i < entryCount; i++) {
            int specFlags = rd.readInt();
        }

        rd.close();
    }

    protected ChunkWriter write() {
        return null;
    }

    public String getName() {
        return parent.getTypeNames().getString(id - 1);
    }
}
