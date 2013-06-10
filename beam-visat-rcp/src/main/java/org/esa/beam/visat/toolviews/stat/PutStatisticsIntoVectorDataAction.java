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
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.statistics.output.BandNameCreator;
import org.esa.beam.statistics.output.FeatureStatisticsWriter;
import org.esa.beam.statistics.output.StatisticsOutputContext;
import org.esa.beam.statistics.output.Util;
import org.esa.beam.util.logging.BeamLogManager;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Thomas Storm
 */
class PutStatisticsIntoVectorDataAction extends AbstractAction {

    private Mask[] selectedMasks;
    private final Map<SimpleFeatureType, VectorDataNode> featureType2VDN = new HashMap<SimpleFeatureType, VectorDataNode>();
    private final Map<SimpleFeatureType, Set<Mask>> featureType2Mask = new HashMap<SimpleFeatureType, Set<Mask>>();
    private final Map<Mask, Histogram> mask2Histogram = new HashMap<Mask, Histogram>();
    private final Map<Mask, String> mask2RegionName = new HashMap<Mask, String>();
    private final StatisticalExportContext provider;

    PutStatisticsIntoVectorDataAction(StatisticalExportContext provider) {
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
            final VectorDataNode originalVDN = featureType2VDN.get(featureType);
            if (originalVDN.isPermanent()) {
                BeamLogManager.getSystemLogger().warning("Unable to put statistics into permanent vector data.");
                VisatApp.getApp().showErrorDialog("Unable to put statistics into permanent vector data (such as pins/GCPs).");
                continue;
            }

            FeatureStatisticsWriter featureStatisticsWriter =
                    FeatureStatisticsWriter.createFeatureStatisticsWriter(getFeatureCollection(featureType),
                                                                          null,
                                                                          new BandNameCreator());
            featureStatisticsWriter.initialiseOutput(
                    StatisticsOutputContext.create(
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
                            }));
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
                featureStatisticsWriter.addToOutput(provider.getRasterDataNode().getName(), mask2RegionName.get(mask),
                                                    statistics);
            }

            exchangeVDN(featureType, featureStatisticsWriter);
            JOptionPane.showMessageDialog(VisatApp.getApp().getApplicationWindow(),
                                          "The vector data have successfully been extended with the computed statistics.",
                                          "Extending vector data with statistics",
                                          JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void exchangeVDN(SimpleFeatureType featureType, FeatureStatisticsWriter featureStatisticsWriter) {
        final VectorDataNode originalVDN = featureType2VDN.get(featureType);
        final VectorDataNode vectorDataNode = createVectorDataNode(featureStatisticsWriter, originalVDN);
        final ProductNodeGroup<VectorDataNode> vectorDataNodeGroup = provider.getVectorDataNodeGroup();
        vectorDataNodeGroup.remove(originalVDN);
        originalVDN.dispose();
        vectorDataNodeGroup.add(vectorDataNode);
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

    private static VectorDataNode createVectorDataNode(FeatureStatisticsWriter featureStatisticsWriter,
                                                       VectorDataNode originalVDN) {
        final SimpleFeatureType updatedFeatureType = featureStatisticsWriter.getUpdatedFeatureType();
        final List<SimpleFeature> features = featureStatisticsWriter.getFeatures();
        final ListFeatureCollection featureCollection = new ListFeatureCollection(updatedFeatureType, features);
        final PlacemarkDescriptor placemarkDescriptor = originalVDN.getPlacemarkDescriptor();
        final VectorDataNode vectorDataNode = new VectorDataNode(originalVDN.getName(), featureCollection, placemarkDescriptor);
        vectorDataNode.setPermanent(originalVDN.isPermanent());
        vectorDataNode.setModified(true);
        vectorDataNode.setDescription(originalVDN.getDescription());
        return vectorDataNode;
    }

    private Histogram getHistogram(Mask mask) {
        return mask2Histogram.get(mask);
    }

    private Mask[] getMasks(SimpleFeatureType featureType) {
        final Set<Mask> masks = featureType2Mask.get(featureType);
        return masks.toArray(new Mask[masks.size()]);
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
                    featureType2Mask.put(featureType, new HashSet<Mask>());
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
        mask2RegionName.put(selectedMask, Util.getFeatureName(features.next()));
        features.close();
    }

    public void setSelectedMasks(Mask[] selectedMasks) {
        this.selectedMasks = selectedMasks;
    }

}
