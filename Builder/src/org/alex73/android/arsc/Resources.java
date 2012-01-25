package org.alex73.android.arsc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.alex73.android.Assert;

public class Resources extends BaseChunked {
    protected static final Set<String> KNOWN_TYPES = new HashSet<String>(Arrays.asList(
    /** Translated. */
    "string", "array", "plurals",
    /** Non-translated. */
    "attr", "id", "style", "dimen", "color", "drawable", "layout", "menu", "anim", "xml", "raw", "bool",
            "integer", "fraction", "mipmap", "animator", "interpolator"));

    private StringTable stringTable;

    private Package[] packages;

    public Resources(ChunkReader rd) throws Exception {
        super(rd);

        if (rd.header.chunkType != ChunkHeader.TYPE_TABLE) {
            throw new Exception("Main chunk should be TABLE");
        }
        int packageCount = rd.readInt();

        stringTable = new StringTable(new ChunkReader(rd));
        stringTable.read();

        packages = new Package[packageCount];
        for (int i = 0; i < packageCount; i++) {
            packages[i] = new Package(this, new ChunkReader(rd));
        }
        rd.close();

        for (Package p : packages) {
            for (Type t : p.getAllTypes()) {
                Assert.assertTrue("Unknown type: " + t.getName(), KNOWN_TYPES.contains(t.getName()));
            }
        }
    }

    public ChunkWriter write() {
        ChunkWriter wr = new ChunkWriter(rd);

        wr.writeInt(packages.length);

        ChunkWriter wrStringTable = stringTable.write();
        wr.write(wrStringTable.getBytes());

        for (Package pkg : packages) {
            ChunkWriter wrPackage = pkg.write();
            wr.write(wrPackage.getBytes());
        }

        wr.close();

        return wr;
    }

    public StringTable getStringTable() {
        return stringTable;
    }

    public Package[] getPackages() {
        return packages;
    }
}
