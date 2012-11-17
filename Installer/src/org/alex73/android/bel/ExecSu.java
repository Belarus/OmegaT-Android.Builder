package org.alex73.android.bel;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ExecSu {

    public static List<String> execEvenFail(String cat) throws Exception {
        return exec(cat, false);
    }

    public static List<String> exec(String cat) throws Exception {
        return exec(cat, true);
    }

    private static List<String> exec(String cat, boolean throwError) throws Exception {
        Process process = null;

        try {
            process = new ProcessBuilder().command("su").start();
            if (cat != null) {
                OutputStream out = null;
                try {
                    out = process.getOutputStream();
                    out.write(cat.getBytes("UTF-8"));
                    out.flush();
                } finally {
                    Utils.mustClose(out);
                }
            }
            List<String> result = new ArrayList<String>();
            int status = process.waitFor();
            if (status == 0) {
                Utils.readAndClose(process.getInputStream(), result, "");
            } else if (throwError) {
                Utils.readAndClose(process.getErrorStream(), result, "");
                if (result.isEmpty()) {
                    throw new Exception("Error execute su");
                } else {
                    throw new Exception("Error execute su: " + result.get(0));
                }
            } else {
                Utils.readAndClose(process.getInputStream(), result, "");
                Utils.readAndClose(process.getErrorStream(), result, "ERROR: ");
            }
            return result;
        } finally {
            if (process != null) {
                try {
                    process.destroy();
                } catch (Throwable ex) {
                }
            }
        }
    }
}
