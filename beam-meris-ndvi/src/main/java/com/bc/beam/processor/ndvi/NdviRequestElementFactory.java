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
package com.bc.beam.processor.ndvi;

import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.param.ParamException;
import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.DefaultRequestElementFactory;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.RequestElementFactory;
import org.esa.beam.framework.processor.RequestElementFactoryException;
import org.esa.beam.util.SystemUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Responsible for creating the elements for a processing request used by the NDVI Processor.
 *
 * @author Marco Peters
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 *
 * @deprecated since BEAM 4.11. No replacement.
 */
@Deprecated
public class NdviRequestElementFactory implements RequestElementFactory {


    private DefaultRequestElementFactory _defaultFactory;
    private Map<String, ParamProperties> _paramPropertiesMap;

    /**
     * Singleton interface - creates the one and only instance of this factory.
     *
     * @return a reference to the factory
     */
    public static NdviRequestElementFactory getInstance() {
        return Holder.instance;
    }

    @Override
    public ProductRef createInputProductRef(final File url, final String fileFormat, final String typeId) throws
                                                                                                         RequestElementFactoryException {
        return _defaultFactory.createInputProductRef(url, fileFormat, typeId);
    }

    @Override
    public ProductRef createOutputProductRef(final File url, final String fileFormat, final String typeId) throws
                                                                                                          RequestElementFactoryException {
        return _defaultFactory.createOutputProductRef(url, fileFormat, typeId);
    }

    @Override
    public Parameter createDefaultInputProductParameter() {
        return _defaultFactory.createDefaultInputProductParameter();
    }

    @Override
    public Parameter createDefaultOutputProductParameter() {
        final File defaultValue = new File(SystemUtils.getUserHomeDir(), NdviProcessor.DEFAULT_OUTPUT_PRODUCT_NAME);
        final Parameter parameter = _defaultFactory.createDefaultOutputProductParameter();
        parameter.setValue(defaultValue, null);
        return parameter;
    }

    public Parameter createOutputFormatParameter() {
        return _defaultFactory.createOutputFormatParameter();
    }

    @Override
    public Parameter createDefaultLogPatternParameter(final String prefix) {
        return _defaultFactory.createDefaultLogPatternParameter(prefix);
    }

    @Override
    public Parameter createLogToOutputParameter(final String value) throws ParamValidateException {
        return _defaultFactory.createLogToOutputParameter(value);
    }

    // todo: move this implementation up to DefaultREF
    @Override
    public Parameter createParameter(final String name, final String value) throws RequestElementFactoryException {
        final Parameter parameter;
        if (ProcessorConstants.OUTPUT_FORMAT_PARAM_NAME.equalsIgnoreCase(name)) {
            parameter = getInstance().createOutputFormatParameter();
        } else {
            parameter = createParameter(name);
        }
        if (parameter == null) {
            throw new RequestElementFactoryException("Unknown processing parameter '" + name + "'");
        }
        try {
            parameter.setValueAsText(value);
        } catch (ParamException e) {
            throw new RequestElementFactoryException(e.getMessage(), e);
        }
        return parameter;
    }

    public Parameter createParameter(final String name) {
        final ParamProperties paramProperties = _paramPropertiesMap.get(name);
        if (paramProperties != null) {
            return new Parameter(name, paramProperties);
        }
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Constructs the object.
     */
    private NdviRequestElementFactory() {
        _defaultFactory = DefaultRequestElementFactory.getInstance();
        _paramPropertiesMap = new HashMap<String, ParamProperties>(7);

        _paramPropertiesMap.put(NdviProcessor.LOWER_FACTOR_PARAM_NAME, createLowerFactorProperties());
        _paramPropertiesMap.put(NdviProcessor.UPPER_FACTOR_PARAM_NAME, createUpperFactorProperties());
        _paramPropertiesMap.put(NdviProcessor.UPPER_BAND_PARAM_NAME, createUpperbandProperties());
        _paramPropertiesMap.put(NdviProcessor.LOWER_BAND_PARAM_NAME, createLowerBandProperties());
    }

    private static ParamProperties createLowerBandProperties() {
        final ParamProperties paramProperties = new ParamProperties(String.class,
                                                                    NdviProcessor.LOWER_BAND_PARAM_DEFAULT,
                                                                    EnvisatConstants.MERIS_L1B_BAND_NAMES);
        paramProperties.setLabel("Lower band");
        return paramProperties;
    }

    private static ParamProperties createUpperbandProperties() {
        final ParamProperties paramProperties = new ParamProperties(String.class,
                                                                    NdviProcessor.UPPER_BAND_PARAM_DEFAULT,
                                                                    EnvisatConstants.MERIS_L1B_BAND_NAMES);
        paramProperties.setLabel("Upper band");
        return paramProperties;
    }

    private static ParamProperties createUpperFactorProperties() {
        final ParamProperties paramProperties = new ParamProperties(Float.class,
                                                                    NdviProcessor.UPPER_FACTOR_PARAM_DEFAULT);
        paramProperties.setMinValue((float) 0);
        paramProperties.setMaxValue((float) 10);
        paramProperties.setLabel("Upper factor");
        return paramProperties;
    }

    private static ParamProperties createLowerFactorProperties() {
        final ParamProperties paramProperties = new ParamProperties(Float.class,
                                                                    NdviProcessor.LOWER_FACTOR_PARAM_DEFAULT);
        paramProperties.setMinValue((float) 0);
        paramProperties.setMaxValue((float) 10);
        paramProperties.setLabel("Lower factor");
        return paramProperties;
    }
    
    // Initialization on demand holder idiom
    private static class Holder {
        private static final NdviRequestElementFactory instance = new NdviRequestElementFactory();
    }
}
