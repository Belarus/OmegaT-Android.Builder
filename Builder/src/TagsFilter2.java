import java.io.File;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

public class TagsFilter2 {
    String str;
    StringBuilder out;
    int pos;

    public String filterSpaces(File fileIn) throws Exception {
        str = FileUtils.readFileToString(fileIn);
        if (str.charAt(0) == '\uFEFF') {
            // BOM
            str = str.substring(1);
        }
        out = new StringBuilder(str.length());
        for (pos = 0; pos < str.length(); pos++) {
            if (str.charAt(pos) == '<') {
                if (isNext("<string ")) {
                    append("</string>");
                } else if (isNext("<item")) {
                    append("</item>");
                } else {
                    out.append(str.charAt(pos));
                }
            } else {
                out.append(str.charAt(pos));
            }
        }
        return out.toString();
    }

    boolean isNext(String check) {
        return str.substring(pos).startsWith(check);
    }

    static Pattern RE_UNICODE = Pattern.compile("\\\\u([0-9A-Fa-f]{4})");

    void append(String end) {
        StringBuilder local = new StringBuilder(500);
        boolean wasSpace = false;
        while (true) {
            char ch = str.charAt(pos);
            if (ch == '<' && isNext(end)) {
                local.append(ch);
                String v = local.toString();
                v = v.replaceAll("<xliff:g.*?>", "").replaceAll("</xliff:g>", "");
                v = v.replaceAll("<!--.*?-->", "");
                Matcher m = RE_UNICODE.matcher(v);
                int pos = 0;
                while (m.find()) {
                    out.append(v.substring(pos, m.start()));
                    int cu = Integer.parseInt(m.group(1), 16);
                    switch (cu) {
                    case '"':
                        out.append("\\\"");
                        break;
                    case '&':
                        out.append("&amp;");
                        break;
                    case '\'':
                        out.append("\\'");
                        break;
                    case '<':
                        out.append("&lt;");
                        break;
                    case '>':
                        out.append("&gt;");
                        break;
                    default:
                        out.append((char) cu);
                        break;
                    }
                    pos = m.end();
                }
                out.append(v.substring(pos));
                return;
            }
            pos++;
            boolean isSpace = Character.isWhitespace(ch);
            if (isSpace) {
                if (!wasSpace) {
                    local.append(' ');
                }
            } else {
                local.append(ch);
            }
            wasSpace = isSpace;
        }
    }
}
