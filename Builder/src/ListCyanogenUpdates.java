import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.omegat.util.WikiGet;

public class ListCyanogenUpdates {
    public static void main(String[] args) throws Exception {
        String s = WikiGet.getURL("http://wiki.cyanogenmod.com/wiki/Latest_Version");
        Matcher m = Pattern.compile("(http://([^\"]+)\\.zip)").matcher(s);
        int pos = 0;
        while (m.find(pos)) {
            System.out.println("wget -c " + m.group(1));
            pos = m.end();
        }
    }
}
