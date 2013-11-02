package org.alex73.android.arsc2;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.alex73.android.Assert;
import org.alex73.android.StyledString;
import org.alex73.android.arsc2.reader.ChunkHeader2;
import org.alex73.android.arsc2.reader.ChunkMapper;
import org.alex73.android.arsc2.reader.ChunkReader2;
import org.alex73.android.arsc2.writer.ChunkWriter2;

public class ResourceProcessor {
    protected static final Set<String> KNOWN_TYPES = new HashSet<String>(Arrays.asList(
    /** Translated. */
    "string", "array", "plurals",
    /** Non-translated. */
    "attr", "id", "style", "dimen", "color", "drawable", "layout", "menu", "anim", "xml", "raw", "bool",
            "integer", "fraction", "mipmap", "animator", "interpolator"));

    public static final int BAG_KEY_PLURALS_START = 0x01000004;
    public static final int BAG_KEY_PLURALS_END = 0x01000009;
    private static final String[] QUANTITY_MAP = new String[] { "other", "zero", "one", "two", "few", "many" };

    protected static final Set<String> KNOWN_TYPES_TRANSLATED = new HashSet<String>(Arrays.asList("string",
            "array", "plurals"));

    protected Set<String> LEAVE_LOCALES = new TreeSet<String>(Arrays.asList("", "en", "en-US", "ru", "ru-RU"));
    protected Set<String> TRANSLATE_TYPES = new TreeSet<String>(Arrays.asList("string", "array", "plurals"));

    // Which strings are used
    BitSet stringsUsed;
    int[] stringsRemap;

    public StringTable2 globalStringTable;
    public Package2[] packages;

    /**
     * Read.
     */
    public ResourceProcessor(ChunkReader2 rd, Callback callback) {
        Assert.assertTrue("Main chunk should be TABLE", rd.header.chunkType == ChunkHeader2.TYPE_TABLE
                && rd.header.chunkType2 == ChunkHeader2.TYPE2_TABLE);

        int packageCount = rd.readInt();

        globalStringTable = new StringTable2();
        ChunkMapper stringChunk = rd.readChunk();
        try {
            if (callback!=null) {
                callback.onGlobalStringTable(stringChunk);
            }
            globalStringTable.read(stringChunk);
        } finally {
            stringChunk.close();
        }

        packages = new Package2[packageCount];
        // translate
        for (int i = 0; i < packageCount; i++) {
            ChunkMapper packageChunk = rd.readChunk();
            try {
                packages[i] = new Package2(null, packageChunk);
            } finally {
                packageChunk.close();
            }
        }

        Assert.assertNull("Chunk after packages", rd.readChunk());
    }

    public void process(String packageName, Translation translation) {
        markUsedStrings();
        removeUnusedStrings();
        remapAllConfigs();
        removeUnusedConfigs();
        translate(packageName, translation);
    }

    public ChunkWriter2 save() {
        ChunkWriter2 wr = new ChunkWriter2(ChunkHeader2.TYPE_TABLE, ChunkHeader2.TYPE2_TABLE);

        wr.writeInt(packages.length);

        wr.write(globalStringTable.write());

        for (Package2 pkg : packages) {
            wr.write(pkg.write());
        }

        // write '-1' chunk
        // wr.writeShort((short) -1);
        // wr.writeShort((short) -1);
        // wr.writeInt(-1);
        wr.close();

        return wr;
    }

    private void removeUnusedStrings() {

        // count removed strings
        int rc = 0;
        for (int i = 0; i < stringsRemap.length; i++) {
            if (stringsRemap[i] < 0)
                rc++;
        }
        Assert.assertEquals("nonused: " + stringsUsed.cardinality() + " from " + stringsUsed.length(),
                stringsUsed.cardinality(), stringsUsed.length());

        // remove unused strings
        for (int pos = 0, newPos = 0; pos < stringsRemap.length; pos++) {
            if (stringsRemap[pos] >= 0) {
                stringsRemap[pos] = newPos;
                newPos++;
            } else {
                globalStringTable.getStrings().remove(newPos);
            }
        }
    }

    private void remapAllConfigs() {
        // remap strings in configs
        for (Package2 pkg : packages) {
            for (Type2 t : pkg.getAllTypes()) {
                Assert.assertTrue("Unknown type: " + t.getName(), KNOWN_TYPES.contains(t.getName()));
            }
            for (int j = 0; j < pkg.getContent().size(); j++) {
                if (pkg.getContent().get(j) instanceof Type2) {
                    Type2 type = (Type2) pkg.getContent().get(j);
                    continue;
                }
                if (!(pkg.getContent().get(j) instanceof Config2)) {
                    continue;
                }
                Config2 c = (Config2) pkg.getContent().get(j);

                for (int e = 0; e < c.getEntriesCount(); e++) {
                    Entry2 ee = c.getEntry(e);
                    if (ee == null) {
                        continue;
                    }
                    if (ee.isComplex()) {
                        for (Entry2.KeyValue kv : ee.getKeyValues()) {
                            int si = kv.getComplexStringIndex();
                            if (si >= 0) {
                                // translate
                                si = stringsRemap[si];
                                kv.setComplexStringIndex(si);
                            }
                        }
                    } else {
                        int si = ee.getSimpleStringIndex();
                        if (si >= 0) {
                            // translate
                            si = stringsRemap[si];
                            ee.setSimpleStringIndex(si);
                        }
                    }
                }
            }
        }

        // remap tags
        for (StringTable2.StringInstance str : globalStringTable.getStrings()) {
            StringTable2.StringInstance.Tag[] tags = str.getTags();
            if (tags != null) {
                for (StringTable2.StringInstance.Tag tag : tags) {
                    int si = tag.tagIndex();
                    si = stringsRemap[si];
                    tag.setTagIndex(si);
                }
            }
        }
    }

