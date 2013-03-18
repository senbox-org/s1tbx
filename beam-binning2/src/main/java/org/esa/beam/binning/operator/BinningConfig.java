/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.operator;

/*
* Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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


import com.bc.ceres.binding.BindingException;
import org.esa.beam.binning.Aggregator;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.AggregatorDescriptorRegistry;
import org.esa.beam.binning.BinManager;
import org.esa.beam.binning.BinningContext;
import org.esa.beam.binning.CompositingType;
import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.binning.VariableContext;
import org.esa.beam.binning.support.BinningContextImpl;
import org.esa.beam.binning.support.SEAGrid;
import org.esa.beam.binning.support.VariableContextImpl;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.ParameterBlockConverter;

import java.lang.reflect.Constructor;

/**
 * Configuration for the binning.
 *
 * @author Norman Fomferra
 * @author Marco Zühlke
 */
@SuppressWarnings({"UnusedDeclaration"})
public class BinningConfig {

    /**
     * Specifies the planetary grid used for the binning. It must be the fully qualified
     * name of a class implementing the {@link PlanetaryGrid} interface.
     * <p/>
     * If the {@code numRows} is given (meaning it is greater than 0), the planetary grid class
     * must have a public one-argument constructor which takes the {@code numRows} as input.
     * Otherwise a public no-arg constructor is expected.
     */
    // do not expose planetary grid as parameter (GUI and CLI)
//    @Parameter(defaultValue = "org.esa.beam.binning.support.SEAGrid")
    private String planetaryGrid = "org.esa.beam.binning.support.SEAGrid";

    /**
     * Number of rows in the planetary grid.
     */
    @Parameter
    private int numRows;

    /**
     * The compositing type used for the binning process.
     */
    // do not expose compositing type as parameter (GUI and CLI)
//    @Parameter(defaultValue = "BINNING", valueSet = {"BINNING", "MOSAICKING"})

    private CompositingType compositingType = CompositingType.BINNING;

    /**
     * The number of pixels used for super-sampling an input pixel into sub-pixel.
     */
    @Parameter
    private Integer superSampling;

    /**
     * The band maths expression used to filter input pixels.
     */
    @Parameter
    private String maskExpr;

    /**
     * List of variables. A variable will generate a {@link org.esa.beam.framework.datamodel.VirtualBand VirtualBand}
     * in the input data product to be binned, so that it can be used for binning.
     */
    @Parameter(alias = "variables", itemAlias = "variable")
    private VariableConfig[] variableConfigs;

    /**
     * List of aggregators. Aggregators generate the bands in the binned output products.
     */
    @Parameter(alias = "aggregators", domConverter = AggregatorConfigDomConverter.class)
    private AggregatorConfig[] aggregatorConfigs;

    public String getPlanetaryGrid() {
        return planetaryGrid;
    }

    public void setPlanetaryGrid(String planetaryGrid) {
        this.planetaryGrid = planetaryGrid;
    }

    public int getNumRows() {
        return numRows;
    }

    public void setNumRows(int numRows) {
        this.numRows = numRows;
    }

    public String getMaskExpr() {
        return maskExpr;
    }

    public void setMaskExpr(String maskExpr) {
        this.maskExpr = maskExpr;
    }

    public Integer getSuperSampling() {
        return superSampling;
    }

    public CompositingType getCompositingType() {
        return compositingType;
    }

    public void setCompositingType(CompositingType type) {
        this.compositingType = type;
    }

    public void setSuperSampling(Integer superSampling) {
        this.superSampling = superSampling;
    }

    public VariableConfig[] getVariableConfigs() {
        return variableConfigs;
    }

    public void setVariableConfigs(VariableConfig... variableConfigs) {
        this.variableConfigs = variableConfigs;
    }

    public AggregatorConfig[] getAggregatorConfigs() {
        return aggregatorConfigs;
    }

    public void setAggregatorConfigs(AggregatorConfig... aggregatorConfigs) {
        this.aggregatorConfigs = aggregatorConfigs;
    }

    public static BinningConfig fromXml(String xml) throws BindingException {
        return new ParameterBlockConverter().convertXmlToObject(xml, new BinningConfig());
    }

    public String toXml() {
        try {
            return new ParameterBlockConverter().convertObjectToXml(this);
        } catch (BindingException e) {
            throw new RuntimeException(e);
        }
    }

    public BinningContext createBinningContext() {
        VariableContext variableContext = createVariableContext();
        return new BinningContextImpl(createPlanetaryGrid(),
                                      createBinManager(variableContext),
                                      compositingType, getSuperSampling() != null ? getSuperSampling() : 1);
    }

    /**
     * Creates the planetary grid used for the binning.
     *
     * @return A newly created planetary grid instance used for the binning.
     *
     * @throws IllegalArgumentException if either the {@code numRows} parameter is less or equal zero or
     *                                  the {@code planetaryGrid} parameter denotes a class that couldn't be instantiated.
     */
    public PlanetaryGrid createPlanetaryGrid() {
        if (planetaryGrid == null) {
            if (numRows > 0) {
                return new SEAGrid(numRows);
            } else {
                return new SEAGrid();
            }
        } else {
            try {
                if (numRows > 0) {
                    Constructor<?> constructor = Class.forName(planetaryGrid).getConstructor(Integer.TYPE);
                    return (PlanetaryGrid) constructor.newInstance(numRows);
                } else {
                    return (PlanetaryGrid) Class.forName(planetaryGrid).newInstance();
                }
            } catch (Exception e) {
                throw new IllegalArgumentException(planetaryGrid, e);
            }
        }
    }

    private BinManager createBinManager(VariableContext variableContext) {
        Aggregator[] aggregators = createAggregators(variableContext);
        return createBinManager(variableContext, aggregators);
    }

    public Aggregator[] createAggregators(VariableContext variableContext) {
        Aggregator[] aggregators = new Aggregator[aggregatorConfigs.length];
        for (int i = 0; i < aggregators.length; i++) {
            AggregatorConfig aggregatorConfig = aggregatorConfigs[i];
            AggregatorDescriptor descriptor = AggregatorDescriptorRegistry.getInstance().getAggregatorDescriptor(
                    aggregatorConfig.getAggregatorName());
            if (descriptor != null) {
                aggregators[i] = descriptor.createAggregator(variableContext, aggregatorConfig);
            } else {
                throw new IllegalArgumentException("Unknown aggregator type: " + aggregatorConfig.getAggregatorName());
            }
        }
        return aggregators;
    }

    protected BinManager createBinManager(VariableContext variableContext, Aggregator[] aggregators) {
        return new BinManager(variableContext, aggregators);
    }

    public VariableContext createVariableContext() {
        VariableContextImpl variableContext = new VariableContextImpl();
        if (maskExpr == null) {
            maskExpr = "";
        }
        variableContext.setMaskExpr(maskExpr);

        // define declared variables
        //
        if (variableConfigs != null) {
            for (VariableConfig variableConfig : variableConfigs) {
                variableContext.defineVariable(variableConfig.getName(), variableConfig.getExpr());
            }
        }

        // define variables of all aggregators
        //
        if (aggregatorConfigs != null) {
            for (AggregatorConfig aggregatorConfig : aggregatorConfigs) {
                String[] varNames = aggregatorConfig.getVarNames();
                for (String varName : varNames) {
                    variableContext.defineVariable(varName);
                }
            }
        }

        return variableContext;
    }

}
