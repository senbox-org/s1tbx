/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.binning.operator;

import com.bc.ceres.binding.BindingException;
import com.vividsolutions.jts.geom.Geometry;
import org.esa.snap.binning.Aggregator;
import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.binning.AggregatorDescriptor;
import org.esa.snap.binning.BinManager;
import org.esa.snap.binning.BinningContext;
import org.esa.snap.binning.CellProcessorConfig;
import org.esa.snap.binning.CompositingType;
import org.esa.snap.binning.DataPeriod;
import org.esa.snap.binning.PlanetaryGrid;
import org.esa.snap.binning.TemporalDataPeriod;
import org.esa.snap.binning.TypedDescriptorsRegistry;
import org.esa.snap.binning.VariableContext;
import org.esa.snap.binning.support.BinTracer;
import org.esa.snap.binning.support.BinningContextImpl;
import org.esa.snap.binning.support.SEAGrid;
import org.esa.snap.binning.support.SpatialDataPeriod;
import org.esa.snap.binning.support.VariableContextImpl;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.ParameterBlockConverter;
import org.esa.snap.core.util.converters.JtsGeometryConverter;

import java.lang.reflect.Constructor;

/**
 * Configuration for the binning.
 *
 * @author Norman Fomferra
 * @author Marco Zühlke
 */
@SuppressWarnings({"UnusedDeclaration"})
public class BinningConfig {

    private static final int MAX_DISTANCE_ON_EARTH_DEFAULT = -1;
    private static final int SUPER_SAMPLING_DEFAULT = 1;
    
    /**
     * Specifies the planetary grid used for the binning. It must be the fully qualified
     * name of a class implementing the {@link PlanetaryGrid} interface.
     * <p>
     * If the {@code numRows} is given (meaning it is greater than 0), the planetary grid class
     * must have a public one-argument constructor which takes the {@code numRows} as input.
     * Otherwise a public no-arg constructor is expected.
     */
    // do not expose planetary grid as parameter (GUI and CLI)
//    @Parameter(defaultValue = "org.esa.snap.binning.support.SEAGrid")
    private String planetaryGrid = "org.esa.snap.binning.support.SEAGrid";

    /**
     * Number of rows in the planetary grid.
     */
    @Parameter(description = "Number of rows in the (global) planetary grid. Must be even.", defaultValue = "2160")
    private int numRows;

    /**
     * The compositing type used for the binning process.
     */
    // do not expose compositing type as parameter (GUI and CLI)
//    @Parameter(defaultValue = "BINNING", valueSet = {"BINNING", "MOSAICKING"})

    private CompositingType compositingType = CompositingType.BINNING;

    /**
     * The number of pixels used for supersampling an input pixel into sub-pixel.
     */
    @Parameter(description = "The square of the number of pixels used for super-sampling an input pixel into multiple sub-pixels", defaultValue = "1")
    private Integer superSampling;


    @Parameter(description = "Skips binning of sub-pixel if distance on earth to the center of the main-pixel is larger as this value. A value <=0 disables this check", defaultValue = "-1")
    private Integer maxDistanceOnEarth;

    /**
     * The band maths expression used to filter input pixels.
     */
    @Parameter(description = "The band maths expression used to filter input pixels")
    private String maskExpr;

    /**
     * List of variables. A variable will generate a {@link VirtualBand VirtualBand}
     * in the input data product to be binned, so that it can be used for binning.
     */
    @Parameter(alias = "variables", itemAlias = "variable",
            description = "List of variables. A variable will generate a virtual band " +
                    "in each source data product, so that it can be used as input for the binning.")
    private VariableConfig[] variableConfigs;

    /**
     * List of aggregators. Aggregators generate the bands in the binned output products.
     */
    @Parameter(alias = "aggregators", domConverter = AggregatorConfigDomConverter.class,
            description = "List of aggregators. Aggregators generate the bands in the binned output products")
    private AggregatorConfig[] aggregatorConfigs;

    @Parameter(alias = "postProcessor", domConverter = CellProcessorConfigDomConverter.class)
    private CellProcessorConfig postProcessorConfig;


