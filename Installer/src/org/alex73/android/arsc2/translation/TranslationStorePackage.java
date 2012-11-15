package org.alex73.android.arsc2.translation;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;

import org.alex73.android.StyledIdString;
import org.alex73.android.StyledString;
import org.alex73.android.bel.R;
import org.alex73.android.common.UTFUtils;

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
            DataInputStream data = new DataInputStream(new BufferedInputStream(inTr, 16384));

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
            for (int i = 0; i < shortsCount; i++) {
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
            StyledIdString curr = createKeyString(i);
            if (curr.equals(source)) {
                return createValueString(i);
            }
        }
        return null;
    }

    private StyledIdString createKeyString(int stringIndex) {
        int shortIndex = shortsOffsets[stringIndex];

        StyledIdString result = new StyledIdString();
        result.id = getPlainString(shorts[shortIndex++]);
        result.raw = getPlainString(shorts[shortIndex++]);
        shortIndex++; // raw translation

        int sourceTagsCount = shorts[shortIndex] & 0x7F;
        shortIndex++; // tags length
        result.tags = new StyledString.Tag[sourceTagsCount];
        for (int j = 0; j < sourceTagsCount; j++) {
            result.tags[j] = new StyledString.Tag();
            result.tags[j].tagName = getPlainString(shorts[shortIndex++]);
            result.tags[j].start = shorts[shortIndex++];
            result.tags[j].end = shorts[shortIndex++];
        }

        return result;
    }

    private StyledString createValueString(int stringIndex) {
        int shortIndex = shortsOffsets[stringIndex];

        StyledString result = new StyledString();
        shortIndex++;// id
        shortIndex++;// raw source
        result.raw = getPlainString(shorts[shortIndex++]);

        int sourceTagsCount = shorts[shortIndex] & 0x7F;
        int translationTagsCount = (shorts[shortIndex] >>> 8) & 0x7F;
        shortIndex++; // tags length
        shortIndex += sourceTagsCount * 3; // source tags
        result.tags = new StyledString.Tag[sourceTagsCount];
        for (int j = 0; j < translationTagsCount; j++) {
            result.tags[j] = new StyledString.Tag();
            result.tags[j].tagName = getPlainString(shorts[shortIndex++]);
            result.tags[j].start = shorts[shortIndex++];
            result.tags[j].end = shorts[shortIndex++];
        }

        return result;
    }

    private String getPlainString(int stringIndex) {
        int begin = plainStringOffsets[stringIndex];
        int end = stringIndex + 1 < plainStringOffsets.length ? plainStringOffsets[stringIndex + 1]
                : text.length;
        return UTFUtils.utf8decoder(text, begin, end - begin);
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
