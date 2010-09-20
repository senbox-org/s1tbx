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

package org.esa.beam;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class EqualizationAlgorithm {

    private static final String ELEM_NAME_MPH = "MPH";
    private static final String ATTRIB_SOFTWARE_VER = "SOFTWARE_VER";

    private EqualizationLUT equalizationLUT;
    private long julianDate;

    public EqualizationAlgorithm(Product product, ReprocessingVersion version) {
        this(getReprocessingVersion(product, version), isFullResolution(product), product.getStartTime());
    }

    public EqualizationAlgorithm(int reprocessingVersion, boolean fullResolution, ProductData.UTC utc) {
        // compute julian date
        final Calendar calendar = utc.getAsCalendar();
        long productJulianDate = toJulianDay(calendar.get(Calendar.YEAR),
                                             calendar.get(Calendar.MONTH),
                                             calendar.get(Calendar.DAY_OF_MONTH));
        julianDate = productJulianDate - toJulianDay(2002, 4, 1);

        try {
            equalizationLUT = new EqualizationLUT(reprocessingVersion, fullResolution);
        } catch (IOException e) {
            throw new OperatorException("Not able to create LUT.", e);
        }
    }

    /**
     * Performs the equalization on the given <code>value</code>.
     *
     * @param value         the value to be equalized
     * @param spectralIndex the spectral index of the band the value is from [0, 14]
     * @param detectorIndex the index of the detector the value is from
     *
     * @return the equalized value
     */
    public double performEqualization(double value, int spectralIndex, int detectorIndex) {
        final double[] coefficients = equalizationLUT.getCoefficients(spectralIndex, detectorIndex);
        double cEq = coefficients[0] +
                     coefficients[1] * julianDate +
                     coefficients[2] * julianDate * julianDate;
        return value / cEq;

    }

    private static boolean isFullResolution(Product product) {
        return product.getProductType().startsWith("MER_F");
    }

    static long toJulianDay(int year, int month, int dayOfMonth) {
        final double millisPerDay = 86400000.0;

        // The epoch (days) for the Julian Date (JD) which corresponds to 4713-01-01 12:00 BC.
        final double epochJulianDate = -2440587.5;

        final GregorianCalendar utc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(year, month, dayOfMonth, 0, 0, 0);
        utc.set(Calendar.MILLISECOND, 0);

        return (long) (utc.getTimeInMillis() / millisPerDay - epochJulianDate);
    }

    private static int getReprocessingVersion(Product product, ReprocessingVersion version) {
        if (ReprocessingVersion.AUTO_DETECT.equals(version)) {
            return autoDetectReprocessingVersion(product);
        } else {
            return version.getVersion();
        }
    }

    private static int autoDetectReprocessingVersion(Product product) {
        final MetadataElement mphElement = product.getMetadataRoot().getElement(ELEM_NAME_MPH);
        if (mphElement != null) {
            final String softwareVer = mphElement.getAttributeString(ATTRIB_SOFTWARE_VER);
            if (softwareVer != null) {
                final String[] strings = softwareVer.split("/");
                final String processorName = strings[0];
                final int maxLength = Math.min(strings[1].length(), 5); // first 5 characters
                final String processorVersion = strings[1].substring(0, maxLength);
                try {
                    return detectReprocessingVersion(processorName, processorVersion);
                } catch (Exception e) {
                    final String msgPattern = String.format("Not able to detect reprocessing version [%s=%s]. \n" +
                                                            "Please specify reprocessing version manually.",
                                                            ATTRIB_SOFTWARE_VER, softwareVer);
                    throw new OperatorException(msgPattern, e);
                }
            }
        }
        throw new OperatorException(
                "Not able to detect reprocessing version.\nMetadata attribute 'MPH/SOFTWARE_VER' not found.");
    }

    static int detectReprocessingVersion(String processorName, String processorVersion) throws Exception {
        final float version;
        version = versionToFloat(processorVersion);
        if ("MERIS".equalsIgnoreCase(processorName)) {
            if (version >= 4.1f && version <= 5.06f) {
                return 2;
            }
        }
        if ("MEGS-PC".equalsIgnoreCase(processorName)) {
            if (version >= 7.4f && version <= 7.5f) {
                return 2;
            } else if (version >= 8.0f) {
                return 3;
            }
        }

        throw new Exception("Unknown reprocessing version.");
    }

    static float versionToFloat(String processorVersion) throws Exception {
        final String[] values = processorVersion.trim().split("\\.");
        float version = 0.0f;
        try {
            for (int i = 0; i < values.length; i++) {
                String value = values[i];
                final int integer = Integer.parseInt(value);
                int leadingZeros = 0;
                for (int j = 0; j < value.length(); j++) {
                    if (value.charAt(j) == '0') {
                        leadingZeros++;
                    }
                }
                version += integer / Math.pow(10, i + leadingZeros);
            }
        } catch (NumberFormatException nfe) {
            throw new Exception(String.format("Could not parse version [%s]", processorVersion), nfe);
        }
        return version;
    }

}
