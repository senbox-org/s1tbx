package org.esa.beam.gpf.common.reproject.ui;

import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductFilter;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.util.ProductUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class CollocationCrsForm extends CrsForm {

    private SourceProductSelector collocateProductSelector;
    private JPanel collocationPanel;


    public CollocationCrsForm(AppContext appContext) {
        super(appContext);
    }

    @Override
    public CoordinateReferenceSystem getCRS(GeoPos referencePos) {
        Product collocationProduct = collocateProductSelector.getSelectedProduct();
        if (collocationProduct != null) {
            return collocationProduct.getGeoCoding().getMapCRS();
        }
        return null;

    }

    @Override
    public JComponent getCrsUI() {
        if (collocationPanel == null) {
            collocationPanel = createCrsUI();
        }
        return collocationPanel;
    }

    @Override
    public void prepareShow() {
        collocateProductSelector.initProducts();
    }

    @Override
    public void prepareHide() {
        collocateProductSelector.releaseProducts();
    }

    private JPanel createCrsUI() {
        collocateProductSelector = new SourceProductSelector(getAppContext(), "Product:");
        collocateProductSelector.setProductFilter(new CollocateProductFilter());
        collocateProductSelector.addSelectionChangeListener(new AbstractSelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                fireCrsChanged();
            }
        });

        final JPanel panel = new JPanel(new BorderLayout(2, 2));
        panel.add(collocateProductSelector.getProductNameComboBox(), BorderLayout.CENTER);
        panel.add(collocateProductSelector.getProductFileChooserButton(), BorderLayout.EAST);
        panel.addPropertyChangeListener("enabled", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                collocateProductSelector.getProductNameComboBox().setEnabled(panel.isEnabled());
                collocateProductSelector.getProductFileChooserButton().setEnabled(panel.isEnabled());
            }
        });
        return panel;
    }


    public Product getCollocationProduct() {
        return collocateProductSelector.getSelectedProduct();
    }

    private class CollocateProductFilter implements ProductFilter {

        @Override
        public boolean accept(Product collocationProduct) {
            if (getReferenceProduct() == collocationProduct ||
                collocationProduct.getGeoCoding() == null) {
                return false;
            }
            final GeoCoding geoCoding = collocationProduct.getGeoCoding();
            if (geoCoding.canGetGeoPos() && geoCoding.canGetPixelPos() && (geoCoding instanceof CrsGeoCoding)) {
                final GeneralPath[] sourcePath = ProductUtils.createGeoBoundaryPaths(getReferenceProduct());
                final GeneralPath[] collocationPath = ProductUtils.createGeoBoundaryPaths(collocationProduct);
                for (GeneralPath path : sourcePath) {
                    Rectangle bounds = path.getBounds();
                    for (GeneralPath colPath : collocationPath) {
                        if (colPath.getBounds().intersects(bounds)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }


}
