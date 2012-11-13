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

public class TranslationStoreDefaults {
    public static final Charset UTF8 = Charset.forName("UTF-8");

    private Map<String, String> defaults = new HashMap<>();

    public void addDefaultTranslation(String source, String translation) {
        defaults.put(source, translation);
    }

    public void save() throws Exception {
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(
                new FileOutputStream("../Installer/res/raw/translation.bin"))));

        SortedStringMap list = new SortedStringMap();
        for (Map.Entry<String, String> en : defaults.entrySet()) {
            list.add(en.getKey(), en.getValue());
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
                        return 0;
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
}
