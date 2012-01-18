package org.alex73.android.bel;

import android.text.util.Linkify;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class StepFinish extends Step {
    private final CharSequence text;

    public StepFinish(AndroidBel ui, CharSequence text) {
        super(ui);
        this.text = text;
    }

    protected void show() {
        ui.setContentView(R.layout.step5);

        textLog = (TextView) ui.findViewById(R.id.textLog5);
        textLog.setText(text);
        Linkify.addLinks(textLog, Linkify.ALL);

        btnNext = (Button) ui.findViewById(R.id.btnNext5);
        btnNext.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                ui.finish();
            }
        });
    }
}
