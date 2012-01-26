package org.alex73.android.arsc2;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.alex73.android.Assert;
import org.alex73.android.arsc2.reader.ChunkHeader2;
import org.alex73.android.arsc2.reader.ChunkMapper;
import org.alex73.android.arsc2.reader.ChunkReader2;

import android.util.TypedValue;

public class Resources2 {
    protected static final Set<String> KNOWN_TYPES = new HashSet<String>(Arrays.asList(
    /** Translated. */
    "string", "array", "plurals",
    /** Non-translated. */
    "attr", "id", "style", "dimen", "color", "drawable", "layout", "menu", "anim", "xml", "raw", "bool",
            "integer", "fraction", "mipmap", "animator", "interpolator"));

    protected static final Set<String> KNOWN_TYPES_TRANSLATED = new HashSet<String>(Arrays.asList("string",
            "array", "plurals"));

    StringTable2 stringTable;

    Set<Integer> stringIndexesForTranslate = new HashSet<Integer>();

    public void processResources(ChunkReader2 rd) throws IOException {
        Assert.assertTrue("Main chunk should be TABLE", rd.header.chunkType == ChunkHeader2.TYPE_TABLE);

        int packageCount = rd.readInt();

        stringTable = new StringTable2();
        ChunkMapper stringChunk = rd.readChunk();
        stringTable.read(stringChunk);
        boolean[] used = new boolean[stringTable.getStringCount()];

        for (int i = 0; i < packageCount; i++) {
            Package2 pkg = new Package2(stringTable, rd.readChunk());
            for (Type2 t : pkg.getAllTypes()) {
                Assert.assertTrue("Unknown type: " + t.getName(), KNOWN_TYPES.contains(t.getName()));
            }
            for (int j = 0; j < pkg.getContent().size(); j++) {
                if (!(pkg.getContent().get(j) instanceof Config2)) {
                    continue;
                }
                Config2 c = (Config2) pkg.getContent().get(j);
                // if (!c.getFlags().isEmpty() ||
                // !KNOWN_TYPES_TRANSLATED.contains(c.getParentType().getName())) {
                // continue;
                // }

                for (int e = 0; e < c.getEntriesCount(); e++) {
                    Entry2 ee = c.getEntry(e);
                    if (ee == null) {
                        continue;
                    }
                    if (ee.isComplex()) {
                        for (int k = 0; k < ee.getKeyValueCount(); k++) {
                            int si = ee.getKeyValue(k).getStringIndex();
                            if (si >= 0) {
                                used[si] = true;
                            }
                        }
                    } else {
                        int si = ee.getSimpleStringIndex();
                        if (si >= 0) {
                            used[si] = true;
                        }
                    }
                }

                // copy to new config
                // c = c.duplicate();
                // c.getFlags().setLocale("be");
                // pkg.getContent().add(j + 1, c);
                // j++;

            }
        }
        int nu = 0;
        for (int i = 0; i < used.length; i++) {
            if (!used[i])
                nu++;
        }
        if (nu > 0) {
            System.out.println("nonused: " + nu + " from " + used.length);
        }
    }

    private int performTranslate(byte type, int data) {
        // need to translate
        if (type == TypedValue.TYPE_STRING) {
            stringIndexesForTranslate.add(data);
            System.out.println("    " + stringTable.getString(data));
        }
        return data;
    }
}