    @Parameter(description = "The time in hours of a day (0 to 24) at which a given sensor has a minimum number of " +
            "observations at the date line (the 180 degree meridian). Only used if parameter 'startDate' is set.")
    private Double minDataHour;

    @Parameter(description = "The type of metadata aggregation to be used. Possible values are:\n" +
            "'NAME': aggregate the name of each input product\n" +
            "'FIRST_HISTORY': aggregates all input product names and the processing history of the first product\n" +
            "'ALL_HISTORIES': aggregates all input product names and processing histories",
            valueSet = {"NAME", "FIRST_HISTORY", "ALL_HISTORIES"},
            defaultValue = "FIRST_HISTORY")
    private String metadataAggregatorName;

    @Parameter(pattern = "\\d{4}-\\d{2}-\\d{2}(\\s\\d{2}:\\d{2}:\\d{2})?",
            description = "The UTC start date of the binning period. " +
                    "The format is either 'yyyy-MM-dd HH:mm:ss' or 'yyyy-MM-dd'. If only the date part is given, the time 00:00:00 is assumed.")
    private String startDateTime;

    @Parameter(description = "Duration of the binning period in days.")
    private Double periodDuration;

    @Parameter(description = "The method that is used to decide which source pixels are used with respect to their observation time. " +
            "'NONE': ignore pixel observation time, use all source pixels. " +
            "'TIME_RANGE': use all pixels that have been acquired in the given binning period. " +
            "'SPATIOTEMPORAL_DATA_DAY': use a sensor-dependent, spatial \"data-day\" definition with the goal " +
            "to minimise the time between the first and last observation contributing to the same bin in the given binning period. " +
            "The decision, whether a source pixel contributes to a bin or not, is a function of the pixel's observation longitude and time. " +
            "Requires the parameter 'minDataHour'.",
            defaultValue = "NONE")
    private BinningOp.TimeFilterMethod timeFilterMethod;

    @Parameter
    private String outputFile;

    @Parameter(converter = JtsGeometryConverter.class,
            description = "The considered geographical region as a geometry in well-known text format (WKT).\n" +
                    "If not given, the geographical region will be computed according to the extents of the " +
                    "input products.")
    Geometry region;

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

    public Double getMinDataHour() {
        return minDataHour;
    }

