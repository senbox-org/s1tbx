/*
 * $Id: CloudConstants.java,v 1.1.1.1 2006/09/11 08:16:52 norman Exp $
 *
 * Copyright (C) 2006 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.processor.cloud;

import org.esa.beam.framework.processor.ProcessorConstants;


/**
 * Description of CloudConstants
 *
 * @author Marco Peters
 */
public class CloudConstants implements ProcessorConstants {

    public static final String LOGGER_NAME = "beam.processor.cloud";
    public static final String DEFAULT_LOG_PREFIX = "cloud_prob";
    public static final String DEFAULT_OUTPUT_PRODUCT_NAME = "cloud.dim";

    public static final String LOG_MSG_OUTPUT_CREATED = "Output product successfully created";
}
