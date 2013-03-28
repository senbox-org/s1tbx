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

package org.esa.beam.timeseries.ui.graph;

import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.timeseries.core.TimeSeriesMapper;
import org.esa.beam.timeseries.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.timeseries.core.timeseries.datamodel.TimeSeriesChangeEvent;
import org.esa.beam.timeseries.core.timeseries.datamodel.TimeSeriesListener;
import org.esa.beam.timeseries.ui.player.TimeSeriesPlayerToolView;
import org.esa.beam.visat.VisatApp;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;

import javax.swing.JComponent;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static org.esa.beam.timeseries.core.timeseries.datamodel.AbstractTimeSeries.rasterToVariableName;

/**
 * Main class for the graph tool.
 *
 * @author Marco Peters
 * @author Thomas Storm
 * @author Sabine Embacher
 */
public class TimeSeriesGraphToolView extends AbstractToolView {

    private static final String DEFAULT_RANGE_LABEL = "Value";
    private static final String DEFAULT_DOMAIN_LABEL = "Time";

    private final TimeSeriesPPL pixelPosListener;
    private final PropertyChangeListener pinSelectionListener;
    private final PropertyChangeListener sliderListener;
    private final TimeSeriesListener timeSeriesGraphTSL;

    private String titleBase;
    private JFreeChart chart;
    private TimeSeriesGraphForm graphForm;
    private TimeSeriesGraphModel graphModel;

    private ProductSceneView currentView;
    private final TimeSeriesValidator validator;

    public TimeSeriesGraphToolView() {
        pixelPosListener = new TimeSeriesPPL();
        pinSelectionListener = new PinSelectionListener();
        sliderListener = new SliderListener();
        timeSeriesGraphTSL = new TimeSeriesGraphTSL();
        validator = new TimeSeriesValidator();
    }

    @Override
    protected JComponent createControl() {
        titleBase = getDescriptor().getTitle();

        final boolean displayLegend = true;
        final boolean showTooltips = true;
        final boolean showUrls = false;
        chart = ChartFactory.createTimeSeriesChart(null,
                                                   DEFAULT_DOMAIN_LABEL,
                                                   DEFAULT_RANGE_LABEL,
                                                   null, displayLegend, showTooltips, showUrls);
        graphModel = new TimeSeriesGraphModel(chart.getXYPlot(), validator);
        graphForm = new TimeSeriesGraphForm(graphModel, chart, validator, getDescriptor().getHelpId());

        final VisatApp visatApp = VisatApp.getApp();
        visatApp.addInternalFrameListener(new TimeSeriesIFL());

        ProductSceneView view = visatApp.getSelectedProductSceneView();
        if (view != null) {
            maySetCurrentView(view);
        }
        return graphForm.getControl();
    }

    /*
     * Checks if the view displays a timeseries product.
     * If so it is set as the current view.
     */
    private void maySetCurrentView(ProductSceneView view) {
        final String viewProductType = view.getProduct().getProductType();
        if (view != currentView &&
            !view.isRGB() &&
            viewProductType.equals(AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE) &&
            TimeSeriesMapper.getInstance().getTimeSeries(view.getProduct()) != null) {
            setCurrentView(view);
        }
    }

    private void setCurrentView(ProductSceneView newView) {
        if (currentView == newView) {
            return;
        }
        if (currentView != null) {
            final AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(
                    currentView.getProduct());
            timeSeries.removeTimeSeriesListener(timeSeriesGraphTSL);
            currentView.removePixelPositionListener(pixelPosListener);
            currentView.removePropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN, pinSelectionListener);
            currentView.removePropertyChangeListener(TimeSeriesPlayerToolView.TIME_PROPERTY, sliderListener);
        }
        currentView = newView;
        graphForm.setButtonsEnabled(currentView != null);
        if (currentView != null) {
            final Product currentProduct = currentView.getProduct();
            final AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(currentProduct);
            timeSeries.addTimeSeriesListener(timeSeriesGraphTSL);
            currentView.addPixelPositionListener(pixelPosListener);
            currentView.addPropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN, pinSelectionListener);
            currentView.addPropertyChangeListener(TimeSeriesPlayerToolView.TIME_PROPERTY, sliderListener);

            final RasterDataNode raster = currentView.getRaster();

            graphModel.adaptToTimeSeries(timeSeries);
            graphModel.updateAnnotation(raster);
            graphModel.updateTimeSeries(null, TimeSeriesType.INSITU);
            graphModel.updateTimeSeries(null, TimeSeriesType.PIN);

