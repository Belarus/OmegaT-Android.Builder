package org.alex73.android.arsc;

import java.math.BigInteger;

import org.alex73.android.Assert;
import org.alex73.android.StyledString;

import android.util.TypedValue;
import brut.androlib.res.data.ResConfigFlags;

public class Config extends BaseChunked {
    private final static short ENTRY_FLAG_COMPLEX = 0x0001;

    private static final int KNOWN_CONFIG_BYTES = 32;

    private Type parent;

    private int typeId;

    private Flags flags;

    private Entry[] entries;

    public static class Flags {
        int size;

        short mcc;
        short mnc;

        char[] language;
        char[] country;

        byte orientation;
        byte touchscreen;
        short density;

        byte keyboard;
        byte navigation;
        byte inputFlags;

        short screenWidth;
        short screenHeight;

        short sdkVersion;

        byte screenLayout;
        byte uiMode;
        short smallestScreenWidthDp;

        byte[] exceeded;

        public String generateQualifiers() {
            ResConfigFlags f = new ResConfigFlags(mcc, mnc, language, country, orientation, touchscreen,
                    density, keyboard, navigation, inputFlags, screenWidth, screenHeight, sdkVersion,
                    screenLayout, uiMode, false);
            return f.getQualifiers();
        }

        public String getLanguage() {
            return language[0] == 0 ? "" : new String(language);
        }

        public void setLanguage(String lang) {
            if (lang.matches("^[a-z]{2}\\-[A-Z]{2}$")) {
                language[0] = lang.charAt(0);
                language[1] = lang.charAt(1);
                country[0] = lang.charAt(3);
                country[1] = lang.charAt(4);
            } else if (lang.matches("^[a-z]{2}$")) {
                language[0] = lang.charAt(0);
                language[1] = lang.charAt(1);
                country[0] = 0;
                country[1] = 0;
            } else {
                Assert.fail("");
            }
        }

        public boolean isEmpty() {
            boolean empty = mcc == 0 && mnc == 0 && language[0] == 0 && language[1] == 0 && country[0] == 0
                    && country[1] == 0 && orientation == 0 && touchscreen == 0 && density == 0
                    && keyboard == 0 && navigation == 0 && inputFlags == 0 && screenWidth == 0
                    && screenHeight == 0 && sdkVersion == 0 && screenLayout == 0 && uiMode == 0;
            if (empty && exceeded != null) {
                for (byte b : exceeded) {
                    if (b != 0) {
                        empty = false;
                    }
                }
            }
            return empty;
        }
    }

    public Type getParentType() {
        return parent;
    }

    public Flags getFlags() {
        return flags;
    }

    public Entry[] getEntries() {
        return entries;
    }

    public Config(ChunkReader rd) {
        super(rd);
    }

    public void read(Package pkg) {
        Assert.assertEquals("", rd.header.chunkType, 0x201);

        typeId = rd.readInt();
        int entryCount = rd.readInt();
        int entriesStart = rd.readInt();

        parent = pkg.getTypeById(typeId);
        Assert.assertNotNull("Type not exist for config", parent);

        flags = readConfigFlags();

        entries = new Entry[entryCount];
        int[] entryOffsets = rd.readIntArray(entryCount);
        for (int i = 0; i < entryOffsets.length; i++) {
            if (entryOffsets[i] != -1) {
                getParentType().mMissingResSpecs[i] = false;

                int mResId = getParentType().getParentPackage().getId() << 24;// mResId of package
                mResId = (0xff000000 & mResId) | getParentType().id << 16;// mResId of type
                mResId = (mResId & 0xffff0000) | i;// mResId of entry

                entries[i] = readEntry(mResId);
            }
        }

        rd.close();
    }

    int[] entriesOffsets;

    public ChunkWriter write() {
        ChunkWriter wr = new ChunkWriter(rd);

        wr.writeInt(typeId);
        wr.writeInt(entries.length);

        ChunkWriter.LaterInt entriesStartOffset = wr.new LaterInt();

        writeConfigFlags(wr, flags);

        ChunkWriter.LaterInt[] entriesOffsets = new ChunkWriter.LaterInt[entries.length];
        for (int i = 0; i < entriesOffsets.length; i++) {
            entriesOffsets[i] = wr.new LaterInt();
        }

        entriesStartOffset.setValue(wr.pos());
        for (int i = 0; i < entries.length; i++) {
            if (entries[i] == null) {
                entriesOffsets[i].setValue(-1);
            } else {
                entriesOffsets[i].setValue(wr.pos() - entriesStartOffset.getValue());
                writeEntry(wr, i);
            }
        }

        wr.close();

        return wr;
    }

