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
package org.esa.snap.classification.gpf.randomForest;

import org.esa.snap.classification.gpf.BaseClassifier;
import org.esa.snap.classification.gpf.ClassifierDescriptor;
import org.esa.snap.classification.gpf.SupervisedClassifier;
import be.abeel.util.MTRandom;
import net.sf.javaml.classification.Classifier;
import net.sf.javaml.classification.tree.RandomForest;

/**
 * Random Forest
 */
public class RandomForestClassifier extends BaseClassifier implements SupervisedClassifier {

    private final int treeCount;

    public RandomForestClassifier(final ClassifierParams params, final int treeCount) {
        super(params);
        this.treeCount = treeCount;
    }

    private static int getNumSplitFeatures(final int numberOfFeatures) {
        return (int) Math.sqrt((double) numberOfFeatures);
    }

    public Classifier createMLClassifier(final FeatureInfo[] featureInfos) {
        final int numSplitFeatures = getNumSplitFeatures(featureInfos.length);
        //SystemUtils.LOG.info("********* #trees = " + treeCount + " #split features = " + numSplitFeatures);
        return new RandomForest(treeCount, false, numSplitFeatures, new MTRandom());
    }

    public Classifier retrieveMLClassifier(final ClassifierDescriptor classifierDescriptor) {
        return (RandomForest) classifierDescriptor.getObject();
    }

    public static class UserInfo {
        private ClassifierUserInfo commonInfo;
        private int numTrees;

        public UserInfo(final ClassifierUserInfo commonInfo, final int numTrees) {
            this.commonInfo = commonInfo;
            this.numTrees = numTrees;
        }
    }

    @Override
    protected Object getXMLInfoToSave(final ClassifierUserInfo commonInfo) {
        final UserInfo userInfo = new UserInfo(commonInfo, treeCount);
        return userInfo;
    }
}
