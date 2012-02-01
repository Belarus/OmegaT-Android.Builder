package org.alex73.android.arsc2;

import org.alex73.android.Assert;
import org.alex73.android.arsc2.reader.ChunkMapper;

import android.util.TypedValue;

public class Entry2 {
    private final static short ENTRY_FLAG_COMPLEX = 0x0001;

    public short flags;

    private final ChunkMapper rd;
    private final int resId;
    private final StringTable2 packageSpecNames;

    private final int specNamesIndex;
    private final short entryFlags;

    public byte simpleType;
    public int simpleData;
    public int simpleDataPos;
    public int vParent;
    public KeyValue keyValues[];

    public Entry2(ChunkMapper rd, int resId, StringTable2 packageSpecNames) {
        this.rd = rd;
        this.resId = resId;
        this.packageSpecNames = packageSpecNames;

        short sz = rd.readShort();
        entryFlags = rd.readShort();
        specNamesIndex = rd.readInt();

        if (isComplex()) {
            // complex value
            Assert.assertEquals("Wrong complex entry size", sz, 16);

            vParent = rd.readInt();

            int keyValueCount = rd.readInt();
            keyValues = new KeyValue[keyValueCount];
            for (int i = 0; i < keyValues.length; i++) {
                keyValues[i] = readComplex();
            }
        } else {
            // simple value
            Assert.assertEquals("Wrong simple entry size", sz, 8);

            short vSize = rd.readShort();
            Assert.assertEquals("Wrong simple entry value size", 8, vSize);

            byte vZero = rd.readByte();
            Assert.assertEquals("Wrong simple entry zero", 0, vZero);

            simpleType = rd.readByte();
            simpleDataPos = rd.getPos();
            simpleData = rd.readInt();
        }
    }

    private KeyValue readComplex() {
        KeyValue kv = new KeyValue();

        kv.key = rd.readInt();

        short vSize = rd.readShort();// mIn.skipCheckShort((short) 8);
        Assert.assertEquals("Wrong complex entry kv size", vSize, 8);

        byte vZero = rd.readByte();
        Assert.assertEquals("Wrong complex entry kv zero", vZero, 0);

        kv.complexType = rd.readByte();
        kv.comlexDataPos = rd.getPos();
        kv.complexData = rd.readInt();

        return kv;
    }

    public int getId() {
        return resId;
    }

    public boolean isComplex() {
        return (entryFlags & ENTRY_FLAG_COMPLEX) != 0;
    }

    public int getSimpleStringIndex() {
        return getStringIndex(simpleType, simpleData);
    }

    public void setSimpleStringIndex(int newIndex) {
        rd.setPos(simpleDataPos);
        rd.writeInt(newIndex);
        simpleData = newIndex;
    }

    public KeyValue[] getKeyValues() {
        return keyValues;
    }

    public String getName() {
        return packageSpecNames.getStrings().get(specNamesIndex).getRawString();
    }

    private int getStringIndex(byte vType, int vData) {
        switch (vType) {
        case TypedValue.TYPE_STRING:
            return vData;
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
            throw new RuntimeException("Unknown entry type: " + vType);
        }
    }

    public class KeyValue {
        public int key;
        public byte complexType;
        public int comlexDataPos;
        public int complexData;

        public int getComplexStringIndex() {
            return getStringIndex(complexType, complexData);
        }

        public void setComplexStringIndex(int newIndex) {
            rd.setPos(comlexDataPos);
            rd.writeInt(newIndex);
            complexData = newIndex;
        }
    }
}
