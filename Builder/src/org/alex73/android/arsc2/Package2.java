package org.alex73.android.arsc2;

import java.util.ArrayList;
import java.util.List;

import org.alex73.android.Assert;
import org.alex73.android.arsc2.reader.ChunkMapper;

public class Package2 {
    private final StringTable2 globalStringTable;
    private int id;
    private String name;
    private StringTable2 typeNames;
    private StringTable2 specNames;

    /** List of child chunks. They may be Type's and Config's. */
    private List<Object> content = new ArrayList<Object>();

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public StringTable2 getGlobalStringTable() {
        return globalStringTable;
    }

    public Package2(StringTable2 globalStringTable, ChunkMapper rd) {

        Assert.assertTrue("Package chunk", rd.header.chunkType == 0x200);

        this.globalStringTable = globalStringTable;

        id = rd.readInt();
        Assert.assertTrue("Wrong package id", id > 0 && id < 255);

        name = rd.readNulEndedString(128, true);
        System.out.println("    package: " + name);
        int typeNameStrings = rd.readInt();
        int typeNameCount = rd.readInt();
        int specNameStrings = rd.readInt();
        int specNameCount = rd.readInt();

        typeNames = new StringTable2();
        typeNames.read(rd.readChunk());

        specNames = new StringTable2();
        specNames.read(rd.readChunk());

        Assert.assertEquals("Package types not equals", typeNameCount, typeNames.getStringCount());
        Assert.assertEquals("Package specs not equals", specNameCount, specNames.getStringCount());

        while (rd.left() > 0) {
            ChunkMapper chunk = rd.readChunk();
            switch (chunk.header.chunkType) {
            case 0x202:
                Type2 t = new Type2(this, chunk);
                content.add(t);
                break;
            case 0x201:
                Config2 c = new Config2(chunk, this);
                content.add(c);
                break;
            default:
                Assert.fail("Unknown chunk in Package");
            }
        }
    }

    public Type2 getTypeById(int typeId) {
        for (Object o : content) {
            if (o instanceof Type2) {
                Type2 t = (Type2) o;
                if (t.getId() == typeId) {
                    return t;
                }
            }
        }
        return null;
    }

    public StringTable2 getSpecNames() {
        return specNames;
    }

    public StringTable2 getTypeNames() {
        return typeNames;
    }

    public List<Type2> getAllTypes() {
        List<Type2> result = new ArrayList<Type2>();
        for (Object o : content) {
            if (o instanceof Type2) {
                result.add((Type2) o);
            }
        }
        return result;
    }

    public List<Config2> getAllConfigs() {
        List<Config2> result = new ArrayList<Config2>();
        for (Object o : content) {
            if (o instanceof Config2) {
                result.add((Config2) o);
            }
        }
        return result;
    }

    public List<Object> getContent() {
        return content;
    }
}
