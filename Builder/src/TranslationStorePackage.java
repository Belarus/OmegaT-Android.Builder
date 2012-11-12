import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.alex73.android.Assert;
import org.alex73.android.StyledIdString;
import org.alex73.android.StyledString;

import TranslationStoreDefaults.SortedString;

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
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(
                new FileOutputStream("../Installer/res/raw/translation-" + name + ".bin"))));

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

        private short getPlainStringIndex(CharSequence chars) {
            String str = chars.toString();
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
                        return 0;
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

            out.write(text.size());
            out.write(text.toByteArray());

            List<Short> allShorts = new ArrayList<>();

            out.write(styledStrings.size());
            for (SortedStyledString s : styledStrings) {
                out.writeInt(s.hash);
                out.writeInt(allShorts.size());
                allShorts.add(s.idIndex);
                allShorts.add(s.sourceIndex);
                allShorts.add(i2s(s.sourceTags.length));
                for(int i=0;i<s.sourceTags.length;i++) {
                    allShorts.add(s.sourceTags[i].tagNameIndex);
                    TODO
                }
                allShorts.add(s.translationIndex);
                allShorts.add(i2s(s.translationTags.length));
              TODO
            }
        }
    }

    static short i2s(int val) {
        Assert.assertTrue("Value too big", val < Short.MAX_VALUE);
        return (short) val;
    }
}
