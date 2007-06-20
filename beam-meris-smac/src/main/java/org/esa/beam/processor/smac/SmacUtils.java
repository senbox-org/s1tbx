/*
 * $Id: SmacUtils.java,v 1.1.1.1 2006/09/11 08:16:52 norman Exp $
 *
 * Copyright (C) 2002,2003  by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the 
 * Free Software Foundation. This program is distributed in the hope it will 
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU General Public License for more details.
 */
package org.esa.beam.processor.smac;

import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.util.Guardian;

public class SmacUtils {

     /**
     * Converts the sensor type given by the request to a string that can be understood by the
     * <code>SensorCoefficientManager</code>.
     *
     * @param productType the request type string
     *
     * @throws ProcessorException on unsupported input product type
     */
    public static String getSensorType(String productType) throws ProcessorException {
        Guardian.assertNotNull("productType", productType);
        String sensorType = null;

        if (EnvisatConstants.AATSR_L1B_TOA_PRODUCT_TYPE_NAME.equalsIgnoreCase(productType)) {
            sensorType = SensorCoefficientManager.AATSR_NAME;
        } else if (EnvisatConstants.MERIS_FR_L1B_PRODUCT_TYPE_NAME.equalsIgnoreCase(productType)) {
            sensorType = SensorCoefficientManager.MERIS_NAME;
        } else if (EnvisatConstants.MERIS_RR_L1B_PRODUCT_TYPE_NAME.equalsIgnoreCase(productType)) {
            sensorType = SensorCoefficientManager.MERIS_NAME;
        } else {
            throw new ProcessorException(
                    SmacConstants.LOG_MSG_UNSUPPORTED_INPUT_1 + productType + SmacConstants.LOG_MSG_UNSUPPORTED_INPUT_2);
        }

        return sensorType;
    }

    public static boolean isSupportedProductType(String productType) {
        Guardian.assertNotNull("productType", productType);
        boolean bRet = true;

        try {
            getSensorType(productType);
        } catch (ProcessorException e) {
            bRet = false;
        }

        return bRet;
    }
}
