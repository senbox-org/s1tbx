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

import com.bc.ceres.core.ProgressMonitor;
import net.sf.javaml.classification.Classifier;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;

import java.awt.*;
import java.io.IOException;
import java.util.Map;

/**
 * Classifier interface
 */
public interface SupervisedClassifier {

    void initialize() throws OperatorException, IOException;

    Product createTargetProduct();

    void computeTileStack(Operator operator, Map<Band, Tile> targetTileMap, Rectangle targetRectangle,
                          ProgressMonitor pm) throws OperatorException, IOException;

    Classifier createMLClassifier(final BaseClassifier.FeatureInfo[] featureInfos);

    Classifier getMLClassifier();

    Classifier retrieveMLClassifier(final ClassifierDescriptor classifierDescriptor);

    String getClassifierType();

    String getProductSuffix();

    String getClassifierName();
}