    /**
     * Config structure described at
     * https://github.com/CyanogenMod/android_frameworks_base/blob/ics/include/utils/ResourceTypes.h
     */
    protected Flags readConfigFlags() {
        Flags f = new Flags();
        f.size = rd.readInt();
        Assert.assertTrue("", f.size >= 28);

        f.mcc = rd.readShort();
        f.mnc = rd.readShort();

        f.language = new char[] { (char) rd.readByte(), (char) rd.readByte() };
        f.country = new char[] { (char) rd.readByte(), (char) rd.readByte() };

        f.orientation = rd.readByte();
        f.touchscreen = rd.readByte();
        f.density = rd.readShort();

        f.keyboard = rd.readByte();
        f.navigation = rd.readByte();
        f.inputFlags = rd.readByte();
        Assert.assertEquals("", 0, rd.readByte());// was skip

        f.screenWidth = rd.readShort();
        f.screenHeight = rd.readShort();

        f.sdkVersion = rd.readShort();
        Assert.assertEquals("", 0, rd.readShort());// was skip

        f.screenLayout = 0;
        f.uiMode = 0;
        if (f.size >= 32) {
            f.screenLayout = rd.readByte();
            f.uiMode = rd.readByte();
            f.smallestScreenWidthDp = rd.readShort();
        }

        int exceedingSize = f.size - KNOWN_CONFIG_BYTES;
        if (exceedingSize > 0) {
            f.exceeded = new byte[exceedingSize];
            rd.readFully(f.exceeded);
            BigInteger exceedingBI = new BigInteger(f.exceeded);

            if (exceedingBI.equals(BigInteger.ZERO)) {
                // Config flags size > 32, but exceeding bytes are all zero, so it should be ok.");
            } else {
                // Config flags size > 32. Exceeding bytes: " + exceedingBI + (exceedingSize * 2) + "X.");
            }
        }
        return f;
    }

    protected void writeConfigFlags(ChunkWriter wr, Flags f) {
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

        if (f.size > 28) {
            wr.write(f.screenLayout);
            wr.write(f.uiMode);
            wr.writeShort(f.smallestScreenWidthDp);
        }
        if (f.exceeded != null) {
            wr.write(f.exceeded);
        }
    }

    protected Entry readEntry(int mResId) {
        int entryID = mResId;

        short sz = rd.readShort();
        short entryFlags = rd.readShort();
        int specNamesId = rd.readInt();

        String entryName = getParentType().getParentPackage().getSpecNames().getString(specNamesId);

        Entry e;
        if ((entryFlags & ENTRY_FLAG_COMPLEX) == 0) {
            Assert.assertEquals("", sz, 8);
            // simple value
            Entry.SimpleEntry es = new Entry.SimpleEntry();
            e = es;

            short vSize = rd.readShort();// mIn.skipCheckShort((short) 8);
            Assert.assertEquals("", 8, vSize);

            byte vZero = rd.readByte();
            Assert.assertEquals("", 0, vZero);

            es.vType = rd.readByte();
            es.vData = rd.readInt();

            es.styledStringValue = readValue(vSize, es.vType, es.vData);
        } else {
            Assert.assertEquals("", sz, 16);
            // complex value
            Entry.ComplexEntry ec = new Entry.ComplexEntry();
            e = ec;

            ec.vParent = rd.readInt();

            readComplexEntry(ec);
        }

        e.id = entryID;
        e.flags = entryFlags;
        e.name = entryName;

        return e;
    }

    protected void writeEntry(ChunkWriter wr, int entryIndex) {
        Entry e = entries[entryIndex];

        ChunkWriter.LaterShort laterSize = wr.new LaterShort();
        int sz;

        wr.writeShort(e.flags);

        int specNameId = getParentType().getParentPackage().getSpecNames().getStringIndex(e.name);
        wr.writeInt(specNameId);

        if ((e.flags & ENTRY_FLAG_COMPLEX) == 0) {
            Entry.SimpleEntry es = (Entry.SimpleEntry) e;
            wr.writeShort((short) 8);// size
            wr.write(0);
            wr.write(es.vType);
            wr.writeInt(es.vData);
            sz = 8;
        } else {
            Entry.ComplexEntry ec = (Entry.ComplexEntry) e;

            wr.writeInt(ec.vParent);
            writeComplexEntry(ec, wr);
            sz = 16;
        }

        laterSize.setValue((short) sz);
    }

    private StyledString readValue(short size, byte type, int data) {
        if (type == TypedValue.TYPE_STRING) {
            return getParentType().getParentPackage().getParentResources().getStringTable()
                    .getStyledString(data);
        } else {
            return null;
        }
    }

    private void readComplexEntry(Entry.ComplexEntry e) {
        int count = rd.readInt();

        e.values = new Entry.KeyValue[count];

        for (int i = 0; i < e.values.length; i++) {
            e.values[i] = new Entry.KeyValue();

            e.values[i].key = rd.readInt();

            short vSize = rd.readShort();// mIn.skipCheckShort((short) 8);
            Assert.assertEquals("", 8, vSize);

            byte vZero = rd.readByte();
            Assert.assertEquals("", 0, vZero);

            e.values[i].vType = rd.readByte();
            e.values[i].vData = rd.readInt();

            e.values[i].value = readValue(vSize, e.values[i].vType, e.values[i].vData);
        }
    }

    private void writeComplexEntry(Entry.ComplexEntry e, ChunkWriter wr) {
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
