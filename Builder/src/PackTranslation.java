import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.JAXBContext;

import org.alex73.android.Assert;
import org.alex73.android.StAXDecoderReader2;
import org.alex73.android.StyledIdString;
import org.alex73.android.StyledString;
import org.omegat.core.Core;
import org.omegat.core.data.IProject;
import org.omegat.core.data.ProjectProperties;
import org.omegat.core.data.RealProject;
import org.omegat.core.data.TMXEntry;
import org.omegat.filters2.master.PluginUtils;
import org.omegat.util.ProjectFileStorage;
import org.omegat.util.RuntimePreferences;

import android.control.App;
import android.control.Translation;

public class PackTranslation {
    static String projectPath = "../../Android.OmegaT/Android/";

    static final Charset UTF8 = Charset.forName("UTF-8");
    static int countDefault, countTranslations;
    static DataOutputStream out;
    static PrintStream log;

    static Map<String, String> dirToPackages = new HashMap<String, String>();
    static Set<String> packages = new TreeSet<String>();
    static Map<String, String> defaults = new HashMap<String, String>(20000);
    static Map<String, Map<StyledIdString, StyledString>> exact = new TreeMap<String, Map<StyledIdString, StyledString>>();
    static Set<CharSequence> usedTags = new TreeSet<CharSequence>();
    static StringBuilder outstr = new StringBuilder(1000000);
    static Map<CharSequence, Integer> outstrpos = new HashMap<CharSequence, Integer>();

    public static void main(String[] args) throws Exception {
        RuntimePreferences.setConfigDir("../../Android.OmegaT/Android.settings/");
        Map<String, String> params = new TreeMap<String, String>();
        params.put("alternate-filename-from", "/.+.xml$");
        params.put("alternate-filename-to", "/");
        Core.initializeConsole(params);
        PluginUtils.loadPlugins(params);

        ProjectProperties props = ProjectFileStorage.loadProjectProperties(new File(
                "../../Android.OmegaT/Android/"));
        RealProject project = new RealProject(props);
        project.loadProject();
        project.compileProject(".*");

        log = new PrintStream(new File("log"), "UTF-8");

        JAXBContext ctx = JAXBContext.newInstance(Translation.class);
        Translation translationInfo = (Translation) ctx.createUnmarshaller().unmarshal(
                new File(projectPath + "../translation.xml"));

        for (App app : translationInfo.getApp()) {
            packages.add(app.getPackageName());
            Assert.assertNull("Already defined", dirToPackages.get(app.getDirName()));
            dirToPackages.put(app.getDirName(), app.getPackageName());
        }

        final TranslationStoreDefaults store = new TranslationStoreDefaults();
        project.iterateByDefaultTranslations(new IProject.DefaultTranslationsIterator() {
            public void iterate(String source, TMXEntry trans) {
                if (!source.equals(trans.translation) && source.indexOf('<') < 0) {
                    countDefault++;
                    store.addDefaultTranslation(source, trans.translation);
                    defaults.put(source, trans.translation);
                }
            }
        });
        store.save();

        File[] dirs = new File(props.getSourceRoot()).listFiles();
        for (File dir : dirs) {
            File dirOut = new File(props.getTargetRoot(), dir.getName());

            String dirName = dir.getName();
            String pkg = dirToPackages.get(dirName);
            Assert.assertNotNull("Unknown package", pkg);
            TranslationStorePackage packageStore = new TranslationStorePackage(pkg);
            Assert.assertNotNull("Unknown package", packageStore);

            Map<StyledIdString, StyledString> collected = new HashMap<StyledIdString, StyledString>();
            File[] fs = dir.listFiles();
            Assert.assertNotNull("There is no files", fs);
            for (File f : fs) {
                readSourceAndTrans(f, new File(dirOut, f.getName()), collected, packageStore);
            }
            Assert.assertTrue("Not translated", !collected.isEmpty());
            Assert.assertNull("Exist package", exact.get(pkg));
            exact.put(pkg, collected);
            countTranslations += collected.size();
            packageStore.save();
        }

        System.out.println("countDefault = " + countDefault);
        System.out.println("countTranslated = " + countTranslations);
        System.out.println("countPackages = " + packages.size());
        System.out.print("Used tags: ");
        for (CharSequence tn : usedTags) {
            System.out.print(" <" + tn + ">");
        }
        System.out.println();

        //write();
        //write();

        log.close();
        System.exit(0);
    }

