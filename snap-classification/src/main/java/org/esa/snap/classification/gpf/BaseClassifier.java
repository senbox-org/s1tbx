/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.classification.gpf;

import be.abeel.util.Pair;
import com.bc.ceres.core.ProgressMonitor;
import com.google.common.collect.ImmutableSet;
import com.thoughtworks.xstream.XStream;
import net.sf.javaml.classification.Classifier;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.dataop.downloadable.StatusProgressMonitor;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.StackUtils;
import org.esa.snap.engine_utilities.gpf.ThreadManager;
import org.esa.snap.engine_utilities.gpf.TileIndex;
import org.esa.snap.engine_utilities.util.VectorUtils;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/**
 * Base class for classifiers.
 */
public abstract class BaseClassifier implements SupervisedClassifier {

    private final ClassifierParams params;
    private final ClassifierReport classifierReport;

    private Product maskProduct = null;
    private Product[] featureProducts;
    private int sourceImageWidth;
    private int sourceImageHeight;
    private boolean classifierTrained = false;
    private boolean classifierFailed = false;

    private Product targetProduct = null;
    private Band labelBand = null; // target

    // E.g., for Random Forest, this is the number of trees that vote for the label over the total number of trees
    private Band confidenceBand = null; // target

    private Band trainingSetMaskBand = null; // source (only applicable for train on raster)
    private double maskNoDataValue = Double.NaN;
    private double maxClassValue = Double.NaN;

    private FeatureInfo[] featureInfoList;

    protected Classifier mlClassifier;

    private final static String LabelBandName = "LabeledClasses"; // target
    private final static String ConfidenceBandName = "Confidence"; // target
    public final static String VectorNodeNameLabelSource = "VectorNodeName"; // not used

    //private boolean doLoadClassifier;

    private VectorDataNode[] polygonVectorDataNodes;
    private Map<VectorDataNode, Integer> polygonVectorDataNodeToVectorIndex;
    private Map<Integer, String> classLabelMap;
    private Map<String, Integer> labelClassMap; // not used because useVectorNodeNameAsLabel is always true
    private boolean useVectorNodeNameAsLabel;

    private ClassifierDescriptor loadedClassifierDescriptor = null; // only for when doLoadClassifier is true

    private final static int INT_NO_DATA_VALUE = -1;
    private final static double DOUBLE_NO_DATA_VALUE = Double.NaN;
    private final static int NOT_IN_POLYGON = -1;
    private Double noDataVal;

    public final static String CLASSIFIER_FILE_EXTENSION = ".class";
    public final static String CLASSIFIER_USER_INFO_FILE_EXTENSION = ".xml";
    public final static String CLASSIFIER_ROOT_FOLDER = "classifiers";

    private double topClassifierPercent = 0;
    private String topClassifierName;
    private FeatureInfo[] topFeatureInfoList;

    private static final String[] excludedBands = new String[] {"lat_band", "long_band", "flags"};

    public static class ClassifierParams {
        private final String classifierType;
        private final String productSuffix;
        private final Product[] sourceProducts;
        private final int numTrainSamples;
        private double minClassValue;
        private double classValStepSize;
        private int classLevels;
        private String savedClassifierName;
        private boolean doLoadClassifier;
        private boolean doClassValQuantization;
        private final boolean trainOnRaster;
        private final String[] trainingBands;
        private String[] trainingVectors;
        private String labelSource;         // vector node name or attribute name
        private String[] featureBands;
        private final boolean evaluateClassifier;
        private final boolean evaluateFeaturePowerSet;
        private final int minPowerSetSize;
        private final int maxPowerSetSize;

        public ClassifierParams(final String classifierType, final String productSuffix, final Product[] sourceProducts,
                                final int numTrainSamples, final double minClassValue,
                                final double classValStepSize, final int classLevels,
                                final String savedClassifierName, final boolean doLoadClassifier,
                                final boolean doClassValQuantization,
                                final boolean trainOnRaster,
                                final String[] trainingBands,
                                final String[] trainingVectors,
                                final String[] featureBands,
                                final String labelSource,
                                final boolean evaluateClassifier,
                                final boolean evaluateFeaturePowerSet,
                                final int minPowerSetSize,
                                final int maxPowerSetSize) {
            this.classifierType = classifierType;
            this.productSuffix = productSuffix;
            this.sourceProducts = sourceProducts;
            this.numTrainSamples = numTrainSamples;
            this.minClassValue = minClassValue;
            this.classValStepSize = classValStepSize;
            this.classLevels = classLevels;
            this.savedClassifierName = savedClassifierName;
            this.doLoadClassifier = doLoadClassifier;
            this.doClassValQuantization = doClassValQuantization;
            this.trainOnRaster = trainOnRaster;
            this.trainingBands = trainingBands;
            this.trainingVectors = trainingVectors;
            this.featureBands = featureBands;
            this.labelSource = labelSource;
            this.evaluateClassifier = evaluateClassifier;
            this.evaluateFeaturePowerSet = evaluateFeaturePowerSet;
            this.minPowerSetSize = minPowerSetSize;
            this.maxPowerSetSize = maxPowerSetSize;
        }
    }

    public BaseClassifier(final ClassifierParams params) {
        this.params = params;
        this.classifierReport = new ClassifierReport(params.classifierType, params.savedClassifierName);
    }

    protected Object getObjectToSave(final Dataset trainDataset) {
        return mlClassifier;
    }

    protected Object getXMLInfoToSave(final ClassifierUserInfo commonInfo) {
        return commonInfo;
    }

    public String getClassifierType() {
        return params.classifierType;
    }

    public String getProductSuffix() {
        return params.productSuffix;
    }

    public String getClassifierName() {
        return params.savedClassifierName;
    }

    public Classifier getMLClassifier() {
        return mlClassifier;
    }

