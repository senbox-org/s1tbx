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

package org.esa.beam.timeseries.ui.player;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneImage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glevel.BandImageMultiLevelSource;
import org.esa.beam.timeseries.core.TimeSeriesMapper;
import org.esa.beam.timeseries.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.timeseries.core.timeseries.datamodel.TimeSeriesChangeEvent;
import org.esa.beam.timeseries.core.timeseries.datamodel.TimeSeriesListener;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Container;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Main class for the player tool.
 * @author Thomas Storm
 */
public class TimeSeriesPlayerToolView extends AbstractToolView {

    public static final String TIME_PROPERTY = "timeProperty";

    private final SceneViewListener sceneViewListener;
    private final TimeSeriesListener timeSeriesPlayerTSL;

    private ProductSceneView currentView;
    private TimeSeriesPlayerForm form;

    public TimeSeriesPlayerToolView() {
        sceneViewListener = new SceneViewListener();
        timeSeriesPlayerTSL = new TimeSeriesPlayerTSL();
    }

    @Override
    protected JComponent createControl() {
        VisatApp.getApp().addInternalFrameListener(sceneViewListener);
        form = new TimeSeriesPlayerForm(getDescriptor());
        form.getTimeSlider().addChangeListener(new SliderChangeListener());
        ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        if (view != null) {
            final String viewProductType = view.getProduct().getProductType();
            if (!view.isRGB() &&
                viewProductType.equals(AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE) &&
                TimeSeriesMapper.getInstance().getTimeSeries(view.getProduct()) != null) {
                setCurrentView(view);
            }
        }
        return form;
    }

    private void setCurrentView(ProductSceneView newView) {
        if (currentView != newView) {
            if (currentView != null) {
                final AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(
                        currentView.getProduct());
                timeSeries.removeTimeSeriesListener(timeSeriesPlayerTSL);
            }
            currentView = newView;
            form.setView(currentView);
            if (currentView != null) {
                final Product currentProduct = currentView.getProduct();
                final AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(currentProduct);
                timeSeries.addTimeSeriesListener(timeSeriesPlayerTSL);
                form.setTimeSeries(timeSeries);
                exchangeRasterInProductSceneView(currentView.getRaster());
                reconfigureBaseImageLayer(currentView);
                form.configureTimeSlider(currentView.getRaster());
            } else {
                form.setTimeSeries(null);
                form.configureTimeSlider(null);
                form.getTimer().stop();
            }
        }
    }

    // todo (mp) - The following should be done on ProductSceneView.setRasters()

    private void exchangeRasterInProductSceneView(RasterDataNode nextRaster) {
        // todo use a real ProgressMonitor
        final RasterDataNode currentRaster = currentView.getRaster();
        final ImageInfo imageInfoClone = currentRaster.getImageInfo(ProgressMonitor.NULL).createDeepCopy();
        nextRaster.setImageInfo(imageInfoClone);
        currentView.setRasters(new RasterDataNode[]{nextRaster});
        currentView.setImageInfo(imageInfoClone.createDeepCopy());
        VisatApp.getApp().getSelectedInternalFrame().setTitle(nextRaster.getDisplayName());
    }

    private void reconfigureBaseImageLayer(ProductSceneView sceneView) {
        final Layer rootLayer = currentView.getRootLayer();
        final ImageLayer baseImageLayer = (ImageLayer) LayerUtils.getChildLayerById(rootLayer,
                                                                                    ProductSceneView.BASE_IMAGE_LAYER_ID);
        final List<Band> bandList = form.getBandList(currentView.getRaster().getName());
        final Band band = (Band) sceneView.getRaster();
        int nextIndex = bandList.indexOf(band) + 1;
        if (nextIndex >= bandList.size()) {
            nextIndex = 0;
        }

        if (!(baseImageLayer instanceof BlendImageLayer)) {
            final Band nextBand = bandList.get(nextIndex);
            MultiLevelSource nextLevelSource = BandImageMultiLevelSource.create(nextBand, ProgressMonitor.NULL);
            final BlendImageLayer blendLayer = new BlendImageLayer(baseImageLayer.getMultiLevelSource(),
                                                                   nextLevelSource);

            final List<Layer> children = rootLayer.getChildren();
            final int baseIndex = children.indexOf(baseImageLayer);
            children.remove(baseIndex);
            blendLayer.setId(ProductSceneView.BASE_IMAGE_LAYER_ID);
            blendLayer.setName(band.getDisplayName());
            blendLayer.setTransparency(0);
            children.add(baseIndex, blendLayer);
            configureSceneView(sceneView, blendLayer.getBaseMultiLevelSource());
        }
    }

    // todo (mp) - The following should be done on ProductSceneView.setRasters()

