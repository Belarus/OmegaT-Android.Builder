package org.alex73.android.bel;

import java.io.PrintWriter;
import java.io.StringWriter;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public abstract class Step extends Thread {
    protected final AndroidBel ui;

    protected TextView labelFile;
    protected TextView labelOperation;
    protected TextView textLog;
    protected Button btnCancel;
    protected Button btnNext;
    protected ProgressBar progress;

    boolean stopped;
    protected LocalStorage local = new LocalStorage();
    protected RemoteSite remote = new RemoteSite();

    public Step(AndroidBel activity) {
        this.ui = activity;
    }

    protected void show() {
    }

    protected void process() throws Exception {
    }

    @Override
    final public void run() {
        try {
            process();
            if (stopped) {
                ui.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new StepFinish(ui, ui.getResources().getText(R.string.textStopped)).doit();
                    }
                });
            }
        } catch (Exception ex) {
            StringWriter wr = new StringWriter();
            ex.printStackTrace(new PrintWriter(wr));
            final String s = wr.toString();
            ui.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new StepFinish(ui, ui.getResources().getText(R.string.textError) + s).doit();
                }
            });
        }
    }

    public void doit() {
        show();

        if (btnCancel != null) {
            btnCancel.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    btnCancel.setEnabled(false);
                    btnCancel.setText(R.string.btnStopping);
                    stopped = true;
                    local.stopped = true;
                    remote.stopped = true;
                }
            });
        }

        start();
    }

    public void setNext(final OnClickListener listener) {
        ui.runOnUiThread(new Runnable() {
            public void run() {
                btnNext.setOnClickListener(listener);
                btnNext.setEnabled(true);
            }
        });
    }

    protected void showFile(final String text) {
        ui.runOnUiThread(new Runnable() {
            public void run() {
                labelFile.setText(text);
            }
        });
    }

    protected void showOperation(final int res) {
        ui.runOnUiThread(new Runnable() {
            public void run() {
                labelOperation.setText(res);
            }
        });
    }

    protected void setProgressTotal(final int num) {
        ui.runOnUiThread(new Runnable() {
            public void run() {
                progress.setProgress(0);
                progress.setMax(num);
            }
        });
    }

    protected void incProgress() {
        ui.runOnUiThread(new Runnable() {
            public void run() {
                progress.setProgress(progress.getProgress() + 1);
            }
        });
    }

    protected void addText(final String text) {
        ui.runOnUiThread(new Runnable() {
            public void run() {
                textLog.setText(textLog.getText() + text);
            }
        });
    }
}
