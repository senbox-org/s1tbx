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

package org.esa.beam.visat.toolviews.stat;

import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.statistics.BandNameCreator;
import org.esa.beam.statistics.ShapefileOutputter;
import org.esa.beam.visat.VisatApp;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import javax.media.jai.Histogram;
import javax.swing.AbstractAction;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Thomas Storm
 */
class PutStatisticsIntoVectorDataAction extends AbstractAction {

    private Mask[] selectedMasks;
    private final Map<SimpleFeatureType, VectorDataNode> featureType2VDN = new HashMap<SimpleFeatureType, VectorDataNode>();
    private final Map<SimpleFeatureType, List<Mask>> featureType2Mask = new HashMap<SimpleFeatureType, List<Mask>>();
    private final Map<Mask, Histogram> mask2Histogram = new HashMap<Mask, Histogram>();
    private final Map<Mask, String> mask2RegionName = new HashMap<Mask, String>();
    private final StatisticalDataProvider provider;

    PutStatisticsIntoVectorDataAction(StatisticalDataProvider provider) {
        super("Put statistics into vector data");
        this.provider = provider;
    }

    @Override
    public boolean isEnabled() {
        boolean hasSelectedMasks = hasSelectedMasks();
        final boolean hasTarget = getFeatureTypes().length != 0;
        return super.isEnabled() && hasSelectedMasks && hasTarget;
    }

