package org.alex73.android.bel;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.alex73.android.arsc2.ResourceProcessor;
import org.alex73.android.arsc2.Translation;
import org.alex73.android.arsc2.reader.ChunkReader2;
import org.alex73.android.common.FileInfo;

import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class Step4 extends Step {
    public Step4(AndroidBel ui) {
        super(ui);
    }

    @Override
    protected void show() {
        ui.setContentView(R.layout.step4);

        labelOperation = (TextView) ui.findViewById(R.id.labelOperation4);
        labelFile = (TextView) ui.findViewById(R.id.labelFile4);
        progress = (ProgressBar) ui.findViewById(R.id.progress4);
        textLog = (TextView) ui.findViewById(R.id.textLog4);
        btnCancel = (Button) ui.findViewById(R.id.btnCenter4);

        textLog.setMovementMethod(new ScrollingMovementMethod());

        btnCancel.setEnabled(true);
    }

    @Override
    protected void process() throws Exception {
        setProgressTotal(2);

        showOperation(R.string.opReadTranslation);
        incProgress();
        InputStream inTr = ui.getResources().openRawResource(R.raw.translation);
        if (inTr == null) {
            throw new Exception("Translation not found");
        }
        try {
            ui.translation = new Translation(new GZIPInputStream(inTr));
        } finally {
            inTr.close();
        }

        showOperation(R.string.opCheckInstalled);
        incProgress();

        // delete .new files
        List<File> filesOldNew = local.getLocalFilesNew();
        for (File f : filesOldNew) {
            f.delete();
        }

        List<FileInfo> files = new ArrayList<FileInfo>();
        // remove non-translated apk from list
        for (File f : local.getLocalFiles()) {
            if (stopped) {
                return;
            }
            FileInfo fi = new FileInfo(f);
            if (needTranslate(fi)) {
                files.add(fi);
            }
        }

        setProgressTotal(files.size());
        showOperation(R.string.opInstall);
        showFile("");
        local.remountSystem(true);
        // translate
        for (FileInfo fi : files) {
            if (stopped) {
                return;
            }
            incProgress();

            showFile(fi.localFile.getName());
            showOperation(R.string.opInstallTranslation);
            local.backupApk(fi.localFile);

            translateApk(fi);
            if (stopped) {
                return;
            }

            final File ff = fi.localFile;
            ui.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textLog.setText(textLog.getText() + ff.getName() + " перакладзены\n");
                }
            });
        }
        try {
            local.remountSystem(false);
        } catch (Exception ex) {
            // hide remount exception
        }
        if (stopped) {
            return;
        }

        showOperation(R.string.opSetup);
        ui.setGlobalLanguage();

        if (stopped) {
            return;
        }

        ui.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new StepFinish(ui, ui.getResources().getText(R.string.textFinished)).doit();
            }
        });
    }

    boolean needTranslate(FileInfo fi) throws Exception {
        fi.readManifestInfo();

        ZipFile zip = new ZipFile(fi.localFile);
        ZipEntry en = zip.getEntry("resources.arsc");
        zip.close();

        if (en == null) {
            return false;
        }

        return ui.translation.isPackageTranslated(fi.packageName);
    }

    void translateApk(FileInfo fi) throws Exception {
        if (stopped) {
            return;
        }

        Log.v("AndroidBel", "Translate " + fi.localFile);

        ResourceProcessor rs;

        ZipFile zip = new ZipFile(fi.localFile);
        try {
            ZipEntry en = zip.getEntry("resources.arsc");

            if (stopped) {
                return;
            }
            InputStream in = zip.getInputStream(en);
            try {
                ChunkReader2 rsReader = new ChunkReader2(in);
                rs = new ResourceProcessor(rsReader);
            } finally {
                in.close();
            }
        } finally {
            zip.close();
        }

        if (stopped) {
            return;
        }
        rs.process(fi.packageName, ui.translation);

        if (stopped) {
            return;
        }

        byte[] translatedResources = rs.save();

        if (stopped) {
            return;
        }
        local.patchFile(fi.localFile, translatedResources);
    }
}
