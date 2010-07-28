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
package org.esa.beam.dataio.netcdf.metadata.profiles.cf;

import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
import org.esa.beam.dataio.netcdf.metadata.ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.ReaderUtils;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Product;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;

public class CfFlagCodingPart extends ProfilePart {

    private static final String FLAG_MASKS = "flag_masks";
    private static final String FLAG_MEANINGS = "flag_meanings";

    @Override
    public void read(ProfileReadContext ctx, Product p) throws IOException {
        final Band[] bands = p.getBands();
        for (Band band : bands) {
            final FlagCoding flagCoding = readFlagCoding(ctx, band.getName());
            if (flagCoding != null) {
                p.getFlagCodingGroup().add(flagCoding);
                band.setSampleCoding(flagCoding);
            }
        }
    }

    @Override
    public void define(ProfileWriteContext ctx, Product p) throws IOException {
        final Band[] bands = p.getBands();
        for (Band band : bands) {
            writeFlagCoding(band, ctx.getNetcdfFileWriteable());
        }
    }

    public static void writeFlagCoding(Band band, NetcdfFileWriteable ncFile) {
        final FlagCoding flagCoding = band.getFlagCoding();
        if (flagCoding != null) {
            final String[] flagNames = flagCoding.getFlagNames();
            final int[] flagValues = new int[flagNames.length];
            final StringBuffer meanings = new StringBuffer();
            for (int i = 0; i < flagValues.length; i++) {
                if (meanings.length() > 0) {
                    meanings.append(" ");
                }
                String name = flagNames[i];
                meanings.append(name);
                flagValues[i] = flagCoding.getFlagMask(name);
            }
            String variableName = ReaderUtils.getVariableName(band);
            ncFile.addVariableAttribute(variableName, new Attribute(FLAG_MEANINGS, meanings.toString()));
            ncFile.addVariableAttribute(variableName, new Attribute(FLAG_MASKS, Array.factory(flagValues)));
        }
    }

    public static FlagCoding readFlagCoding(ProfileReadContext ctx, String variableName) throws ProductIOException {
        final Variable variable = ctx.getNetcdfFile().getRootGroup().findVariable(variableName);
        final String codingName = variableName + "_flag_coding";
        return readFlagCoding(variable, codingName);
    }

    private static FlagCoding readFlagCoding(Variable variable, String codingName)
            throws ProductIOException {
        final Attribute flagMasks = variable.findAttribute(FLAG_MASKS);
        final int[] maskValues;
        if (flagMasks != null) {
            maskValues = new int[flagMasks.getLength()];
            for (int i = 0; i < maskValues.length; i++) {
                maskValues[i] = flagMasks.getNumericValue(i).intValue();
            }
        } else {
            maskValues = null;
        }

        final Attribute flagMeanings = variable.findAttribute(FLAG_MEANINGS);
        final String[] flagNames;
        if (flagMeanings != null) {
            flagNames = flagMeanings.getStringValue().split(" ");
        } else {
            flagNames = null;
        }

        return createFlagCoding(codingName, maskValues, flagNames);
    }

    private static FlagCoding createFlagCoding(String codingName, int[] maskValues, String[] flagNames)
            throws ProductIOException {
        if (maskValues != null && flagNames != null && maskValues.length == flagNames.length) {
            final FlagCoding coding = new FlagCoding(codingName);
            for (int i = 0; i < maskValues.length; i++) {
                final String sampleName = flagNames[i];
                final int sampleValue = maskValues[i];
                coding.addSample(sampleName, sampleValue, "");
            }
            if (coding.getNumAttributes() > 0) {
                return coding;
            }
        }
        return null;
    }
}
