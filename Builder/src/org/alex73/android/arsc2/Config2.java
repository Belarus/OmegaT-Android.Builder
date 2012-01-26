package org.alex73.android.arsc2;

import java.math.BigInteger;

import org.alex73.android.Assert;
import org.alex73.android.arsc2.reader.ChunkMapper;

/**
 * Config structure described at
 * https://github.com/CyanogenMod/android_frameworks_base/blob/ics/include/utils/ResourceTypes.h
 */
public class Config2 {
    public final static short ENTRY_FLAG_COMPLEX = 0x0001;

    public static final int KNOWN_CONFIG_BYTES = 32;

    public Type2 parent;

    public int typeId;
    private int entryCount;
    private int entriesStart;
    private int entriesOffsetsStart;

    public Flags flags;

    private final ChunkMapper rd;

    public Config2(ChunkMapper rd, Package2 pkg) {
        this.rd = rd;

        typeId = rd.readInt();
        entryCount = rd.readInt();
        entriesStart = rd.readInt();

        parent = pkg.getTypeById(typeId);
        Assert.assertNotNull("Type not exist for config", parent);

        flags = new Flags();
        entriesOffsetsStart = rd.getPos();
        rd.skip(rd.left());
    }

    public int getEntriesCount() {
        return entryCount;
    }

    public Entry2 getEntry(int index) {
        Assert.assertTrue("Entries out of bound", index >= 0 && index < entryCount);

        // find offset of entry
        rd.setPos(entriesOffsetsStart + index * 4);
        int entryOffset = rd.readInt();
        if (entryOffset == -1) {
            return null;
        }
        rd.setPos(entriesStart + entryOffset);

        // read entry
        int mResId = parent.getParentPackage().getId() << 24;// mResId of package
        mResId = (0xff000000 & mResId) | parent.getId() << 16;// mResId of type
        mResId = (mResId & 0xffff0000) | index;// mResId of entry

        return new Entry2(rd, mResId, parent.getParentPackage().getSpecNames(), parent.getParentPackage()
                .getGlobalStringTable());
    }

    public class Flags {
        public int startPos;
        public int size;

        boolean empty;

        public Flags() {
            startPos = rd.getPos();
            size = rd.readInt();
            Assert.assertTrue("", size >= 28);

            short mcc = rd.readShort();
            short mnc = rd.readShort();

            char[] language = new char[] { (char) rd.readByte(), (char) rd.readByte() };
            char[] country = new char[] { (char) rd.readByte(), (char) rd.readByte() };

            byte orientation = rd.readByte();
            byte touchscreen = rd.readByte();
            short density = rd.readShort();

            byte keyboard = rd.readByte();
            byte navigation = rd.readByte();
            byte inputFlags = rd.readByte();
            Assert.assertEquals("", 0, rd.readByte());// was skip

            short screenWidth = rd.readShort();
            short screenHeight = rd.readShort();

            short sdkVersion = rd.readShort();
            Assert.assertEquals("", 0, rd.readShort());// was skip

            byte screenLayout = 0;
            byte uiMode = 0;
            short smallestScreenWidthDp = 0;
            if (size >= 32) {
                screenLayout = rd.readByte();
                uiMode = rd.readByte();
                smallestScreenWidthDp = rd.readShort();
            }

            int exceedingSize = size - KNOWN_CONFIG_BYTES;
            boolean allExceedZeros = true;
            if (exceedingSize > 0) {
                byte[] exceeded = new byte[exceedingSize];
                rd.readFully(exceeded);
                for (byte b : exceeded) {
                    if (b != 0) {
                        allExceedZeros = false;
                    }
                }
                BigInteger exceedingBI = new BigInteger(exceeded);

                if (exceedingBI.equals(BigInteger.ZERO)) {
                    // Config flags size > 32, but exceeding bytes are all zero, so it should be ok.");
                } else {
                    // Config flags size > 32. Exceeding bytes: " + exceedingBI + (exceedingSize * 2) + "X.");
                }
            }

            empty = mcc == 0 && mnc == 0 && language[0] == 0 && language[1] == 0 && country[0] == 0
                    && country[1] == 0 && orientation == 0 && touchscreen == 0 && density == 0
                    && keyboard == 0 && navigation == 0 && inputFlags == 0 && screenWidth == 0
                    && screenHeight == 0 && sdkVersion == 0 && screenLayout == 0 && uiMode == 0
                    && smallestScreenWidthDp == 0 && allExceedZeros;
        }

        public String getLocale() {
            rd.setPos(startPos + 8);
            char[] language = new char[] { (char) rd.readByte(), (char) rd.readByte() };
            char[] country = new char[] { (char) rd.readByte(), (char) rd.readByte() };

            StringBuilder locale = new StringBuilder(5);
            if (language[0] != 0) {
                locale.append(language);
            }
            if (country[0] != 0) {
                locale.append('-');
                locale.append(country);
            }
            return locale.toString();
        }

        public void setLocale(String locale) {
            char[] language = new char[2];
            char[] country = new char[2];
            if (locale.matches("^[a-z]{2}\\-[A-Z]{2}$")) {
                language[0] = locale.charAt(0);
                language[1] = locale.charAt(1);
                country[0] = locale.charAt(3);
                country[1] = locale.charAt(4);
            } else if (locale.matches("^[a-z]{2}$")) {
                language[0] = locale.charAt(0);
                language[1] = locale.charAt(1);
                country[0] = 0;
                country[1] = 0;
            } else if (locale.equals("")) {
                language[0] = 0;
                language[1] = 0;
                country[0] = 0;
                country[1] = 0;
            } else {
                Assert.fail("Wrong locale for config");
            }

            rd.setPos(startPos + 8);
            rd.writeByte((byte) language[0]);
            rd.writeByte((byte) language[1]);
            rd.writeByte((byte) country[0]);
            rd.writeByte((byte) country[1]);
        }

        public boolean isEmpty() {
            return empty;
        }
    }

    public Type2 getParentType() {
        return parent;
    }

    public Flags getFlags() {
        return flags;
    }

    public Config2 duplicate() {
        return null;
    }
}
