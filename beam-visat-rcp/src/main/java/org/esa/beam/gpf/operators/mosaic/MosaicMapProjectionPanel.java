/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.gpf.operators.mosaic;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModelRegistry;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.BoundsInputPanel;
import org.esa.beam.framework.ui.WorldMapPane;
import org.esa.beam.framework.ui.WorldMapPaneDataModel;
import org.esa.beam.framework.ui.crs.CrsForm;
import org.esa.beam.framework.ui.crs.CrsSelectionPanel;
import org.esa.beam.framework.ui.crs.CustomCrsForm;
import org.esa.beam.framework.ui.crs.PredefinedCrsForm;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
class MosaicMapProjectionPanel extends JPanel {

    private final AppContext appContext;
    private final MosaicFormModel mosaicModel;

    private CrsSelectionPanel crsSelectionPanel;
    private BoundsInputPanel boundsInputPanel;
    private final BindingContext bindingCtx;
    private String[] demValueSet;

    MosaicMapProjectionPanel(AppContext appContext, MosaicFormModel mosaicModel) {
        this.appContext = appContext;
        this.mosaicModel = mosaicModel;
        bindingCtx = new BindingContext(mosaicModel.getPropertySet());
        init();
        createUI();
        updateForCrsChanged();
        bindingCtx.adjustComponents();
    }

    private void init() {
        final ElevationModelDescriptor[] descriptors = ElevationModelRegistry.getInstance().getAllDescriptors();
        demValueSet = new String[descriptors.length];
        for (int i = 0; i < descriptors.length; i++) {
            demValueSet[i] = descriptors[i].getName();
        }
        if (demValueSet.length > 0) {
            mosaicModel.getPropertySet().setValue("elevationModelName", demValueSet[0]);
        }
        bindingCtx.addPropertyChangeListener("updateMode", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final Boolean updateMode = (Boolean) evt.getNewValue();
                Boolean enabled1 = !updateMode;
                crsSelectionPanel.setEnabled(enabled1);
            }
        });
    }

    private void createUI() {
        final TableLayout layout = new TableLayout(1);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.BOTH);
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(0.0);
        layout.setRowWeightY(2, 1.0);
        layout.setTablePadding(3, 3);
        setLayout(layout);
        CrsForm customCrsUI = new CustomCrsForm(appContext);
        CrsForm predefinedCrsUI = new PredefinedCrsForm(appContext);
        crsSelectionPanel = new CrsSelectionPanel(customCrsUI, predefinedCrsUI);
        crsSelectionPanel.addPropertyChangeListener("crs", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateForCrsChanged();
            }
        });
        add(crsSelectionPanel);
        add(createOrthorectifyPanel());
        add(createMosaicBoundsPanel());
    }

    private void updateForCrsChanged() {
        final float lon = (float) mosaicModel.getTargetEnvelope().getMedian(0);
        final float lat = (float) mosaicModel.getTargetEnvelope().getMedian(1);
        try {
            final CoordinateReferenceSystem crs = crsSelectionPanel.getCrs(new GeoPos(lat, lon));
            if (crs != null) {
                updatePixelUnit(crs);
                mosaicModel.setTargetCRS(crs.toWKT());
            } else {
                mosaicModel.setTargetCRS(null);
            }
        } catch (FactoryException ignored) {
            mosaicModel.setTargetCRS(null);
        }
    }

    private void updatePixelUnit(CoordinateReferenceSystem crs) {
        boundsInputPanel.updatePixelUnit(crs);
    }

    private JPanel createMosaicBoundsPanel() {
        final TableLayout layout = new TableLayout(1);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.BOTH);
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(0.0);
        layout.setRowWeightY(1, 1.0);
        layout.setRowAnchor(2, TableLayout.Anchor.EAST);
        layout.setRowFill(2, TableLayout.Fill.NONE);
        layout.setTablePadding(3, 3);
        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Mosaic Bounds"));
        final WorldMapPaneDataModel worldMapModel = mosaicModel.getWorldMapModel();
        setMapBoundary(worldMapModel);

        final WorldMapPane worldMapPanel = new WorldMapPane(worldMapModel);
        bindingCtx.addPropertyChangeListener(new MapBoundsChangeListener());
        worldMapPanel.setMinimumSize(new Dimension(250, 125));
        worldMapPanel.setBorder(BorderFactory.createEtchedBorder());

        final JCheckBox showSourceProductsCheckBox = new JCheckBox("Display source products");
        bindingCtx.bind(MosaicFormModel.PROPERTY_SHOW_SOURCE_PRODUCTS, showSourceProductsCheckBox);

        boundsInputPanel = new BoundsInputPanel(bindingCtx, MosaicFormModel.PROPERTY_UPDATE_MODE);

        panel.add(boundsInputPanel.createBoundsInputPanel(true));
        panel.add(worldMapPanel);
        panel.add(showSourceProductsCheckBox);

        return panel;
    }

    private JPanel createOrthorectifyPanel() {
        final TableLayout layout = new TableLayout(2);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(1.0);
        layout.setTablePadding(3, 3);
        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Orthorectification"));

        final JCheckBox orthoCheckBox = new JCheckBox("Orthorectify input products");
        bindingCtx.bind("orthorectify", orthoCheckBox);
        bindingCtx.bindEnabledState("orthorectify", false, "updateMode", true);
        final JComboBox demComboBox = new JComboBox(new DefaultComboBoxModel(demValueSet));
        bindingCtx.bind("elevationModelName", demComboBox);
        bindingCtx.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ("orthorectify".equals(evt.getPropertyName()) ||
                    "updateMode".equals(evt.getPropertyName())) {
                    final PropertySet propertySet = bindingCtx.getPropertySet();
                    boolean updateMode = Boolean.TRUE.equals(propertySet.getValue("updateMode"));
                    boolean orthorectify = Boolean.TRUE.equals(propertySet.getValue("orthoretify"));
                    demComboBox.setEnabled(orthorectify && !updateMode);
                }
            }
        });
        layout.setCellColspan(0, 0, 2);
        panel.add(orthoCheckBox);

        layout.setCellWeightX(1, 0, 0.0);
        panel.add(new JLabel("Elevation model:"));
        layout.setCellWeightX(1, 1, 1.0);
        panel.add(demComboBox);
        return panel;
    }

    private void setMapBoundary(WorldMapPaneDataModel worldMapModel) {
        Product boundaryProduct;
        try {
            boundaryProduct = mosaicModel.getBoundaryProduct();
        } catch (Throwable ignored) {
            boundaryProduct = null;
        }
        worldMapModel.setSelectedProduct(boundaryProduct);
    }

    public void prepareShow() {
        crsSelectionPanel.prepareShow();
    }

    public void prepareHide() {
        crsSelectionPanel.prepareHide();
    }

    private class MapBoundsChangeListener implements PropertyChangeListener {

        private final List<String> knownProperties;

        private MapBoundsChangeListener() {
            knownProperties = Arrays.asList("westBound", "northBound", "eastBound", "southBound", "crs");
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (knownProperties.contains(evt.getPropertyName())) {
                setMapBoundary(mosaicModel.getWorldMapModel());
            }
        }
    }
}
