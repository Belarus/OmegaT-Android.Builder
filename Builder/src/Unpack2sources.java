import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;

import org.alex73.android.StAXDecoderReader2;
import org.apache.commons.io.FileUtils;

import android.control.App;
import android.control.Translation;

public class Unpack2sources {
    static String projectPath = "../../Android.OmegaT/";
    static Pattern RE_SOURCES = Pattern
            .compile("(strings|plurals|arrays)_.*(cyanogen|android)-[0-9]\\.[0-9]\\.xml");

    public static void main(String[] args) throws Exception {
        FileUtils.deleteDirectory(new File(projectPath, "source"));
        File[] dirs = new File(projectPath, "source-raw").listFiles();
        if (dirs == null) {
            throw new RuntimeException("There are no source files");
        }
        JAXBContext ctx = JAXBContext.newInstance(Translation.class);
        Translation translationInfo = (Translation) ctx.createUnmarshaller().unmarshal(
                new File(projectPath + "translation.xml"));

        for (File dir : dirs) {
            App thisApp = null;
            for (App app : translationInfo.getApp()) {
                if (app.getDirName().equals(dir.getName())) {
                    if (thisApp != null) {
                        throw new RuntimeException();
                    }
                    thisApp = app;
                }
            }
            if (thisApp == null) {
                throw new RuntimeException();
            }
            Set<File> fromSources = new TreeSet<>();
            Set<File> fromBinaries = new TreeSet<>();
            for (File file : dir.listFiles()) {
                Matcher m = RE_SOURCES.matcher(file.getName());
                if (m.matches()) {
                    fromSources.add(file);
                } else {
                    fromBinaries.add(file);
                }
            }
            for (File f : fromBinaries) {
                StAXDecoderReader2 rd = new StAXDecoderReader2();
                rd.read(f);
                if (!rd.getNonTranslatable().isEmpty()) {
                    System.err.println("Non-translatable from binaries " + f);
                }
            }
            Map<String, String> nonTranslatable = new HashMap<>();
            Set<String> forceTranslatable = new HashSet<>();
            Set<String> forceNotranslatable = new HashSet<>();
            for (File f : fromSources) {
                StAXDecoderReader2 rd = new StAXDecoderReader2();
                rd.read(f);
                nonTranslatable.putAll(rd.getNonTranslatableIds());
            }
            for (App.ForceTranslation ft : thisApp.getForceTranslation()) {
                forceTranslatable.add(ft.getName());
                nonTranslatable.remove(ft.getName());
            }
            for (App.ForceNotranslation ft : thisApp.getForceNotranslation()) {
                forceNotranslatable.add(ft.getName());
                nonTranslatable.put(ft.getName(), "FORCE");
            }
            for (File f : fromSources) {
                StAXDecoderReader2 rd = new StAXDecoderReader2();
                rd.read(f);
                Set<String> nonTranslatableInfile = new HashSet<>(rd.getNonTranslatableIds().keySet());
                nonTranslatableInfile.removeAll(forceTranslatable);
                nonTranslatableInfile.addAll(forceNotranslatable);
                for (String key : rd.getStrings().keySet()) {
                    boolean isNonTranslatableInFile = nonTranslatableInfile.contains(key);
                    boolean isNonTranslatableGlobal = nonTranslatable.containsKey(key);
                    if (rd.getStrings().get(key) == null || rd.getStrings().get(key).isEmpty()) {
                        isNonTranslatableInFile = isNonTranslatableGlobal;
                    }
                    if (isNonTranslatableInFile != isNonTranslatableGlobal) {
                        System.err.println("Non-translatable(" + key + " from "
                                + nonTranslatable.get(key).replaceAll(".+/(.+?/.+?)", "$1")
                                + ") is translatable in some string files: "
                                + f.getPath().replaceAll(".+/(.+?/.+?)", "$1"));
                    }
                }
                for (String key : rd.getArrays().keySet()) {
                    boolean isNonTranslatableInFile = nonTranslatableInfile.contains(key);
                    boolean isNonTranslatableGlobal = nonTranslatable.containsKey(key);
                    if (isNonTranslatableInFile != isNonTranslatableGlobal) {
                        System.err.println("Non-translatable(" + key + " from "
                                + nonTranslatable.get(key).replaceAll(".+/(.+?/.+?)", "$1")
                                + ") is translatable in some array files: "
                                + f.getPath().replaceAll(".+/(.+?/.+?)", "$1"));
                    }
                }
                for (String key : rd.getPlurals().keySet()) {
                    boolean isNonTranslatableInFile = nonTranslatableInfile.contains(key);
                    boolean isNonTranslatableGlobal = nonTranslatable.containsKey(key);
                    if (isNonTranslatableInFile != isNonTranslatableGlobal) {
                        System.err.println("Non-translatable(" + key + " from "
                                + nonTranslatable.get(key).replaceAll(".+/(.+?/.+?)", "$1")
                                + ") is translatable in some plurals files: "
                                + f.getPath().replaceAll(".+/(.+?/.+?)", "$1"));
                    }
                }
            }
            FileUtils.writeLines(new File(projectPath + "source/" + dir.getName() + "/#nontranslatable.ids"),
                    nonTranslatable.keySet());
            for (File file : dir.listFiles()) {
                File outFile = new File(projectPath + "source/" + dir.getName() + "/" + file.getName());
                System.out.println(file + " -> " + outFile);
                String xml = new TagsFilter2().filterSpaces(file);
                new TagsFilter().filter(xml, outFile, nonTranslatable.keySet());
            }
        }
    }
}
