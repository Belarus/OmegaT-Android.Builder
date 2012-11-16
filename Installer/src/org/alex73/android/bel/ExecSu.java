package org.alex73.android.bel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ExecSu {

    public static void execEvenFail(String cat) {
        try {
            exec(cat);
        } catch (Exception ex) {
        }
    }

    public static List<String> exec(String cat) throws Exception {
        Process process = null;
        OutputStream out = null;
        BufferedReader rd = null;

        List<String> result;
        try {
            process = new ProcessBuilder().command("su").start();
            if (cat != null) {
                out = process.getOutputStream();
                try {
                    out.write(cat.getBytes("UTF-8"));
                    out.flush();
                } catch (Exception ex) {
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (Throwable ex) {
                    }
                }
            }
            int status = process.waitFor();
            if (status == 0) {
                result = new ArrayList<String>();
                rd = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"), 8192);
                String s;
                while ((s = rd.readLine()) != null) {
                    result.add(s);
                }
                rd.close();
            } else {
                rd = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"), 8192);
                String s = rd.readLine();
                throw new Exception(s);
            }
            return result;
        } finally {
            if (process != null) {
                try {
                    process.destroy();
                } catch (Throwable ex) {
                }
            }
            if (rd != null) {
                try {
                    rd.close();
                } catch (Throwable ex) {
                }
            }
        }
    }
}