    private void configureSceneView(ProductSceneView sceneView, MultiLevelSource multiLevelSource) {
        // This is needed because sceneView must return correct ImageInfo
        try {
            final Field sceneImageField = ProductSceneView.class.getDeclaredField("sceneImage");
            sceneImageField.setAccessible(true);
            final Object sceneImage = sceneImageField.get(sceneView);
            final Field multiLevelSourceField = ProductSceneImage.class.getDeclaredField("bandImageMultiLevelSource");
            multiLevelSourceField.setAccessible(true);
            multiLevelSourceField.set(sceneImage, multiLevelSource);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private class SceneViewListener extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                ProductSceneView view = (ProductSceneView) contentPane;
                final RasterDataNode viewRaster = view.getRaster();
                final String viewProductType = viewRaster.getProduct().getProductType();
                if (currentView != view &&
                    !view.isRGB() &&
                    viewProductType.equals(AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE) &&
                    TimeSeriesMapper.getInstance().getTimeSeries(view.getProduct()) != null) {
                    setCurrentView(view);
                }
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (currentView == contentPane) {
                setCurrentView(null);
            }
        }
    }

    private class SliderChangeListener implements ChangeListener {

        private int value;

        @Override
        public void stateChanged(ChangeEvent e) {
            if (currentView == null) {
                return;
            }
            final int currentValue = form.getTimeSlider().getValue();
            if (currentValue == value || currentValue == -1) {
                // nothing has changed -- do nothing
                return;
            }
            if (currentView.getBaseImageLayer() instanceof BlendImageLayer) {
                BlendImageLayer blendLayer = (BlendImageLayer) currentView.getBaseImageLayer();
                int stepsPerTimespan = form.getStepsPerTimespan();
                final float transparency = (currentValue % stepsPerTimespan) / (float) stepsPerTimespan;
                blendLayer.setBlendFactor(transparency);
                boolean forward = currentValue > value;
                final List<Band> bandList = form.getBandList(currentView.getRaster().getName());
                value = currentValue;
                final int firstBandIndex = MathUtils.floorInt(currentValue / (float) stepsPerTimespan);
                final int secondBandIndex = MathUtils.ceilInt(currentValue / (float) stepsPerTimespan);
                BandImageMultiLevelSource newSource;
                if (!forward) {
                    // go backwards in time
                    newSource = BandImageMultiLevelSource.create(bandList.get(firstBandIndex), ProgressMonitor.NULL);
                } else {
                    // go forward in time
                    newSource = BandImageMultiLevelSource.create(bandList.get(secondBandIndex), ProgressMonitor.NULL);
                }
                if (secondBandIndex == firstBandIndex) {

                    exchangeRasterInProductSceneView(bandList.get(forward ? firstBandIndex : secondBandIndex));
                    blendLayer.swap(newSource, forward);

                    configureSceneView(currentView, blendLayer.getBaseMultiLevelSource());
                    blendLayer.setName(currentView.getRaster().getDisplayName());
//                 todo why use view to fire property changes and not time series itself?
                    currentView.firePropertyChange(TIME_PROPERTY, -1, firstBandIndex);
                } else {
                    currentView.getLayerCanvas().repaint();
                }
            }
        }

    }

    private class TimeSeriesPlayerTSL extends TimeSeriesListener {

        @Override
        public void timeSeriesChanged(TimeSeriesChangeEvent event) {
            if (event.getType() == TimeSeriesChangeEvent.PROPERTY_PRODUCT_LOCATIONS ||
                event.getType() == TimeSeriesChangeEvent.PROPERTY_EO_VARIABLE_SELECTION) {
                form.configureTimeSlider(currentView.getRaster());
            }
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            final ProductNode productNode = event.getSourceNode();
            if (isValidProductNode(productNode) && currentView != null) {
                form.configureTimeSlider((RasterDataNode) productNode);
            }
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            final ProductNode productNode = event.getSourceNode();
            if (isValidProductNode(productNode) && currentView != null) {
                if(currentView.getRaster() == productNode) {
                    form.configureTimeSlider((RasterDataNode) productNode);
                }
            }
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            String propertyName = event.getPropertyName();
            if (propertyName.equals(RasterDataNode.PROPERTY_NAME_IMAGE_INFO)) {
                adjustImageInfos(event);
            }
        }


        private boolean isValidProductNode(ProductNode productNode) {
            return productNode instanceof RasterDataNode && !(productNode instanceof Mask);
        }

        private void adjustImageInfos(ProductNodeEvent event) {

            final ProductNode node = event.getSourceNode();
            if (isValidProductNode(node)) {
                final RasterDataNode raster = (RasterDataNode) node;
                final ImageLayer baseImageLayer = currentView.getBaseImageLayer();
                final ImageInfo imageInfo = raster.getImageInfo();
                if (baseImageLayer instanceof BlendImageLayer) {
                    BlendImageLayer blendLayer = (BlendImageLayer) baseImageLayer;
                    blendLayer.getBlendMultiLevelSource().setImageInfo(imageInfo.createDeepCopy());
                }

            }
        }
    }

}
