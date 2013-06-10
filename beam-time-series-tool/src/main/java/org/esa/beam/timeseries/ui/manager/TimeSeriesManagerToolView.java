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

package org.esa.beam.timeseries.ui.manager;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductTreeListenerAdapter;
import org.esa.beam.timeseries.core.TimeSeriesMapper;
import org.esa.beam.timeseries.core.insitu.InsituSource;
import org.esa.beam.timeseries.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.timeseries.core.timeseries.datamodel.TimeSeriesChangeEvent;
import org.esa.beam.timeseries.core.timeseries.datamodel.TimeSeriesListener;
import org.esa.beam.visat.VisatApp;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;


/**
 * Main class for the manager tool.
 *
  * @author Marco Peters
 * @author Thomas Storm
 * @author Sabine Embacher
 */
public class TimeSeriesManagerToolView extends AbstractToolView {

    private final AppContext appContext;
    private JPanel controlPanel;
    private Product selectedProduct;
    private String prefixTitle;

    private final WeakHashMap<Product, TimeSeriesManagerForm> formMap;
    private TimeSeriesManagerForm activeForm;
    private final TimeSeriesManagerTSL timeSeriesManagerTSL;

    public TimeSeriesManagerToolView() {
        formMap = new WeakHashMap<Product, TimeSeriesManagerForm>();
        appContext = VisatApp.getApp();
        timeSeriesManagerTSL = new TimeSeriesManagerTSL();
    }

    @Override
    protected JComponent createControl() {
        controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(new EmptyBorder(4, 4, 4, 4));

        prefixTitle = getDescriptor().getTitle();

        setSelectedProduct(appContext.getSelectedProduct());

        VisatApp.getApp().addProductTreeListener(new TSManagerPTL());
        realizeActiveForm();
        updateTitle();
        return controlPanel;
    }

    Product getSelectedProduct() {
        return selectedProduct;
    }

    private void productClosed(Product product) {
        formMap.remove(product);
        setSelectedProduct(null);
    }

    private void updateTitle() {
        final String suffix;
        final Product product = getSelectedProduct();
        if (product != null) {
            suffix = " - " + product.getDisplayName();
        } else {
            suffix = "";
        }
        getDescriptor().setTitle(prefixTitle + suffix);
    }

    private void setSelectedProduct(Product newProduct) {
        Product oldProduct = selectedProduct;
        if (newProduct != oldProduct) {
            if (oldProduct != null) {
                final AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(oldProduct);
                if (timeSeries != null) {
                    timeSeries.removeTimeSeriesListener(timeSeriesManagerTSL);
                }
            }

            selectedProduct = newProduct;
            realizeActiveForm();
            updateTitle();

            if (newProduct != null) {
                final AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(newProduct);
                if (timeSeries != null) {
                    timeSeries.addTimeSeriesListener(timeSeriesManagerTSL);
                }
            }
        }
    }

    private void realizeActiveForm() {
        final JPanel controlPanel = this.controlPanel;

        if (controlPanel.getComponentCount() > 0) {
            controlPanel.remove(0);
        }

        activeForm = getOrCreateActiveForm(getSelectedProduct());
        controlPanel.add(activeForm.getControl(), BorderLayout.CENTER);

        controlPanel.validate();
        controlPanel.repaint();
    }

    private TimeSeriesManagerForm getOrCreateActiveForm(Product product) {
        if (formMap.containsKey(product)) {
            activeForm = formMap.get(product);
        } else {
            activeForm = new TimeSeriesManagerForm(getDescriptor());
            formMap.put(product, activeForm);
        }
        activeForm.updateFormControl(product);
        return activeForm;
    }

    private void updateInsituPins() {
        final AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(selectedProduct);
        timeSeries.clearInsituPlacemarks();
        addPlacemarks(timeSeries);
    }

    private void addPlacemarks(AbstractTimeSeries timeSeries) {
        final InsituSource insituSource = timeSeries.getInsituSource();
        final List<String> selectedInsituVariables = getSelectedInsituVariables(timeSeries, insituSource);
        final Set<GeoPos> geoPoses = new TreeSet<GeoPos>(createGeoPosComparator());
        for (String selectedInsituVariable : selectedInsituVariables) {
            geoPoses.addAll(insituSource.getInsituPositionsFor(selectedInsituVariable));
        }

        final Product tsProduct = timeSeries.getTsProduct();
        final GeoCoding geoCoding = tsProduct.getGeoCoding();

        final PixelPos pixelPos = new PixelPos();
        for (GeoPos geoPos : geoPoses) {
            geoCoding.getPixelPos(geoPos, pixelPos);
            if (!AbstractTimeSeries.isPixelValid(tsProduct, pixelPos)) {
                continue;
            }
            String name;
            if (insituSource.hasStationNames()) {
                name = insituSource.getNameFor(geoPos);
            } else {
                name = geoPos.getLatString() + "_" + geoPos.getLonString();
            }

            final String pinName = "Insitu_" + name;
            final String pinLabel = name;
            final String pinDescription = name;
            final Placemark placemark = Placemark.createPointPlacemark(
                        PinDescriptor.getInstance(),
                        pinName, pinLabel, pinDescription,
                        null, new GeoPos(geoPos), geoCoding);
            timeSeries.registerRelation(placemark, geoPos);
        }
    }

    private Comparator<GeoPos> createGeoPosComparator() {
        return new Comparator<GeoPos>() {
            @Override
            public int compare(GeoPos o1, GeoPos o2) {
                return o1.toString().compareTo(o2.toString());
            }
        };
    }

    private List<String> getSelectedInsituVariables(AbstractTimeSeries timeSeries, InsituSource insituSource) {
        final String[] parameterNames = insituSource.getParameterNames();
        final List<String> selectedInsituVariables = new ArrayList<String>();
        for (String parameterName : parameterNames) {
            if (timeSeries.isInsituVariableSelected(parameterName)) {
                selectedInsituVariables.add(parameterName);
            }
        }
        return selectedInsituVariables;
    }

    private class TSManagerPTL extends ProductTreeListenerAdapter {

        @Override
        public void productRemoved(Product product) {
            productClosed(product);
        }

        @Override
        public void productNodeSelected(ProductNode productNode, int clickCount) {
            setSelectedProduct(getProduct(productNode));
        }

        private Product getProduct(ProductNode productNode) {
            while (true) {
                if (productNode instanceof ProductNodeGroup<?>) {
                    ProductNodeGroup<?> productNodeGroup = (ProductNodeGroup<?>) productNode;
                    if (productNodeGroup.getNodeCount() > 0) {
                        productNode = productNodeGroup.get(0);
                        continue;
                    }
                }
                return productNode.getProduct();
            }
        }
    }

    private class TimeSeriesManagerTSL extends TimeSeriesListener {

        @Override
        public void timeSeriesChanged(TimeSeriesChangeEvent event) {
            final int type = event.getType();
            if (type == TimeSeriesChangeEvent.START_TIME_PROPERTY_NAME ||
                type == TimeSeriesChangeEvent.END_TIME_PROPERTY_NAME) {
                activeForm.updateFormControl(getSelectedProduct());
            } else if (type == TimeSeriesChangeEvent.PROPERTY_INSITU_VARIABLE_SELECTION) {
                updateInsituPins();
            }
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            activeForm.updateFormControl(getSelectedProduct());
        }
    }
}
