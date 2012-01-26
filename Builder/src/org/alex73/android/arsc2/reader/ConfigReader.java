package org.alex73.android.arsc2.reader;

import java.math.BigInteger;

import org.alex73.android.Assert;
import org.alex73.android.StyledString;
import org.alex73.android.arsc2.Config2;
import org.alex73.android.arsc2.Config2.Flags;
import org.alex73.android.arsc2.Entry2;
import org.alex73.android.arsc2.Package2;

import android.util.TypedValue;

public class ConfigReader {
//    public static Config2 read(ChunkReader2 rd, Package2 pkg) {
//        Assert.assertTrue("Config2 header", rd.header.chunkType == 0x201);
//        Assert.assertTrue("Config2 header2", rd.header.chunkType2 == 52);
//
//        Config2 config = new Config2();
//
//        config.typeId = rd.readInt();
//        int entryCount = rd.readInt();
//        int entriesStart = rd.readInt();
//
//        config.parent = pkg.getTypeById(config.typeId);
//        Assert.assertNotNull("Type not exist for config", config.parent);
//
//        config.flags = readConfigFlags(rd, config);
//
//        config.entries = new Entry2[entryCount];
//        int[] entryOffsets = rd.readIntArray(entryCount);
//        for (int i = 0; i < entryOffsets.length; i++) {
//            if (entryOffsets[i] != -1) {
//                // getParentType().mMissingResSpecs[i] = false;
//
//                int mResId = config.getParentType().getParentPackage().getId() << 24;// mResId of package
//                mResId = (0xff000000 & mResId) | config.getParentType().getId() << 16;// mResId of type
//                mResId = (mResId & 0xffff0000) | i;// mResId of entry
//
//                config.entries[i] = readEntry(rd, config, mResId);
//            }
//        }
//        return config;
//    }

    /**
     * Config structure described at
     * https://github.com/CyanogenMod/android_frameworks_base/blob/ics/include/utils/ResourceTypes.h
     */
    protected static Flags readConfigFlags(ChunkReader2 rd, Config2 config) {
        Flags f = new Flags();
        f.size = rd.readInt();
        Assert.assertTrue("Wrong config flags", f.size >= 28);

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
        Assert.assertEquals("Config flags fill", 0, rd.readByte());// was skip

        f.screenWidth = rd.readShort();
        f.screenHeight = rd.readShort();

        f.sdkVersion = rd.readShort();
        Assert.assertEquals("Config flags fill", 0, rd.readShort());// was skip

        f.screenLayout = 0;
        f.uiMode = 0;
        if (f.size >= 32) {
            f.screenLayout = rd.readByte();
            f.uiMode = rd.readByte();
            f.smallestScreenWidthDp = rd.readShort();
        }

        int exceedingSize = f.size - Config2.KNOWN_CONFIG_BYTES;
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

    protected static Entry2 readEntry(ChunkReader2 rd, Config2 config, int mResId) {
        int entryID = mResId;

        short sz = rd.readShort();
        short entryFlags = rd.readShort();
        int specNameIndex = rd.readInt();

        Entry2 e;
        if ((entryFlags & Config2.ENTRY_FLAG_COMPLEX) == 0) {
            Assert.assertEquals("Wrong simple value", sz, 8);
            // simple value
            Entry2.SimpleEntry2 es = new Entry2.SimpleEntry2();
            e = es;

            short vSize = rd.readShort();// mIn.skipCheckShort((short) 8);
            Assert.assertEquals("Wrong simple value", 8, vSize);

            byte vZero = rd.readByte();
            Assert.assertEquals("Wrong simple value", 0, vZero);

            es.vType = rd.readByte();
            es.vData = rd.readInt();

            es.styledStringValue = readValue(rd, config, vSize, es.vType, es.vData);
        } else {
            Assert.assertEquals("Wrong complex value", sz, 16);
            // complex value
            Entry2.ComplexEntry2 ec = new Entry2.ComplexEntry2();
            e = ec;

            ec.vParent = rd.readInt();

            readComplexEntry(rd, config, ec);
        }

        e.id = entryID;
        e.flags = entryFlags;
        e.strings = config.getParentType().getParentPackage().getSpecNames();
        e.specNameIndex = specNameIndex;

        return e;
    }

    private static void readComplexEntry(ChunkReader2 rd, Config2 config, Entry2.ComplexEntry2 e) {
        int count = rd.readInt();

        e.values = new Entry2.KeyValue[count];

        for (int i = 0; i < e.values.length; i++) {
            e.values[i] = new Entry2.KeyValue();

            e.values[i].key = rd.readInt();

            short vSize = rd.readShort();// mIn.skipCheckShort((short) 8);
            Assert.assertEquals("Wrong complex entry", 8, vSize);

            byte vZero = rd.readByte();
            Assert.assertEquals("Wrong complex entry", 0, vZero);

            e.values[i].vType = rd.readByte();
            e.values[i].vData = rd.readInt();

            e.values[i].value = readValue(rd, config, vSize, e.values[i].vType, e.values[i].vData);
        }
    }

    private static StyledString readValue(ChunkReader2 rd, Config2 config, short size, byte type, int data) {
        if (type == TypedValue.TYPE_STRING) {
            return config.getParentType().getParentPackage().getGlobalStringTable().getStyledString(data);
        } else {
            return null;
        }
    }
}
