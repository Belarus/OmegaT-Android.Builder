package org.alex73.android.arsc2;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Translation {
    String text;

    Map<String, String> defaults;
    Map<MultipleKey, String> multiples;

    public static class MultipleKey {
        public String source, file, id;

        public int hashCode() {
            return source.hashCode() + file.hashCode() + id.hashCode();
        }

        public boolean equals(Object obj) {
            MultipleKey k2 = (MultipleKey) obj;
            return source.equals(k2.source) && file.equals(k2.file) && id.equals(k2.id);
        }
    }

    public Translation(InputStream in) throws IOException {
        DataInputStream data = new DataInputStream(new BufferedInputStream(in));

        readText(data);

        int defaultsCount = data.readInt();
        defaults = new HashMap<String, String>(defaultsCount);
        for (int i = 0; i < defaultsCount; i++) {
            String source = readString(data);
            String translation = readString(data);
            defaults.put(source, translation);
        }

        int multiplesCount = data.readInt();
        multiples = new HashMap<MultipleKey, String>(multiplesCount);
        for (int i = 0; i < multiplesCount; i++) {
            MultipleKey key = new MultipleKey();
            key.source = readString(data);
            key.file = readString(data);
            key.id = readString(data);
            String translation = readString(data);
            multiples.put(key, translation);
        }
    }

    private void readText(DataInputStream data) throws IOException {
        int strsz = data.readInt();
        byte[] strb = new byte[strsz];
        data.readFully(strb);
        text = new String(strb, "UTF-8");
        strb = null;
    }

    private String readString(DataInputStream data) throws IOException {
        int pos = data.readInt();
        int len = data.readInt();
        return text.substring(pos, pos + len);
    }

    public String getTranslation(String file, String id, String source) {
        MultipleKey key = new MultipleKey();
        key.source = source;
        key.file = file;
        key.id = id;
        String translation = multiples.get(key);
        if (translation == null) {
            defaults.get(translation);
        }
        return translation;
    }
}
