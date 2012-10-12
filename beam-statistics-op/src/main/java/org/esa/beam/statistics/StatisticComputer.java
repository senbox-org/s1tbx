package org.esa.beam.statistics;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.HistogramStxOp;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.StxFactory;
import org.esa.beam.framework.datamodel.SummaryStxOp;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.statistics.output.Util;
import org.esa.beam.util.FeatureUtils;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.media.jai.Histogram;
import java.awt.Shape;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticComputer {

    private final FeatureCollection<SimpleFeatureType, SimpleFeature> features;
    private final FeatureUtils.FeatureCrsProvider crsProvider;
    private final ProgressMonitor pm;
    private final StatisticsOp.BandConfiguration[] bandConfigurations;
    private final Map<String, SummaryStxOp> summaryStxOps;
    private final Map<String, HistogramStxOp> histogramStxOps;
    private final int initialBinSize;

    public StatisticComputer(File shapefile, StatisticsOp.BandConfiguration[] bandConfigurations, int initialBinSize) {
        this.initialBinSize = initialBinSize;
        if (shapefile != null) {
            try {
                features = FeatureUtils.loadFeatureCollectionFromShapefile(shapefile);
            } catch (IOException e) {
                throw new OperatorException("Unable to load shapefile '" + shapefile.getAbsolutePath() + "'", e);
            }
        } else {
            features = null;
        }
        crsProvider = new FeatureUtils.FeatureCrsProvider() {
            @Override
            public CoordinateReferenceSystem getFeatureCrs(Product targetProduct) {
                if (ImageManager.getModelCrs(targetProduct.getGeoCoding()) == ImageManager.DEFAULT_IMAGE_CRS) {
                    return ImageManager.DEFAULT_IMAGE_CRS;
                }
                return DefaultGeographicCRS.WGS84;
            }
        };
        pm = ProgressMonitor.NULL;
        this.bandConfigurations = bandConfigurations;
        summaryStxOps = new HashMap<String, SummaryStxOp>();
        histogramStxOps = new HashMap<String, HistogramStxOp>();
    }

    public void computeStatistic(Product product) {
        final FeatureCollection<SimpleFeatureType, SimpleFeature> productFeatures;
        productFeatures = FeatureUtils.clipFeatureCollectionToProductBounds(features, product, crsProvider, pm);
        final VectorDataNode[] vectorDataNodes = createVectorDataNodes(productFeatures);
        for (int i = 0; i < bandConfigurations.length; i++) {
            StatisticsOp.BandConfiguration bandConfiguration = bandConfigurations[i];
            final Band band = getBand(bandConfiguration, product);

            for (int j = 0; j < vectorDataNodes.length; j++) {
                VectorDataNode vectorDataNode = vectorDataNodes[j];
                product.getVectorDataGroup().add(vectorDataNode);
                final String vdnName = vectorDataNode.getName();
                Mask currentMask = product.getMaskGroup().get(vdnName);
                final Shape roiShape = currentMask.getValidShape();
                final MultiLevelImage roiImage = currentMask.getSourceImage();
                final SummaryStxOp summaryStxOp = getSummaryOp(vdnName);
                StxFactory.accumulate(band, 0, roiImage, roiShape, summaryStxOp, SubProgressMonitor.create(pm, 50));
                final double minimum = summaryStxOp.getMinimum();
                final double maximum = summaryStxOp.getMaximum();
                final HistogramStxOp histogramStxOp = getHistogramOp(vdnName, minimum, maximum, band);
                StxFactory.accumulate(band, 0, roiImage, roiShape, histogramStxOp, SubProgressMonitor.create(pm, 50));
                final Histogram histogram = histogramStxOp.getHistogram();
            }
        }


        // prepare Product
        try {
            // compute Statistic
        } finally {
            // remove Preparations
        }
    }

    private HistogramStxOp getHistogramOp(String vdnName, double minimum, double maximum, Band band) {
        HistogramStxOp histogramStxOp = histogramStxOps.get(vdnName);
        boolean intHistogram;
        intHistogram = band.getGeophysicalImage().getSampleModel().getDataType() < DataBuffer.TYPE_FLOAT;
        if (histogramStxOp == null) {
            histogramStxOp = new HistogramStxOp(initialBinSize, minimum, maximum, intHistogram, false);
        } else {
            final Histogram oldHistogram = histogramStxOp.getHistogram();
            final double oldMin = oldHistogram.getLowValue()[0];
            final double oldMax = oldHistogram.getHighValue()[0];
            if (minimum < oldMin || maximum > oldMax) {
                final int oldNumBins = oldHistogram.getNumBins()[0];
                double binSize = (oldMax - oldMin) / oldNumBins;
                int numNewMinBins = 0;
                int numNewMaxBins = 0;
                if (minimum < oldMin) {
                    final double minDiff = oldMin - minimum;
                    numNewMinBins = (int) Math.ceil(minDiff / binSize);
                }
                if (maximum > oldMax) {
                    final double maxDiff = maximum - oldMax;
                    numNewMaxBins = (int) Math.ceil(maxDiff / binSize);
                }
                double newMinimum = oldMin - numNewMinBins * binSize;
                double newMaximum = oldMax + numNewMaxBins * binSize;
                int newNumBins = oldNumBins + numNewMinBins + numNewMaxBins;
                int numberOfDivisions = 0;
                while (newNumBins > 2 * initialBinSize) {
                    if (newNumBins % 2 != 0) {
                        newMaximum += binSize;
                        newNumBins++;
                    }
                    newNumBins /= 2;
                    binSize *= 2;
                    numberOfDivisions++;
                }
                histogramStxOp = new HistogramStxOp(newNumBins, newMinimum, newMaximum, intHistogram, false);
                // migrate data
                final int[] oldBins = oldHistogram.getBins(0);
                final Histogram newHistogram = histogramStxOp.getHistogram();
                final int[] newBins = newHistogram.getBins(0);
                for (int i = 0; i < oldBins.length; i++) {
                    int newBinsIndex = numNewMinBins + (i / (int) Math.pow(2, numberOfDivisions));
                    newBins[newBinsIndex] += oldBins[i];
                }
            }
        }
        histogramStxOps.put(vdnName, histogramStxOp);
        return histogramStxOp;
    }

    private SummaryStxOp getSummaryOp(String vdnName) {
        SummaryStxOp summaryStxOp = summaryStxOps.get(vdnName);
        if (summaryStxOp == null) {
            summaryStxOp = new SummaryStxOp();
            summaryStxOps.put(vdnName, summaryStxOp);
        }
        return summaryStxOp;
    }

    private VectorDataNode[] createVectorDataNodes(FeatureCollection<SimpleFeatureType, SimpleFeature> productFeatures) {
        final FeatureIterator<SimpleFeature> featureIterator = productFeatures.features();
        final List<VectorDataNode> result = new ArrayList<VectorDataNode>();
        while (featureIterator.hasNext()) {
            final SimpleFeature simpleFeature = featureIterator.next();
            final DefaultFeatureCollection fc = new DefaultFeatureCollection(simpleFeature.getID(),
                                                                             simpleFeature.getFeatureType());
            fc.add(simpleFeature);
            String name = Util.getFeatureName(simpleFeature);
            result.add(new VectorDataNode(name, fc));
        }

        return result.toArray(new VectorDataNode[result.size()]);
    }

    static Band getBand(StatisticsOp.BandConfiguration configuration, Product product) {
        final Band band;
        if (configuration.sourceBandName != null) {
            band = product.getBand(configuration.sourceBandName);
            band.setNoDataValueUsed(true);
        } else {
            band = product.addBand(configuration.expression.replace(" ", "_"), configuration.expression,
                                   ProductData.TYPE_FLOAT64);
        }
        if (band == null) {
            throw new OperatorException(MessageFormat.format("Band ''{0}'' does not exist in product ''{1}''.",
                                                             configuration.sourceBandName, product.getName()));
        }
        return band;
    }
}
