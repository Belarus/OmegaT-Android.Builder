import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

public class convApk2Own {
    static Pattern RE_S = Pattern.compile("(\\s+<string name=\".+?\">)(.*)(</string>)");
    static Pattern RE_SE = Pattern.compile("(\\s+<string name=\".+?\")\\s*/>");
    static Pattern RE_I = Pattern.compile("(\\s+<item>)(.*)(</item>)");
    static Pattern RE_IE = Pattern.compile("(\\s+<item)\\s*/>");

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        for (File f : (Collection<File>) FileUtils.listFiles(new File("../../Android.OmegaT/Android/source/"),
                new String[] { "xml" }, true)) {

            List<String> lines = FileUtils.readLines(f, "UTF-8");
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                String s = lines.get(i);

                if (s.equals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")) {
                    s = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
                } else {
                    Matcher m;
                    ;
                    if ((m = RE_S.matcher(s)).matches()) {
                        s = m.group(1) + patchText(m.group(2)) + m.group(3);
                    } else if ((m = RE_I.matcher(s)).matches()) {
                        s = m.group(1) + patchText(m.group(2)) + m.group(3);
                    } else if ((m = RE_SE.matcher(s)).matches()) {
                        s = m.group(1) + "></string>";
                    } else if ((m = RE_IE.matcher(s)).matches()) {
                        s = m.group(1) + "></item>";
                    }
                }

                out.append(s).append("\n");
            }
            FileUtils.writeStringToFile(f, out.toString(), "UTF-8");
        }
    }

    protected static String patchText(String s) {
        // s = s.replace(">", "&gt;");
        s = s.replace("\\\"", "&quot;");
        s = s.replace("\\u0020", " ");
        return s;
    }
}
