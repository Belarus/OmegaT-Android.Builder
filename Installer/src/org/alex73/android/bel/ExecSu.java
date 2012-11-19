package org.alex73.android.bel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ExecSu {
    static final File SH_FILE = new File(LocalStorage.OUR_DIR, "command");

    public static List<String> execEvenFail(String input) throws Exception {
        return exec(input, false);
    }

    public static List<String> exec(String input) throws Exception {
        return exec(input, true);
    }

    public static byte[] cat(File file) throws Exception {
        Process process = null;

        try {
            Utils.writeString(SH_FILE, "cat '" + file.getPath() + "'");
            process = new ProcessBuilder().command("su", "-c", "sh '" + SH_FILE.getPath() + "'").start();
            List<String> result = new ArrayList<String>();
            byte[] r = Utils.readBytesAndClose(process.getInputStream());
            int status = process.waitFor();
            if (status == 0) {
                return r;
            } else {
                Utils.readLinesAndClose(process.getErrorStream(), result, "");
                if (result.isEmpty()) {
                    throw new Exception("Error execute su");
                } else {
                    throw new Exception("Error execute su: " + result.get(0));
                }
            }
        } finally {
            if (process != null) {
                try {
                    process.destroy();
                } catch (Throwable ex) {
                }
            }
        }
    }

    private static List<String> exec(String input, boolean throwError) throws Exception {
        Process process = null;

        try {
            Utils.writeString(SH_FILE, input);
            process = new ProcessBuilder().command("su", "-c", "sh '" + SH_FILE.getPath() + "'").start();
//            if (input != null) {
//                OutputStream out = null;
//                try {
//                    out = process.getOutputStream();
//                    out.write(input.getBytes("UTF-8"));
//                    out.flush();
//                } finally {
//                    Utils.mustClose(out);
//                }
//            }
            List<String> result = new ArrayList<String>();
            Utils.readLinesAndClose(process.getInputStream(), result, "");
            int status = process.waitFor();
            if (status == 0) {
            } else if (throwError) {
                Utils.readLinesAndClose(process.getErrorStream(), result, "");
                if (result.isEmpty()) {
                    throw new Exception("Error execute su");
                } else {
                    throw new Exception("Error execute su: " + result.get(0));
                }
            } else {
                Utils.readLinesAndClose(process.getErrorStream(), result, "ERROR: ");
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