    public void initialize() throws OperatorException, IOException {

        checkSourceProductsValidity();

        // mask product is always the 1st product (same assumption in BaseOperatorUI)
        maskProduct = params.sourceProducts[0];

        if (params.classValStepSize < 0.0) {
            throw new OperatorException("Invalid class value step size = " + params.classValStepSize);
        }
        if (params.classLevels < 2) {
            throw new OperatorException("Invalid class levels = " + params.classLevels + "; it must be at least 2");
        }
        maxClassValue = getMaxValue(params.minClassValue, params.classValStepSize, params.classLevels);

        if (!params.doLoadClassifier) {
            if (params.trainOnRaster) {
                String bandName = "";
                // UI should not allow user to choose more than one training band
                if (params.trainingBands == null || params.trainingBands.length == 0) {
                    // user did not choose any, just take the first one
                    trainingSetMaskBand = maskProduct.getBandAt(0);
                } else {
                    bandName = params.trainingBands[0];
                    if (params.trainingBands[0].contains("::")) {
                        bandName = params.trainingBands[0].substring(0, params.trainingBands[0].indexOf("::"));
                    }
                    trainingSetMaskBand = maskProduct.getBand(bandName);
                }
                if (trainingSetMaskBand == null) {
                    throw new OperatorException("Fail to find training band in 1st source product: " + bandName);
                }
            }

            // Moved this check to trainClassifier() because initially, nothing is selected and we could have this
            // error thrown.
            /*
            if (params.featureBands == null) {
                final List<String> allBandnames = new ArrayList<>();
                for (Product p : params.sourceProducts) {
                    for (Band b : p.getBands()) {
                        if (b == trainingSetMaskBand) continue;
                        String bandName = b.getName();
                        if(excludeBand(bandName)) {
                            continue;
                        }
                        if (allBandnames.contains(bandName)) {
                            throw new OperatorException("Cannot have same feature band " + bandName + " in more than one product");
                        }
                        if (bandName.equals(trainingSetMaskBand.getName())) {
                            throw new OperatorException("Training band cannot be feature band");
                        }
                        allBandnames.add(bandName);
                    }
                } */

            if (params.featureBands != null) {
                List<String> bandNames = new ArrayList<>();
                for (String s : params.featureBands) {
                    final int multiProductIndex = s.indexOf("::");
                    String bandName = s;
                    //String productName = maskProduct.getName();
                    if (multiProductIndex > 0) {
                        bandName = s.substring(0, multiProductIndex);
                        if (bandNames.contains(bandName)) {
                            throw new OperatorException("Cannot select feature band " + bandName + " in more than one product");
                        } else {
                            bandNames.add(bandName);
                        }
                        //productName = s.substring(s.indexOf("::") + 2);
                    }
                    if (trainingSetMaskBand != null && bandName.equals(trainingSetMaskBand.getName())) {
                        throw new OperatorException("Cannot select training band as feature band");
                    }
                }
            }
        }

        // TODO...
        //final int startIdx = (maskProduct == null) ? 0 : 1;
        //featureProducts = new Product[params.sourceProducts.length - startIdx];
        //System.arraycopy(params.sourceProducts, 0 + startIdx, featureProducts, 0, featureProducts.length);
        featureProducts = params.sourceProducts;

        if (trainingSetMaskBand != null && trainingSetMaskBand.isNoDataValueSet()) {
            maskNoDataValue = trainingSetMaskBand.getNoDataValue();
        }

        // polygonsAsClasses contains the names of all the polygons the user has selected to use as classes.
        // E.g., the user can create polygons named "water", "trees" and "shrubs"
        // There will be 3 classes named "water", "trees" and "shrubs".
        // All the pixels in the "water" polygon will have the class "water"
        // Get the corresponding VectorDataNode and store them in polygonVectorDataNodes.
        // The polygons must be in the first product.
        // Can maskProduct ever be null??
        if (!params.doLoadClassifier && (maskProduct != null && !params.trainOnRaster)) {
            // train on vectors/polygons
            polygonVectorDataNodeToVectorIndex = new HashMap<>();

            if (params.trainingVectors == null || params.trainingVectors.length == 0) {
                final List<String> geometryNames = new ArrayList<>();
                final ProductNodeGroup<VectorDataNode> vectorDataNodes = maskProduct.getVectorDataGroup();
                for(int i=0; i< vectorDataNodes.getNodeCount(); ++i) {
                    VectorDataNode node = vectorDataNodes.get(i);
                    if(!node.getFeatureCollection().isEmpty()) {
                        geometryNames.add(node.getName() + "::" + maskProduct.getName());
                    }
                }

                // Here we are assuming that the vectors must come from the one product (namely the first product).
                // You cannot choose one polygon from one product and another polygon from another product.
                // Do we want to allow user to do that? It should work?!
                if (geometryNames.size() < 2) {
                    throw new OperatorException("Cannot train on vectors because first source product has less than 2 vectors");
                }
                params.trainingVectors = geometryNames.toArray(new String[geometryNames.size()]);
            }
            if (params.trainingVectors != null) { // Can this ever be false?
                //System.out.println("params.trainingVectors.length = " + params.trainingVectors.length);
                if (params.trainingVectors.length == 1) {
                    throw new OperatorException("Please select two or more vectors as classes");
                }

                polygonVectorDataNodes = new VectorDataNode[params.trainingVectors.length];
                final ProductNodeGroup<VectorDataNode> vectorGroup = maskProduct.getVectorDataGroup();
                for (int i = 0; i < params.trainingVectors.length; ++i) {
                    int multiProductIndex = params.trainingVectors[i].indexOf("::");
                    String name = params.trainingVectors[i];
                    if (multiProductIndex > 0) {
                        name = params.trainingVectors[i].substring(0, multiProductIndex);
                    }
                    //System.out.println("vector " + name);
                    polygonVectorDataNodes[i] = vectorGroup.get(name);
                    // So for class value "i", the label is polygonVectorDataNodes[i] which corresponds to
                    // params.trainingVectors[i]. This is used to set up classLabelMap.
                    if (polygonVectorDataNodes[i] == null) {
                        //System.out.println("fail to find vector " + name);
                        throw new OperatorException("Cannot find vector " + params.trainingVectors[i]);
                    /*} else {
                        System.out.println("found vector " + name);*/
                    }
                }

                //useVectorNodeNameAsLabel = params.labelSource == null || params.labelSource.isEmpty()
                //        || params.labelSource.equals(VectorNodeNameLabelSource);
                useVectorNodeNameAsLabel = true;

                // The index is going to be the class value
                classLabelMap = new HashMap<>();
                labelClassMap = new HashMap<>();
                int classIndex = 0;
                final Set<String> attribValues = new HashSet<>();
                for (int i = 0; i < polygonVectorDataNodes.length; i++) {
                    polygonVectorDataNodeToVectorIndex.put(polygonVectorDataNodes[i], i);
                    if (useVectorNodeNameAsLabel) {
                        classIndex = i;
                        classLabelMap.put(classIndex, polygonVectorDataNodes[i].getName());
                    } else {
                        String classLabel = VectorUtils.getAttribStringValue(polygonVectorDataNodes[i], params.labelSource);

                        if (!classLabelMap.values().contains(classLabel)) {
                            classLabelMap.put(classIndex, classLabel);
                            labelClassMap.put(classLabel, classIndex);
                            classIndex++;
                        }
                    }
                }
            }
        }

        //SystemUtils.LOG.info("doLoadClassifier = " + params.doLoadClassifier);
        //SystemUtils.LOG.info("trainOnRaster = " + params.trainOnRaster);
        //SystemUtils.LOG.info("doClassValQuantization = " + doClassValQuantization);
        //SystemUtils.LOG.info("Min class value = " + minClassValue + "; class value step size = " + classValStepSize
        //        + "; class levels = " + classLevels + "; max class value = " + maxClassValue);
    }

    public static double getMaxValue(final double minVal, final double stepSize, final int levels) {
        return minVal + stepSize * (levels - 1);
    }

    private void checkSourceProductsValidity() {

        // All the source products must have the same raster dimensions.
        // All bands must have the same raster dimensions as the product it belongs to.

        sourceImageHeight = params.sourceProducts[0].getSceneRasterHeight();
        sourceImageWidth = params.sourceProducts[0].getSceneRasterWidth();

        for (int i = 0; i < params.sourceProducts.length; i++) {
            if (sourceImageHeight != params.sourceProducts[i].getSceneRasterHeight() ||
                    sourceImageWidth != params.sourceProducts[i].getSceneRasterWidth()) {
                throw new OperatorException("Source products are of different dimensions");
            }

            for (Band band:params.sourceProducts[i].getBands()) {
                if (band.getRasterWidth() != sourceImageWidth || band.getRasterHeight() != sourceImageHeight) {
                    throw new OperatorException("Bands in source product " + params.sourceProducts[i].getName() +
                            " are of different dimensions");
                }
            }
        }
    }

