/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.binning.cellprocessor;

import org.esa.snap.binning.CellProcessor;
import org.esa.snap.binning.CellProcessorConfig;
import org.esa.snap.binning.CellProcessorDescriptor;
import org.esa.snap.binning.VariableContext;
import org.esa.snap.binning.Vector;
import org.esa.snap.binning.WritableVector;
import org.esa.snap.core.gpf.annotations.Parameter;

/**
 * A cell processor that select a number of features from the available ones.
 */
public class FeatureSelection extends CellProcessor {

    private final int[] varIndexes;

    public FeatureSelection(VariableContext varCtx, String... featureNames) {
        super(getOutputFeatureNames(featureNames));
        String[] inputFeatureNames = getInputFeatureNames(featureNames);
        varIndexes = new int[inputFeatureNames.length];
        for (int i = 0; i < inputFeatureNames.length; i++) {
            String name = inputFeatureNames[i];
            int variableIndex = varCtx.getVariableIndex(name);
            if (variableIndex == -1) {
                throw new IllegalArgumentException("unknown feature name: " + name);
            }
            varIndexes[i] = variableIndex;
        }
    }

    private static String[] getInputFeatureNames(String[] featureNames) {
        return getFeatureNames(featureNames, 1);
    }

    private static String[] getOutputFeatureNames(String[] featureNames) {
        return getFeatureNames(featureNames, 0);
    }

    private static String[] getFeatureNames(String[] featureNames, int index) {
        String[] result = new String[featureNames.length];
        for (int i = 0; i < featureNames.length; i++) {
            String featureName = featureNames[i];
            if (featureName.contains("=")) {
                featureName = featureName.split("=")[index];
            }
            result[i] = featureName.trim();
        }
        return result;
    }

    @Override
    public void compute(Vector inputVector, WritableVector outputVector) {
        for (int i = 0; i < varIndexes.length; i++) {
            outputVector.set(i, inputVector.get(varIndexes[i]));
        }
    }

    public static class Config extends CellProcessorConfig {
        @Parameter(notEmpty = true, notNull = true)
        private String[] varNames;

        public Config(String...varNames) {
            super(Descriptor.NAME);
            this.varNames = varNames;
        }

    }

    public static class Descriptor implements CellProcessorDescriptor {

        public static final String NAME = "Selection";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public CellProcessor createCellProcessor(VariableContext varCtx, CellProcessorConfig cellProcessorConfig) {
            Config config = (Config) cellProcessorConfig;
            return new FeatureSelection(varCtx, config.varNames);
        }

        @Override
        public CellProcessorConfig createConfig() {
            return new Config();
        }
    }
}
