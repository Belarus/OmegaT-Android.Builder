package org.alex73.android.bel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class ExecProcess {
    private final String[] command;

    public ExecProcess(String... command) {
        this.command = command;
    }

    public int exec() throws Exception {
        return exec(null);
    }

    public int exec(String cat) throws Exception {
        Process process = null;
        OutputStream out = null;
        BufferedReader rd = null;

        try {
            process = new ProcessBuilder().command(command).start();
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
                rd = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"), 8192);
                String s;
                while ((s = rd.readLine()) != null) {
                    processOutputLine(s);
                }
                rd.close();
            } else {
                rd = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"), 8192);
                String s = rd.readLine();
                throw new Exception(s);
            }
            return status;
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

    protected void processOutputLine(String line) {
    }
}