    public void setMinDataHour(Double minDataHour) {
        this.minDataHour = minDataHour;
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

    public void setSuperSampling(Integer superSampling) {
        this.superSampling = superSampling;
    }

    public Integer getMaxDistanceOnEarth() {
        return maxDistanceOnEarth;
    }

    public void setMaxDistanceOnEarth(Integer maxDistanceOnEarth) {
        this.maxDistanceOnEarth = maxDistanceOnEarth;
    }

    public CompositingType getCompositingType() {
        return compositingType;
    }

    public void setCompositingType(CompositingType type) {
        this.compositingType = type;
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

    public CellProcessorConfig getPostProcessorConfig() {
        return postProcessorConfig;
    }

    public void setPostProcessorConfig(CellProcessorConfig cellProcessorConfig) {
        this.postProcessorConfig = cellProcessorConfig;
    }

    public String getMetadataAggregatorName() {
        return metadataAggregatorName;
    }

    public void setMetadataAggregatorName(String metadataAggregatorName) {
        this.metadataAggregatorName = metadataAggregatorName;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(String startDateTime) {
        this.startDateTime = startDateTime;
    }

    public Double getPeriodDuration() {
        return periodDuration;
    }

    public void setPeriodDuration(Double periodDuration) {
        this.periodDuration = periodDuration;
    }

    public BinningOp.TimeFilterMethod getTimeFilterMethod() {
        return timeFilterMethod;
    }

    public void setTimeFilterMethod(BinningOp.TimeFilterMethod timeFilterMethod) {
        this.timeFilterMethod = timeFilterMethod;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public Geometry getRegion() {
        return region;
    }

    public void setRegion(Geometry region) {
        this.region = region;
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


    public BinningContext createBinningContext(Geometry region, ProductData.UTC startDate, Double periodDuration) {
        PlanetaryGrid planetaryGridInst = createPlanetaryGrid();
        VariableContext variableContext = createVariableContext();
        Aggregator[] aggregators = createAggregators(variableContext);
        BinManager binManager = createBinManager(variableContext, aggregators);
        binManager.setBinTracer(BinTracer.create(binManager, planetaryGridInst, outputFile));
        DataPeriod dataPeriod = createDataPeriod(startDate, periodDuration, minDataHour);
        int effSuperSampling = getSuperSampling() != null ? getSuperSampling() : SUPER_SAMPLING_DEFAULT;
        int effMaxDistanceOnEarth = getMaxDistanceOnEarth() != null ? getMaxDistanceOnEarth() : MAX_DISTANCE_ON_EARTH_DEFAULT;
        return new BinningContextImpl(planetaryGridInst,
                                      binManager,
                                      compositingType,
                                      effSuperSampling,
                                      effMaxDistanceOnEarth,
                                      dataPeriod,
                                      region);
    }

    /**
     * Creates the planetary grid used for the binning.
     *
     * @return A newly created planetary grid instance used for the binning.
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

    public Aggregator[] createAggregators(VariableContext variableContext) {
        if (aggregatorConfigs == null) {
            return new Aggregator[0];
        }
        Aggregator[] aggregators = new Aggregator[aggregatorConfigs.length];
        TypedDescriptorsRegistry registry = TypedDescriptorsRegistry.getInstance();
        for (int i = 0; i < aggregators.length; i++) {
            AggregatorConfig aggregatorConfig = aggregatorConfigs[i];
            AggregatorDescriptor descriptor = registry.getDescriptor(AggregatorDescriptor.class, aggregatorConfig.getName());
            if (descriptor != null) {
                aggregators[i] = descriptor.createAggregator(variableContext, aggregatorConfig);
            } else {
                throw new IllegalArgumentException("Unknown aggregator type: " + aggregatorConfig.getName());
            }
        }
        return aggregators;
    }

    protected BinManager createBinManager(VariableContext variableContext, Aggregator[] aggregators) {
        return new BinManager(variableContext, postProcessorConfig, aggregators);
    }

    public VariableContext createVariableContext() {
        VariableContextImpl variableContext = new VariableContextImpl();
        variableContext.setMaskExpr(maskExpr);

        // define declared variables
        //
        if (variableConfigs != null) {
            for (VariableConfig varConfig : variableConfigs) {
                variableContext.defineVariable(varConfig.getName(), varConfig.getExpr(), varConfig.getValidExpr());
            }
        }

        // define variables of all aggregators
        //
        if (aggregatorConfigs != null) {
            TypedDescriptorsRegistry registry = TypedDescriptorsRegistry.getInstance();
            for (AggregatorConfig aggregatorConfig : aggregatorConfigs) {
                AggregatorDescriptor descriptor = registry.getDescriptor(AggregatorDescriptor.class, aggregatorConfig.getName());
                if (descriptor != null) {
                    String[] varNames = descriptor.getSourceVarNames(aggregatorConfig);
                    for (String varName : varNames) {
                        variableContext.defineVariable(varName);
                    }
                } else {
                    throw new IllegalArgumentException("Unknown aggregator type: " + aggregatorConfig.getName());
                }
            }
        }
        if (BinTracer.isActive()) {
             variableContext.defineVariable("_TRACE_X", "X");
             variableContext.defineVariable("_TRACE_Y", "Y");
        }

        return variableContext;
    }


    // used on Calvalus
    public static DataPeriod createDataPeriod(ProductData.UTC startUtc, Double periodDuration, Double minDataHour) {
        if (startUtc != null) {
            if (minDataHour != null) {
                return new SpatialDataPeriod(startUtc.getMJD(), periodDuration, minDataHour);
            } else {
                return new TemporalDataPeriod(startUtc.getMJD(), periodDuration);
            }
        }
        return null;
    }

}
