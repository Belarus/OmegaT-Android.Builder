package org.alex73.android.arsc2.translation;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.zip.GZIPInputStream;

import org.alex73.android.StyledIdString;
import org.alex73.android.StyledString;
import org.alex73.android.bel.R;

import android.content.res.Resources;

public class TranslationStorePackage {
    int[] plainStringOffsets;
    byte[] text;
    int[] styledStringHash, shortsOffsets;
    short[] shorts;

    public TranslationStorePackage(Resources res, String packageName) throws Exception {
        int resID = getResourceID(packageName);
        if (resID == 0) {
            return;
        }
        InputStream inTr = res.openRawResource(resID);
        try {
            DataInputStream data = new DataInputStream(new BufferedInputStream(new GZIPInputStream(inTr), 16384));

            int plainStringsCount = data.readInt();
            plainStringOffsets = new int[plainStringsCount];
            for (int i = 0; i < plainStringsCount; i++) {
                plainStringOffsets[i] = data.readInt();
            }

            int textSize = data.readInt();
            text = new byte[textSize];
            data.readFully(text);

            int styledStringCount = data.readInt();
            styledStringHash = new int[styledStringCount];
            shortsOffsets = new int[styledStringCount];
            for (int i = 0; i < styledStringCount; i++) {
                styledStringHash[i] = data.readInt();
                shortsOffsets[i] = data.readInt();
            }

            int shortsCount = data.readInt();
            shorts = new short[shortsCount];
            for (int i = 0; i < styledStringCount; i++) {
                shorts[i] = data.readShort();
            }
        } finally {
            inTr.close();
        }
    }
    
    public StyledString getTranslation(StyledIdString source) {
        int h = source.id.hashCode() + source.raw.hashCode();
        HashFilter.Range range = HashFilter.filterHashes(styledStringHash, h);
        for (int i = range.min; i <= range.max; i++) {
            String key = createKeyString(i);
            if (key.equals(source)) {
                return createValueString(i);
            }
        }
        return null;
    }

    public static boolean isPackageTranslated(Resources res, String packageName) {
        return getResourceID(packageName) != 0;
    }

    private static int getResourceID(String packageName) {
        try {
            Field f = R.raw.class.getField("translation_" + packageName.replace('.', '_'));
            return f.getInt(R.raw.class);
        } catch (Exception ex) {
            return 0;
        }
    }
}
