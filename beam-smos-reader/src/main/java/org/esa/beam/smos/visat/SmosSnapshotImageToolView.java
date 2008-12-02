package org.esa.beam.smos.visat;

import javax.swing.JComponent;
import javax.swing.JLabel;

public class SmosSnapshotImageToolView extends SmosToolView {

    public static final String ID =SmosSnapshotImageToolView.class.getName();

    @Override
    protected JComponent createSmosControl() {
        return new JLabel("TODO - place snapshot image here");
    }
}