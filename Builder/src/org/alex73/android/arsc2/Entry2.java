package org.alex73.android.arsc2;

import org.alex73.android.Assert;
import org.alex73.android.StyledString;
import org.alex73.android.arsc2.reader.ChunkMapper;

import android.util.TypedValue;

public class Entry2 {
    private final static short ENTRY_FLAG_COMPLEX = 0x0001;

    public short flags;

    private final ChunkMapper rd;
    private final int resId;
    private final int startOffset;
    private final StringTable2 packageSpecNames;
    private final StringTable2 globalStrings;

    private final int specNamesIndex;
    private final short entryFlags;

    public byte simpleType;
    public int simpleData;
    public int vParent;
    public int keyValueCount;

    public Entry2(ChunkMapper rd, int resId, StringTable2 packageSpecNames, StringTable2 globalStrings) {
        this.rd = rd;
        this.resId = resId;
        this.startOffset = rd.getPos();
        this.packageSpecNames = packageSpecNames;
        this.globalStrings = globalStrings;

        short sz = rd.readShort();
        entryFlags = rd.readShort();
        specNamesIndex = rd.readInt();

        if (isComplex()) {
            // complex value
            Assert.assertEquals("Wrong complex entry size", sz, 16);

            vParent = rd.readInt();

            keyValueCount = rd.readInt();
        } else {
            // simple value
            Assert.assertEquals("Wrong simple entry size", sz, 8);

            short vSize = rd.readShort();
            Assert.assertEquals("Wrong simple entry value size", 8, vSize);

            byte vZero = rd.readByte();
            Assert.assertEquals("Wrong simple entry zero", 0, vZero);

            simpleType = rd.readByte();
            simpleData = rd.readInt();
        }
    }

    public int getId() {
        return resId;
    }

    public boolean isComplex() {
        return (entryFlags & ENTRY_FLAG_COMPLEX) != 0;
    }

    public StyledString getSimpleStringValue() {
        switch (simpleType) {
        case TypedValue.TYPE_STRING:
            return globalStrings.getStyledString(simpleData);
        case TypedValue.TYPE_ATTRIBUTE:
        case TypedValue.TYPE_FRACTION:
        case TypedValue.TYPE_FLOAT:
        case TypedValue.TYPE_INT_BOOLEAN:
        case TypedValue.TYPE_INT_HEX:
        case TypedValue.TYPE_INT_COLOR_ARGB8:
        case TypedValue.TYPE_INT_COLOR_RGB8:
        case TypedValue.TYPE_DIMENSION:
        case TypedValue.TYPE_FIRST_INT:
        case TypedValue.TYPE_REFERENCE:
        case TypedValue.TYPE_LAST_COLOR_INT:
        case TypedValue.TYPE_INT_COLOR_ARGB4:
            return null;
        default:
            throw new RuntimeException("Unknown entry type: " + simpleType);
        }
    }

    public int getSimpleStringIndex() {
        switch (simpleType) {
        case TypedValue.TYPE_STRING:
            return simpleData;
        case TypedValue.TYPE_ATTRIBUTE:
        case TypedValue.TYPE_FRACTION:
        case TypedValue.TYPE_FLOAT:
        case TypedValue.TYPE_INT_BOOLEAN:
        case TypedValue.TYPE_INT_HEX:
        case TypedValue.TYPE_INT_COLOR_ARGB8:
        case TypedValue.TYPE_INT_COLOR_RGB8:
        case TypedValue.TYPE_DIMENSION:
        case TypedValue.TYPE_FIRST_INT:
        case TypedValue.TYPE_REFERENCE:
        case TypedValue.TYPE_LAST_COLOR_INT:
        case TypedValue.TYPE_INT_COLOR_ARGB4:
            return -1;
        default:
            throw new RuntimeException("Unknown entry type: " + simpleType);
        }
    }

    public int getKeyValueCount() {
        return keyValueCount;
    }

    public KeyValue getKeyValue(int index) {
        Assert.assertTrue("Wrong keyvalue index", index >= 0 && index < keyValueCount);
        KeyValue kv = new KeyValue();

        rd.setPos(startOffset + 16);

        kv.key = rd.readInt();

        short vSize = rd.readShort();// mIn.skipCheckShort((short) 8);
        Assert.assertEquals("Wrong complex entry kv size", vSize, 8);

        byte vZero = rd.readByte();
        Assert.assertEquals("Wrong complex entry kv zero", vZero, 0);

        kv.complexType = rd.readByte();
        kv.complexData = rd.readInt();

        return kv;
    }

    public String getName() {
        return packageSpecNames.getString(specNamesIndex);
    }

    public class KeyValue {
        public int key;
        public byte complexType;
        public int complexData;

        public StyledString getStringValue() {
            switch (complexType) {
            case TypedValue.TYPE_STRING:
                return globalStrings.getStyledString(complexType);
            case TypedValue.TYPE_ATTRIBUTE:
            case TypedValue.TYPE_FRACTION:
            case TypedValue.TYPE_FLOAT:
            case TypedValue.TYPE_INT_BOOLEAN:
            case TypedValue.TYPE_INT_HEX:
            case TypedValue.TYPE_INT_COLOR_ARGB8:
            case TypedValue.TYPE_INT_COLOR_RGB8:
            case TypedValue.TYPE_DIMENSION:
            case TypedValue.TYPE_FIRST_INT:
            case TypedValue.TYPE_REFERENCE:
            case TypedValue.TYPE_LAST_COLOR_INT:
            case TypedValue.TYPE_INT_COLOR_ARGB4:
                return null;
            default:
                throw new RuntimeException("Unknown entry type: " + complexType);
            }
        }

        public int getStringIndex() {
            switch (complexType) {
            case TypedValue.TYPE_STRING:
                return complexType;
            case TypedValue.TYPE_ATTRIBUTE:
            case TypedValue.TYPE_FRACTION:
            case TypedValue.TYPE_FLOAT:
            case TypedValue.TYPE_INT_BOOLEAN:
            case TypedValue.TYPE_INT_HEX:
            case TypedValue.TYPE_INT_COLOR_ARGB8:
            case TypedValue.TYPE_INT_COLOR_RGB8:
            case TypedValue.TYPE_DIMENSION:
            case TypedValue.TYPE_FIRST_INT:
            case TypedValue.TYPE_REFERENCE:
            case TypedValue.TYPE_LAST_COLOR_INT:
            case TypedValue.TYPE_INT_COLOR_ARGB4:
                return -1;
            default:
                throw new RuntimeException("Unknown entry type: " + complexType);
            }
        }
    }
}
