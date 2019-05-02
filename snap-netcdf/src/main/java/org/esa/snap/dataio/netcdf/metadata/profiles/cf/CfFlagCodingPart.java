/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.dataio.netcdf.metadata.profiles.cf;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartIO;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.util.ReaderUtils;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import java.io.IOException;

public class CfFlagCodingPart extends ProfilePartIO {

    private static final String FLAG_MASKS = "flag_masks";
    private static final String FLAG_MEANINGS = "flag_meanings";

    @Override
    public void decode(ProfileReadContext ctx, Product p) throws IOException {
        // already handled in CfBandPart
    }

    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {
        final Band[] bands = p.getBands();
        for (Band band : bands) {
            writeFlagCoding(band, ctx.getNetcdfFileWriteable());
        }
    }

    public static void writeFlagCoding(Band band, NFileWriteable ncFile) throws IOException {
        final FlagCoding flagCoding = band.getFlagCoding();
        if (flagCoding != null) {
            final String[] flagNames = flagCoding.getFlagNames();
            ProductData flagValueData = ProductData.createInstance(band.getDataType(), flagNames.length);
            final StringBuilder meanings = new StringBuilder();
            for (int i = 0; i < flagValueData.getNumElems(); i++) {
                if (meanings.length() > 0) {
                    meanings.append(" ");
                }
                String name = flagNames[i];
                meanings.append(name);
                flagValueData.setElemIntAt(i, flagCoding.getFlagMask(name));
            }
            String variableName = ReaderUtils.getVariableName(band);
            String description = flagCoding.getDescription();
            if (description != null) {
                ncFile.findVariable(variableName).addAttribute("long_name", description);
            }
            ncFile.findVariable(variableName).addAttribute(FLAG_MEANINGS, meanings.toString());

            final Array maskValues = Array.factory(flagValueData.getElems());
            maskValues.setUnsigned(flagValueData.isUnsigned());
            ncFile.findVariable(variableName).addAttribute(FLAG_MASKS, maskValues);
        }
    }

    public static FlagCoding readFlagCoding(ProfileReadContext ctx, String variableName) {
        final Variable variable = ctx.getNetcdfFile().getRootGroup().findVariable(variableName);
        final String codingName = variableName + "_flag_coding";
        if (variable != null) {
            return readFlagCoding(variable, codingName);
        } else {
            return null;
        }
    }

    private static FlagCoding readFlagCoding(Variable variable, String codingName) {
        final Attribute flagMasks = variable.findAttribute(FLAG_MASKS);
        final int[] maskValues;
        if (flagMasks != null) {
            final Array flagMasksArray = flagMasks.getValues();
            // must set the unsigned property explicitly,
            // even though it is set when writing the flag_masks attribute
            flagMasksArray.setUnsigned(variable.isUnsigned());
            maskValues = new int[flagMasks.getLength()];
            for (int i = 0; i < maskValues.length; i++) {
                maskValues[i] = flagMasksArray.getInt(i);
            }
        } else {
            maskValues = null;
        }

        final Attribute flagMeanings = variable.findAttribute(FLAG_MEANINGS);
        final String[] flagNames;
        if (flagMeanings != null) {
            flagNames = StringUtils.makeStringsUnique(flagMeanings.getStringValue().split(" "));
        } else {
            flagNames = null;
        }
        return createFlagCoding(codingName, maskValues, flagNames);
    }

    private static FlagCoding createFlagCoding(String codingName, int[] maskValues, String[] flagNames) {
        if (maskValues != null && flagNames != null && maskValues.length == flagNames.length) {
            final FlagCoding coding = new FlagCoding(codingName);
            for (int i = 0; i < maskValues.length; i++) {
                final String sampleName = replaceNonWordCharacters(flagNames[i]);
                final int sampleValue = maskValues[i];
                coding.addSample(sampleName, sampleValue, "");
            }
            if (coding.getNumAttributes() > 0) {
                return coding;
            }
        }
        return null;
    }

    static String replaceNonWordCharacters(String flagName) {
        return flagName.replaceAll("\\W+", "_");
    }
}
