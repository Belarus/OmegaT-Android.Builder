package org.alex73.android.bel;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.alex73.android.arsc2.ResourceProcessor;
import org.alex73.android.arsc2.Translation;
import org.alex73.android.arsc2.reader.ChunkReader2;
import org.alex73.android.arsc2.translation.TranslationStoreDefaults;
import org.alex73.android.arsc2.translation.TranslationStorePackage;
import org.alex73.android.arsc2.writer.ChunkWriter2;
import org.alex73.android.bel.LocalStorage.Permissions;
import org.alex73.android.common.FileInfo;
import org.alex73.android.common.JniWrapper;

import android.os.StatFs;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class Step4 extends Step {
    TranslationStoreDefaults translationDefaults;

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

        showOperation(R.string.opCheckInstalled);
        incProgress();

        local = new LocalStorage();

        List<FileInfo> files = local.getLocalFiles();

        // remove non-translatable apk from list
        for (Iterator<FileInfo> it = files.iterator(); it.hasNext();) {
            FileInfo fi = it.next();
            if (!needTranslate(fi)) {
                it.remove();
            }

            if (stopped) {
                return;
            }
        }

        StatFs freeStat = local.getFreeSpaceForBackups();
        int requiredBlocks = 0;
        int blockSize = freeStat.getBlockSize();
        for (FileInfo fi : files) {
            if (!local.isFileTranslated(fi.localFile)) {
                requiredBlocks += (fi.localSize + blockSize - 1) / blockSize;
            }
        }
        requiredBlocks += 4;
        if (requiredBlocks > freeStat.getAvailableBlocks()) {
            String txt = ui.getResources().getText(R.string.textNoSpace).toString();
            txt = txt.replace("$0", LocalStorage.BACKUP_DIR);
            txt = txt.replace("$1", Utils.textSize(freeStat.getAvailableBlocks() * blockSize));
            txt = txt.replace("$2", Utils.textSize(requiredBlocks * blockSize));
            final String txtOut = txt;
            ui.runOnUiThread(new Runnable() {
                public void run() {
                    new StepFinish(ui, txtOut).doit();
                }
            });
            return;
        }

        showOperation(R.string.opReadTranslation);
        incProgress();

        translationDefaults = new TranslationStoreDefaults(ui.getResources());

        setProgressTotal(files.size());
        showOperation(R.string.opInstall);
        showFile("");

        // translate
        for (final FileInfo fi : files) {
            if (stopped) {
                return;
            }
            incProgress();
            phase = "захоўвалі рэзэрвовую копію " + fi.localFile.getPath();
            showFile(fi.packageName);
            showOperation(R.string.opTranslate);

            if (!local.isFileTranslated(fi.localFile)) {
                local.backupApk(fi);
            }

            phase = "перакладалі " + fi.localFile.getPath();
            translateApk(fi, local);
            phase = "";
            if (stopped) {
                return;
            }

            ui.runOnUiThread(new Runnable() {
                public void run() {
                    textLog.setText(fi.packageName + " перакладзены\n" + textLog.getText());
                }
            });
        }
        local = null;

        if (stopped) {
            return;
        }

        showOperation(R.string.opSetup);
        ui.setGlobalLanguage();

        if (stopped) {
            return;
        }

        ui.runOnUiThread(new Runnable() {
            public void run() {
                String txt = ui.getResources().getText(R.string.textFinished).toString();
                txt = txt.replace("$0", LocalStorage.BACKUP_DIR);
                new StepFinish(ui, txt).doit();
            }
        });
    }

    boolean needTranslate(FileInfo fi) throws Exception {
        return TranslationStorePackage.isPackageTranslated(ui.getResources(), fi.packageName);
    }

    void translateApk(FileInfo fi, LocalStorage local) throws Exception {
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
                rs = new ResourceProcessor(rsReader, null);
            } finally {
                in.close();
            }
        } finally {
            zip.close();
        }

        if (stopped) {
            return;
        }
        rs.process(fi.packageName, new Translation(new TranslationStorePackage(ui.getResources(), fi.packageName),
                translationDefaults));

        if (stopped) {
            return;
        }

        ChunkWriter2 res = rs.save();
        rs = null;
        byte[] translatedResources = res.getBytes();
        res = null;

        if (stopped) {
            return;
        }

        File f = local.patchFileToTemp(fi.localFile, translatedResources);

        if (!local.isFilesEquals(f, fi.localFile)) {
            showOperation(R.string.opInstallTranslation);
            long free = JniWrapper.getSpaceNearFile(fi.localFile);
            long requiredAdditional = f.length() - fi.localFile.length();
            if (free < requiredAdditional + 16 * 1024) {
                throw new Exception("There is no space to overwrite " + fi.localFile.getPath());
            }

            Permissions p = local.getPermissions(fi);
            local.openFileAccess(p);
            try {
                if (p.mountedUid) {
                    local.moveToOriginalUid(f, fi.localFile, p);
                } else {
                    local.moveToOriginal(f, fi.localFile, p);
                }
            } finally {
                local.closeFileAccess(p);
            }
        }
    }
}
