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
package org.esa.beam.framework.processor;

import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;

import java.io.File;

/**
 * The factory for the elements of a processing request. A <code>RequestElementFactory</code> allows a particular
 * processor implementation to influence the process of generating request elements with a <code>RequestLoader</code>.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see RequestLoader
 * @see Request
 *
 * @deprecated since BEAM 4.11. Use the {@link org.esa.beam.framework.gpf Graph Processing Framework} instead.
 */
@Deprecated
public interface RequestElementFactory {
    /**
     * Creates a new reference to an input product for the current processing request.
     *
     * @param file       the input product's file, must not be <code>null</code>
     * @param fileFormat the file format, can be <code>null</code> if not known
     * @param typeId     the product type identifier, can be <code>null</code> if not known
     * @throws IllegalArgumentException       if <code>url</code> is <code>null</code>
     * @throws RequestElementFactoryException if the element could not be created
     */
    ProductRef createInputProductRef(File file, String fileFormat, String typeId)
            throws RequestElementFactoryException;

    /**
     * Creates a new reference to an output product for the current processing request.
     *
     * @param file       the output product's file, must not be <code>null</code>
     * @param fileFormat the file format, can be <code>null</code> if not known
     * @param typeId     the product type identifier, can be <code>null</code> if not known
     * @throws IllegalArgumentException       if <code>url</code> is <code>null</code>
     * @throws RequestElementFactoryException if the element could not be created
     */
    ProductRef createOutputProductRef(File file, String fileFormat, String typeId)
            throws RequestElementFactoryException;

    /**
     * Creates a new processing parameter for the current processing request.
     *
     * @param name  the parameter name, must not be <code>null</code> or empty
     * @param value the parameter value, can be <code>null</code> if yet not known
     * @throws IllegalArgumentException       if <code>name</code> is <code>null</code> or empty
     * @throws RequestElementFactoryException if the parameter could not be created or is invalid
     */
    Parameter createParameter(String name, String value)
            throws RequestElementFactoryException;

    /**
     * Creates a parameter for the default input product path - which is the current user's home directory.
     */
    Parameter createDefaultInputProductParameter();

    /**
     * Creates an output product parameter set to the default path.
     */
    Parameter createDefaultOutputProductParameter();

    /**
     * Creates a default logging pattern parameter set to the prefix passed in.
     *
     * @param prefix the default setting for the logging pattern
     * @return a logging pattern Parameter conforming the system settings
     */
    Parameter createDefaultLogPatternParameter(String prefix);

    /**
     * Creates a logging to output product parameter set to true.
     *
     * @return the created logging to output product parameter.
     */
    Parameter createLogToOutputParameter(String value) throws ParamValidateException;
}
