/*
 * $Id$
 *
 * Copyright (C) 2010 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.dataio.netcdf4.convention.beam;

import org.esa.beam.dataio.netcdf4.Nc4Constants;
import org.esa.beam.dataio.netcdf4.convention.HeaderDataWriter;
import org.esa.beam.dataio.netcdf4.convention.Model;
import org.esa.beam.dataio.netcdf4.convention.ModelPart;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.Stx;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.List;

public class BeamStxPart implements ModelPart {

    public final String STATISTICS = "statistics";
    public final String SAMPLE_FREQUENCIES = "sample_frequencies";
    public final int INDEX_SCALED_MIN = 0;
    public final int INDEX_SCALED_MAX = 1;
    public final int INDEX_MEAN = 2;
    public final int INDEX_STANDARD_DEVIATION = 3;

    @Override
    public void read(Product p, Model model) throws IOException {
        final List<Variable> variableList = model.getReaderParameters().getNetcdfFile().getVariables();
        for (Variable variable : variableList) {
            final Attribute statistics = variable.findAttributeIgnoreCase(STATISTICS);
            final Attribute sampleFrequencies = variable.findAttributeIgnoreCase(SAMPLE_FREQUENCIES);
            if (statistics == null && sampleFrequencies == null) {
                continue;
            }
            if (statistics == null || sampleFrequencies == null || statistics.getLength() < 2) {
                throw new ProductIOException(Nc4Constants.EM_INVALID_STX_ATTRIBUTES);
            } else {
                final Band band = p.getBand(variable.getName());

                final double scaledMin = statistics.getNumericValue(INDEX_SCALED_MIN).doubleValue();
                final double min = band.scaleInverse(scaledMin);

                final double scaledMax = statistics.getNumericValue(INDEX_SCALED_MAX).doubleValue();
                final double max = band.scaleInverse(scaledMax);

                final Number meanNumber = statistics.getNumericValue(INDEX_MEAN);
                final double scaledMean = meanNumber != null ? meanNumber.doubleValue() : Double.NaN;
                final double mean = band.scaleInverse(scaledMean);

                final Number stdDevNumber = statistics.getNumericValue(INDEX_STANDARD_DEVIATION);
                final double scaledStdDev = stdDevNumber != null ? stdDevNumber.doubleValue() : Double.NaN;
                final double stdDev = band.scaleInverse(scaledStdDev);

                final boolean intType = variable.getDataType().isIntegral();

                final int[] frequencies = new int[sampleFrequencies.getLength()];
                for (int i = 0; i < frequencies.length; i++) {
                    final Number fNumber = sampleFrequencies.getNumericValue(i);
                    frequencies[i] = fNumber != null ? fNumber.intValue() : 0;
                }

                final int resolutionLevel = 0;

                band.setStx(new Stx(min, max, mean, stdDev, intType, frequencies, resolutionLevel));
            }
        }
    }

    @Override
    public void write(Product p, NetcdfFileWriteable ncFile, HeaderDataWriter hdw, Model model) throws IOException {
        final Band[] bands = p.getBands();
        for (Band band : bands) {
            if (band.isStxSet()) {
                final Stx stx = band.getStx();
                final Variable variable = ncFile.findVariable(band.getName());
                final double[] statistics = new double[4];
                statistics[INDEX_SCALED_MIN] = stx.getMin();
                statistics[INDEX_SCALED_MAX] = stx.getMax();
                statistics[INDEX_MEAN] = stx.getMean();
                statistics[INDEX_STANDARD_DEVIATION] = stx.getStandardDeviation();
                variable.addAttribute(new Attribute(STATISTICS, Array.factory(statistics)));
                variable.addAttribute(new Attribute(SAMPLE_FREQUENCIES, Array.factory(stx.getHistogramBins())));
            }
        }
    }
}
