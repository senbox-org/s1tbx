package org.esa.beam.smos.visat;

import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.JComponent;
import javax.swing.JLabel;

public class SmosSnapshotImageToolView extends SmosToolView {

    public static final String ID =SmosSnapshotImageToolView.class.getName();

    @Override
    protected JComponent createSmosComponent(ProductSceneView smosView) {
        return new JLabel("TODO - place snapshot image here");
    }
}