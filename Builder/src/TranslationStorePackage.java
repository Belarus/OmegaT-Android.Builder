import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alex73.android.Assert;
import org.alex73.android.StyledIdString;
import org.alex73.android.StyledString;

public class TranslationStorePackage {
    public static final Charset UTF8 = Charset.forName("UTF-8");

    String name;
    private Map<StyledIdString, StyledString> collected = new HashMap<StyledIdString, StyledString>();

    public TranslationStorePackage(String packageName) {
        this.name = packageName;
    }

    public void addTranslation(StyledIdString source, StyledString translation) {
        collected.put(source, translation);
    }

    public void save() throws Exception {
        File outFile = new File("../Installer/res/raw/translation_" + name.replace('.', '_') + ".bin");
        if (outFile.exists()) {
            throw new Exception(outFile + " already exist");
        }
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)));

        SortedStyledStringMap list = new SortedStyledStringMap();
        for (Map.Entry<StyledIdString, StyledString> en : collected.entrySet()) {
            list.addTranslation(en.getKey(), en.getValue());
        }

        list.write(out);

        out.close();
    }

    public static class SortedStyledString {
        int hash;
        short idIndex;
        short sourceIndex;
        SortedStyledStringTag[] sourceTags;
        short translationIndex;
        SortedStyledStringTag[] translationTags;
    }

    public static class SortedStyledStringTag {
        short tagNameIndex;
        short start;
        short end;
    }

    public static class SortedStyledStringMap {
        private List<String> plainStrings = new ArrayList<>();
        private Map<String, Short> plainStringIndex = new HashMap<>();
        private List<SortedStyledString> styledStrings = new ArrayList<>();

        public void addTranslation(StyledIdString source, StyledString translation) {
            SortedStyledString r = new SortedStyledString();
            r.hash = source.id.hashCode() + source.raw.hashCode();
            r.idIndex = getPlainStringIndex(source.id);
            r.sourceIndex = getPlainStringIndex(source.raw);
            r.sourceTags = new SortedStyledStringTag[source.tags.length];
            for (int i = 0; i < source.tags.length; i++) {
                r.sourceTags[i] = new SortedStyledStringTag();
                r.sourceTags[i].tagNameIndex = getPlainStringIndex(source.tags[i].tagName);
                r.sourceTags[i].start = i2s(source.tags[i].start);
                r.sourceTags[i].end = i2s(source.tags[i].end);
            }
            r.translationIndex = getPlainStringIndex(translation.raw);
            r.translationTags = new SortedStyledStringTag[translation.tags.length];
            for (int i = 0; i < translation.tags.length; i++) {
                r.translationTags[i] = new SortedStyledStringTag();
                r.translationTags[i].tagNameIndex = getPlainStringIndex(translation.tags[i].tagName);
                r.translationTags[i].start = i2s(translation.tags[i].start);
                r.translationTags[i].end = i2s(translation.tags[i].end);
            }
            styledStrings.add(r);
        }

        private short getPlainStringIndex(String str) {
            Short index = plainStringIndex.get(str);
            if (index == null) {
                index = i2s(plainStrings.size());
                plainStrings.add(str);
                plainStringIndex.put(str, index);
            }
            return index;
        }

        public void write(DataOutputStream out) throws Exception {
            Collections.sort(styledStrings, new Comparator<SortedStyledString>() {
                @Override
                public int compare(SortedStyledString o1, SortedStyledString o2) {
                    if (o1.hash < o2.hash) {
                        return -1;
                    } else if (o1.hash > o2.hash) {
                        return 1;
                    } else {
                        int r = Short.compare(o1.idIndex, o2.idIndex);
                        if (r == 0) {
                            r = Short.compare(o1.sourceIndex, o2.sourceIndex);
                        }
                        return r;
                    }
                }
            });

            int[] plainStringsOffsets = new int[plainStrings.size()];
            ByteArrayOutputStream text = new ByteArrayOutputStream();
            for (int i = 0; i < plainStrings.size(); i++) {
                plainStringsOffsets[i] = text.size();
                text.write(plainStrings.get(i).getBytes(UTF8));
            }

            out.writeInt(plainStringsOffsets.length);
            for (int off : plainStringsOffsets) {
                out.writeInt(off);
            }

            out.writeInt(text.size());
            out.write(text.toByteArray());

            List<Short> allShorts = new ArrayList<>();

            out.writeInt(styledStrings.size());
            for (SortedStyledString s : styledStrings) {
                out.writeInt(s.hash);
                out.writeInt(allShorts.size());
                allShorts.add(s.idIndex);
                allShorts.add(s.sourceIndex);
                allShorts.add(s.translationIndex);

                if (s.sourceTags.length >= 128 || s.translationTags.length >= 128) {
                    throw new Exception("Too many tags");
                }
                allShorts.add(i2s(s.sourceTags.length | (s.translationTags.length << 8)));
                for (int i = 0; i < s.sourceTags.length; i++) {
                    allShorts.add(s.sourceTags[i].tagNameIndex);
                    allShorts.add(s.sourceTags[i].start);
                    allShorts.add(s.sourceTags[i].end);
                }
                for (int i = 0; i < s.translationTags.length; i++) {
                    allShorts.add(s.translationTags[i].tagNameIndex);
                    allShorts.add(s.translationTags[i].start);
                    allShorts.add(s.translationTags[i].end);
                }
            }

            out.writeInt(allShorts.size());
            for (short s : allShorts) {
                out.writeShort(s);
            }
        }
    }

    static short i2s(int val) {
        Assert.assertTrue("Value too big", val < Short.MAX_VALUE);
        return (short) val;
    }
}
