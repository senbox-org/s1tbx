package org.esa.beam.visat.toolviews.placemark.gcp;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GcpDescriptor;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.visat.toolviews.placemark.PlacemarkManagerToolView;
import org.esa.beam.visat.toolviews.placemark.TableModelFactory;
import org.esa.beam.visat.toolviews.placemark.AbstractPlacemarkTableModel;

import javax.swing.table.TableColumnModel;
import java.awt.Component;
import java.text.DecimalFormat;

/**
 * A dialog used to manage the list of pins associated with a selected product.
 */
public class GcpManagerToolView extends PlacemarkManagerToolView {

    public static final String ID = GcpManagerToolView.class.getName();
    private GcpGeoCodingForm geoCodingForm;
    private final ProductNodeListenerAdapter geoCodinglistener;

    public GcpManagerToolView() {
        super(GcpDescriptor.INSTANCE, new TableModelFactory() {
            public AbstractPlacemarkTableModel createTableModel(PlacemarkDescriptor placemarkDescriptor,
                                                                Product product,
                                                                Band[] selectedBands, TiePointGrid[] selectedGrids) {
                return new GcpTableModel(placemarkDescriptor, product, selectedBands, selectedGrids);
            }
        });
        geoCodinglistener = new ProductNodeListenerAdapter() {

            @Override
            public void nodeChanged(ProductNodeEvent event) {
                if (Product.PROPERTY_NAME_GEOCODING.equals(event.getPropertyName())) {
                    updateUIState();
                }

            }
        };
    }

    @Override
    protected Component getSouthExtension() {
        geoCodingForm = new GcpGeoCodingForm();
        return geoCodingForm;
    }

    @Override
    public void setProduct(Product product) {
        final Product oldProduct = getProduct();
        if (oldProduct != product) {
            if (oldProduct != null) {
                oldProduct.removeProductNodeListener(geoCodinglistener);
            }
            if(product != null) {
                product.addProductNodeListener(geoCodinglistener);
            }
        }
        super.setProduct(product);
    }

    @Override
    protected void addCellRenderer(TableColumnModel columnModel) {
        super.addCellRenderer(columnModel);
        columnModel.getColumn(4).setCellRenderer(new FloatTableCellRenderer(new DecimalFormat("0.000000")));
        columnModel.getColumn(5).setCellRenderer(new FloatTableCellRenderer(new DecimalFormat("0.000000")));
    }

    @Override
    protected void updateUIState() {
        super.updateUIState();
        geoCodingForm.setProduct(getProduct());
        geoCodingForm.updateUIState();
    }
}
