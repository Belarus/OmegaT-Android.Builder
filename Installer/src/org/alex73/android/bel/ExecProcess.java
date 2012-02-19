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
        Process process = new ProcessBuilder().command(command).start();
        try {
            if (cat != null) {
                OutputStream out = process.getOutputStream();
                out.write(cat.getBytes("UTF-8"));
                out.flush();
                out.close();
            }
            int status = process.waitFor();
            if (status == 0) {
                BufferedReader rd = new BufferedReader(new InputStreamReader(process.getInputStream(),
                        "UTF-8"), 8192);
                try {
                    String s;
                    while ((s = rd.readLine()) != null) {
                        processOutputLine(s);
                    }
                } finally {
                    rd.close();
                }
            } else {
                BufferedReader rd = new BufferedReader(new InputStreamReader(process.getErrorStream(),
                        "UTF-8"), 8192);
                try {
                    String s = rd.readLine();
                    throw new Exception(s);
                } finally {
                    rd.close();
                }
            }
            return status;
        } finally {
            process.destroy();
        }
    }

    protected void processOutputLine(String line) {
    }
}
