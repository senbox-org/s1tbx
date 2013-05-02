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

package org.esa.beam.binning.postprocessor;

import com.bc.ceres.binding.PropertySet;
import org.esa.beam.binning.PostProcessor;
import org.esa.beam.binning.PostProcessorConfig;
import org.esa.beam.binning.PostProcessorDescriptor;
import org.esa.beam.binning.VariableContext;
import org.esa.beam.binning.Vector;
import org.esa.beam.binning.WritableVector;
import org.esa.beam.framework.gpf.annotations.Parameter;

/**
 * A cell processor that select a number of features from the available ones.
 */
public class PPSelection extends PostProcessor{

    private final int[] varIndexes;

    public PPSelection(VariableContext varCtx, String...outputFeatureNames) {
        super(outputFeatureNames);
        varIndexes = new int[outputFeatureNames.length];
        for (int i = 0; i < outputFeatureNames.length; i++) {
            String name = outputFeatureNames[i];
            int variableIndex = varCtx.getVariableIndex(name);
            if (variableIndex == -1) {
                throw new IllegalArgumentException("unknown feature name: " + name);
            }
            varIndexes[i] = variableIndex;
        }
    }

    @Override
    public void compute(Vector outputVector, WritableVector postVector) {
        for (int i = 0; i < varIndexes.length; i++) {
            postVector.set(i, outputVector.get(varIndexes[i]));
        }
    }

    public static class Config extends PostProcessorConfig {
        @Parameter(notEmpty = true, notNull = true)
        private String[] varNames;

        public Config(String...varNames) {
            super(Descriptor.NAME);
            this.varNames = varNames;
        }

    }

    public static class Descriptor implements PostProcessorDescriptor {

        public static final String NAME = "Selection";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public PostProcessor createPostProcessor(VariableContext varCtx, PostProcessorConfig postProcessorConfig) {
            PropertySet propertySet = postProcessorConfig.asPropertySet();
            return new PPSelection(varCtx, (String[]) propertySet.getValue("varNames"));
        }

        @Override
        public PostProcessorConfig createConfig() {
            return new Config();
        }
    }
}
