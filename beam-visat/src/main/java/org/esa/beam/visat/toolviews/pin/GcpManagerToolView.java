package org.esa.beam.visat.toolviews.pin;

import org.esa.beam.framework.ui.product.GcpDescriptor;

import javax.swing.JComponent;
import java.awt.BorderLayout;

/**
 * A dialog used to manage the list of pins associated with a selected product.
 */
public class GcpManagerToolView extends PlacemarkManagerToolView {

    public static final String ID = GcpManagerToolView.class.getName();

    public GcpManagerToolView() {
        super(GcpDescriptor.INSTANCE);
    }

    @Override
    public JComponent createControl() {
        JComponent control = super.createControl();

        control.add(new GcpGeoCodingForm(), BorderLayout.SOUTH);
        return control;
    }
}