    private void removeUnusedConfigs() {
        // translate
        for (Package2 pkg : packages) {
            for (int j = 0; j < pkg.getContent().size(); j++) {
                if (pkg.getContent().get(j) instanceof Type2) {
                    Type2 type = (Type2) pkg.getContent().get(j);
                    continue;
                }
                if (!(pkg.getContent().get(j) instanceof Config2)) {
                    continue;
                }
                Config2 c = (Config2) pkg.getContent().get(j);
                boolean configToTranslate = c.getFlags().getLocale().equals("")
                        && TRANSLATE_TYPES.contains(c.getParentType().getName());
                boolean configToLeave = LEAVE_LOCALES.contains(c.getFlags().getLocale());
                if (!configToLeave) {
                    // remove config of non-required locale
                    pkg.getContent().remove(j);
                    j--;
                }
            }
        }
    }

    private void translate(String packageName, Translation translation) {
        for (Package2 pkg : packages) {
            for (Type2 t : pkg.getAllTypes()) {
                Assert.assertTrue("Unknown type: " + t.getName(), KNOWN_TYPES.contains(t.getName()));
            }
            for (int j = 0; j < pkg.getContent().size(); j++) {
                if (pkg.getContent().get(j) instanceof Type2) {
                    Type2 type = (Type2) pkg.getContent().get(j);
                    continue;
                }
                if (!(pkg.getContent().get(j) instanceof Config2)) {
                    continue;
                }
                Config2 c = (Config2) pkg.getContent().get(j);
                boolean configToTranslate = c.getFlags().getLocale().equals("")
                        && TRANSLATE_TYPES.contains(c.getParentType().getName());
                if (!configToTranslate) {
                    continue;
                }

                Config2 newConfig = c.duplicate();
                newConfig.getFlags().setLocale("be");
                pkg.getContent().add(j + 1, newConfig);

                for (int e = 0; e < newConfig.getEntriesCount(); e++) {
                    Entry2 ee = newConfig.getEntry(e);
                    if (ee == null) {
                        continue;
                    }
                    if (ee.isComplex()) {
                        for (Entry2.KeyValue kv : ee.getKeyValues()) {
                            int si = kv.getComplexStringIndex();
                            if (si >= 0) {
                                String name = ee.getName();
                                // may be plural ?
                                if (kv.key >= BAG_KEY_PLURALS_START && kv.key <= BAG_KEY_PLURALS_END) {
                                    name += '/' + QUANTITY_MAP[kv.key - BAG_KEY_PLURALS_START];
                                } else {
                                    // array ?
                                    name += "*array";
                                }
                                // translate
                                int newsi = translateString(translation, packageName, name, si);
                                kv.setComplexStringIndex(newsi);
                            }
                        }
                    } else {
                        int si = ee.getSimpleStringIndex();
                        if (si >= 0) {
                            // translate
                            int newsi = translateString(translation, packageName, ee.getName(), si);
                            ee.setSimpleStringIndex(newsi);
                        }
                    }
                }
            }
        }
    }

    private int translateString(Translation tr, String packageName, String entryName, int origStringIndex) {
        // translate
        StringTable2.StringInstance source = globalStringTable.getStrings().get(origStringIndex);

        StyledString orig = source.getStyledString();

        StyledString trans = tr.getTranslation(packageName, entryName, orig);
        if (trans != null) {
            return globalStringTable.addString(trans);
        } else {
            return origStringIndex;
        }
    }

    private void markUsedStrings() {
        int stringsCount = globalStringTable.getStringCount();

        stringsUsed = new BitSet(stringsCount);
        stringsRemap = new int[stringsCount];
        for (int i = 0; i < stringsRemap.length; i++) {
            stringsRemap[i] = -1;
        }

        // check used strings
        for (Package2 pkg : packages) {
            for (Type2 t : pkg.getAllTypes()) {
                Assert.assertTrue("Unknown type: " + t.getName(), KNOWN_TYPES.contains(t.getName()));
            }
            for (int j = 0; j < pkg.getContent().size(); j++) {
                if (!(pkg.getContent().get(j) instanceof Config2)) {
                    continue;
                }
                Config2 c = (Config2) pkg.getContent().get(j);
                boolean configToLeave = LEAVE_LOCALES.contains(c.getFlags().getLocale());

                for (int e = 0; e < c.getEntriesCount(); e++) {
                    Entry2 ee = c.getEntry(e);
                    if (ee == null) {
                        continue;
                    }
                    if (ee.isComplex()) {
                        for (Entry2.KeyValue kv : ee.getKeyValues()) {
                            int si = kv.getComplexStringIndex();
                            if (si >= 0) {
                                stringsUsed.set(si);
                                if (configToLeave) {
                                    stringsRemap[si] = si;
                                }
                            }
                        }
                    } else {
                        int si = ee.getSimpleStringIndex();
                        if (si >= 0) {
                            stringsUsed.set(si);
                            if (configToLeave) {
                                stringsRemap[si] = si;
                            }
                        }
                    }
                }
            }
        }

        // find tags for strings
        for (int i = 0; i < globalStringTable.getStringCount(); i++) {
            StringTable2.StringInstance.Tag[] tags = globalStringTable.getStrings().get(i).getTags();
            if (tags != null) {
                for (StringTable2.StringInstance.Tag tag : tags) {
                    stringsUsed.set(tag.tagIndex());
                    if (stringsRemap[i] >= 0) {
                        stringsRemap[tag.tagIndex()] = tag.tagIndex();
                    }
                }
            }
        }
    }
    
    public interface Callback {
        void onGlobalStringTable(ChunkMapper stringChunk);
    }
}
