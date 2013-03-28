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
package org.esa.beam.processor.cloud;

import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.DefaultRequestElementFactory;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.RequestElementFactory;
import org.esa.beam.framework.processor.RequestElementFactoryException;

import java.io.File;

/**
 * Description of CloudRequestElementFactory
 *
 * @author Marco Peters
 *
 * @deprecated since BEAM 4.11. No replacement.
 */
@Deprecated
public class CloudRequestElementFactory implements RequestElementFactory {

    private DefaultRequestElementFactory _defFactory = DefaultRequestElementFactory.getInstance();

    public static CloudRequestElementFactory getInstance() {
        return Holder.instance;
    }

    /**
     * Creates a new reference to an input product for the current processing request.
     *
     * @param url        the input product's URL, must not be <code>null</code>
     * @param fileFormat the file format, can be <code>null</code> if not known
     * @param typeId     the product type identifier, can be <code>null</code> if not known
     *
     * @throws IllegalArgumentException if <code>url</code> is <code>null</code>
     * @throws org.esa.beam.framework.processor.RequestElementFactoryException
     *                                  if the element could not be created
     */
    public ProductRef createInputProductRef(final File url, final String fileFormat, final String typeId) throws RequestElementFactoryException {
        return _defFactory.createInputProductRef(url, fileFormat, typeId);
    }

    /**
     * Creates a new reference to an output product for the current processing request.
     *
     * @param url        the output product's URL, must not be <code>null</code>
     * @param fileFormat the file format, can be <code>null</code> if not known
     * @param typeId     the product type identifier, can be <code>null</code> if not known
     *
     * @throws IllegalArgumentException if <code>url</code> is <code>null</code>
     * @throws org.esa.beam.framework.processor.RequestElementFactoryException
     *                                  if the element could not be created
     */
    public ProductRef createOutputProductRef(final File url, final String fileFormat, final String typeId) throws RequestElementFactoryException {
        return _defFactory.createOutputProductRef(url, fileFormat, typeId);
    }

    /**
     * Creates a new processing parameter for the current processing request.
     *
     * @param name  the parameter name, must not be <code>null</code> or empty
     * @param value the parameter value, can be <code>null</code> if yet not known
     *
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code> or empty
     * @throws org.esa.beam.framework.processor.RequestElementFactoryException
     *                                  if the parameter could not be created or is invalid
     */
    public Parameter createParameter(final String name, final String value) throws RequestElementFactoryException {
        return _defFactory.createParameter(name, value);
    }

    /**
     * Creates a parameter for the default input product path - which is the current user's home directory.
     */
    public Parameter createDefaultInputProductParameter() {
        return _defFactory.createDefaultInputProductParameter();
    }

    public Parameter createDefaultOutputProductParameter() {
        final Parameter defaultOutputProductParameter = _defFactory.createDefaultOutputProductParameter();
        final ParamProperties properties = defaultOutputProductParameter.getProperties();
        final Object defaultValue = properties.getDefaultValue();
        if (defaultValue instanceof File) {
            properties.setDefaultValue(new File((File) defaultValue, CloudConstants.DEFAULT_OUTPUT_PRODUCT_NAME));
        }
        defaultOutputProductParameter.setDefaultValue();
        return defaultOutputProductParameter;
    }

    /**
     * Creates a default logging pattern parameter set to the prefix passed in.
     *
     * @param prefix the default setting for the logging pattern
     *
     * @return a logging pattern Parameter conforming the system settings
     */
    public Parameter createDefaultLogPatternParameter(final String prefix) {
        return _defFactory.createDefaultLogPatternParameter(prefix);
    }

    /**
     * Creates a logging to output product parameter set to true.
     *
     * @return the created logging to output product parameter.
     */
    public Parameter createLogToOutputParameter(final String value) throws ParamValidateException {
        final Parameter logToOutputParameter = _defFactory.createLogToOutputParameter(value);
        logToOutputParameter.getProperties().setDefaultValue(new Boolean(false));
        return logToOutputParameter;
    }


    public Parameter createOutputFormatParameter() {
        return _defFactory.createOutputFormatParameter();
    }
    
    // Initialization on demand holder idiom
    private static class Holder {
        private static final CloudRequestElementFactory instance = new CloudRequestElementFactory();
    }
}
