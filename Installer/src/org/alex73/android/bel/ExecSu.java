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
            MyLog.log("## ExecSu.cat: " + file.getPath());
            Utils.writeString(SH_FILE, "cat '" + file.getPath() + "'");
            process = new ProcessBuilder().command("su", "-c", "sh '" + SH_FILE.getPath() + "'").start();
            List<String> result = new ArrayList<String>();
            byte[] r = Utils.readBytesAndClose(process.getInputStream());
            int status = process.waitFor();
            if (status == 0) {
                MyLog.log("## ExecSu.cat: OK");
                return r;
            } else {
                Utils.readLinesAndClose(process.getErrorStream(), result, "");
                if (result.isEmpty()) {
                    MyLog.log("## ExecSu.cat: ERROR");
                    throw new Exception("Error execute su");
                } else {
                    MyLog.log("## ExecSu.cat: ERROR: " + result.get(0));
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
            MyLog.log("## ExecSu.exec: " + input);
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
                MyLog.log("## ExecSu.exec: OK");
            } else if (throwError) {
                Utils.readLinesAndClose(process.getErrorStream(), result, "");
                if (result.isEmpty()) {
                    MyLog.log("## ExecSu.exec: ERROR");
                    throw new Exception("Error execute su");
                } else {
                    MyLog.log("## ExecSu.exec: ERROR: " + result.get(0));
                    throw new Exception("Error execute su: " + result.get(0));
                }
            } else {
                MyLog.log("## ExecSu.exec: ERROR_HIDE");
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
