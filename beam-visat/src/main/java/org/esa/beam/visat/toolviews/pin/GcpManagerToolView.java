package org.esa.beam.visat.toolviews.pin;

import org.esa.beam.framework.datamodel.*;

import java.awt.Component;

/**
 * A dialog used to manage the list of pins associated with a selected product.
 */
public class GcpManagerToolView extends PlacemarkManagerToolView {

    public static final String ID = GcpManagerToolView.class.getName();
    private GcpGeoCodingForm geoCodingForm;

    public GcpManagerToolView() {
        super(GcpDescriptor.INSTANCE, new TableModelFactory() {
            public AbstractPlacemarkTableModel createTableModel(PlacemarkDescriptor placemarkDescriptor, Product product,
                                                                Band[] selectedBands, TiePointGrid[] selectedGrids) {
                return new GcpTableModel(placemarkDescriptor, product, selectedBands, selectedGrids);
            }
        });
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
