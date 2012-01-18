package org.alex73.android.arsc;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;

public class Package extends BaseChunked {
    private int id;
    private String name;
    private StringTable typeNames;
    private StringTable specNames;

    /** List of child chunks. They may be Type's and Config's. */
    private List<BaseChunked> content = new ArrayList<BaseChunked>();

    private Resources parent;

    public Resources getParentResources() {
        return parent;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<BaseChunked> getContent() {
        return content;
    }

    public Package(Resources parent, ChunkReader rd) throws Exception {
        super(rd);

        if (rd.header.chunkType != 0x200) {
            throw new Exception("Package chunk");
        }
        this.parent = parent;

        id = rd.readInt();
        Assert.assertTrue(id > 0 && id < 255);

        name = rd.readNulEndedString(128, true);
        int typeNameStrings = rd.readInt();
        int typeNameCount = rd.readInt();
        int specNameStrings = rd.readInt();
        int specNameCount = rd.readInt();

        typeNames = new StringTable(new ChunkReader(rd));
        typeNames.read();

        specNames = new StringTable(new ChunkReader(rd));
        specNames.read();

        Assert.assertEquals(typeNameCount, typeNames.getStringCount());
        Assert.assertEquals(specNameCount, specNames.getStringCount());

        while (rd.left() > 0) {
            ChunkReader rd2 = new ChunkReader(rd);
            switch (rd2.header.chunkType) {
            case 0x202:
                Type t = new Type(this, rd2);
                content.add(t);
                break;
            case 0x201:
                Config c = new Config(rd2);
                c.read(this);
                content.add(c);
                break;
            default:
                throw new Exception("Unknown chunk in Package");
            }
        }
        rd.close();
    }

    public ChunkWriter write() {
        ChunkWriter wr = new ChunkWriter(rd);

        wr.writeInt(id);
        wr.writeNulEndedString(128, name);

        ChunkWriter.LaterInt typeNameStringsOffset = wr.new LaterInt();
        wr.writeInt(typeNames.getStringCount());

        ChunkWriter.LaterInt specNameStringsOffset = wr.new LaterInt();
        wr.writeInt(specNames.getStringCount());

        typeNameStringsOffset.setValue(wr.pos());
        ChunkWriter wrTypes = typeNames.write();
        wr.write(wrTypes.getBytes());

        specNameStringsOffset.setValue(wr.pos());
        ChunkWriter wrSpecs = specNames.write();
        wr.write(wrSpecs.getBytes());

        for (BaseChunked c : content) {
            if (c instanceof Type) {
                wr.write(c.getOriginalBytes());
            } else {
                ChunkWriter w = c.write();
                wr.write(w.getBytes());
            }
        }

        wr.close();

        return wr;
    }

    public Type getTypeById(int typeId) {
        for (Object o : content) {
            if (o instanceof Type) {
                Type t = (Type) o;
                if (t.id == typeId) {
                    return t;
                }
            }
        }
        return null;
    }

    public List<Type> getAllTypes() {
        List<Type> result = new ArrayList<Type>();
        for (Object o : content) {
            if (o instanceof Type) {
                result.add((Type) o);
            }
        }
        return result;
    }

    public List<Config> getAllConfigs() {
        List<Config> result = new ArrayList<Config>();
        for (Object o : content) {
            if (o instanceof Config) {
                result.add((Config) o);
            }
        }
        return result;
    }

    public StringTable getSpecNames() {
        return specNames;
    }

    public StringTable getTypeNames() {
        return typeNames;
    }
}
