package org.esa.beam.visat.toolviews.pin;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.ui.PlacemarkDescriptor;

interface TableModelFactory {
    AbstractPlacemarkTableModel createTableModel(PlacemarkDescriptor placemarkDescriptor, Product product, Band[] selectedBands,
                                                 TiePointGrid[] selectedGrids);
}
