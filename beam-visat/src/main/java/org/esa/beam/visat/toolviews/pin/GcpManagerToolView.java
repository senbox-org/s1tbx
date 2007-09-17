package org.esa.beam.visat.toolviews.pin;

import org.esa.beam.framework.ui.product.GcpDescriptor;

import java.awt.Component;

/**
 * A dialog used to manage the list of pins associated with a selected product.
 */
public class GcpManagerToolView extends PlacemarkManagerToolView {

    public static final String ID = GcpManagerToolView.class.getName();
    private GcpGeoCodingForm geoCodingForm;

    public GcpManagerToolView() {
        super(GcpDescriptor.INSTANCE);
    }

    @Override
    protected Component getSouthExtension() {
        geoCodingForm = new GcpGeoCodingForm();
        return geoCodingForm;
    }

    @Override
    protected void updateUIState() {
        super.updateUIState();
        geoCodingForm.setProduct(getProduct());
        geoCodingForm.updateUIState();
    }

}
