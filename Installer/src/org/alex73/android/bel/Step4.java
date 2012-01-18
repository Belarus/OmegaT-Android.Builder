package org.alex73.android.bel;

import java.io.File;

import android.os.Build;
import android.text.method.ScrollingMovementMethod;
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
        setProgressTotal(ui.countVersions(FileInfo.STATUS.NONTRANSLATED)
                + ui.countVersions(FileInfo.STATUS.UPDATE) * 2);

        showOperation(R.string.opUploadTrans);
        showFile("");
        for (FileInfo fi : ui.filesForProcess) {
            if (stopped) {
                return;
            }
            if (fi.remoteStatus != FileInfo.STATUS.NONTRANSLATED) {
                continue;
            }
            incProgress();

            showFile(fi.localFile.getName());

            remote.upload(fi.localFile, fi.remoteFilename, fi.transferSize);
        }

        showOperation(R.string.opDownloadTrans);
        showFile("");

        for (FileInfo fi : ui.filesForProcess) {
            if (stopped) {
                return;
            }
            if (fi.remoteStatus != FileInfo.STATUS.UPDATE) {
                continue;
            }
            incProgress();

            showFile(fi.localFile.getName());

            String localName = fi.remoteFilename.replace(".arsc.gz", ".arsc");
            if (!local.existFile(localName)) {
                // download file
                showOperation(R.string.opDownload);

                File outFile = local.storeFileBegin(localName);
                remote.loadFile(fi.localFile.getName(), fi.remoteFilename, outFile);
                local.storeFileEnd(localName);
            } else {
                showOperation(R.string.opRead);
                // just load from sdcard for calc sha1
                // translatedResources = local.loadFile(localName);
            }
            fi.localStatusOrigin = fi.translatedID == null;
            fi.translatedID = local.getVersion(localName);
        }

        showOperation(R.string.opInstall);
        showFile("");
        local.remountSystem(true);
        for (FileInfo fi : ui.filesForProcess) {
            if (stopped) {
                break;
            }
            if (fi.remoteStatus != FileInfo.STATUS.UPDATE) {
                continue;
            }
            incProgress();

            showFile(fi.localFile.getName());
            showOperation(R.string.opInstallTranslation);
            if (fi.localStatusOrigin) {
                local.backupApk(fi.localFile);
            }

            String localName = fi.remoteFilename.replace(".arsc.gz", ".arsc");
            byte[] translated = local.loadFile(localName);

            local.patchFile(fi.localFile, fi, translated);
            final File ff = fi.localFile;
            ui.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textLog.setText(textLog.getText() + ff.getName() + " installed\n");
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
}
