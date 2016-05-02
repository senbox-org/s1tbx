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
package org.esa.snap.classification.gpf.maximumlikelihood;

import org.esa.snap.classification.gpf.BaseClassifier;
import org.esa.snap.classification.gpf.ClassifierDescriptor;
import org.esa.snap.classification.gpf.SupervisedClassifier;
import net.sf.javaml.classification.Classifier;

/**
 * Maximum Likelihood
 */
public class MaximumLikelihoodClassifier extends BaseClassifier implements SupervisedClassifier {

    public MaximumLikelihoodClassifier(final ClassifierParams params) {
        super(params);
    }

    public Classifier createMLClassifier(final FeatureInfo[] featureInfos) {
        return new MaximumLikelihood();
    }

    public Classifier retrieveMLClassifier(final ClassifierDescriptor classifierDescriptor) {
        return (MaximumLikelihood) classifierDescriptor.getObject();
    }
}
