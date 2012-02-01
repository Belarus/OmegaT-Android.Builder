package org.alex73.android.bel;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.Build;

public class RemoteSite {
    final static String URL = "http://android.mounik.org/";// "http://alex73.byethost14.com/android/";

    protected boolean stopped;

    public void requestTranslatedPackages(Set<String> translatedPackages) throws Exception {
        HttpClient client = new DefaultHttpClient();
        HttpPost request = new HttpPost(URL + "translated2.txt");

        HttpResponse response = client.execute(request);
        if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception("Памылка перадачы: " + response.getStatusLine().getStatusCode());
        }

        HttpEntity responseEntity = response.getEntity();
        try {
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(responseEntity.getContent(), "UTF-8"), 8192);
            try {
                String s;
                while ((s = rd.readLine()) != null) {
                    translatedPackages.add(s);
                }
            } finally {
                rd.close();
            }
        } finally {
            responseEntity.consumeContent();
        }
    }

    /**
     * Чытаем назвы файлаў з сэрвера.
     * 
     * @param fiels
     *            <local_filename,version>
     * @return <local_filename,remote_filename>
     */
    public void requestExist(List<FileInfo> files) throws Exception {
        HttpClient client = new DefaultHttpClient();
        HttpPost request = new HttpPost(URL + "android2.php");

        ByteArrayOutputStream buf = new ByteArrayOutputStream(1024);
        OutputStreamWriter wr = new OutputStreamWriter(buf, "UTF-8");
        wr.write("i|Build.BOARD=" + Build.BOARD + "\n");
        wr.write("i|Build.BRAND=" + Build.BRAND + "\n");
        wr.write("i|Build.DEVICE=" + Build.DEVICE + "\n");
        wr.write("i|Build.MANUFACTURER=" + Build.MANUFACTURER + "\n");
        wr.write("i|Build.MODEL=" + Build.MODEL + "\n");
        wr.write("i|Build.PRODUCT=" + Build.PRODUCT + "\n");
        wr.write("i|Build.VERSION=" + Build.VERSION.RELEASE + "\n");
        for (FileInfo f : files) {
            wr.write("f|");
            wr.write(f.packageName);
            wr.write('|');
            wr.write(f.originID);
            wr.write('|');
            wr.write(f.translatedID != null ? f.translatedID : "0");
            wr.write('|');
            wr.write(f.localFile.getName());
            wr.write("\n");
        }
        wr.flush();

        if (stopped) {
            return;
        }

        ByteArrayEntity entity = new ByteArrayEntity(buf.toByteArray());
        request.setEntity(entity);
        HttpResponse response = client.execute(request);
        if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception("Памылка перадачы: " + response.getStatusLine().getStatusCode());
        }

        if (stopped) {
            return;
        }

        HttpEntity responseEntity = response.getEntity();
        try {
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(responseEntity.getContent(), "UTF-8"), 8192);
            try {
                String s;
                while ((s = rd.readLine()) != null) {
                    String[] v = s.split("\\|");
                    for (FileInfo fi : files) {
                        if (v[0].equals(fi.packageName) && v[1].equals(fi.originID)) {
                            fi.remoteStatus = FileInfo.STATUS.valueOf(v[2]);
                            fi.remoteFilename = v[4];
                            fi.transferSize = Integer.parseInt(v[3]);
                        }
                    }
                }
            } finally {
                rd.close();
            }
        } finally {
            responseEntity.consumeContent();
        }
    }

    public void loadFile(String apkName, String remoteName, File outFile) throws Exception {
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(URL + remoteName);

        HttpResponse response = client.execute(request);
        if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception("Памылка злучэньня: " + response.getStatusLine().getStatusCode() + ' '
                    + response.getStatusLine().getReasonPhrase() + " / " + apkName);
        }
        HttpEntity entity = response.getEntity();
        try {
            InputStream in = entity.getContent();
            OutputStream out = new FileOutputStream(outFile);
            try {
                Utils.copy(new GZIPInputStream(in), out);
            } finally {
                in.close();
                out.close();
            }
        } finally {
            entity.consumeContent();
        }
    }

    byte[] buffer = new byte[64 * 1024];

    public void upload(File localFile, String uploadName, int offset) throws Exception {
        HttpClient client = new DefaultHttpClient();

        InputStream in = new FileInputStream(localFile);
        try {
            in.skip(offset);
            while (true) {
                int len = in.read(buffer);
                if (len < 0) {
                    break;
                }

                if (stopped) {
                    return;
                }

                HttpPost request = new HttpPost(URL + "upload2.php?file=" + uploadName + "&pos=" + offset);
                byte[] b;
                if (len < buffer.length) {
                    b = new byte[len];
                    System.arraycopy(buffer, 0, b, 0, len);
                } else {
                    b = buffer;
                }
                ByteArrayEntity entity = new ByteArrayEntity(b);
                request.setEntity(entity);
                HttpResponse response = client.execute(request);
                if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
                    throw new Exception("Памылка перадачы: " + response.getStatusLine().getStatusCode());
                }
                entity.consumeContent();

                offset += len;
            }
        } finally {
            in.close();
        }
    }
}
