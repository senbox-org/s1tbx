package org.esa.beam.framework.ui.crs;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.JComponent;
import javax.swing.JTextField;

public class ProductCrsForm extends CrsForm {

    private final Product product;

    public ProductCrsForm(AppContext appContext, Product product) {
        super(appContext);
        this.product = product;
    }

    @Override
    protected String getLabelText() {
        return "Use target CRS";
    }

    @Override
    public CoordinateReferenceSystem getCRS(GeoPos referencePos) throws FactoryException {
        return getMapCrs();
    }

    private CoordinateReferenceSystem getMapCrs() {
        return product.getGeoCoding().getMapCRS();
    }

    @Override
    protected JComponent createCrsComponent() {
        final JTextField field = new JTextField();
        field.setEditable(false);
        field.setText(getMapCrs().getName().getCode());
        return field;
    }

    @Override
    public void prepareShow() {
    }

    @Override
    public void prepareHide() {
    }
}
