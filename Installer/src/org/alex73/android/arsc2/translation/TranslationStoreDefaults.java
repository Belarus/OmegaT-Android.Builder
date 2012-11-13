package org.alex73.android.arsc2.translation;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.alex73.android.bel.R;
import org.alex73.android.common.UTFUtils;

import android.content.res.Resources;

public class TranslationStoreDefaults {
    private int stringsCount;
    private int[] hash, keyOffset, valueOffset;
    private byte[] text;

    public TranslationStoreDefaults(Resources res) throws Exception {
        InputStream inTr = res.openRawResource(R.raw.translation);
        try {
            DataInputStream data = new DataInputStream(new BufferedInputStream(new GZIPInputStream(inTr), 16384));

            stringsCount = data.readInt();
            hash = new int[stringsCount];
            keyOffset = new int[stringsCount];
            valueOffset = new int[stringsCount];
            for (int i = 0; i < stringsCount; i++) {
                hash[i] = data.readInt();
                keyOffset[i] = data.readInt();
                valueOffset[i] = data.readInt();
            }

            int textSize = data.readInt();
            text = new byte[textSize];
            data.readFully(text);
        } finally {
            inTr.close();
        }
    }

    public String getTranslation(String source) {
        filterHashes(source);
        for (int i = hashIndexFirst; i <= hashIndexLast; i++) {
            String key = createKeyString(i);
            if (key.equals(source)) {
                return createValueString(i);
            }
        }
        return null;
    }

    private String createKeyString(int stringIndex) {
        int begin = keyOffset[stringIndex];
        int end = valueOffset[stringIndex];
        return UTFUtils.utf8decoder(text, begin, end - begin);
    }

    private String createValueString(int stringIndex) {
        int begin = valueOffset[stringIndex];
        int end = stringIndex + 1 < keyOffset.length ? keyOffset[stringIndex + 1] : text.length;
        return UTFUtils.utf8decoder(text, begin, end - begin);
    }

    int hashIndexFirst, hashIndexLast;

    private void filterHashes(String source) {
        int h = source.hashCode();

        int minIndex = 0;
        int maxIndex = hash.length - 1;
        int index = 0;
        while (minIndex <= maxIndex) {
            index = (minIndex + maxIndex) >>> 1;
            if (hash[index] < h) {
                minIndex = index + 1;
            } else if (hash[index] > h) {
                maxIndex = index - 1;
            } else {
                break;
            }
        }
        if (hash[index] != h) {
            hashIndexFirst = 1;
            hashIndexLast = 0;
            return;
        }

        hashIndexFirst = index;
        hashIndexLast = index;
        while (hashIndexFirst > 0 && hash[hashIndexFirst - 1] == h) {
            hashIndexFirst--;
        }
        while (hashIndexLast < hash.length - 1 && hash[hashIndexLast + 1] == h) {
            hashIndexLast++;
        }
    }
}
