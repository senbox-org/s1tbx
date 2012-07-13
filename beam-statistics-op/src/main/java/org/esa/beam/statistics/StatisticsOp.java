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

package org.esa.beam.statistics;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.framework.datamodel.StxFactory;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.FeatureUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.io.WildcardMatcher;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An operator that is used to compute statistics for any number of source products, restricted to regions given by an
 * ESRI shapefile.
 * <p/>
 * Its output is an ASCII file where the statistics are mapped to the source regions.
 * <p/>
 * Unlike most other operators, that can compute single {@link org.esa.beam.framework.gpf.Tile tiles},
 * the statistics operator processes all of its source products in its {@link #initialize()} method.
 *
 * @author Thomas Storm
 */
@OperatorMetadata(alias = "StatisticsOp",
                  version = "1.0",
                  authors = "Thomas Storm",
                  copyright = "(c) 2012 by Brockmann Consult GmbH",
                  description = "Computes statistics for an arbitrary number of source products.")
public class StatisticsOp extends Operator implements Output {

    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @SourceProducts(description =
                            "The source products to be considered for statistics computation. If not given, the " +
                            "parameter 'sourceProductPaths' must be provided.")
    Product[] sourceProducts;

    @Parameter(description = "A comma-separated list of file paths specifying the source products.\n" +
                             "Each path may contain the wildcards '**' (matches recursively any directory),\n" +
                             "'*' (matches any character sequence in path names) and\n" +
                             "'?' (matches any single character).")
    String[] sourceProductPaths;

    @Parameter(description =
                       "An ESRI shapefile, providing the considered geographical region(s) given as polygons. If " +
                       "null, all pixels are considered.")
    File shapefile;

    @Parameter(description =
                       "The start date. If not given, taken from the 'oldest' source product. Products that have " +
                       "a start date before the start date given by this parameter are not considered.",
               format = DATETIME_PATTERN, converter = UtcConverter.class)
    ProductData.UTC startDate;

    @Parameter(description =
                       "The end date. If not given, taken from the 'youngest' source product. Products that have " +
                       "an end date after the end date given by this parameter are not considered.",
               format = DATETIME_PATTERN, converter = UtcConverter.class)
    ProductData.UTC endDate;

    @Parameter(description = "The band configurations. These configurations determine the input of the operator.",
               alias = "bandConfigurations", itemAlias = "bandConfiguration")
    BandConfiguration[] bandConfigurations;

    @Parameter(description = "Determines if a copy of the input shapefile shall be created and augmented with the " +
                             "statistical data.")
    boolean doOutputShapefile;

    @Parameter(description = "The target file for shapefile output.")
    File outputShapefile;

    @Parameter(description = "Determines if the output shall be written into an ASCII file.")
    boolean doOutputAsciiFile;

    @Parameter(description = "The target file for ASCII output.")
    File outputAsciiFile;

    Set<Product> collectedProducts;

    final Set<Outputter> outputters = new HashSet<Outputter>();

    final Map<Product, VectorDataNode[]> productVdnMap = new HashMap<Product, VectorDataNode[]>();

    final SortedSet<String> regionNames = new TreeSet<String>();

    private PrintStream metadataOutputStream;

    private PrintStream csvOutputStream;

    private PrintStream bandMappingOutputStream;

    @Override
    public void initialize() throws OperatorException {
        setDummyTargetProduct();
        validateInput();
        setupOutputter();
        Product[] allSourceProducts = collectSourceProducts();
        initializeVectorDataNodes(allSourceProducts);
        initializeOutput(allSourceProducts);
        computeOutput(allSourceProducts);
        writeOutput();
    }

    @Override
    public void dispose() {
        super.dispose();
        for (Product collectedProduct : collectedProducts) {
            collectedProduct.dispose();
        }
    }

    private void initializeVectorDataNodes(Product[] allSourceProducts) {
        for (Product sourceProduct : allSourceProducts) {
            productVdnMap.put(sourceProduct, createVectorDataNodes(sourceProduct));
        }
    }

    private VectorDataNode[] createVectorDataNodes(Product product) {
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection;
        final FeatureUtils.FeatureCrsProvider crsProvider = new FeatureUtils.FeatureCrsProvider() {
            @Override
            public CoordinateReferenceSystem getFeatureCrs(Product targetProduct) {
                if (ImageManager.getModelCrs(targetProduct.getGeoCoding()) == ImageManager.DEFAULT_IMAGE_CRS) {
                    return ImageManager.DEFAULT_IMAGE_CRS;
                }
                return DefaultGeographicCRS.WGS84;
            }
        };
        try {
            featureCollection = FeatureUtils.loadShapefileForProduct(shapefile, product,
                                                                     crsProvider, ProgressMonitor.NULL);
        } catch (Exception e) {
            throw new OperatorException("Unable to load shapefile '" + shapefile.getAbsolutePath() + "'.", e);
        }

        final FeatureIterator<SimpleFeature> featureIterator = featureCollection.features();
        final List<VectorDataNode> result = new ArrayList<VectorDataNode>();
        while (featureIterator.hasNext()) {
            final SimpleFeature simpleFeature = featureIterator.next();
            final DefaultFeatureCollection fc = new DefaultFeatureCollection(simpleFeature.getID(), simpleFeature.getFeatureType());
            fc.add(simpleFeature);
            result.add(new VectorDataNode(simpleFeature.getID(), fc));
        }

        for (final VectorDataNode vectorDataNode : result) {
            regionNames.add(vectorDataNode.getName());
        }
        return result.toArray(new VectorDataNode[result.size()]);
    }

    void initializeOutput(Product[] allSourceProducts) {
        final List<String> bandNamesList = new ArrayList<String>();
        for (BandConfiguration bandConfiguration : bandConfigurations) {
            if (bandConfiguration.sourceBandName != null) {
                bandNamesList.add(bandConfiguration.sourceBandName);
            } else {
                bandNamesList.add(bandConfiguration.expression);
            }
        }
        final String[] algorithmNames = new String[]{"minimum", "maximum", "median", "average", "sigma", "p90", "p95", "total"};
        final String[] bandNames = bandNamesList.toArray(new String[bandNamesList.size()]);
        for (Outputter outputter : outputters) {
            outputter.initialiseOutput(allSourceProducts, bandNames, algorithmNames, startDate, endDate, regionNames.toArray(new String[regionNames.size()]));
        }
    }

    void setupOutputter() {
        if (doOutputAsciiFile) {
            try {
                final StringBuilder metadataFileName = new StringBuilder(FileUtils.getFilenameWithoutExtension(outputAsciiFile));
                metadataFileName.append("_metadata.txt");
                final File metadataFile = new File(outputAsciiFile.getParent(), metadataFileName.toString());
                metadataOutputStream = new PrintStream(new FileOutputStream(metadataFile));
                csvOutputStream = new PrintStream(new FileOutputStream(outputAsciiFile));
                outputters.add(new CsvOutputter(metadataOutputStream, csvOutputStream));
            } catch (FileNotFoundException e) {
                throw new OperatorException(
                        "Directory '" + outputAsciiFile.getParent() + "' does not exist.");
            }
        }
        if (doOutputShapefile) {
            try {
                final String baseName = FileUtils.getFilenameWithoutExtension(outputShapefile);
                final File file = new File(outputShapefile.getParent(), baseName + "_band_mapping.txt");
                final FileOutputStream outputStream = new FileOutputStream(file);
                bandMappingOutputStream = new PrintStream(outputStream);
                BandNameCreator bandNameCreator = new BandNameCreator(bandMappingOutputStream);
                outputters.add(new ShapefileOutputter(shapefile.toURI().toURL(), outputShapefile.getAbsolutePath(), bandNameCreator));
            } catch (MalformedURLException e) {
                throw new OperatorException("Unable to create shapefile outputter", e);
            } catch (FileNotFoundException e) {
                throw new OperatorException("Unable to create shapefile outputter", e);
            }
        }
    }

    void computeOutput(Product[] allSourceProducts) {
        final List<Band> bands = new ArrayList<Band>();
        for (BandConfiguration configuration : bandConfigurations) {
            final HashMap<String, List<Mask>> map = new HashMap<String, List<Mask>>();
            for (Product product : allSourceProducts) {
                final Band band = getBand(configuration, product);
                band.setValidPixelExpression(configuration.validPixelExpression);
                bands.add(band);
                for (VectorDataNode vectorDataNode : productVdnMap.get(product)) {
                    product.getVectorDataGroup().add(vectorDataNode);
                    final Mask mask = product.addMask(vectorDataNode.getName(), vectorDataNode, "", Color.BLUE, Double.NaN);
                    if (!map.containsKey(vectorDataNode.getName())) {
                        map.put(vectorDataNode.getName(), new ArrayList<Mask>());
                    }
                    map.get(vectorDataNode.getName()).add(mask);
                }
            }
            for (String regionName : regionNames) {
                final List<Mask> maskList = map.get(regionName);
                final Mask[] roiMasks = maskList.toArray(new Mask[maskList.size()]);
                final Stx stx = new StxFactory()
                        .withHistogramBinCount(1024 * 1024)
                        .create(ProgressMonitor.NULL, roiMasks, bands.toArray(new Band[bands.size()]));
                final HashMap<String, Double> stxMap = new HashMap<String, Double>();
                stxMap.put("minimum", stx.getMinimum());
                stxMap.put("maximum", stx.getMaximum());
                stxMap.put("average", stx.getMean());
                stxMap.put("sigma", stx.getStandardDeviation());
                stxMap.put("total", (double) stx.getSampleCount());
                stxMap.put("median", stx.getHistogram().getPTileThreshold(0.5)[0]);
                stxMap.put("p90", stx.getHistogram().getPTileThreshold(0.9)[0]);
                stxMap.put("p95", stx.getHistogram().getPTileThreshold(0.95)[0]);
                for (Outputter outputter : outputters) {
                    outputter.addToOutput(bands.get(0).getName(), regionName, stxMap);
                }
            }
            bands.clear();
        }
    }

    static Band getBand(BandConfiguration configuration, Product product) {
        final Band band;
        if (configuration.sourceBandName == null && configuration.expression == null) {
            throw new OperatorException("Configuration must contain either a source band name or an expression.");
        }
        if (configuration.sourceBandName != null && configuration.expression != null) {
            throw new OperatorException("Configuration must contain either a source band name or an expression.");
        }
        if (configuration.sourceBandName != null) {
            band = product.getBand(configuration.sourceBandName);
        } else {
            band = product.addBand(configuration.expression.replace(" ", "_"), configuration.expression, ProductData.TYPE_FLOAT64);
        }
        if(band == null) {
            throw new OperatorException("Band '" + configuration.sourceBandName + "' does not exist in product '" + product.getName() + "'.");
        }
        return band;
    }

    void writeOutput() {
        try {
            for (Outputter outputter : outputters) {
                outputter.finaliseOutput();
            }
        } catch (IOException e) {
            throw new OperatorException("Unable to write output.", e);
        } finally {
            if (metadataOutputStream != null) {
                metadataOutputStream.close();
            }
            if (csvOutputStream != null) {
                csvOutputStream.close();
            }
            if (bandMappingOutputStream != null) {
                bandMappingOutputStream.close();
            }
        }
    }

    void validateInput() {
        if (startDate != null && endDate != null && endDate.getAsDate().before(startDate.getAsDate())) {
            throw new OperatorException("End date '" + this.endDate + "' before start date '" + this.startDate + "'");
        }
        if (sourceProducts == null && (sourceProductPaths == null || sourceProductPaths.length == 0)) {
            throw new OperatorException("Either source products must be given or parameter 'sourceProductPaths' must be specified");
        }
        if (outputAsciiFile != null && outputAsciiFile.isDirectory()) {
            throw new OperatorException("Parameter 'outputAsciiFile' must not point to a directory.");
        }
        if (outputShapefile != null && outputShapefile.isDirectory()) {
            throw new OperatorException("Parameter 'outputShapefile' must not point to a directory.");
        }
    }

    Product[] collectSourceProducts() {
        final List<Product> products = new ArrayList<Product>();
        collectedProducts = new HashSet<Product>();
        if (sourceProducts != null) {
            Collections.addAll(products, sourceProducts);
        }
        if (sourceProductPaths != null) {
            SortedSet<File> fileSet = new TreeSet<File>();
            for (String filePattern : sourceProductPaths) {
                try {
                    WildcardMatcher.glob(filePattern, fileSet);
                } catch (IOException e) {
                    logReadProductError(filePattern);
                }
            }
            for (File file : fileSet) {
                try {
                    Product sourceProduct = ProductIO.readProduct(file);
                    if (sourceProduct != null) {
                        products.add(sourceProduct);
                        collectedProducts.add(sourceProduct);
                    } else {
                        logReadProductError(file.getAbsolutePath());
                    }
                } catch (IOException e) {
                    logReadProductError(file.getAbsolutePath());
                }
            }
        }

        return products.toArray(new Product[products.size()]);
    }

    private void logReadProductError(String file) {
        getLogger().severe(String.format("Failed to read from '%s' (not a data product or reader missing)", file));
    }

    private void setDummyTargetProduct() {
        final Product product = new Product("dummy", "dummy", 2, 2);
        product.addBand("dummy", ProductData.TYPE_INT8);
        setTargetProduct(product);
    }

    public static class BandConfiguration {

        @Parameter(description = "The name of the band in the source products. If empty, parameter 'expression' must " +
                                 "be provided.")
        String sourceBandName;

        @Parameter(description =
                           "The band maths expression serving as input band. If empty, parameter 'sourceBandName'" +
                           "must be provided.")
        String expression;

        @Parameter(description = "The band maths expression serving as criterion for whether to consider pixels for " +
                                 "computation.")
        String validPixelExpression;

        public BandConfiguration() {
            // used by DOM converter
        }
    }

    public static class UtcConverter implements Converter<ProductData.UTC> {

        @Override
        public ProductData.UTC parse(String text) throws ConversionException {
            try {
                return ProductData.UTC.parse(text, DATETIME_PATTERN);
            } catch (ParseException e) {
                throw new ConversionException(e);
            }
        }

        @Override
        public String format(ProductData.UTC value) {
            if (value != null) {
                return value.format();
            }
            return "";
        }

        @Override
        public Class<ProductData.UTC> getValueType() {
            return ProductData.UTC.class;
        }

    }

    interface Outputter {

        void initialiseOutput(Product[] sourceProducts, String[] bandNames, String[] algorithmNames, ProductData.UTC startDate, ProductData.UTC endDate,
                              String[] regionIds);

        void addToOutput(String bandName, String regionId, Map<String, Double> statistics);

        void finaliseOutput() throws IOException;
    }

    /**
     * The service provider interface (SPI) which is referenced
     * in {@code /META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(StatisticsOp.class);
        }
    }

}