    /**
     * Create target product.
     */
    public Product createTargetProduct() {

        targetProduct = new Product(
                params.sourceProducts[0].getName() + getProductSuffix(),
                params.sourceProducts[0].getProductType(),
                sourceImageWidth,
                sourceImageHeight);

        ProductUtils.copyProductNodes(params.sourceProducts[0], targetProduct);

        int dataType;
        String unit;
        String bandName = LabelBandName;
        if (params.doLoadClassifier) {
            ClassifierUserInfo info = readClassifierXML();
            dataType = info.datatype;
            unit = info.unit;
            if (!info.className.contains("???")) {
                bandName = "predicted" + info.className;
            }
        }  else {
            dataType = (params.trainOnRaster && trainingSetMaskBand != null) ? trainingSetMaskBand.getDataType() : ProductData.TYPE_INT16;
            unit = (params.trainOnRaster && trainingSetMaskBand != null ? trainingSetMaskBand.getUnit() : "discrete classes");
            bandName = (params.trainOnRaster && trainingSetMaskBand != null) ? "predicted" + trainingSetMaskBand.getName() : LabelBandName;
        }

        labelBand = new Band(
                bandName,
                dataType,
                sourceImageWidth,
                sourceImageHeight);

        labelBand.setUnit(unit);
        //final double noDataVal = (params.trainOnRaster && trainingSetMaskBand != null) ? trainingSetMaskBand.getNoDataValue() : INT_NO_DATA_VALUE;
        noDataVal = (dataType == ProductData.TYPE_INT16) ? INT_NO_DATA_VALUE : DOUBLE_NO_DATA_VALUE;
        labelBand.setNoDataValue(noDataVal);
        labelBand.setNoDataValueUsed(true);
        labelBand.setValidPixelExpression(ConfidenceBandName + " >= 0.5"); // Can change this in properties of band

        //System.out.println("BaseClassifier.createTargetProduct: trainOnRaster = " + params.trainOnRaster +
        //    " doLoadClassifier = " + params.doLoadClassifier);
        if (!params.trainOnRaster) {
            if (!params.doLoadClassifier) {
                // train on vectors/polygons
                final IndexCoding indexCoding = new IndexCoding("Classes");
                indexCoding.addIndex("no data", INT_NO_DATA_VALUE, "no data");
                for (Integer i : classLabelMap.keySet()) {
                    String label = classLabelMap.get(i);
                    if (label == null || label.isEmpty())
                        label = "null";
                    indexCoding.addIndex(label, i, "");
                }
                targetProduct.getIndexCodingGroup().add(indexCoding);
                labelBand.setSampleCoding(indexCoding);

                // remove training vectors
                final ProductNodeGroup<VectorDataNode> vectorDataGroup = targetProduct.getVectorDataGroup();
                for (String vector : params.trainingVectors) {
                    vectorDataGroup.remove(vectorDataGroup.get(createClassLabel(vector)));
                }
            }
        } else if (!params.doLoadClassifier) {
            // train on raster
            IndexCoding indexCoding = trainingSetMaskBand.getIndexCoding();
            if(indexCoding != null) {
                IndexCoding icCopy = ProductUtils.copyIndexCoding(indexCoding, targetProduct);
                labelBand.setSampleCoding(icCopy);
            }
        }
        targetProduct.addBand(labelBand);

        confidenceBand = new Band(
                ConfidenceBandName,
                ProductData.TYPE_FLOAT32,
                sourceImageWidth,
                sourceImageHeight);

        confidenceBand.setUnit("(0, 1]");
        confidenceBand.setNoDataValue(DOUBLE_NO_DATA_VALUE);
        confidenceBand.setNoDataValueUsed(true);
        targetProduct.addBand(confidenceBand);

        return targetProduct;
    }

    private static String createClassLabel(String vectorName) {
        String label = vectorName;
        if (vectorName.contains("::")) {
            label = vectorName.substring(0, vectorName.indexOf("::"));
        }
        return label;
    }

