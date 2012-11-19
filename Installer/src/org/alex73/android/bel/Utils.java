package org.alex73.android.bel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Utils {

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[32768];
        while (true) {
            int sz = in.read(buffer);
            if (sz < 0) {
                break;
            }
            out.write(buffer, 0, sz);
        }
    }

    public static void writeLines(File file, List<String> lines) throws IOException {
        BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
        try {
            for (String s : lines) {
                wr.write(s + "\n");
            }
        } finally {
            wr.close();
        }
    }

    public static void writeString(File file, String line) throws IOException {
        Writer wr = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
        try {
            wr.write(line);
        } finally {
            mustClose(wr);
        }
    }

    public static byte[] readBytesAndClose(InputStream in) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            copy(in, out);
            return out.toByteArray();
        } finally {
            Utils.mustClose(in);
        }
    }

    public static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        GZIPOutputStream go = new GZIPOutputStream(o);
        go.write(data);
        go.flush();
        go.close();
        return o.toByteArray();
    }

    public static void readLinesAndClose(InputStream in, List<String> result, String prefix) throws IOException {
        BufferedReader rd = null;
        try {
            rd = new BufferedReader(new InputStreamReader(in, "UTF-8"), 8192);
            String s;
            while ((s = rd.readLine()) != null) {
                result.add(prefix + s);
            }
        } finally {
            Utils.mustClose(rd);
        }
    }

    public static void mustClose(Closeable in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException ex) {
            }
        }
    }

    /**
     * calculate sha1
     */
    public static String sha1(ZipFile zip, ZipEntry entry) throws Exception {
        InputStream in = zip.getInputStream(entry);
        try {
            return sha1(in);
        } finally {
            in.close();
        }
    }

    /**
     * calculate sha1
     */
    public static String sha1(File f) throws Exception {
        InputStream in = new FileInputStream(f);
        try {
            return sha1(in);
        } finally {
            in.close();
        }
    }

    /**
     * calculate sha1
     */
    public static String sha1(byte[] data) throws Exception {
        InputStream in = new ByteArrayInputStream(data);
        try {
            return sha1(in);
        } finally {
            in.close();
        }
    }

    /**
     * calculate sha1
     */
    public static String sha1(InputStream in) throws Exception {
        MessageDigest digester = MessageDigest.getInstance("SHA-1");

        int byteCount;
        byte[] bytes = new byte[8192];
        while ((byteCount = in.read(bytes)) > 0) {
            digester.update(bytes, 0, byteCount);
        }

        // convert digest to string
        byte[] digest = digester.digest();
        StringBuilder s = new StringBuilder(40);
        for (int b : digest) {
            String hex = Integer.toHexString(b);
            if (hex.length() == 1) {
                s.append('0');
            }
            if (hex.length() > 2) {
                hex = hex.substring(hex.length() - 2);
            }
            s.append(hex);
        }

        return s.toString();
    }

    public static String textSize(long size) {
        if (size > 4 * 1024 * 1024) {
            return size / 1024 / 1024 + "MiB";
        } else if (size > 4 * 1024) {
            return size / 1024 + "KiB";
        } else {
            return size + "B";
        }
    }
}
