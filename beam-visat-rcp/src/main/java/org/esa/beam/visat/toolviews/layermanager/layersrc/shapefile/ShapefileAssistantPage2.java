/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import com.bc.ceres.swing.TableLayout;
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

import javax.swing.JLabel;
import javax.swing.JPanel;
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


        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(1.0);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        final JPanel pageComponent = new JPanel(tableLayout);
        final JLabel label = new JLabel("<html><b>No CRS found for ESRI Shapefile. Please specify.</b>");
        crsSelectionPanel = new CrsSelectionPanel(productCrsForm, customCrsForm, predefinedCrsForm);
        pageComponent.add(label);
        pageComponent.add(crsSelectionPanel);
        return pageComponent;
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