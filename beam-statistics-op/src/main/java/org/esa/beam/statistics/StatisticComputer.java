package org.esa.beam.statistics;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import java.awt.Shape;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.media.jai.Histogram;
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

public class StatisticComputer {

    private final FeatureCollection<SimpleFeatureType, SimpleFeature> features;
    private final FeatureUtils.FeatureCrsProvider crsProvider;
    private final ProgressMonitor pm;
    private final StatisticsOp.BandConfiguration[] bandConfigurations;
    private final Map<StatisticsOp.BandConfiguration, StxOpMapping> stxOpMappings;
    private final int initialBinCount;

    public StatisticComputer(File shapefile, StatisticsOp.BandConfiguration[] bandConfigurations, int initialBinCount) {
        this.initialBinCount = initialBinCount;
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
        stxOpMappings = new HashMap<StatisticsOp.BandConfiguration, StxOpMapping>();
    }

    public void computeStatistic(Product product) {
        final FeatureCollection<SimpleFeatureType, SimpleFeature> productFeatures;
        VectorDataNode[] vectorDataNodes = null;
        if (features != null) {
            productFeatures = FeatureUtils.clipFeatureCollectionToProductBounds(features, product, crsProvider, pm);
            vectorDataNodes = createVectorDataNodes(productFeatures);
        }
        for (int i = 0; i < bandConfigurations.length; i++) {
            StatisticsOp.BandConfiguration bandConfiguration = bandConfigurations[i];
            final Band band = getBand(bandConfiguration, product);
            band.setValidPixelExpression(bandConfiguration.validPixelExpression);
            final StxOpMapping stxOpsMapping = getStxOpsMapping(bandConfiguration);
            if (features != null) {
                for (int j = 0; j < vectorDataNodes.length; j++) {
                    VectorDataNode vectorDataNode = vectorDataNodes[j];
                    product.getVectorDataGroup().add(vectorDataNode);
                    final String vdnName = vectorDataNode.getName();
                    Mask currentMask = product.getMaskGroup().get(vdnName);
                    final Shape roiShape = currentMask.getValidShape();
                    final MultiLevelImage roiImage = currentMask.getSourceImage();
                    computeStatistic(vdnName, stxOpsMapping, band, roiShape, roiImage);
                }
            } else {
                computeStatistic("world", stxOpsMapping, band, null, null);
            }
        }
    }

    private void computeStatistic(String regionName, StxOpMapping stxOpsMapping, Band band, Shape roiShape, MultiLevelImage roiImage) {
        final SummaryStxOp summaryStxOp = stxOpsMapping.getSummaryOp(regionName);
        StxFactory.accumulate(band, 0, roiImage, roiShape, summaryStxOp, SubProgressMonitor.create(pm, 50));
        final double minimum = summaryStxOp.getMinimum();
        final double maximum = summaryStxOp.getMaximum();
        final HistogramStxOp histogramStxOp = stxOpsMapping.getHistogramOp(regionName, minimum, maximum, band);
        StxFactory.accumulate(band, 0, roiImage, roiShape, histogramStxOp, SubProgressMonitor.create(pm, 50));
    }

