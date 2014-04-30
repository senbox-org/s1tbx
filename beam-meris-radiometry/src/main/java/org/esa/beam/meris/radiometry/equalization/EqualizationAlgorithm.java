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

package org.esa.beam.meris.radiometry.equalization;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

public class EqualizationAlgorithm {


    private EqualizationLUT equalizationLUT;
    private long julianDate;

    public EqualizationAlgorithm(Product product, ReprocessingVersion version) {
        this(product.getStartTime(), createLut(getReprocessingVersion(product, version), isFullResolution(product)));
    }

    public EqualizationAlgorithm(ProductData.UTC utc, EqualizationLUT equalizationLUT) {
        // compute julian date
        final Calendar calendar = utc.getAsCalendar();
        long productJulianDate = toJulianDay(calendar.get(Calendar.YEAR),
                                             calendar.get(Calendar.MONTH),
                                             calendar.get(Calendar.DAY_OF_MONTH));
        julianDate = productJulianDate - toJulianDay(2002, 4, 1);

        this.equalizationLUT = equalizationLUT;
    }

    long getJulianDate() {
        return julianDate;
    }

    /**
     * Performs the equalization on the given <code>value</code>.
     *
     * @param value         the value to be equalized
     * @param spectralIndex the spectral index of the band the value is from [0, 14]
     * @param detectorIndex the index of the detector the value is from
     *
     * @return the equalized value
     *
     * @throws IllegalArgumentException if {@code spectralIndex < 0 || spectralIndex > 14} or {@code detectorIndex < 0}
     */
    public double performEqualization(double value, int spectralIndex, int detectorIndex) {
        Assert.argument(spectralIndex >= 0 && spectralIndex <= 14, "spectralIndex < 0 && spectralIndex > 14");
        Assert.argument(detectorIndex >= 0, "detectorIndex < 0");
        final double[] coefficients = equalizationLUT.getCoefficients(spectralIndex, detectorIndex);
        double cEq = coefficients[0] +
                     coefficients[1] * julianDate +
                     coefficients[2] * julianDate * julianDate;
        if (cEq == 0.0) {
            return value;
        }
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

    private static ReprocessingVersion getReprocessingVersion(Product product, ReprocessingVersion version) {
        if (ReprocessingVersion.AUTO_DETECT.equals(version)) {
            return autoDetectReprocessingVersion(product);
        } else {
            return version;
        }
    }

    private static ReprocessingVersion autoDetectReprocessingVersion(Product product) {
        ReprocessingVersion reprocessingVersion = ReprocessingVersion.autoDetect(product);
        if (ReprocessingVersion.AUTO_DETECT.equals(reprocessingVersion)) {
            throw new OperatorException("Reprocessing version could not be detected.\n" +
                                        "Please specify reprocessing version manually.");
        }
        return reprocessingVersion;
    }

    private static EqualizationLUT createLut(ReprocessingVersion reprocessingVersion, boolean fullResolution) {
        EqualizationLUT lut;
        try {
            List<Reader> readerList = getCoefficientsReaders(reprocessingVersion, fullResolution);
            Reader[] readers = readerList.toArray(new Reader[readerList.size()]);
            lut = new EqualizationLUT(readers);
        } catch (IOException e) {
            throw new IllegalStateException("Not able to create LUT.", e);
        }
        return lut;
    }

    private static List<Reader> getCoefficientsReaders(ReprocessingVersion reprocessingVersion, boolean fullResolution) {
        final String coefFilePattern = "Equalization_coefficient_band_%02d_reprocessing_r%d_%s.txt";
        final int bandCount = 15;
        List<Reader> readerList = new ArrayList<>();
        for (int i = 1; i <= bandCount; i++) {
            final InputStream stream = EqualizationLUT.class.getResourceAsStream(
                    String.format(coefFilePattern, i, reprocessingVersion.getVersion(), fullResolution ? "FR" : "RR"));
            readerList.add(new InputStreamReader(stream));
        }
        return readerList;
    }
}
