/*
 * $Id: SmileConstants.java,v 1.2 2006/09/11 10:02:01 norman Exp $
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

import org.esa.beam.framework.processor.ProcessorConstants;

/**
 * Constants definitions for the Smile Correction Processor.
 *
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class SmileConstants implements ProcessorConstants {

    // default value for logging file parameter default filename
    // @todo 1 nf/se - remove this param, its not required
    public static final String DEFAULT_LOG_FILE_FILENAME = "smile_corr_log.txt";

    // name for the processor logger
    public static final String LOGGER_NAME = "beam.processor.smile";
    public static final String DEFAULT_LOG_PREFIX = "smile_corr";

    // the default output product name
    public static final String DEFAULT_OUTPUT_PRODUCT_NAME = "smile_corr_out.dim";

    // the required request type
    public static final String REQUEST_TYPE = "SMILE_CORRECTION";

    public static final String AUXDATA_DIR_PROPERTY = "smile.auxdata.dir";

    public static final String PARAM_NAME_OUTPUT_INCLUDE_ALL_SPECTRAL_BANDS = "include_all";
    public static final String PARAM_NAME_BANDS_TO_PROCESS = "bands";

    public static final String BITMASK_TERM_LAND = "l1_flags.LAND_OCEAN";
    public static final String BITMASK_TERM_PROCESS = "NOT l1_flags.INVALID";

    public final static String LOG_MSG_INCLUDE_ALL_BANDS = "... Output will contain all spectral bands.";
    public final static String LOG_MSG_PROCESS_BANDS = "... Processing spectral bands:";
    public final static String LOG_MSG_NO_BANDS = "... No spectral bands to process!";
    public static final String LOG_MSG_WRONG_PRODUCT = "The input product does not contain a 'detector index' band.";

    public static final String LOG_MSG_COPY_DETECTOR_BAND = "Copying detector index band data to output product.";
    public static final String LOG_MSG_COPY_FLAG_BAND = "Copying flag band data to output product.";
    public static final String LOG_MSG_COPY_UNPROCESSED_BANDS = "Copying unprocessed spectral band data to output product.";
}
