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
        HashFilter.Range range = HashFilter.filterHashes(hash, source.hashCode());
        for (int i = range.min; i <= range.max; i++) {
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
}
