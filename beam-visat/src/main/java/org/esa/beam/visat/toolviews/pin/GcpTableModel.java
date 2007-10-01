package org.esa.beam.visat.toolviews.pin;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;

public class GcpTableModel extends AbstractPlacemarkTableModel {

    public GcpTableModel(PlacemarkDescriptor placemarkDescriptor, Product product, Band[] selectedBands,
                         TiePointGrid[] selectedGrids) {
        super(placemarkDescriptor, product, selectedBands, selectedGrids);
    }

    public String[] getDefaultColumnNames() {
        return new String[]{"X", "Y", "Lon", "Lat", "RMSE", "Label"};
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex < getDefaultColumnNames().length && columnIndex != 4;
    }

}