            String variableName = rasterToVariableName(raster.getName());
            setTitle(String.format("%s - %s", titleBase, variableName));
        } else {
            graphModel.removeAnnotation();
            graphModel.adaptToTimeSeries(null);
            graphModel.updateTimeSeries(null, TimeSeriesType.INSITU);
            graphModel.updateTimeSeries(null, TimeSeriesType.PIN);

            setTitle(titleBase);
        }
    }

    private void updateTimeSeries(AbstractTimeSeries timeSeries) {
        graphModel.adaptToTimeSeries(timeSeries);
        graphModel.updateTimeSeries(null, TimeSeriesType.INSITU);
        graphModel.updateTimeSeries(null, TimeSeriesType.PIN);
    }

    private class TimeSeriesIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                maySetCurrentView((ProductSceneView) contentPane);
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane == currentView) {
                setCurrentView(null);
            }
        }
    }

    private class TimeSeriesPPL implements PixelPositionListener {

        @Override
        public void pixelPosChanged(ImageLayer imageLayer, int pixelX, int pixelY,
                                    int currentLevel, boolean pixelPosValid, MouseEvent e) {
            if (!graphModel.isShowCursorTimeSeries()) {
                return;
            }
            if (pixelPosValid && isVisible() && currentView != null) {
                final TimeSeriesGraphUpdater.Position position = new TimeSeriesGraphUpdater.Position(pixelX, pixelY, currentLevel);
                graphModel.updateTimeSeries(position, TimeSeriesType.CURSOR);
            }

            final boolean autorange = e.isShiftDown();
            final XYPlot xyPlot = chart.getXYPlot();
            for (int i = 0; i < xyPlot.getRangeAxisCount(); i++) {
                xyPlot.getRangeAxis(i).setAutoRange(autorange);
            }
            graphModel.updateAnnotation(currentView.getRaster());
        }

        @Override
        public void pixelPosNotAvailable() {
            if (!graphModel.isShowCursorTimeSeries()) {
                return;
            }
            graphModel.updateTimeSeries(null, TimeSeriesType.CURSOR);
        }
    }

    private class PinSelectionListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (graphModel.isShowingSelectedPins()) {
                graphModel.updateTimeSeries(null, TimeSeriesType.PIN);
                graphModel.updateTimeSeries(null, TimeSeriesType.INSITU);
            }
        }
    }

    private class TimeSeriesGraphTSL extends TimeSeriesListener {

        @Override
        public void timeSeriesChanged(TimeSeriesChangeEvent event) {
            int type = event.getType();
            if (type == TimeSeriesChangeEvent.PROPERTY_PRODUCT_LOCATIONS ||
                    type == TimeSeriesChangeEvent.PROPERTY_EO_VARIABLE_SELECTION) {
                graphModel.updateAnnotation(currentView.getRaster());
                updateTimeSeries(event.getTimeSeries());
            } else if (type == TimeSeriesChangeEvent.PROPERTY_INSITU_VARIABLE_SELECTION ||
                    type == TimeSeriesChangeEvent.PROPERTY_AXIS_MAPPING_CHANGED ||
                    type == TimeSeriesChangeEvent.START_TIME_PROPERTY_NAME ||
                    type == TimeSeriesChangeEvent.END_TIME_PROPERTY_NAME) {
                updateTimeSeries(event.getTimeSeries());
            }
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            String propertyName = event.getPropertyName();
            if (propertyName.equals(Placemark.PROPERTY_NAME_PIXELPOS)
                        || propertyName.equals(Placemark.PROPERTY_NAME_LABEL)) {
                graphModel.updateTimeSeries(null, TimeSeriesType.PIN);
                graphModel.updateTimeSeries(null, TimeSeriesType.INSITU);
            }
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            final ProductNode node = event.getSourceNode();
            if (node instanceof Placemark) {
                handlePlacemarkChanged();
            } else if (node instanceof RasterDataNode && currentView != null) {
                graphModel.adaptToTimeSeries(getTimeSeries());
                graphModel.updateTimeSeries(null, TimeSeriesType.INSITU);
                graphModel.updateTimeSeries(null, TimeSeriesType.PIN);
            }
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            final ProductNode node = event.getSourceNode();
            if (node instanceof Placemark) {
                handlePlacemarkChanged();
            } else if (node instanceof RasterDataNode && currentView != null) {
                graphModel.adaptToTimeSeries(getTimeSeries());
                graphModel.updateTimeSeries(null, TimeSeriesType.INSITU);
                graphModel.updateTimeSeries(null, TimeSeriesType.PIN);
            }
        }

        private AbstractTimeSeries getTimeSeries() {
            final Product product = currentView.getProduct();
            return TimeSeriesMapper.getInstance().getTimeSeries(product);
        }

        private void handlePlacemarkChanged() {
            final boolean placemarksSet = currentView.getProduct().getPinGroup().getNodeCount() > 0;
            graphForm.setExportEnabled(placemarksSet);
            updateTimeSeries(getTimeSeries());
        }
    }

    private class SliderListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            graphModel.updateAnnotation(currentView.getRaster());
        }
    }
}
