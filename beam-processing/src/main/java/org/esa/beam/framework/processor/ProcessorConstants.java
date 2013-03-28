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

/*
 *
 * @deprecated since BEAM 4.11. Use the {@link org.esa.beam.framework.gpf Graph Processing Framework} instead.
 */
@Deprecated
public interface ProcessorConstants {

    // Parameter name for the request parameter describing the input_product.
    String INPUT_PRODUCT_PARAM_NAME = "input_product";
    // Default value for input product parameter label
    String INPUT_PRODUCT_LABELTEXT = "Input product file"; /*I18N*/
    // Default value for input product parameter description
    String INPUT_PRODUCT_DESCRIPTION = "Input product file path."; /*I18N*/

    // Parameter name for the request parameter describing the output_product.
    String OUTPUT_PRODUCT_PARAM_NAME = "output_product";
    // Default value for output product parameter label
    String OUTPUT_PRODUCT_LABELTEXT = "Output product file"; /*I18N*/
    // Default value for output product parameter description
    String OUTPUT_PRODUCT_DESCRIPTION = "Output product file path."; /*I18N*/

    // Parameter name for the request parameter describing the output format.
    String OUTPUT_FORMAT_PARAM_NAME = "output_format";  /*I18N*/
    // Default value for output format parameter label
    String OUTPUT_FORMAT_LABELTEXT = "Output product format"; /*I18N*/
    // Default value for output format parameter description
    String OUTPUT_FORMAT_DESCRIPTION = "Select one of the available output product formats."; /*I18N*/
    // parameter name for the parameter log_file
    String LOG_FILE_PARAM_NAME = "log_file"; /*I18N*/
    // default value for label for parameter logging file
    String LOG_FILE_LABELTEXT = "Log File"; /*I18N*/
    // default value for logging file parameter description
    String LOG_FILE_DESCRIPTION = "Log file path."; /*I18N*/
    /**
     * parameter name for the parameter log_prefix
     */
    String LOG_PREFIX_PARAM_NAME = "log_prefix";
    /**
     * parameter description for the parameter log_prefix
     */
    String LOG_PREFIX_DESCRIPTION = "Log file prefix.";
    /**
     * parameter label for the parameter log_prefix
     */
    String LOG_PREFIX_LABELTEXT = "Log filename prefix";
    /**
     * parameter name for the parameter log_to_output
     */
    String LOG_TO_OUTPUT_PARAM_NAME = "log_to_output";
    /**
     * parameter description for the parameter log_prefix
     */
    String LOG_TO_OUTPUT_DESCRIPTION = "Create additional log file in the output product directory.";
    /**
     * parameter label for the parameter log_prefix
     */
    String LOG_TO_OUTPUT_LABELTEXT = "Extra log to output directory";

    // a bunch of predefined logging messages for the processors
    // ---------------------------------------------------------
    String LOG_MSG_PROC_ABORTED = "Processing aborted.";
    String LOG_MSG_PROC_CANCELED = "Processing canceled by user.";
    String LOG_MSG_PROC_SUCCESS = "... processing successful";
    String LOG_MSG_PROC_ERROR = "An error occurred during processing: ";

    String LOG_MSG_START_REQUEST = "STARTING REQUEST ...";
    String LOG_MSG_FINISHED_REQUEST = "... FINISHED";

    String LOG_MSG_LOAD_REQUEST = "Loading request ...";

    String LOG_MSG_NO_INPUT_IN_REQUEST = "Unable to retrieve input product from processing request.";
    String LOG_MSG_NO_INPUT_TYPE = "Unable to retrieve product type from input product.";

    String LOG_MSG_NO_OUTPUT_IN_REQUEST = "Unable to retrieve output product from processing request.\nPlease select an output product to be processed.";
    String LOG_MSG_NO_OUTPUT_FORMAT = "No output product format set.";
    String LOG_MSG_FAIL_CREATE_WRITER = "Unable to create product writer for format: ";

    String LOG_MSG_REQUEST_MALFORMED = "Processing request file is malformed.\nPlease check the syntax.";

    String LOG_MSG_SUCCESS = "... success";

    String LOG_MSG_NO_INVALID_PIXEL = "No value for invalid pixels set.";
    String LOG_MSG_USING = "... using ";

    String LOG_MSG_LOADED_BAND = "... loaded band: ";

    String LOG_MSG_NO_BITMASK = "No bitmask set.";
    String LOG_MSG_PROCESS_ALL = "... processing all pixel";
    String LOG_MSG_NO_OUTPUT = "Unable to retrieve output product from request.";
    String LOG_MSG_NO_OUTPUT_NAME = "Unable to retrieve output product name from request.";

    String PACKAGE_LOGGER_NAME = "beam.processor";

    // Parameter name for the request parameter describing the request type
    String REQUEST_TYPE_PARAM_NAME = "type"; /*I18N*/

    // Processor status definitions
    // ----------------------------
    int STATUS_UNKNOWN = 0;
    int STATUS_STARTED = 1;
    int STATUS_COMPLETED = 2;
    int STATUS_COMPLETED_WITH_WARNING = 3;
    int STATUS_ABORTED = 4;
    int STATUS_FAILED = 5;

}
