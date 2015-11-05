package org.esa.snap.statistics;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.HistogramStxOp;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.StxFactory;
import org.esa.snap.core.datamodel.SummaryStxOp;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.FeatureUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.statistics.output.Util;
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
import java.util.logging.Logger;

public class StatisticComputer {

    private final FeatureCollection<SimpleFeatureType, SimpleFeature> features;
    private final FeatureUtils.FeatureCrsProvider crsProvider;
    private final ProgressMonitor pm;
    private final BandConfiguration[] bandConfigurations;
    private final Map<BandConfiguration, StxOpMapping> stxOpMappings;
    private final int initialBinCount;
    private final Logger logger;

    public StatisticComputer(File shapefile, BandConfiguration[] bandConfigurations, int initialBinCount, Logger logger) {
        this.initialBinCount = initialBinCount;
        this.logger = logger != null ? logger : SystemUtils.LOG;
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
                if (targetProduct.getSceneCRS() == Product.DEFAULT_IMAGE_CRS) {
                    return Product.DEFAULT_IMAGE_CRS;
                }
                return DefaultGeographicCRS.WGS84;
            }

            @Override
            public boolean clipToProductBounds() {
                return true;
            }
        };
        pm = ProgressMonitor.NULL;
        this.bandConfigurations = bandConfigurations;
        stxOpMappings = new HashMap<BandConfiguration, StxOpMapping>();
    }

    public void computeStatistic(final Product product) {
        VectorDataNode[] vectorDataNodes = null;
        if (features != null) {
            final FeatureCollection<SimpleFeatureType, SimpleFeature> productFeatures
                    = FeatureUtils.clipFeatureCollectionToProductBounds(features, product, crsProvider, pm);
            vectorDataNodes = createVectorDataNodes(productFeatures);
            for (VectorDataNode vectorDataNode : vectorDataNodes) {
                product.getVectorDataGroup().add(vectorDataNode);
            }
        }
        for (BandConfiguration bandConfiguration : bandConfigurations) {
            final Band band = getBand(bandConfiguration, product);
            final String newExpression = bandConfiguration.validPixelExpression;
            if (newExpression != null) {
                final String oldExpression = band.getValidPixelExpression();
                if (oldExpression != null) {
                    logger.info(
                            "Replaced old valid pixel expression '" + oldExpression + "' by '" + newExpression + "'.");
                }
                band.setValidPixelExpression(newExpression);
            }
            final StxOpMapping stxOpsMapping = getStxOpsMapping(bandConfiguration);
            if (features != null) {
                for (VectorDataNode vectorDataNode : vectorDataNodes) {
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

    private StxOpMapping getStxOpsMapping(BandConfiguration bandConfiguration) {
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

    static Band getBand(BandConfiguration configuration, Product product) {
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

    public Map<BandConfiguration, StxOpMapping> getResults() {
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
                    histogramStxOp = HistogramExpanderTransmitter.createExpandedHistogramOp(oldHistogram, minimum, maximum, intHistogram, this.initialBinCount);
                    histogramMap.put(vdnName, histogramStxOp);
                }
            }
            return histogramStxOp;
        }
    }
}
