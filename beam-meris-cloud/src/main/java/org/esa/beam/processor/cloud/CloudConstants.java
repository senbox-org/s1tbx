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

import org.esa.beam.framework.processor.ProcessorConstants;


/**
 * Description of CloudConstants
 *
 * @author Marco Peters
 *
 * @deprecated since BEAM 4.11. No replacement.
 */
@Deprecated
public class CloudConstants implements ProcessorConstants {

    public static final String LOGGER_NAME = "beam.processor.cloud";
    public static final String DEFAULT_LOG_PREFIX = "cloud_prob";
    public static final String DEFAULT_OUTPUT_PRODUCT_NAME = "cloud.dim";

    public static final String LOG_MSG_OUTPUT_CREATED = "Output product successfully created";
}