    private boolean hasSelectedMasks() {
        boolean hasSelectedMasks = selectedMasks != null && selectedMasks.length != 0;
        if (hasSelectedMasks) {
            for (Mask selectedMask : selectedMasks) {
                if (selectedMask != null) {
                    break;
                }
                hasSelectedMasks = false;
            }
        }
        return hasSelectedMasks;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (selectedMasks[0] == null) {
            return;
        }

        for (final SimpleFeatureType featureType : getFeatureTypes()) {
            ShapefileOutputter shapefileOutputter = ShapefileOutputter.createShapefileOutputter(featureType,
                                                                                                getFeatureCollection(
                                                                                                        featureType),
                                                                                                null,
                                                                                                new BandNameCreator());
            shapefileOutputter.initialiseOutput(
                    new Product[]{provider.getRasterDataNode().getProduct()},
                    new String[]{provider.getRasterDataNode().getName()},
                    new String[]{
                            "minimum",
                            "maximum",
                            "median",
                            "average",
                            "sigma",
                            "p90",
                            "p95",
                            "pxx_max_error",
                            "total"
                    },
                    null,
                    null,
                    getRegionIds(featureType));
            for (final Mask mask : getMasks(featureType)) {
                HashMap<String, Number> statistics = new HashMap<String, Number>();
                Histogram histogram = getHistogram(mask);
                statistics.put("minimum", histogram.getLowValue(0));
                statistics.put("maximum", histogram.getHighValue(0));
                statistics.put("median", histogram.getPTileThreshold(0.5)[0]);
                statistics.put("average", histogram.getMean()[0]);
                statistics.put("sigma", histogram.getStandardDeviation()[0]);
                statistics.put("p90", histogram.getPTileThreshold(0.9)[0]);
                statistics.put("p95", histogram.getPTileThreshold(0.95)[0]);
                statistics.put("pxx_max_error", StatisticsPanel.getBinSize(histogram));
                statistics.put("total", histogram.getTotals()[0]);
                shapefileOutputter.addToOutput(provider.getRasterDataNode().getName(), mask2RegionName.get(mask),
                                               statistics);
            }

            exchangeVDN(featureType, shapefileOutputter);
            JOptionPane.showMessageDialog(VisatApp.getApp().getApplicationWindow(),
                                          "The vector data have successfully been extended with the computed statistics.",
                                          "Extending vector data with statistics",
                                          JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void exchangeVDN(SimpleFeatureType featureType, ShapefileOutputter shapefileOutputter) {
        final VectorDataNode originalVDN = featureType2VDN.get(featureType);
        final VectorDataNode vectorDataNode = updateVectorDataNode(shapefileOutputter, originalVDN);
        provider.getRasterDataNode().getProduct().getVectorDataGroup().remove(originalVDN);
        originalVDN.dispose();
        provider.getRasterDataNode().getProduct().getVectorDataGroup().add(vectorDataNode);
        final JInternalFrame internalFrame = VisatApp.getApp().findInternalFrame(originalVDN);
        if (internalFrame != null) {
            try {
                internalFrame.setClosed(true);
            } catch (PropertyVetoException ignored) {
                // ok
            }
        }
        final ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (sceneView != null) {
            sceneView.setLayersVisible(vectorDataNode);
        }
    }

    private static VectorDataNode updateVectorDataNode(ShapefileOutputter shapefileOutputter,
                                                       VectorDataNode originalVDN) {
        final SimpleFeatureType updatedFeatureType = shapefileOutputter.getUpdatedFeatureType();
        final List<SimpleFeature> features = shapefileOutputter.getFeatures();
        final ListFeatureCollection featureCollection = new ListFeatureCollection(updatedFeatureType, features);
        final PlacemarkDescriptor placemarkDescriptor = originalVDN.getPlacemarkDescriptor();
        final VectorDataNode update = new VectorDataNode(originalVDN.getName(), featureCollection, placemarkDescriptor);
        update.setPermanent(originalVDN.isPermanent());
        update.setModified(true);
        update.setDescription(originalVDN.getDescription());
        return update;
    }

    private Histogram getHistogram(Mask mask) {
        return mask2Histogram.get(mask);
    }

    private Mask[] getMasks(SimpleFeatureType featureType) {
        final List<Mask> masks = featureType2Mask.get(featureType);
        return masks.toArray(new Mask[masks.size()]);
    }

    private String[] getRegionIds(SimpleFeatureType featureType) {
        List<String> result = new ArrayList<String>();
        final FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = featureType2VDN.get(
                featureType).getFeatureCollection();
        final FeatureIterator<SimpleFeature> featureIterator = featureCollection.features();
        while (featureIterator.hasNext()) {
            final SimpleFeature simpleFeature = featureIterator.next();
            result.add(simpleFeature.getIdentifier().toString());

        }
        featureIterator.close();
        return result.toArray(new String[result.size()]);
    }

    private FeatureCollection<SimpleFeatureType, SimpleFeature> getFeatureCollection(SimpleFeatureType featureType) {
        return featureType2VDN.get(featureType).getFeatureCollection();
    }

    private SimpleFeatureType[] getFeatureTypes() {
        if (!hasSelectedMasks()) {
            return new SimpleFeatureType[0];
        }
        List<SimpleFeatureType> result = new ArrayList<SimpleFeatureType>();
        final Histogram[] histograms = provider.getHistograms();
        for (int i = 0; i < selectedMasks.length; i++) {
            final Mask selectedMask = selectedMasks[i];
            mask2Histogram.put(selectedMask, histograms[i]);
            if (selectedMask.getImageType().getName().equals(Mask.VectorDataType.TYPE_NAME)) {
                VectorDataNode vectorDataNode = Mask.VectorDataType.getVectorData(selectedMask);
                SimpleFeatureType featureType = vectorDataNode.getFeatureType();
                if (!result.contains(featureType)) {
                    result.add(featureType);
                }
                if (!featureType2Mask.containsKey(featureType)) {
                    featureType2Mask.put(featureType, new ArrayList<Mask>());
                }
                featureType2Mask.get(featureType).add(selectedMask);
                featureType2VDN.put(featureType, vectorDataNode);
                setMaskRegionName(selectedMask, vectorDataNode);
            }
        }

        return result.toArray(new SimpleFeatureType[result.size()]);
    }

    private void setMaskRegionName(Mask selectedMask, VectorDataNode vectorDataNode) {
        FeatureIterator<SimpleFeature> features = vectorDataNode.getFeatureCollection().features();
        mask2RegionName.put(selectedMask, features.next().getIdentifier().toString());
        features.close();
    }

    public void setSelectedMasks(Mask[] selectedMasks) {
        this.selectedMasks = selectedMasks;
    }

    static interface StatisticalDataProvider {

        RasterDataNode getRasterDataNode();

        Histogram[] getHistograms();

    }

}