    public static boolean containsFeature(final Product product, final String[] featureNames) {
        final String[] bandnames = product.getBandNames();
        if (featureNames == null || bandnames == null) {
            return false;
        }
        for (String featureName : featureNames) {
            for (String bandname : bandnames) {
                if (bandname.contains(featureName)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected double getConfidence(final Instance instance, final Object classVal) {
        final Map<Object, Double> classDis = mlClassifier.classDistribution(instance);
        return classDis.get(classVal);
    }

    public void computeTileStack(final Operator operator, final Map<Band, Tile> targetTileMap,
                                 final Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException, IOException {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int xMax = x0 + targetRectangle.width;
        final int yMax = y0 + targetRectangle.height;

        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + targetRectangle.width + ", h = " + targetRectangle.height);

        if (classifierFailed) return;

        if (!classifierTrained) {
            //System.out.println("#### x0 = " + x0 + ", y0 = " + y0 + ", w = " + targetRectangle.width + ", h = " + targetRectangle.height);
            if (params.doLoadClassifier) {
                loadClassifier(operator);
            } else {
                trainClassifier(operator, pm);
            }
        }

        if (classifierFailed) return;

        //System.out.println("%%%% x0 = " + x0 + ", y0 = " + y0 + ", w = " + targetRectangle.width + ", h = " + targetRectangle.height);

        final Tile labelTile = targetTileMap.get(labelBand);
        final Tile confidenceTile = targetTileMap.get(confidenceBand);
        final ProductData labelBuffer = labelTile.getDataBuffer();
        final ProductData confidenceBuffer = confidenceTile.getDataBuffer();
        final TileIndex tgtIndex = new TileIndex(labelTile);

        final Tile[] featureTiles = new Tile[featureInfoList.length];
        int i = 0;
        for (FeatureInfo feature : featureInfoList) {
            featureTiles[i++] = operator.getSourceTile(feature.featureBand, targetRectangle);
        }

        for (int y = y0; y < yMax; ++y) {
            tgtIndex.calculateStride(y);
            for (int x = x0; x < xMax; ++x) {
                final int tgtIdx = tgtIndex.getIndex(x);
                final double[] features = getFeatures(featureTiles, featureInfoList, x, y);
                if (features == null) {
                    //labelBuffer.setElemDoubleAt(tgtIdx, params.trainOnRaster ? DOUBLE_NO_DATA_VALUE : INT_NO_DATA_VALUE);
                    labelBuffer.setElemDoubleAt(tgtIdx, noDataVal);
                    confidenceBuffer.setElemDoubleAt(tgtIdx, DOUBLE_NO_DATA_VALUE);
                    continue;
                }

                final Instance instance = new DenseInstance(features);

                double confidence = DOUBLE_NO_DATA_VALUE;
                Object classVal = mlClassifier.classify(instance);
                if (classVal == null) {
                    //classVal = params.trainOnRaster ? DOUBLE_NO_DATA_VALUE : INT_NO_DATA_VALUE;
                    classVal = noDataVal;
                } else {
                    confidence = getConfidence(instance, classVal);
                }
                labelBuffer.setElemDoubleAt(tgtIdx, (double) classVal);

                // classVal MUST be a key in classDis?
                //final double confidence = classDis.containsKey(classVal) ? classDis.get(classVal) : 0.0 ;
                confidenceBuffer.setElemDoubleAt(tgtIdx, confidence);
            }
        }
        //System.out.println("DONE x0 = " + x0 + ", y0 = " + y0 + ", w = " + targetRectangle.width + ", h = " + targetRectangle.height);
    }

    public static int getTotalNumBands(final Product[] products) {
        int numBands = 0;
        for (Product product : products) {
            numBands += product.getNumBands();
        }
        return numBands;
    }

    private void getVectorInstanceLists(final List<Instance> parentList,
                                        final List<Instance> trainList, final List<Instance> testList) {

        // This is only for the case where we get the samples a.k.a. instances from polygons.
        // parentList contains a balanced list of the instances. E.g., if there are 3 classes, namely 0, 1 and 2
        // and there are 300 Instances in parentList, there should be 100 of each class in parentList.
        // We want to put 50 of each class into trainList and testList.

        final HashMap<Integer, List<Instance>> classToInstanceListMap = new HashMap<>();
        for (Integer i : classLabelMap.keySet()) {
            classToInstanceListMap.put(i, new ArrayList<>());
        }

        for (Instance instance : parentList) {
            final int classVal = (int) ((double) instance.classValue());
            classToInstanceListMap.get(classVal).add(instance);
        }

        for (Integer i : classLabelMap.keySet()) {
            final List<Instance> list = classToInstanceListMap.get(i);

            // add every other to train or test list
            boolean addToTrainList = true;
            for(Instance instance : list) {
                if(addToTrainList) {
                    trainList.add(instance);
                    addToTrainList = false;
                } else {
                    testList.add(instance);
                    addToTrainList = true;
                }
            }
        }
    }

    public static boolean excludeBand(final String bandName) {
        for(String excludedBand : excludedBands) {
            if (bandName.startsWith(excludedBand)) {
                return true;
            }
        }
        return false;
    }

    private synchronized void trainClassifier(final Operator operator, final ProgressMonitor opPM) throws IOException {

        if (classifierTrained || classifierFailed) return;

        Path path = getClassifierFilePath(true);
        if (path == null) {
            classifierFailed = true; // So it won't try to train again
            throw new OperatorException("Stopped by user. Please type in new classifier name.");
        }

        try {
            if (params.featureBands == null) {
                final List<String> allFeatureBands = new ArrayList<>();
                final List<String> allBandnames = new ArrayList<>();
                for (Product p : params.sourceProducts) {
                    for (Band b : p.getBands()) {
                        if (b == trainingSetMaskBand) continue;
                        String bandName = b.getName();
                        if(excludeBand(bandName)) {
                            continue;
                        }
                        if (allBandnames.contains(bandName)) {
                            classifierFailed = true; // So it won't try to train again
                            throw new OperatorException("Cannot have same feature band " + bandName + " in more than one product");
                        }
                        if (trainingSetMaskBand != null && bandName.equals(trainingSetMaskBand.getName())) {
                            classifierFailed = true; // So it won't try to train again
                            throw new OperatorException("Training band cannot be feature band - " + bandName);
                        }
                        allBandnames.add(bandName);
                        allFeatureBands.add(bandName + "::" + p.getName());
                    }
                }
                params.featureBands = allFeatureBands.toArray(new String[allFeatureBands.size()]);
            }

            final Map<String, Product> productHashMap = new HashMap<>();
            for (Product product : params.sourceProducts) {
                productHashMap.put(product.getName(), product);
            }
            int i = 0;
            final List<FeatureInfo> featureInfos = new ArrayList<>(params.featureBands.length);
            for (String s : params.featureBands) {
                //System.out.println("trainClassifier: feature band = " + s);
                final int multiProductIndex = s.indexOf("::");
                String bandName = s;
                String productName = maskProduct.getName();
                if (multiProductIndex > 0) {
                    bandName = s.substring(0, multiProductIndex);
                    productName = s.substring(s.indexOf("::") + 2);
                }

                final Product product = productHashMap.get(productName);
                if (product != null) {
                    final Band featureBand = product.getBand(bandName);
                    if (featureBand == null) {
                        classifierFailed = true; // So it won't try to train again
                        throw new OperatorException("Failed to find feature band " + s);
                    } else if (trainingSetMaskBand != null && featureBand == trainingSetMaskBand) {
                        classifierFailed = true; // So it won't try to train again
                        throw new OperatorException("The training band has also been selected as a feature band");
                    }

                    FeatureInfo featureInfo = new FeatureInfo(featureBand, i);
                    featureInfos.add(featureInfo);

                    i++;
                } else {
                    classifierFailed = true; // So it won't try to train again
                    throw new OperatorException("Failed to find feature product " + s);
                }
            }

            featureInfoList = featureInfos.toArray(new FeatureInfo[featureInfos.size()]);

            final LabeledInstances allLabeledInstances = getLabeledInstances(operator, params.numTrainSamples * 2,
                                                                             featureInfoList);

            if (params.evaluateClassifier && params.evaluateFeaturePowerSet) {
                runFeaturePowerSet(operator, allLabeledInstances, featureInfoList, opPM);
            }

            if (!classifierTrained) {  // classifierTrained can be true if runFeaturePowerSet() is called

                mlClassifier = createMLClassifier(featureInfoList);

                Dataset trainDataset = trainClassifier1(mlClassifier, getClassifierName(), allLabeledInstances,
                                                       featureInfoList, false);

                saveClassifier(trainDataset);
            }

        } finally {
            classifierTrained = true;
        }
    }

    private Dataset trainClassifier1(final Classifier classifier, final String name,
                                    final LabeledInstances labeledInstances,
                                    final FeatureInfo[] featureInfos, boolean quickEvaluation) {

        final List<Instance> trainList;
        final List<Instance> testList;
        if (params.trainOnRaster) {
            trainList = labeledInstances.instanceList.subList(0, labeledInstances.instanceList.size() / 2);
            testList = labeledInstances.instanceList.subList(labeledInstances.instanceList.size() / 2,
                                                             labeledInstances.instanceList.size());
        } else {
            trainList = new ArrayList<>();
            testList = new ArrayList<>();
            getVectorInstanceLists(labeledInstances.instanceList, trainList, testList);
        }

        final Dataset trainDataset = new DefaultDataset(trainList);

        buildClassifier(classifier, trainDataset);

        if (params.evaluateClassifier) {
            final Dataset testDataset = new DefaultDataset(testList);

            if (quickEvaluation) {
                runQuickEvaluation(classifier, name, labeledInstances, featureInfos, testDataset);
            } else {
                runEvaluation(classifier, labeledInstances, featureInfos, testDataset);
            }
        }

        return trainDataset;
    }

    protected void buildClassifier(final Classifier classifier, final Dataset trainDataset) {
        classifier.buildClassifier(trainDataset);
    }

    private void runEvaluation(final Classifier mlClassifier,
                               final LabeledInstances labeledInstances, final FeatureInfo[] featureInfos,
                               final Dataset testDataset) {
        final StatusProgressMonitor pm = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);

        //Thread thread1 = new Thread(new Runnable() {
        //    @Override
        //    public void run(){
        try {
            final Evaluator evaluator = new Evaluator(mlClassifier, classifierReport);

            evaluator.evaluateClassifier(labeledInstances.labelMap,
                                         labeledInstances.instanceList,
                                         testDataset, "Testing");

            evaluator.evaluateFeatures(featureInfos, testDataset, "Testing", pm);

            saveAndOpenReport(true);

        } finally {
            pm.done();
        }
        //    }
        // });
        // thread1.start();
    }

    private void saveAndOpenReport(boolean openReport) {
        try {
            classifierReport.writeReport();

            if (openReport) {
                classifierReport.openClassifierReport();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runQuickEvaluation(final Classifier mlClassifier, final String name,
                                    final LabeledInstances labeledInstances, final FeatureInfo[] featureInfos,
                                    final Dataset testDataset) {

        final Evaluator evaluator = new Evaluator(mlClassifier, new ClassifierReport(params.classifierType, "dummy"));

        Evaluator.Score score = evaluator.evaluateClassifier(labeledInstances.labelMap,
                                                             labeledInstances.instanceList,
                                                             testDataset, "Testing");

        StringBuilder featureBands = new StringBuilder();
        for (FeatureInfo featureInfo : featureInfos) {
            featureBands.append(featureInfo.featureBand.getName());
            featureBands.append(", ");
        }
        classifierReport.addPowerSetEvaluation(name + ": " + "cv " + f(score.crossValidationPercent * 100) + "% "
                                     + featureBands.toString());

        /*
        final StatusProgressMonitor pm = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
        evaluator.evaluateFeatures(featureInfos, testDataset, "Testing", pm);

        StringBuilder featureScoreStr = new StringBuilder();
        for (String key : score.featureScoreMap.keySet()) {
            featureScoreStr.append(String.format("%-20s", key));
            featureScoreStr.append(score.featureScoreMap.get(key) + '\n');
        }
        classifierReport.addPowerSetEvaluation(featureScoreStr.toString());
        */

        if (score.crossValidationPercent > topClassifierPercent) {
            updateTopSpot(score.crossValidationPercent, name, featureInfos);
        }
    }

    private static String f(double val) {
        return String.format("%-6.2f", val);
    }

    private synchronized void updateTopSpot(final double percentCorrect, final String name, final FeatureInfo[] featureInfos) {
        topClassifierPercent = percentCorrect;
        topClassifierName = name;
        topFeatureInfoList = featureInfos;
    }

    private void runFeaturePowerSet(final Operator operator, final LabeledInstances allLabeledInstances,
                                    final FeatureInfo[] completeFeatureInfoList, final ProgressMonitor opPM) {
        final StatusProgressMonitor pm = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);

        try {
            // get the power set of all features
            final PowerSet<FeatureInfo> featurePowerSet = new PowerSet<>(ImmutableSet.copyOf(Arrays.asList(completeFeatureInfoList)),
                    params.minPowerSetSize, params.maxPowerSetSize);

            List<Set<FeatureInfo>> featureSetList = new ArrayList<>();
            for (Set<FeatureInfo> featureSet : featurePowerSet) {
                featureSetList.add(featureSet);
            }
            pm.beginTask("Evaluating feature power set", featureSetList.size());

            int cnt = 1;
            for (Set<FeatureInfo> featureSet : featureSetList) {
                if(opPM.isCanceled()) {
                    break;
                }

                final FeatureInfo[] featureInfos = featureSet.toArray(new FeatureInfo[featureSet.size()]);

                Classifier setClassifier = createMLClassifier(featureInfos);

                // create subset of labeledInstances
                LabeledInstances subsetLabeledInstances = createSubsetLabeledInstances(featureInfos, allLabeledInstances);

                //final LabeledInstances allLabeledInstances2 = getLabeledInstances(operator, params.numTrainSamples * 2,
                //                                                                 featureInfoList);

                trainClassifier1(setClassifier, getClassifierName() + '.' + cnt, subsetLabeledInstances,
                                featureInfos, true);
                ++cnt;
                pm.worked(1);
            }

            classifierReport.setTopClassifier("TOP Classifier = " + topClassifierName + " at " +
                                         String.format("%-6.2f", topClassifierPercent * 100) + '%');

            if (topFeatureInfoList != null) {
                featureInfoList = topFeatureInfoList;

                mlClassifier = createMLClassifier(featureInfoList);

                // create subset of labeledInstances
                //LabeledInstances subsetLabeledInstances = createSubsetLabeledInstances(featureInfoList, allLabeledInstances);

                final LabeledInstances allLabeledInstances2 = getLabeledInstances(operator, params.numTrainSamples * 2,
                                                                                 featureInfoList);

                Dataset trainDataset = trainClassifier1(mlClassifier, getClassifierName(), allLabeledInstances2,
                                                       featureInfoList, false);

                saveClassifier(trainDataset);

                classifierTrained = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            pm.done();
        }
    }

    private LabeledInstances createSubsetLabeledInstances(final FeatureInfo[] featureInfos,
                                                          final LabeledInstances allLabeledInstances) {
        final List<Instance> instanceList = new ArrayList<>();

        final List<Integer> featureIndexList = new ArrayList<>();
        for (FeatureInfo fi : featureInfos) {
            String name = fi.featureBand.getName();
            int i = 0;
            for (FeatureInfo origFI : featureInfoList) {
                if (name.equals(origFI.featureBand.getName())) {
                    featureIndexList.add(i);
                    break;
                }
                ++i;
            }
        }

        for (Instance instance : allLabeledInstances.instanceList) {
            instance.keySet();

            Instance newInstance = new DenseInstance(featureIndexList.size());
            newInstance.setClassValue(instance.classValue());

            int i = 0;
            for (Integer index : featureIndexList) {
                newInstance.put(i++, instance.get(index));
            }

            instanceList.add(newInstance);
        }

        return new LabeledInstances(allLabeledInstances.labelMap, instanceList);
    }

    private Path getClassifierFilePath(boolean doCheck) throws IOException {

        final Path classifierDir = SystemUtils.getAuxDataPath().
                resolve(CLASSIFIER_ROOT_FOLDER).resolve(params.classifierType);
        Path path = null;
        if (Files.notExists(classifierDir)) {
            Files.createDirectories(classifierDir);
        }
        path = classifierDir.resolve(params.savedClassifierName + CLASSIFIER_FILE_EXTENSION);
        if (doCheck && !Files.notExists(path)) {
            final int answer = JOptionPane.showOptionDialog(null,
                    "File " + path + " already exists.\nWould you like to overwrite?", "Overwrite?",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);

            if (answer == JOptionPane.NO_OPTION) {
                return null;
            }
        }
        return path;
    }

    public static void findBandInProducts(final Product[] products, final String bandName, final int[] indices) {
        // indices[0] indexes into featureProducts (i.e., which product)
        // indices[1] indexes into featureProducts[indices[0]].getBandAt() (i.e. which band in the product)
        indices[0] = -1;
        indices[1] = -1;
        for (int i = 0; i < products.length; i++) {
            for (int j = 0; j < products[i].getNumBands(); j++) {
                if (products[i].getBandAt(j).getName().contains(bandName)) {
                    indices[0] = i;
                    indices[1] = j;
                    return;
                }
            }
        }
    }

    private String extractXMLValue(final String s) {
        final int leftIdx = s.indexOf('>');
        final int rightIdx = s.indexOf('<', leftIdx);
        return s.substring(leftIdx + 1, rightIdx);
    }

    private ClassifierUserInfo readClassifierXML() {

        final Path classifierDir = SystemUtils.getAuxDataPath().
                resolve(CLASSIFIER_ROOT_FOLDER).resolve(params.classifierType);
        if (Files.notExists(classifierDir)) {
            throw new OperatorException("Classifier directory does not exist: " + classifierDir.toString());
        }
        Path path = classifierDir.resolve(params.savedClassifierName + CLASSIFIER_USER_INFO_FILE_EXTENSION);
        //System.out.println("BaseClassifier.readClassifierXML " + path.toString());

        ClassifierUserInfo info = new ClassifierUserInfo();
        try {

            final FileReader fileReader = new FileReader(path.toFile());
            final BufferedReader bufferedReader = new BufferedReader(fileReader);

            String s;
            while((s = bufferedReader.readLine()) != null) {
                //System.out.println("* " + s);
                String val;
                // Can add more if needed
                if (s.contains("datatype")) {
                    val = extractXMLValue(s);
                    //System.out.println("datatype = " + val);
                    info.datatype = Integer.parseInt(val);
                } else if (s.contains("unit")) {
                    val = extractXMLValue(s);
                    //System.out.println("unit = " + val);
                    info.unit = val;
                } else if (s.contains("className")) {
                    val = extractXMLValue(s);
                    info.className = val;
                }
            }

            fileReader.close();

        } catch (Exception ex) {
            throw new OperatorException("Failed to read XML classifier " + ex.getMessage());
        }

        return info;
    }

    private void loadClassifierDescriptor() {

        try {
            final Path filePath = getClassifierFilePath(false);

            final FileInputStream fis = new FileInputStream(filePath.toString());
            try (final ObjectInputStream in = new ObjectInputStream(fis)) {

                loadedClassifierDescriptor = (ClassifierDescriptor) in.readObject();

                final String cType = loadedClassifierDescriptor.getClassifierType();
                if (!cType.equals(params.classifierType)) {
                    throw new OperatorException("Loaded classifier is " + cType + " NOT " + params.classifierType);
                }

                params.doClassValQuantization = loadedClassifierDescriptor.getDoClassValQuantization();
                params.minClassValue = loadedClassifierDescriptor.getMinClassValue();
                params.classValStepSize = loadedClassifierDescriptor.getClassValStepSize();
                params.classLevels = loadedClassifierDescriptor.getClassLevels();
                params.trainingVectors = loadedClassifierDescriptor.getPolygonsAsClasses();
            }
        } catch (Exception ex) {
            throw new OperatorException("Failed to load classifier " + ex.getMessage());
        }
    }

    private synchronized void loadClassifier(final Operator operator) throws IOException {

        //System.out.println("BaseClassifier.loadClassifier: classifieredTrained = " + classifierTrained);

        if (classifierTrained || classifierFailed) return;

        try {
            loadClassifierDescriptor();

            final String[] featureNames = loadedClassifierDescriptor.getFeatureNames();
            /*
            for (String s : featureNames) {
                System.out.println("feature name = " + s);
            }*/

            final int totalAvailableFeatures = getTotalNumBands(featureProducts);
            //System.out.println("totalAvailableFeatures = " + totalAvailableFeatures);
            if (featureNames.length > totalAvailableFeatures) {
                classifierFailed = true; // So it won't try to train again
                throw new OperatorException("classifier expects " + featureNames.length
                                                    + " features; source product(s) only have " + totalAvailableFeatures);
            }

            mlClassifier = retrieveMLClassifier(loadedClassifierDescriptor);

            final double[] featureMinValues = loadedClassifierDescriptor.getFeatureMinValues();
            final double[] featureMaxValues = loadedClassifierDescriptor.getFeatureMaxValues();

            SystemUtils.LOG.info("*** Loaded " + params.classifierType + " classifier (filename = " + params.savedClassifierName
                                         + ") to predict " + loadedClassifierDescriptor.getClassName());

            double[] sortedClasses = loadedClassifierDescriptor.getSortedClassValues();
            String[] labels = loadedClassifierDescriptor.getPolygonsAsClasses();
            //final IndexCoding indexCoding = new IndexCoding("Classes");
            //indexCoding.addIndex("no data", INT_NO_DATA_VALUE, "no data");
            if (labels == null || labels.length == 0) {
                /*
                for (int i = 0; i < sortedClasses.length; i++) {
                    indexCoding.addIndex("Label_" + (i+1), (int) sortedClasses[i], "");
                }
                */
            }  else {
                final IndexCoding indexCoding = new IndexCoding("Classes");
                indexCoding.addIndex("no data", INT_NO_DATA_VALUE, "no data");
                for (int i = 0; i < sortedClasses.length; i++) {
                    //indexCoding.addIndex("label_" + i, (int)sortedClasses[i], "");
                    final int idx = labels[i].indexOf("::");
                    // It looks like that...
                    // When you are training and you select (i.e., highlight) the Training vectors that you want, they are
                    // save without "::". If you do not select any Training vectors and by default all are used, they are
                    // saved with "::". Either way, we are handling both cases here.
                    if (idx < 0) {
                        indexCoding.addIndex(labels[i], (int) sortedClasses[i], "");
                    } else {
                        indexCoding.addIndex(labels[i].substring(0, idx), (int) sortedClasses[i], "");
                    }
                }
                //}
                targetProduct.getIndexCodingGroup().add(indexCoding);
                labelBand.setSampleCoding(indexCoding);
            }

            int numFeatures = featureNames.length;
            final List<FeatureInfo> featureInfos = new ArrayList<>(featureNames.length);
            final Set<Pair<Integer, Integer>> indicesSet = new HashSet<>();
            for (int i = 0; i < numFeatures; i++) {
                final int[] indices = new int[2];
                findBandInProducts(featureProducts, featureNames[i], indices);
                if (indices[0] < 0) {
                    classifierFailed = true; // So it won't try to train again
                    throw new OperatorException("Failed to find feature band " + featureNames[i] + " in source product");
                }
                final Pair<Integer, Integer> idxPair = new Pair(indices[0], indices[1]);
                //System.out.println("BaseClassifier.loadClassifier: feature band " + featureNames[i] + ": " + indices[0] + ", " + indices[1]);
                if (indicesSet.contains(idxPair)) {
                    classifierFailed = true; // So it won't try to train again
                    throw new OperatorException(featureProducts[indices[0]].getBandAt(indices[1]).getName() +
                                                        " for " + featureNames[i] + " has already appeared as an earlier feature");
                }
                indicesSet.add(idxPair);
                Band featureBand = featureProducts[indices[0]].getBandAt(indices[1]);
                //System.out.println("loadClassifier: featureBand = " + featureBand);
                double noDataValue = DOUBLE_NO_DATA_VALUE;
                if (featureBand.isNoDataValueSet()) {
                    noDataValue = featureBand.getNoDataValue();
                }

                double offset = featureMinValues[i];
                double scale = 1.0 / (featureMaxValues[i] - offset);

                //System.out.println("loadClassifier: " + featureBand.getName() + " scale = " + scale);
                featureInfos.add(new FeatureInfo(featureBand, i, noDataValue, offset, scale));
            }

            featureInfoList = featureInfos.toArray(new FeatureInfo[featureInfos.size()]);

        } catch (Exception ex) {
            classifierFailed = true; // So it won't try to train again
            throw new OperatorException("Error loading or using loaded classifier (" + ex.getMessage() + ')');
        }

        // Do not support evaluation of classifier if it is loaded from file (for now anyways)
        /*
        if (params.evaluateClassifier && trainingSetMaskBand != null) {
            final LabeledInstances labeledInstances = getLabeledInstances(operator, params.numTrainSamples, featureInfoList);
            final Dataset testDataset = new DefaultDataset(labeledInstances.instanceList);

            runEvaluation(mlClassifier, labeledInstances, featureInfoList, testDataset);
        } */

        classifierTrained = true;
    }

    private static String getFirstPartOfExpression(final String polygonName, final int polygonIdx) {
        return '\'' + polygonName + "' ? " + polygonIdx + " : ";
    }

    private static String getExpression(final VectorDataNode[] polygons,
                                        final Map<VectorDataNode, Integer> indexMap) {

        if (polygons == null || indexMap == null) {
            return null;
        }

        final VectorDataNode firstNode = polygons[0];
        String expression = getFirstPartOfExpression(firstNode.getName(), indexMap.get(firstNode)) + NOT_IN_POLYGON;

        for (int i = 1; i < polygons.length; i++) {
            final VectorDataNode nextNode = polygons[i];
            expression = getFirstPartOfExpression(nextNode.getName(), indexMap.get(nextNode)) + '(' + expression + ')';
        }

        return expression;
    }

    private LabeledInstances getInstanceListFromPolygons(final Operator operator, final int numInstances,
                                                         final FeatureInfo[] featureInfos) throws OperatorException {

        final Dimension tileSize = new Dimension(512, 512);
        final Rectangle[] tileRectangles = OperatorUtils.getAllTileRectangles(maskProduct, tileSize, 0);

        final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
        status.beginTask("Extracting data... ", tileRectangles.length);

        final List<Instance> instanceList = new ArrayList<>();
        final ThreadManager threadManager = new ThreadManager();

        final int numClasses = classLabelMap.size();
        final int[] instancesCnt = new int[numClasses];
        for (int i = 0; i < instancesCnt.length; i++) {
            instancesCnt[i] = 0;
        }
        final int maxCnt = (int) Math.ceil(numInstances / (double) numClasses);

        //SystemUtils.LOG.info("getInstanceListFromPolygons maxCnt = " + maxCnt + " numInstances = " + numInstances);
        //SystemUtils.LOG.info("getInstanceListFromPolygons #tile rectangles = " + tileRectangles.length);

        try {
            // Loop through each rectangle to see if it intersects with any of the class polygons, if it does, then
            // the intersecting pixel is added to instanceList
            for (int i = 0; i < tileRectangles.length; i++) {

                final Rectangle rectangle = tileRectangles[i];

                // Get the class polygons that intersect this rectangle
                final VectorDataNode[] polygons = VectorUtils.getPolygonsForOneRectangle(rectangle,
                                                                                         params.sourceProducts[0].getSceneGeoCoding(),
                                                                                         polygonVectorDataNodes);
                if (polygons.length == 0) {
                    status.worked(1);
                    continue;
                }

                final String virtualBandName = "tmpVirtualBand_" + i;
                final String expression = getExpression(polygons, polygonVectorDataNodeToVectorIndex);
                // The virtual band will contain the class value
                final Band virtualBand = new VirtualBand(
                        virtualBandName,
                        ProductData.TYPE_INT16,
                        sourceImageWidth,
                        sourceImageHeight,
                        expression);
                maskProduct.addBand(virtualBand);

                //System.out.println(virtualBandName + ": " + expression);

                final Thread worker = new Thread() {

                    @Override
                    public void run() {
                        try {
                            final int x0 = rectangle.x, y0 = rectangle.y;
                            final int w = rectangle.width, h = rectangle.height;
                            final int xMax = x0 + w, yMax = y0 + h;

                            final Tile virtualBandTile = operator.getSourceTile(virtualBand, rectangle);
                            final ProductData virtualBandData = virtualBandTile.getDataBuffer();

                            final Tile[] featureTiles = new Tile[featureInfos.length];
                            for (int j = 0; j < featureInfos.length; j++) {
                                featureTiles[j] = operator.getSourceTile(featureInfos[j].featureBand, rectangle);
                            }

                            for (int y = y0; y < yMax; ++y) {
                                for (int x = x0; x < xMax; ++x) {

                                    int classVal = virtualBandData.getElemIntAt(virtualBandTile.getDataBufferIndex(x, y));
                                    if (classVal < 0) {
                                        // This pixel is not inside a class polygon
                                        continue;
                                    }

                                    // Get the features values for this pixel
                                    final double[] features = getFeatures(featureTiles, featureInfos, x, y);
                                    if (features == null) {
                                        continue;
                                    }

                                    final Instance instance = new DenseInstance(features);
                                    if (useVectorNodeNameAsLabel) {
                                        instance.setClassValue((double) classVal);
                                    } else {
                                        int vectorIndex = classVal;
                                        String val = VectorUtils.getAttribStringValue(polygonVectorDataNodes[vectorIndex],
                                                                                params.labelSource);
                                        classVal = labelClassMap.get(val);
                                        instance.setClassValue((double) classVal);
                                    }

                                    synchronized (instanceList) {
                                        if (instanceList.size() < numInstances) {
                                            if (instancesCnt[classVal] < maxCnt) {
                                                instanceList.add(instance);
                                                instancesCnt[classVal]++;
                                                if (instanceList.size() >= numInstances) {
                                                    return;
                                                }
                                            }
                                        } else {
                                            return;
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            SystemUtils.LOG.severe("Error retrieving features from polygons " + e.getMessage());
                        }
                    }
                };

                threadManager.add(worker);
                status.worked(1);
            }

            threadManager.finish();

            for (int i = 0; i < tileRectangles.length; i++) {

                Band band = maskProduct.getBand("tmpVirtualBand_" + i);
                if (band != null) {
                    maskProduct.removeBand(band);
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(params.classifierType + " getTrainingData from polygons ", e);
        } finally {
            status.done();
        }

        final Map<Double, String> labelMap = new HashMap<>();
        for (Integer classIndex : classLabelMap.keySet()) {
            labelMap.put((double) classIndex, classLabelMap.get(classIndex));
        }
        return new LabeledInstances(labelMap, instanceList);
    }

    private LabeledInstances getInstanceListFromMaskProduct(final Operator operator, final int numInstances,
                                                            final FeatureInfo[] featureInfos) throws OperatorException {

        final Dimension tileSize = new Dimension(20, 10);
        final Rectangle[] tileRectangles = OperatorUtils.getAllTileRectangles(maskProduct, tileSize, 0);
        final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
        status.beginTask("Getting training data... ", tileRectangles.length);

        final List<Instance> instanceList = new ArrayList<>();

        try {
            final ThreadManager threadManager = new ThreadManager();

            for (final Rectangle rectangle : tileRectangles) {
                final Thread worker = new Thread() {

                    final int xMin = rectangle.x;
                    final int xMax = rectangle.x + rectangle.width;
                    final int yMin = rectangle.y;
                    final int yMax = rectangle.y + rectangle.height;

                    final Tile maskTile = operator.getSourceTile(trainingSetMaskBand, rectangle);
                    final Tile[] featureTiles = new Tile[featureInfos.length];

                    @Override
                    public void run() {
                        int i = 0;
                        for (FeatureInfo featureInfo : featureInfos) {
                            featureTiles[i++] = operator.getSourceTile(featureInfo.featureBand, rectangle);
                        }

                        getData(xMin, xMax, yMin, yMax, maskTile, featureTiles,
                                numInstances, maskNoDataValue, instanceList);
                    }
                };

                threadManager.add(worker);

                status.worked(1);
            }

            threadManager.finish();

            //SystemUtils.LOG.info("instanceList.size = " + instanceList.size());
            /*for (int i = 0; i < 3; i++) {
                dumpInstance(instanceList.get(i));
            }*/

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(params.classifierType + " getTrainingData ", e);
        } finally {
            status.done();
        }

        final Map<Double, String> labelMap = new HashMap<>();
        labelMap.put(0.0, trainingSetMaskBand.getName());
        return new LabeledInstances(labelMap, instanceList);
    }

    private static class LabeledInstances {
        final Map<Double, String> labelMap;
        final List<Instance> instanceList;

        LabeledInstances(final Map<Double, String> labelMap, List<Instance> instancesList) {
            this.labelMap = labelMap;
            this.instanceList = instancesList;
        }
    }

    private LabeledInstances getLabeledInstances(final Operator operator, final int numInstances,
                                                 final FeatureInfo[] featureInfos) throws OperatorException {
        if (params.trainOnRaster) {
            return getInstanceListFromMaskProduct(operator, numInstances, featureInfos);
        } else {
            return getInstanceListFromPolygons(operator, numInstances, featureInfos);
        }
    }

    private double quantize(double val) {
        if (!params.doClassValQuantization) {
            return val;
        }
        return VectorUtils.quantize(val, params.minClassValue, maxClassValue, params.classValStepSize);
    }

    private void getData(final int xMin, final int xMax, final int yMin, final int yMax,
                         final Tile maskTile,
                         final Tile[] featureTiles, final int maxSamples,
                         final double maskNoDataValue,
                         List<Instance> instanceList) {

        for (int y = yMin; y < yMax; ++y) {
            for (int x = xMin; x < xMax; ++x) {
                final double maskValue = maskTile.getDataBuffer().getElemDoubleAt(maskTile.getDataBufferIndex(x, y));
                if (Double.isNaN(maskValue) || maskValue == maskNoDataValue) {
                    continue;
                }
                final double[] features = getFeatures(featureTiles, featureInfoList, x, y);
                if (features == null) {
                    continue;
                }

                final Instance instance = new DenseInstance(features);
                instance.setClassValue(quantize(maskValue));
                synchronized (instanceList) {
                    if (instanceList.size() < maxSamples) {
                        instanceList.add(instance);
                        if (instanceList.size() >= maxSamples) {
                            return;
                        }
                    } else {
                        return;
                    }
                }
            }
        }
    }

    private static double[] getFeatures(final Tile[] featureTiles, FeatureInfo[] featureInfos, final int x, final int y) {
        final double[] features = new double[featureTiles.length];
        for (int i = 0; i < featureTiles.length; ++i) {
            double val = featureTiles[i].getDataBuffer().getElemDoubleAt(featureTiles[i].getDataBufferIndex(x, y));
            if (val == featureInfos[i].featureNoDataValue) {
                return null;
            }

            // scale the value to [0, 1]
            val = (val - featureInfos[i].featureOffsetValue) * featureInfos[i].featureScaleValue;
            if (val > 1.0) {
                val = 1.0;
            } else if (val < 0.0) {
                val = 0.0;
            }
            features[i] = val;
        }
        return features;
    }

    private String haveDuplicates(final String[] featureNames) {
        for (int i = 0; i < featureNames.length; i++) {
            for (int j = i+1; j < featureNames.length; j++) {
                if (featureNames[i].equals(featureNames[j])) {
                    return featureNames[i];
                }
            }
        }
        return null;
    }

    private void saveClassifier(final Dataset trainDataset) throws IOException {

        //System.out.println("BaseClassifier.saveClassifier");

        // First save the classifier and other info required by the software to use it.

        Object objectToSave = getObjectToSave(trainDataset);

        final String className = trainingSetMaskBand == null ? "???" : StackUtils.getBandNameWithoutDate(trainingSetMaskBand.getName());
        //SystemUtils.LOG.info("*** Save " + classifierType + " classifier (filename = " + savedClassifierName
        //                             + ") to predict " + className);

        final Object[] sortedObjects = ClassifierAttributeEvaluation.getSortedObjects(trainDataset.classes());
        final double[] sortedClassValues = new double[sortedObjects.length];
        for (int i = 0; i < sortedObjects.length; i++) {
            sortedClassValues[i] = (double) sortedObjects[i];
            //SystemUtils.LOG.info("********* sortedClassValues[" + i + "] = " + sortedClassValues[i]);
        }

        // Order is IMPORTANT
        final String[] featureNames = new String[featureInfoList.length];
        final double[] featureMinValues = new double[featureInfoList.length];
        final double[] featureMaxValues = new double[featureInfoList.length];
        for (int i = 0; i < featureNames.length; i++) {
            final Band featureBand = featureInfoList[i].featureBand;
            featureNames[i] = featureBand.getName();
            if (featureNames[i].contains(StackUtils.MST) || featureNames[i].contains(StackUtils.SLV))
                featureNames[i] = StackUtils.getBandNameWithoutDate(featureNames[i]);
            featureMinValues[i] = featureBand.getStx().getMinimum();
            featureMaxValues[i] = featureBand.getStx().getMaximum();
        }
        // If the feature bands are in a co-registered product with virtual bands like:
        // - Intensity_IW3_VV_mst_06Jun2016
        // - Intensity_IW3_VV_slv1_15Nov2015
        // - Intensity_IW3_VV_slv2_27Nov2015
        // - Intensity_IW3_VV_slv3_09Dec2015
        // - Intensity_IW3_VV_slv4_21Dec2015
        // Then featureNames after stripping the date will all be "Intensity_IW3_VV". This will not work because
        // when we load the classifier, we will look for the five feature bands and they all have the same name.
        // Does it even make sense to use the same feature, namely, intensity, five times?
        final String sameName = haveDuplicates(featureNames);
        if (sameName != null) {
            throw new OperatorException("Classifier cannot be saved: some feature bands have same name - " + sameName);
            //SystemUtils.LOG.warning("BaseClassifier.saveClassifier: saved classifier will not work because some feature bands are saved with same name - " + sameName);
        }

        // "sortedClasses" and "trainingVectors" should be aligned, see Initialize().
        // params.trainingVectors[i] corresponds to class value "i".
        // The class values are 0, 1, ... , params.trainingVectors.length
        final String classUnit = labelBand.getUnit();
        ClassifierDescriptor classifierDescriptor =
                new ClassifierDescriptor(params.classifierType, params.savedClassifierName,
                                         objectToSave, sortedClassValues,
                                         className, classUnit, featureNames, featureMinValues, featureMaxValues,
                                         params.doClassValQuantization, params.minClassValue,
                                         params.classValStepSize, params.classLevels, params.trainingVectors);

        final Path filePath = getClassifierFilePath(false);

        FileOutputStream fos;
        ObjectOutputStream out;
        try {
            fos = new FileOutputStream(filePath.toString());
            out = new ObjectOutputStream(fos);
            out.writeObject(classifierDescriptor);
            out.close();
        } catch (Exception ex) {
            throw new OperatorException("Failed to save classifier " + ex.getMessage());
        }

        // Now save in an xml file what the user needs to know to prepare the source products

        ClassifierUserInfo classifierUserInfo =
                new ClassifierUserInfo(params.savedClassifierName, params.classifierType,
                                       className, params.numTrainSamples, sortedClassValues, featureInfoList.length,
                                       params.trainingBands, params.trainingVectors, featureNames,
                                       (params.trainOnRaster && params.doClassValQuantization ? params.minClassValue : 0.0),
                                       (params.trainOnRaster &&  params.doClassValQuantization ? params.classValStepSize : 0.0),
                                       (params.trainOnRaster && params.doClassValQuantization ? params.classLevels : -1),
                                       (params.trainOnRaster && params.doClassValQuantization ? maxClassValue : 0.0),
                                        labelBand.getDataType(), labelBand.getUnit());

        Object xmlToSave = getXMLInfoToSave(classifierUserInfo);

        final XStream xstream = new XStream();
        xstream.processAnnotations(xmlToSave.getClass());
        final String xmlContent = xstream.toXML(xmlToSave);
        final File infoFile = filePath.getParent().resolve(params.savedClassifierName + CLASSIFIER_USER_INFO_FILE_EXTENSION).toFile();
        FileWriter fileWriter = new FileWriter(infoFile);
        fileWriter.write(xmlContent);
        fileWriter.flush();
        fileWriter.close();
    }

    private static void dumpInstance(Instance instance) {
        SystemUtils.LOG.info(" Class value = " + instance.classValue());
        for (int i = 0; i < instance.noAttributes(); i++) {
            SystemUtils.LOG.info(" attr " + i + ": " + instance.value(i));
        }
    }

    public static class FeatureInfo implements Comparable<FeatureInfo> {
        final Band featureBand;
        double featureNoDataValue;
        final double featureOffsetValue;
        final double featureScaleValue;
        private final int id;

        FeatureInfo(Band featureBand, int id) {
            this.featureBand = featureBand;
            this.id = id;

            featureNoDataValue = DOUBLE_NO_DATA_VALUE;
            if (featureBand.isNoDataValueSet()) {
                featureNoDataValue = featureBand.getNoDataValue();
            }
            featureOffsetValue = featureBand.getStx().getMinimum();
            featureScaleValue = 1.0 / (featureBand.getStx().getMaximum() - featureOffsetValue);
        }

        FeatureInfo(Band featureBand, int id, double featureNoDataValue,
                           double featureOffsetValue, double featureScaleValue) {
            this.featureBand = featureBand;
            this.id = id;
            this.featureNoDataValue = featureNoDataValue;
            this.featureOffsetValue = featureOffsetValue;
            this.featureScaleValue = featureScaleValue;
        }

        public int compareTo(FeatureInfo o) {
            return Integer.compare(id, o.id);
        }
    }

    public static class ClassifierUserInfo {
        private String classifierFilename;
        private String classifierType;
        private String className; // E.g., biomass or landcover classes
        private int numSamples;
        private double[] sortedClasses;
        private int numFeatures;
        private String[] trainingBands; // can be null
        private String[] trainingVectors; // can be null
        private String[] featureNames;
        private int datatype = ProductData.TYPE_INT16;
        private String unit = "discrete classes";

        // If quantization is not done, then classLevels is set to -1
        private double minClassValue;
        private double classValStepSize;
        private int classLevels;
        private double maxClassValue;

        ClassifierUserInfo() {

        }

        ClassifierUserInfo(final String classifierFilename, final String classifierType,
                           final String className, final int numSamples, final double[] sortedClasses,
                           final int numFeatures,
                           final String[] trainingBands,
                           final String[] trainingVectors,
                           final String[] featureNames,
                           final double minClassValue, final double classValStepSize, final int classLevels,
                           final double maxClassValue,
                           final int datatype,
                           final String unit) {
            this.classifierFilename = classifierFilename;
            this.classifierType = classifierType;
            this.className = className;
            this.numSamples = numSamples;
            this.sortedClasses = sortedClasses;
            this.numFeatures = numFeatures;
            this.trainingBands = trainingBands;
            this.trainingVectors = trainingVectors;
            this.featureNames = featureNames;
            this.minClassValue = minClassValue;
            this.classValStepSize = classValStepSize;
            this.classLevels = classLevels;
            this.maxClassValue = maxClassValue;
            this.datatype = datatype;
            this.unit = unit;
        }
    }
}
