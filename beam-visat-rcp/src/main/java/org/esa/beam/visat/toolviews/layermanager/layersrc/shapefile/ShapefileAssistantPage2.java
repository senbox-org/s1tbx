package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.crs.CrsSelectionPanel;
import org.esa.beam.framework.ui.crs.CustomCrsForm;
import org.esa.beam.framework.ui.crs.PredefinedCrsForm;
import org.esa.beam.framework.ui.crs.ProductCrsForm;
import org.esa.beam.framework.ui.layer.AbstractLayerSourceAssistantPage;
import org.esa.beam.framework.ui.layer.LayerSourcePageContext;
import org.esa.beam.util.ProductUtils;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Component;

class ShapefileAssistantPage2 extends AbstractLayerSourceAssistantPage {

    private CrsSelectionPanel crsSelectionPanel;


    ShapefileAssistantPage2() {
        super("Define CRS");
    }

    @Override
    public Component createPageComponent() {
        final AppContext appContext = getContext().getAppContext();
        final ProductCrsForm productCrsForm = new ProductCrsForm(appContext, appContext.getSelectedProduct());
        final CustomCrsForm customCrsForm = new CustomCrsForm(appContext);
        final PredefinedCrsForm predefinedCrsForm = new PredefinedCrsForm(appContext);

        crsSelectionPanel = new CrsSelectionPanel(productCrsForm, customCrsForm, predefinedCrsForm);
        return crsSelectionPanel;
    }


    @Override
    public boolean validatePage() {
        try {
            crsSelectionPanel.getCrs(ProductUtils.getCenterGeoPos(getContext().getAppContext().getSelectedProduct()));
        } catch (FactoryException ignored) {
            return false;
        }
        return true;
    }

    @Override
    public boolean hasNextPage() {
        return true;
    }

    @Override
    public AbstractLayerSourceAssistantPage getNextPage() {
        final LayerSourcePageContext context = getContext();
        try {
            final Product product = context.getAppContext().getSelectedProduct();
            final GeoPos referencePos = ProductUtils.getCenterGeoPos(product);
            final CoordinateReferenceSystem crs = crsSelectionPanel.getCrs(referencePos);
            context.setPropertyValue(ShapefileLayerSource.PROPERTY_NAME_FEATURE_COLLECTION_CRS, crs);
            return new ShapefileAssistantPage3();
        } catch (FactoryException e) {
            e.printStackTrace();
            context.showErrorDialog("Could not create CRS:\n" + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean canFinish() {
        return false;
    }
}