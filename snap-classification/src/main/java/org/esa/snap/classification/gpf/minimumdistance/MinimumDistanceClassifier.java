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
import org.esa.snap.classification.gpf.ClassifierDescriptor;
import org.esa.snap.classification.gpf.SupervisedClassifier;
import net.sf.javaml.classification.Classifier;
import net.sf.javaml.core.Instance;
import net.sf.javaml.distance.EuclideanDistance;

/**
 * Minimum Distance
 */
public class MinimumDistanceClassifier extends BaseClassifier implements SupervisedClassifier {

    public MinimumDistanceClassifier(final ClassifierParams params) {
        super(params);
    }

    public Classifier createMLClassifier(final FeatureInfo[] featureInfos) {
        return new MinDistClassifier(new EuclideanDistance());
    }

    public Classifier retrieveMLClassifier(final ClassifierDescriptor classifierDescriptor) {
        return (MinDistClassifier) classifierDescriptor.getObject();
    }

    // The class distribution from the classifier returns 1, but we will make it explicit and override it
    @Override
    protected double getConfidence(final Instance instance, final Object classVal) {
        return 1.0;
    }
}
