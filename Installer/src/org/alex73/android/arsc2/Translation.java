package org.alex73.android.arsc2;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class Translation {
    String text;

    public Map<String, String> defaults;
    public Map<MultipleKey, String> multiples;
    public Set<String> packages;

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
        DataInputStream data = new DataInputStream(new BufferedInputStream(in, 16384));

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
            key.file = readString(data);
            key.id = readString(data);
            key.source = readString(data);
            String translation = readString(data);
            multiples.put(key, translation);
        }

        int segmentationRulesCount = data.readInt();
        Segmenter.rules = new Segmenter.SegmentationRule[segmentationRulesCount];
        for (int i = 0; i < Segmenter.rules.length; i++) {
            Segmenter.rules[i] = new Segmenter.SegmentationRule();
            Segmenter.rules[i].breakRule = data.readBoolean();
            Segmenter.rules[i].beforeBreak = Pattern.compile(readString(data));
            Segmenter.rules[i].afterBreak = Pattern.compile(readString(data));
        }

        int packagesCount = data.readInt();
        packages = new HashSet<String>();
        for (int i = 0; i < packagesCount; i++) {
            packages.add(readString(data));
        }
    }

    public boolean isPackageTranslated(String packageName) {
        return packages.contains(packageName);
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
        List<String> segments = Segmenter.segment(source);
        String[] translatedSegments = new String[segments.size()];
        for (int i = 0; i < segments.size(); i++) {
            translatedSegments[i] = getSegmentTranslation(file, id, segments.get(i));
        }
        // check if all translations not exist
        boolean notExist = true;
        for (int i = 0; i < translatedSegments.length; i++) {
            if (translatedSegments[i] != null) {
                notExist = false;
                break;
            }
        }
        if (notExist) {
            return null;
        }
        return Segmenter.glue(translatedSegments);
    }

    private String getSegmentTranslation(String file, String id, String segment) {
        MultipleKey key = new MultipleKey();
        key.source = segment;
        key.file = file;
        key.id = id;
        String translation = multiples.get(key);
        if (translation == null) {
            translation = defaults.get(segment);
        }
        return translation;
    }
}