    static void write() throws Exception {
        out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(
                "../Installer/res/raw/translation.bin"))));

        byte[] ostr = outstr.toString().getBytes(UTF8);
        out.writeInt(ostr.length);
        out.write(ostr);

        out.writeInt(defaults.size());
        for (Map.Entry<String, String> en : defaults.entrySet()) {
            writeString(en.getKey());
            writeString(en.getValue());
        }

        out.writeInt(packages.size());
        for (String pkg : packages) {
            writeString(pkg);
            if (exact.containsKey(pkg)) {
                out.writeInt(exact.get(pkg).size());
                for (Map.Entry<StyledIdString, StyledString> en : exact.get(pkg).entrySet()) {
                    writeStyledIdString(en.getKey());
                    writeStyledString(en.getValue());
                }
            } else {
                out.writeInt(0);
            }
        }

        out.close();
    }

    static void readSourceAndTrans(File inFile, File outFile, Map<StyledIdString, StyledString> collected,
            TranslationStorePackage packageStore) throws Exception {
        StAXDecoderReader2 rdIn = new StAXDecoderReader2();
        rdIn.read(inFile);
        StAXDecoderReader2 rdOut = new StAXDecoderReader2();
        rdOut.read(outFile);
        {
            Map<String, StyledString> in = rdIn.getStrings();
            Map<String, StyledString> out = rdOut.getStrings();
            Assert.assertTrue("Wrong count", in.size() == out.size());
            for (Map.Entry<String, StyledString> en : in.entrySet()) {
                collect(en.getKey(), en.getValue(), out.get(en.getKey()), collected, packageStore);
            }
        }
        {
            Map<String, List<StyledString>> in = rdIn.getArrays();
            Map<String, List<StyledString>> out = rdOut.getArrays();
            Assert.assertTrue("Wrong count", in.size() == out.size());
            for (String id : in.keySet()) {
                List<StyledString> i = in.get(id);
                List<StyledString> o = out.get(id);
                Assert.assertTrue("Wrong length", i.size() == o.size());
                for (int idx = 0; idx < i.size(); idx++) {
                    collect(id, i.get(idx), o.get(idx), collected, packageStore);
                }
            }
        }
        {
            Map<String, Map<String, StyledString>> in = rdIn.getPlurals();
            Map<String, Map<String, StyledString>> out = rdOut.getPlurals();
            Assert.assertTrue("Wrong count", in.size() == out.size());
            for (String id : in.keySet()) {
                Map<String, StyledString> i = in.get(id);
                Map<String, StyledString> o = out.get(id);
                Assert.assertTrue("Wrong length", i.size() == o.size());
                for (String pid : i.keySet()) {
                    collect(id + "/" + pid, i.get(pid), o.get(pid), collected, packageStore);
                }
            }
        }
    }

    static void collect(String id, StyledString origin, StyledString translated,
            Map<StyledIdString, StyledString> collected, TranslationStorePackage packageStore)
            throws Exception {
        if (origin == null && translated == null) {
            return;
        }
        // TODO Assert.assertTrue("Wrong tags", origin.equalsTagNames(translated));
        removeSomeTags(origin);
        origin.removeSpaces();
        boolean nonTranslated = origin.equals(translated);
        if (nonTranslated) {
            translated = origin;
        } else {
            removeSomeTags(translated);
        }

        for (StyledString.Tag tag : origin.tags) {
            int p = tag.tagName.indexOf(';');
            CharSequence tn = p > 0 ? tag.tagName.subSequence(0, p) : tag.tagName;
            usedTags.add(tn);
        }

        int productLabel = id.lastIndexOf('#');
        if (productLabel > 0) {
            id = id.substring(0, productLabel);
        }

        StyledIdString s = new StyledIdString();
        s.id = id;
        s.raw = origin.raw;
        s.tags = origin.tags;
        log.println("id: " + id);
        s.dump(log);
        packageStore.addTranslation(s, translated);
        StyledString exist = collected.get(s);
        if (exist != null) {
            // TODO Assert.assertTrue("Changed string(id=" + id + "): '" + exist + "' and '" + translated +
            // "'",
            // exist.equals(translated));
        } else {
            collected.put(s, translated);
        }
    }

    static void removeSomeTags(StyledString str) {
        for (int i = 0; i < str.tags.length; i++) {
            if (str.tags[i].tagName.startsWith("g;") || str.tags[i].tagName.startsWith("anno")) {
                List<StyledString.Tag> tags = new ArrayList(Arrays.asList(str.tags));
                tags.remove(i);
                str.tags = tags.toArray(new StyledString.Tag[tags.size()]);
                i--;
            }
        }
    }

    static void writeString(CharSequence str) throws Exception {
        if (str == null) {
            throw new Exception("Empty string");
        }
        if (!outstrpos.containsKey(str)) {
            outstrpos.put(str, outstr.length());
            outstr.append(str);
        }
        int pos = outstrpos.get(str);
        int len = str.length();
        out.writeInt(pos);
        out.writeShort(len);
    }

    static void writeStyledString(StyledString str) throws Exception {
        writeString(str.raw);
        out.writeShort(str.tags.length);
        for (StyledString.Tag tag : str.tags) {
            writeString(tag.tagName);
            out.writeShort(tag.start);
            out.writeShort(tag.end);
        }
    }

    static void writeStyledIdString(StyledIdString str) throws Exception {
        writeString(str.id);
        writeStyledString(str);
    }
}
