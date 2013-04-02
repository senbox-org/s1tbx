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

import org.esa.beam.framework.param.Parameter;
import org.esa.beam.util.Guardian;

import java.util.logging.Logger;

/**
 * The request logger logs the content of a request to any one of the logging sinks attached to this object.
 *
 * @deprecated since BEAM 4.11. Use the {@link org.esa.beam.framework.gpf Graph Processing Framework} instead.
 */
@Deprecated
public class RequestLogger {

    private Logger _logger;

    /**
     * Constructs the object with default parameters.
     */
    public RequestLogger() {
        _logger = Logger.getLogger(ProcessorConstants.PACKAGE_LOGGER_NAME);
    }

    /**
     * Logs the content of a request to the logging sinks attached
     *
     * @param request the request to be logged
     */
    public void logRequest(Request request) {
        Guardian.assertNotNull("Request", request);
        _logger.info("Logging request:");
        logType(request);
        logInputProduct(request);
        logOutputProduct(request);
        logParameter(request);
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Logs the request type information to the logging sinks
     *
     * @param request the request to be logged
     */
    private void logType(Request request) {
        String type = request.getType();

        if ((type == null) || (type.length() == 0)) {
            _logger.info("... Request has no type specification");
        } else {
            _logger.info("... Request of type: " + type);
        }
    }

    /**
     * Logs the input product of the request to the sinks attached
     *
     * @param request the request to be logged
     */
    private void logInputProduct(Request request) {
        ProductRef ref = null;
        String format = null;
        String typeId = null;

        for (int n = 0; n < request.getNumInputProducts(); n++) {
            ref = request.getInputProductAt(n);
            _logger.info("... Input product: " + ref.getFilePath());

            format = ref.getFileFormat();
            if ((format != null) && (format.length() > 0)) {
                _logger.info("... Input product format: " + format);
            }

            typeId = ref.getTypeId();
            if ((typeId != null) && (typeId.length() > 0)) {
                _logger.info("... Input product type: " + typeId);
            }
        }
    }

    /**
     * Logs the output product of the request to the sinks attached
     *
     * @param request the request to be logged
     */
    private void logOutputProduct(Request request) {
        ProductRef ref = null;
        String format = null;
        String typeId = null;

        for (int n = 0; n < request.getNumOutputProducts(); n++) {
            ref = request.getOutputProductAt(n);
            _logger.info("... Output product: " + ref.getFilePath());

            format = ref.getFileFormat();
            if ((format != null) && (format.length() > 0)) {
                _logger.info("... Output product format: " + format);
            }

            typeId = ref.getTypeId();
            if ((typeId != null) && (typeId.length() > 0)) {
                _logger.info("... Output product type: " + typeId);
            }
        }
    }

    /**
     * Logs the parameter of the request to the sinks attached
     */
    private void logParameter(Request request) {
        Parameter param = null;

        for (int n = 0; n < request.getNumParameters(); n++) {
            param = request.getParameterAt(n);
            _logger.info("... Parameter: name=" + param.getName() + " value=" + param.getValueAsText());
        }
    }
}
