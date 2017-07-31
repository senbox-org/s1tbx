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
package org.esa.snap.classification.gpf.minimumdistance;

import org.esa.snap.classification.gpf.BaseClassifier;
import org.esa.snap.classification.gpf.SupervisedClassifier;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;

import java.awt.*;
import java.util.Map;

@OperatorMetadata(alias = "Minimum-Distance-Classifier",
        category = "Raster/Classification/Supervised Classification",
        authors = "Cecilia Wong, Luis Veci",
        copyright = "Copyright (C) 2016 by Array Systems Computing Inc.",
        description = "Minimum Distance classifier")
public class MinimumDistanceClassifierOp extends Operator {

    public final static String CLASSIFIER_TYPE = "MinimumDistance";
    private final static String PRODUCT_SUFFIX = "_MDClass";

    @SourceProducts
    private Product[] sourceProducts;

    @TargetProduct
    private Product targetProduct;

    // Size of training dataset
    @Parameter(description = "The number of training samples", interval = "(1,*]", defaultValue = "5000",
            label = "Number of training samples")
    private int numTrainSamples = 5000;

    @Parameter(description = "The saved classifier name", label = "Classifier name")
    private String savedClassifierName = null;

    @Parameter(defaultValue = "false", description = "Choose to save or load classifier")
    private Boolean doLoadClassifier = false;

    @Parameter(defaultValue = "true", description = "Quantization for raster traiing")
    private Boolean doClassValQuantization = true;

    @Parameter(defaultValue = "0.0", description = "Quantization min class value for raster traiing")
    private Double minClassValue = 0.0;

    @Parameter(defaultValue = "5.0", description = "Quantization step size for raster traiing")
    private Double classValStepSize = 5.0;

    @Parameter(defaultValue = "101", description = "Quantization class levels for raster traiing")
    private int classLevels = 101;

    @Parameter(description = "Raster bands to train on", label = "Raster Classes")
    private String[] trainingBands;

    @Parameter(defaultValue = "true", description = "Train on raster or vector data")
    private Boolean trainOnRaster;

    @Parameter(description = "Vectors to train on", label = "Vector Classes")
    private String[] trainingVectors;

    @Parameter(description = "Names of bands to be used as features", label = "Features")
    private String[] featureBands;

    @Parameter(description = "'VectorNodeName' or specific Attribute name", label = "Label source")
    private String labelSource;

    @Parameter(description = "Evaluate classifier and features", label = "Evaluate Classifier")
    private Boolean evaluateClassifier;

    @Parameter(description = "Evaluate the power set of features", label = "Evaluate Feature Power Set", defaultValue = "false")
    private Boolean evaluateFeaturePowerSet;

    @Parameter(description = "Minimum size of the power set of features", label = "Min PowerSet Size", defaultValue = "2")
    private Integer minPowerSetSize;

    @Parameter(description = "Maximum size of the power set of features", label = "Max PowerSet Size", defaultValue = "7")
    private Integer maxPowerSetSize;

    private SupervisedClassifier classifier;

    @Override
    public void initialize() throws OperatorException {
        try {
            // backwards compatibility in case parameter missing from graph
            if(minPowerSetSize == null) {
                minPowerSetSize = 2;
            }
            if(maxPowerSetSize == null) {
                maxPowerSetSize = 7;
            }

            classifier = new MinimumDistanceClassifier(
                    new BaseClassifier.ClassifierParams(CLASSIFIER_TYPE, PRODUCT_SUFFIX,
                                                        sourceProducts, numTrainSamples,
                                                        minClassValue, classValStepSize, classLevels,
                                                        savedClassifierName, doLoadClassifier,
                                                        doClassValQuantization, trainOnRaster,
                                                        trainingBands, trainingVectors, featureBands, labelSource,
                                                        evaluateClassifier, evaluateFeaturePowerSet,
                                                        minPowerSetSize, maxPowerSetSize));

            classifier.initialize();

            targetProduct = classifier.createTargetProduct();
        } catch (Exception e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {
        try {
            classifier.computeTileStack(this, targetTileMap, targetRectangle, pm);
        } catch (Exception e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(MinimumDistanceClassifierOp.class);
        }
    }
}
