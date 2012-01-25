import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.omegat.core.Core;
import org.omegat.core.data.EntryKey;
import org.omegat.core.data.IProject;
import org.omegat.core.data.ProjectProperties;
import org.omegat.core.data.RealProject;
import org.omegat.core.data.TMXEntry;
import org.omegat.filters2.master.PluginUtils;
import org.omegat.util.RuntimePreferences;

public class PackTranslation {
    static final Charset UTF8 = Charset.forName("UTF-8");
    static int countDefault, countMultiple, countOrphanedDefault, countOrphanedMultiple;
    static Map<String, String> defaults = new HashMap<String, String>(20000);
    static Map<EntryKey, String> multiples = new HashMap<EntryKey, String>(1000);
    static DataOutputStream out;

    static StringBuilder outstr = new StringBuilder(1000000);
    static Map<String, Integer> outstrpos = new HashMap<String, Integer>();

    public static void main(String[] args) throws Exception {
        RuntimePreferences.setConfigDir("../../Android.OmegaT/Android.settings/");
        Map<String, String> params = new TreeMap<String, String>();
        params.put("alternate-filename-from", "/.+.xml$");
        params.put("alternate-filename-to", "/");
        Core.initializeConsole(params);
        PluginUtils.loadPlugins(params);

        ProjectProperties props = new ProjectProperties(new File("../../Android.OmegaT/Android/"));
        RealProject project = new RealProject(props);
        project.loadProject();

        project.iterateByDefaultTranslations(new IProject.DefaultTranslationsIterator() {
            public void iterate(String source, TMXEntry trans) {
                countDefault++;
                defaults.put(source, trans.translation);
            }
        });

        project.iterateByMultipleTranslations(new IProject.MultipleTranslationsIterator() {
            public void iterate(EntryKey source, TMXEntry trans) {
                countMultiple++;
                multiples.put(source, trans.translation);
            }
        });

        project.iterateByOrphanedDefaultTranslations(new IProject.DefaultTranslationsIterator() {
            public void iterate(String source, TMXEntry trans) {
                countOrphanedDefault++;
            }
        });

        project.iterateByOrphanedMultipleTranslations(new IProject.MultipleTranslationsIterator() {
            public void iterate(EntryKey source, TMXEntry trans) {
                countOrphanedMultiple++;
            }
        });

        System.out.println("countDefault = " + countDefault);
        System.out.println("countMultiple = " + countMultiple);
        System.out.println("countOrphanedDefault = " + countOrphanedDefault);
        System.out.println("countOrphanedMultiple = " + countOrphanedMultiple);

        for (Map.Entry<String, String> en : defaults.entrySet()) {
            collectString(en.getKey());
            collectString(en.getValue());
        }
        for (Map.Entry<EntryKey, String> en : multiples.entrySet()) {
            collectString(en.getKey().sourceText);
            collectString(en.getKey().file);
            collectString(en.getKey().id);
            collectString(en.getValue());
        }

        out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(
                "../Installer/res/raw/translation.bin")));

        byte[] ostr = outstr.toString().getBytes(UTF8);
        out.writeInt(ostr.length);
        out.write(ostr);

        out.writeInt(defaults.size());
        for (Map.Entry<String, String> en : defaults.entrySet()) {
            writeString(en.getKey());
            writeString(en.getValue());
        }
        out.writeInt(multiples.size());
        for (Map.Entry<EntryKey, String> en : multiples.entrySet()) {
            writeString(en.getKey().sourceText);
            writeString(en.getKey().file);
            writeString(en.getKey().id);
            writeString(en.getValue());
        }

        out.close();

        System.exit(0);
    }

    static void collectString(String str) throws Exception {
        if (str == null) {
            throw new Exception("Empty string");
        }
        if (outstrpos.containsKey(str)) {
            return;
        }
        outstrpos.put(str, outstr.length());
        outstr.append(str);
    }

    static void writeString(String str) throws Exception {
        if (str == null) {
            throw new Exception("Empty string");
        }
        int pos = outstrpos.get(str);
        int len = str.length();
        out.writeInt(pos);
        out.writeInt(len);
    }
}
