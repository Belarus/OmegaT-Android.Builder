package org.alex73.android.arsc2.writer;

import org.alex73.android.arsc2.Config2;
import org.alex73.android.arsc2.Config2.Flags;
import org.alex73.android.arsc2.Entry2;

public class ConfigWriter {
    public byte[] write(Config2 config) {
        ChunkWriter2 wr = new ChunkWriter2(0x201, 52);

        wr.writeInt(config.typeId);
        wr.writeInt(config.entries.length);

        ChunkWriter2.LaterInt entriesStartOffset = wr.new LaterInt();

        writeConfigFlags(wr, config.flags);

        ChunkWriter2.LaterInt[] entriesOffsets = new ChunkWriter2.LaterInt[config.entries.length];
        for (int i = 0; i < entriesOffsets.length; i++) {
            entriesOffsets[i] = wr.new LaterInt();
        }

        entriesStartOffset.setValue(wr.pos());
        for (int i = 0; i < config.entries.length; i++) {
            if (config.entries[i] == null) {
                entriesOffsets[i].setValue(-1);
            } else {
                entriesOffsets[i].setValue(wr.pos() - entriesStartOffset.getValue());
                writeEntry(wr, config, i);
            }
        }

        wr.close();

        return wr.getBytes();
    }

    protected void writeConfigFlags(ChunkWriter2 wr, Flags f) {
        wr.writeInt(f.size);

        wr.writeShort(f.mcc);
        wr.writeShort(f.mnc);

        wr.write(f.language[0]);
        wr.write(f.language[1]);
        wr.write(f.country[0]);
        wr.write(f.country[1]);

        wr.write(f.orientation);
        wr.write(f.touchscreen);
        wr.writeShort(f.density);

        wr.write(f.keyboard);
        wr.write(f.navigation);
        wr.write(f.inputFlags);
        wr.write(0);

        wr.writeShort(f.screenWidth);
        wr.writeShort(f.screenHeight);

        wr.writeShort(f.sdkVersion);
        wr.writeShort((short) 0);

        if (f.size >= 32) {
            wr.write(f.screenLayout);
            wr.write(f.uiMode);
            wr.writeShort(f.smallestScreenWidthDp);
        }
        if (f.exceeded != null) {
            wr.write(f.exceeded);
        }
    }

    protected void writeEntry(ChunkWriter2 wr, Config2 config, int entryIndex) {
        Entry2 e = config.entries[entryIndex];

        ChunkWriter2.LaterShort laterSize = wr.new LaterShort();
        int sz;

        wr.writeShort(e.flags);

        wr.writeInt(e.specNameIndex);

        if ((e.flags & Config2.ENTRY_FLAG_COMPLEX) == 0) {
            Entry2.SimpleEntry2 es = (Entry2.SimpleEntry2) e;
            wr.writeShort((short) 8);// size
            wr.write(0);
            wr.write(es.vType);
            wr.writeInt(es.vData);
            sz = 8;
        } else {
            Entry2.ComplexEntry2 ec = (Entry2.ComplexEntry2) e;

            wr.writeInt(ec.vParent);
            writeComplexEntry(ec, wr);
            sz = 16;
        }

        laterSize.setValue((short) sz);
    }

    private void writeComplexEntry(Entry2.ComplexEntry2 e, ChunkWriter2 wr) {
        wr.writeInt(e.values.length);

        for (int i = 0; i < e.values.length; i++) {
            wr.writeInt(e.values[i].key);
            wr.writeShort((short) 8);// size
            wr.write(0);

            wr.write(e.values[i].vType);
            wr.writeInt(e.values[i].vData);
        }
    }
}