    private StxOpMapping getStxOpsMapping(StatisticsOp.BandConfiguration bandConfiguration) {
        StxOpMapping stxOpMapping = stxOpMappings.get(bandConfiguration);
        if (stxOpMapping == null) {
            stxOpMapping = new StxOpMapping(initialBinCount);
            stxOpMappings.put(bandConfiguration, stxOpMapping);
        }
        return stxOpMapping;
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

    public Map<StatisticsOp.BandConfiguration, StxOpMapping> getResults() {
        return stxOpMappings;
    }

    static class StxOpMapping {

        final Map<String, SummaryStxOp> summaryMap;
        final Map<String, HistogramStxOp> histogramMap;
        private final int initialBinCount;

        StxOpMapping(int initialBinCount) {
            this.summaryMap = new HashMap<String, SummaryStxOp>();
            this.histogramMap = new HashMap<String, HistogramStxOp>();
            this.initialBinCount = initialBinCount;
        }

        private SummaryStxOp getSummaryOp(String vdnName) {
            SummaryStxOp summaryStxOp = summaryMap.get(vdnName);
            if (summaryStxOp == null) {
                summaryStxOp = new SummaryStxOp();
                summaryMap.put(vdnName, summaryStxOp);
            }
            return summaryStxOp;
        }

        private HistogramStxOp getHistogramOp(String vdnName, double minimum, double maximum, Band band) {
            HistogramStxOp histogramStxOp = histogramMap.get(vdnName);
            boolean intHistogram;
            intHistogram = band.getGeophysicalImage().getSampleModel().getDataType() < DataBuffer.TYPE_FLOAT;
            if (histogramStxOp == null) {
                histogramStxOp = new HistogramStxOp(initialBinCount, minimum, maximum, intHistogram, false);
                histogramMap.put(vdnName, histogramStxOp);
            } else {
                final Histogram oldHistogram = histogramStxOp.getHistogram();
                final double oldMin = oldHistogram.getLowValue()[0];
                final double oldMax = oldHistogram.getHighValue()[0];
                if (minimum < oldMin || maximum > oldMax) {
                    histogramStxOp = createExpandedHistogramOp(oldHistogram, minimum, maximum, intHistogram);
                    histogramMap.put(vdnName, histogramStxOp);
                }
            }
            return histogramStxOp;
        }

        private HistogramStxOp createExpandedHistogramOp(Histogram oldHistogram, double minimum, double maximum, boolean intHistogram) {
            final double oldMin = oldHistogram.getLowValue()[0];
            final double oldMax = oldHistogram.getHighValue()[0];
            HistogramStxOp histogramStxOp;
            final int oldBinCount = oldHistogram.getNumBins()[0];
            double binWidth = computeBinWidth(oldMin, oldMax, oldBinCount);
            double numNewMinBins = 0;
            if (minimum < oldMin) {
                final double minDiff = oldMin - minimum;
                numNewMinBins = Math.ceil(minDiff / binWidth);
            }
            if (numNewMinBins > 200 * initialBinCount) {
                histogramStxOp = new HistogramStxOp(initialBinCount, minimum, maximum, intHistogram, false);
                migrateOldHistogramData(oldHistogram, histogramStxOp.getHistogram());
                return histogramStxOp;
            } else {
                double numNewMaxBins = 0;
                if (maximum > oldMax) {
                    final double maxDiff = maximum - oldMax;
                    numNewMaxBins = Math.ceil(maxDiff / binWidth);
                }
                double newMinimum = oldMin - numNewMinBins * binWidth;
                double newMaximum = oldMax + numNewMaxBins * binWidth;
                double newBinCount = oldBinCount + numNewMinBins + numNewMaxBins;

                final int binRatio;
                if (newBinCount > 2 * initialBinCount) {
                    binRatio = (int) (newBinCount / initialBinCount);
                    final int binRemainder = (int) (newBinCount % binRatio);
                    newMaximum += binRemainder * binWidth;
                    newBinCount = (newBinCount + binRemainder) / binRatio;
                } else {
                    binRatio = 1;
                }

                histogramStxOp = new HistogramStxOp((int) newBinCount, newMinimum, newMaximum, intHistogram, false);
                migrateOldHistogramData(oldHistogram, histogramStxOp.getHistogram(), (int) numNewMinBins, binRatio);
                return histogramStxOp;
            }
        }

        private void migrateOldHistogramData(Histogram oldHistogram, Histogram newHistogram) {
            final double oldMin = oldHistogram.getLowValue(0);
            final double oldMax = oldHistogram.getHighValue(0);
            final int[] oldBins = oldHistogram.getBins(0);
            final int oldNumBins = oldBins.length;
            final double oldBinWidth = computeBinWidth(oldMin, oldMax, oldNumBins);

            final double newMin = newHistogram.getLowValue(0);
            final double newMax = newHistogram.getHighValue(0);
            final int[] newBins = newHistogram.getBins(0);
            final int newNumBins = newBins.length;
            final double newBinWidth = computeBinWidth(newMin, newMax, newNumBins);

            for (int i = 0; i < oldBins.length; i++) {
                int count = oldBins[i];
                if (count == 0) {
                    continue;
                }
                final double binCenterValue = oldMin + oldBinWidth * i + oldBinWidth / 2 ;
                int newBinIndex = (int) Math.floor((binCenterValue - newMin) / newBinWidth);
                if (newBinIndex >= newNumBins) {
                    newBinIndex = newNumBins -1;
                }
                newBins[newBinIndex] += count;
            }
        }

        private void migrateOldHistogramData(Histogram oldHistogram, Histogram newHistogram, int startOffset, int binRatio) {
            final int[] oldBins = oldHistogram.getBins(0);
            final int[] newBins = newHistogram.getBins(0);
            final int newMaxIndex = newBins.length -1;
            for (int i = 0; i < oldBins.length; i++) {
                int newBinsIndex = (startOffset + i) / binRatio;
                if (newBinsIndex > newMaxIndex) {
                    newBinsIndex = newMaxIndex;
                }
                newBins[newBinsIndex] += oldBins[i];
            }
        }

        private double computeBinWidth(double min, double max, int binCount) {
            return (max - min) / binCount;
        }
    }
}
