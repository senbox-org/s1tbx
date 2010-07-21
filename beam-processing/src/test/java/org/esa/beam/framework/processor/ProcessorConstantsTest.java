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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ProcessorConstantsTest extends TestCase {

    public ProcessorConstantsTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(ProcessorConstantsTest.class);
    }

    public void testInputProductConstants() {
        assertEquals("input_product", ProcessorConstants.INPUT_PRODUCT_PARAM_NAME);
    }

    public void testOutputProductConstants() {
        assertEquals("output_product", ProcessorConstants.OUTPUT_PRODUCT_PARAM_NAME);
    }

    public void testOutputFormatConstants() {
        assertEquals("output_format", ProcessorConstants.OUTPUT_FORMAT_PARAM_NAME);
    }

    public void testLogFileConstants() {
        assertEquals("log_file", ProcessorConstants.LOG_FILE_PARAM_NAME);
        assertEquals("log_prefix", ProcessorConstants.LOG_PREFIX_PARAM_NAME);
        assertEquals("log_to_output", ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME);
        assertEquals("beam.processor", ProcessorConstants.PACKAGE_LOGGER_NAME);
    }

    public void testRequestTypeParameter() {
        assertEquals("type", ProcessorConstants.REQUEST_TYPE_PARAM_NAME);
    }

    public void testProcessorStateConstants() {
        assertEquals(0, ProcessorConstants.STATUS_UNKNOWN);
        assertEquals(1, ProcessorConstants.STATUS_STARTED);
        assertEquals(2, ProcessorConstants.STATUS_COMPLETED);
        assertEquals(3, ProcessorConstants.STATUS_COMPLETED_WITH_WARNING);
        assertEquals(4, ProcessorConstants.STATUS_ABORTED);
        assertEquals(5, ProcessorConstants.STATUS_FAILED);
    }
}
