/*
 * $Id: SmileRequestElementFactory.java,v 1.2 2007/04/13 14:39:34 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.processor.smile;

import java.io.File;

import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.DefaultRequestElementFactory;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.RequestElementFactory;
import org.esa.beam.framework.processor.RequestElementFactoryException;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.SystemUtils;

/**
 * Responsible for creating the elements for a processing request used by the Smile Correction Processor.
 *
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 */
public class SmileRequestElementFactory implements RequestElementFactory {

    private DefaultRequestElementFactory _defaultFactory;

    /**
     * Singleton interface - creates the one and only instance of this factory.
     *
     * @return a reference to the factory
     */
    public static SmileRequestElementFactory getInstance() {
        return Holder.instance;
    }

    /**
     * Creates a logging file parameter set to default value
     */
    public Parameter createDefaultLogfileParameter() {
        String curWorkDir = SystemUtils.getCurrentWorkingDir().toString();
        String defaultValue = SystemUtils.convertToLocalPath(
                curWorkDir + "/" + SmileConstants.DEFAULT_LOG_FILE_FILENAME);
        ParamProperties paramProps = _defaultFactory.createFileParamProperties(ParamProperties.FSM_FILES_ONLY,
                                                                               defaultValue);
        paramProps.setLabel(SmileConstants.LOG_FILE_LABELTEXT);
        paramProps.setDescription(SmileConstants.LOG_FILE_DESCRIPTION);
        Parameter _param = new Parameter(SmileConstants.LOG_FILE_PARAM_NAME, paramProps);
        _param.setDefaultValue();
        return _param;
    }

    /**
     * Creates an output format parameter.
     *
     * @return the output format parameter
     */
    public Parameter createOutputFormatParameter() {
        return _defaultFactory.createOutputFormatParameter();
    }


    public Parameter createParameter(String name, String value) throws RequestElementFactoryException {
        if (SmileConstants.PARAM_NAME_OUTPUT_INCLUDE_ALL_SPECTRAL_BANDS.equals(name)) {
            return new Parameter(name, new Boolean(value));
        } else if (SmileConstants.PARAM_NAME_BANDS_TO_PROCESS.equals(name)) {
            return new Parameter(name, StringUtils.csvToArray(value));
        } else {
            return _defaultFactory.createParameter(name, value);
        }
    }

    /**
     * Creates a new reference to an input product for the current processing request.
     *
     * @param url        the input product's URL, must not be <code>null</code>
     * @param fileFormat the file format, can be <code>null</code> if not known
     * @param typeId     the product type identifier, can be <code>null</code> if not known
     *
     * @throws IllegalArgumentException       if <code>url</code> is <code>null</code>
     * @throws RequestElementFactoryException if the element could not be created
     */
    public ProductRef createInputProductRef(File url, String fileFormat, String typeId)
            throws RequestElementFactoryException {
        return _defaultFactory.createInputProductRef(url, fileFormat, typeId);
    }

    /**
     * Creates a new reference to an output product for the current processing request.
     *
     * @param url        the output product's URL, must not be <code>null</code>
     * @param fileFormat the file format, can be <code>null</code> if not known
     * @param typeId     the product type identifier, can be <code>null</code> if not known
     *
     * @throws IllegalArgumentException       if <code>url</code> is <code>null</code>
     * @throws RequestElementFactoryException if the element could not be created
     */
    public ProductRef createOutputProductRef(File url, String fileFormat, String typeId)
            throws RequestElementFactoryException {
        return _defaultFactory.createOutputProductRef(url, fileFormat, typeId);
    }

    /**
     * Creates a parameter for the default input product path - which is the current user's home directory.
     */
    public Parameter createDefaultInputProductParameter() {
        return _defaultFactory.createDefaultInputProductParameter();
    }

    /**
     * Creates an output product parameter set to the default path.
     */
    public Parameter createDefaultOutputProductParameter() {
        Parameter defaultOutputProductParameter = _defaultFactory.createDefaultOutputProductParameter();
        ParamProperties properties = defaultOutputProductParameter.getProperties();
        Object defaultValue = properties.getDefaultValue();
        if (defaultValue instanceof File) {
            properties.setDefaultValue(new File((File) defaultValue, SmileConstants.DEFAULT_OUTPUT_PRODUCT_NAME));
        }
        defaultOutputProductParameter.setDefaultValue();
        // @todo 1 nf/se - also set default output format here, so that it fits to SmileConstants.DEFAULT_FILE_NAME's extension (.dim)
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
        return _defaultFactory.createDefaultLogPatternParameter(prefix);
    }

    /**
     * Creates a logging to output product parameter set to true.
     *
     * @return the created logging to output product parameter
     */
    public Parameter createLogToOutputParameter(final String value) throws ParamValidateException {
        final Parameter logToOutputParameter = _defaultFactory.createLogToOutputParameter(value);
        logToOutputParameter.getProperties().setDefaultValue(new Boolean(false));
        return logToOutputParameter;
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Constructs the object.
     */
    private SmileRequestElementFactory() {
        _defaultFactory = DefaultRequestElementFactory.getInstance();
    }
    
    // Initialization on demand holder idiom
    private static class Holder {
        private static final SmileRequestElementFactory instance = new SmileRequestElementFactory();
    }
}
