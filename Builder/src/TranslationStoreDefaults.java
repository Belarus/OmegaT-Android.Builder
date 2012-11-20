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

import org.alex73.android.StyledIdString;
import org.alex73.android.StyledString;

public class TranslationStoreDefaults {
    public static final Charset UTF8 = Charset.forName("UTF-8");

    private Map<String, String> defaults = new HashMap<>();

    public void addDefaultTranslation(String source, String translation) {
        defaults.put(source, translation);
    }

    public void save() throws Exception {
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(
                "../Installer/res/raw/translation.bin")));

        List<String> allCollected = new ArrayList<String>(defaults.keySet());
        Collections.sort(allCollected);

        SortedStringMap list = new SortedStringMap();
        for (String ids : allCollected) {
            list.add(ids, defaults.get(ids));
        }

        list.write(out);

        out.close();
    }

    public static class SortedString {
        int hash;
        String key;
        String value;
        int keyOffset;
        int valueOffset;
    }

    public static class SortedStringMap {
        private List<SortedString> strings = new ArrayList<>();

        private void add(String key, String value) {
            SortedString s = new SortedString();
            s.key = key;
            s.value = value;
            s.hash = s.key.hashCode();
            strings.add(s);
        }

        public void write(DataOutputStream out) throws Exception {
            Collections.sort(strings, new Comparator<SortedString>() {
                @Override
                public int compare(SortedString o1, SortedString o2) {
                    if (o1.hash < o2.hash) {
                        return -1;
                    } else if (o1.hash > o2.hash) {
                        return 1;
                    } else {
                        int r = o1.key.compareTo(o2.key);
                        if (r == 0) {
                            r = o1.value.compareTo(o2.value);
                        }
                        return r;
                    }
                }
            });

            ByteArrayOutputStream text = new ByteArrayOutputStream();
            for (SortedString s : strings) {
                s.keyOffset = text.size();
                text.write(s.key.getBytes(UTF8));
                s.valueOffset = text.size();
                text.write(s.value.getBytes(UTF8));
            }

            out.writeInt(strings.size());
            for (SortedString s : strings) {
                out.writeInt(s.hash);
                out.writeInt(s.keyOffset);
                out.writeInt(s.valueOffset);
            }

            out.writeInt(text.size());
            out.write(text.toByteArray());
        }
    }

    public static Comparator<StyledString> COMP_STYLED_STRING = new Comparator<StyledString>() {
        public int compare(StyledString o1, StyledString o2) {
            int r = o1.raw.compareTo(o2.raw);
            if (r == 0) {
                r = Integer.compare(o1.tags.length, o2.tags.length);
            }
            if (r == 0) {
                for (int i = 0; i < o1.tags.length; i++) {
                    StyledString.Tag t1 = o1.tags[i];
                    StyledString.Tag t2 = o2.tags[i];
                    if (r == 0) {
                        r = t1.tagName.compareTo(t2.tagName);
                    }
                    if (r == 0) {
                        r = Integer.compare(t1.start, t2.start);
                    }
                    if (r == 0) {
                        r = Integer.compare(t1.end, t2.end);
                    }
                }
            }
            return r;
        }
    };

    public static Comparator<StyledIdString> COMP_STYLED_ID_STRING = new Comparator<StyledIdString>() {
        public int compare(StyledIdString o1, StyledIdString o2) {
            int r = o1.id.compareTo(o2.id);
            if (r == 0) {
                r = COMP_STYLED_STRING.compare(o1, o2);
            }
            return r;
        }
    };
}
