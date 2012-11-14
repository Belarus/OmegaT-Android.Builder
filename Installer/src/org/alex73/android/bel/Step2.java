package org.alex73.android.bel;

import java.io.File;
import java.util.List;

import org.alex73.android.common.FileInfo;

import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class Step2 extends Step {
    public Step2(AndroidBel ui) {
        super(ui);
    }

    @Override
    protected void show() {
        ui.setContentView(R.layout.step2);

        labelFile = (TextView) ui.findViewById(R.id.labelFile2);
        labelOperation = (TextView) ui.findViewById(R.id.labelOperation2);
        textLog = (TextView) ui.findViewById(R.id.textLog2);
        progress = (ProgressBar) ui.findViewById(R.id.progress2);
        btnCancel = (Button) ui.findViewById(R.id.btnCenter2);

        textLog.setMovementMethod(new ScrollingMovementMethod());

        btnCancel.setEnabled(true);
    }

    @Override
    protected void process() throws Exception {
//        List<File> files = local.getLocalFiles();
//        setProgressTotal(files.size());
//
//        showOperation(R.string.opCheckInstalled);
//        showFile("");
//        for (File f : files) {
//            if (stopped) {
//                return;
//            }
//            incProgress();
//            showFile(f.getName());
//
//            FileInfo ver = new FileInfo(f);
//            ver.readManifestInfo();
//            if (!ui.translatedPackages.contains(ver.packageName)) {
//                continue;
//            }
//            local.getVersionInfo(ver);
//            if (ver.originID == null) {
//                continue;
//            }
//            ui.filesForProcess.add(ver);
//        }
//
//        showOperation(R.string.opCheckNew);
//        showFile("");
//        remote.requestExist(ui.filesForProcess);
//
//        if (stopped) {
//            return;
//        }
//
//        ui.runOnUiThread(new Runnable() {
//            public void run() {
//                new Step3(ui).doit();
//            }
//        });
    }
}
