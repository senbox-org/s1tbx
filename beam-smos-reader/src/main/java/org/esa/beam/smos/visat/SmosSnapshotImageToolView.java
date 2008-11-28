package org.esa.beam.smos.visat;

import org.esa.beam.framework.ui.application.support.AbstractToolView;

import javax.swing.JComponent;
import javax.swing.JLabel;

public class SmosSnapshotImageToolView extends AbstractSmosToolView {

    public static final String ID =SmosSnapshotImageToolView.class.getName();

    @Override
    protected JComponent createSmosControl() {
        return new JLabel("TODO - place snapshot image here");
    }
}