package org.alex73.android.arsc2;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.alex73.android.StyledIdString;
import org.alex73.android.StyledString;
import org.alex73.android.arsc2.translation.TranslationStoreDefaults;
import org.alex73.android.arsc2.translation.TranslationStorePackage;

public class Translation {
    TranslationStorePackage packageTranslations;
    TranslationStoreDefaults defaultTranslations;

    char[] text;

    public Map<String, String> defaults;
    public Map<String, Map<StyledIdString, StyledString>> exact = new HashMap<String, Map<StyledIdString, StyledString>>();

    public Translation(TranslationStorePackage packageTranslations, TranslationStoreDefaults defaultTranslations) {
        this.packageTranslations=packageTranslations;
        this.defaultTranslations=defaultTranslations;
    }

    public Translation(InputStream in) throws IOException {
        DataInputStream data = new DataInputStream(new BufferedInputStream(in, 16384));

        readText(data);

        int defaultsCount = data.readInt();
        defaults = new HashMap<String, String>(defaultsCount);
        for (int i = 0; i < defaultsCount; i++) {
            String source = readString(data);
            String translation = readString(data);
            defaults.put(source, translation);
        }

        int packagesCount = data.readInt();
        for (int i = 0; i < packagesCount; i++) {
            String pkg = readString(data);
            int count = data.readInt();
            Map<StyledIdString, StyledString> map = new HashMap<StyledIdString, StyledString>(count);
            for (int j = 0; j < count; j++) {
                StyledIdString key = readStyledIdString(data);
                StyledString value = readStyledString(data);
                map.put(key, value);
            }
            exact.put(pkg, map);
        }
    }

    public boolean isPackageTranslated(String packageName) {
        return exact.containsKey(packageName);
    }

    private void readText(DataInputStream data) throws IOException {
        int strsz = data.readInt();
        byte[] strb = new byte[strsz];
        data.readFully(strb);
        text = new String(strb, "UTF-8").toCharArray();
        strb = null;
    }

    private String readString(DataInputStream data) throws IOException {
        int pos = data.readInt();
        int len = data.readShort();
        return new String(text, pos, len);
    }

    private StyledString readStyledString(DataInputStream data) throws IOException {
        StyledString str = new StyledString();
        str.raw = readString(data);
        str.tags = new StyledString.Tag[data.readShort()];
        for (int i = 0; i < str.tags.length; i++) {
            str.tags[i] = new StyledString.Tag();
            str.tags[i].tagName = readString(data);
            str.tags[i].start = data.readShort();
            str.tags[i].end = data.readShort();
        }
        str.sortTags();
        return str;
    }

    private StyledIdString readStyledIdString(DataInputStream data) throws IOException {
        StyledIdString str = new StyledIdString();
        str.id = readString(data);
        str.raw = readString(data);
        str.tags = new StyledString.Tag[data.readShort()];
        for (int i = 0; i < str.tags.length; i++) {
            str.tags[i] = new StyledString.Tag();
            str.tags[i].tagName = readString(data);
            str.tags[i].start = data.readShort();
            str.tags[i].end = data.readShort();
        }
        str.sortTags();
        return str;
    }

    public StyledString getTranslation(String packageName, String id, StyledString source) {
        
        Map<StyledIdString, StyledString> tr = exact.get(packageName);
        if (tr != null) {
            source.removeSpaces();
            StyledIdString si = new StyledIdString();
            si.id = id;
            si.raw = source.raw;
            si.tags = source.tags;
            StyledString trans = tr.get(si);
            if (trans != null) {
                if (trans.equals(source)) {
                    // the same string
                    return null;
                } else {
                    return trans;
                }
            }
            notFoundInExact(packageName, id, source);
        }

        if (!source.hasTags()) {
            String defaultTranslation = defaultTranslations.getTranslation(source.raw);
            if (defaultTranslation != null) {
                StyledString str = new StyledString();
                str.raw = defaultTranslation;
                return str;
            }
        }

        return null;
    }

    public void notFoundInExact(String packageName, String id, StyledString source) {
    }
}
