package org.alex73.android.bel;

import java.util.ArrayList;
import java.util.List;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class Step3 extends Step {
    LinearLayout list;
    Button btnSelectAll, btnDeselectAll;
    TextView textDownloadSize, textUploadSize;

    int currentDownloadSize;
    int uploadSize;

    public Step3(AndroidBel ui) {
        super(ui);
    }

    @Override
    protected void show() {
        ui.setContentView(R.layout.step3);

        labelOperation = (TextView) ui.findViewById(R.id.labelOperation3);
        btnNext = (Button) ui.findViewById(R.id.btnNext3);
        list = (LinearLayout) ui.findViewById(R.id.list3);
        btnSelectAll = (Button) ui.findViewById(R.id.btnSelectAll3);
        btnDeselectAll = (Button) ui.findViewById(R.id.btnDeselectAll3);
        textDownloadSize = (TextView) ui.findViewById(R.id.textDownloadCount3);
        textUploadSize = (TextView) ui.findViewById(R.id.textUploadCount3);

        final List<CheckBox> cbs = new ArrayList<CheckBox>();
        for (FileInfo fi : ui.filesForProcess) {
            switch (fi.remoteStatus) {
            case UPDATE:
                CheckBox cb = new CheckBox(ui);
                cb.setText(fi.localFile.getName());
                cb.setOnCheckedChangeListener(checkedChangeListener);
                cb.setChecked(true);
                cbs.add(cb);
                LayoutParams lp = new LayoutParams(0, 0);
                lp.width = LayoutParams.FILL_PARENT;
                lp.height = LayoutParams.WRAP_CONTENT;
                list.addView(cb, lp);
                break;
            case NONTRANSLATED:
                uploadSize += fi.localSize - fi.transferSize;
                break;
            }
        }

        String txt = ui.getResources().getText(R.string.textUploadSize).toString();
        txt = txt.replace("$0", Long.toString(Math.round(uploadSize / 1024.0 * 0.7))); // 0.7 - gzip ratio
        textUploadSize.setText(txt);

        btnSelectAll.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                for (CheckBox cb : cbs) {
                    cb.setChecked(true);
                }
            }
        });
        btnDeselectAll.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                for (CheckBox cb : cbs) {
                    cb.setChecked(false);
                }
            }
        });

        btnNext.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // remove non-required translations
                for (CheckBox cb : cbs) {
                    if (!cb.isChecked()) {
                        for (FileInfo fi : ui.filesForProcess) {
                            if (fi.localFile.getName().equals(cb.getText())) {
                                fi.remoteStatus = null;
                            }
                        }
                    }
                }
                new Step4(ui).doit();
            }
        });
    }

    OnCheckedChangeListener checkedChangeListener = new OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            for (FileInfo fi : ui.filesForProcess) {
                if (fi.localFile.getName().equals(buttonView.getText())) {
                    if (isChecked) {
                        currentDownloadSize += fi.transferSize;
                    } else {
                        currentDownloadSize -= fi.transferSize;
                    }
                    String txt = ui.getResources().getText(R.string.textDownloadSize).toString();
                    txt = txt.replace("$0", Long.toString(Math.round(currentDownloadSize / 1024.0)));
                    textDownloadSize.setText(txt);
                }
            }
        }
    };
}
