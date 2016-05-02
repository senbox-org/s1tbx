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
package org.esa.snap.classification.gpf.kdtknn;

import org.esa.snap.classification.gpf.BaseClassifier;
import org.esa.snap.classification.gpf.ClassifierDescriptor;
import org.esa.snap.classification.gpf.SupervisedClassifier;
import net.sf.javaml.classification.Classifier;
import net.sf.javaml.classification.KDtreeKNN;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;

/**
 * K-Nearest Neighbour sped up with a KD Tree
 */
public class KDTreeKNNClassifier extends BaseClassifier implements SupervisedClassifier {

    private int numNeighbours = 5;

    public KDTreeKNNClassifier(final ClassifierParams params, final int numNeighbours) {
        super(params);
        this.numNeighbours = numNeighbours;
    }

    protected Object getObjectToSave(final Dataset trainDataset) {
        return new KDTreeKNNClassifierParams(numNeighbours, trainDataset);
    }

    public Classifier createMLClassifier(final FeatureInfo[] featureInfos) {
        //SystemUtils.LOG.info("********* #neighbours = " + numNeighbours);
        return new KDtreeKNN(numNeighbours);
    }

    public Classifier retrieveMLClassifier(final ClassifierDescriptor classifierDescriptor) {
        final Object object = classifierDescriptor.getObject();
        final KDTreeKNNClassifierParams params = (KDTreeKNNClassifierParams) object;
        final KDtreeKNN classifier = new KDtreeKNN(params.getNumNeighbours());
        classifier.buildClassifier(params.getTrainDataset());

        return classifier;
    }

    // The class distribution from the classifier is not suitable as a confidence, we will override it
    @Override
    protected double getConfidence(final Instance instance, final Object classVal) {
        return 1.0;
    }

    public static class UserInfo {
        private ClassifierUserInfo commonInfo;
        private int numNeighbours;

        public UserInfo(final ClassifierUserInfo commonInfo, final int numNeighbours) {
            this.commonInfo = commonInfo;
            this.numNeighbours = numNeighbours;
        }
    }

    @Override
    protected Object getXMLInfoToSave(final ClassifierUserInfo commonInfo) {
        final UserInfo userInfo = new UserInfo(commonInfo, numNeighbours);
        return userInfo;
    }
}
