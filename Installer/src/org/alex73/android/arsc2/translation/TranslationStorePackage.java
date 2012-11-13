package org.alex73.android.arsc2.translation;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.zip.GZIPInputStream;

import org.alex73.android.bel.R;

import android.content.res.Resources;

public class TranslationStorePackage {

    public TranslationStorePackage(Resources res, String packageName) throws Exception {
        int resID = getResourceID(packageName);
        if (resID == 0) {
            return;
        }
        InputStream inTr = res.openRawResource(resID);
        try {
            DataInputStream data = new DataInputStream(new BufferedInputStream(new GZIPInputStream(inTr), 16384));
        } finally {
            inTr.close();
        }
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
