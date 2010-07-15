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
package org.esa.beam.dataio.netcdf.metadata.profiles.cf;

import org.esa.beam.dataio.netcdf.util.AttributeMap;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.FileInfo;
import org.esa.beam.dataio.netcdf.metadata.Profile;
import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
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

    public static final String FLAG_MASKS = "flag_masks";
    public static final String FLAG_MEANINGS = "flag_meanings";


    @Override
    public void read(Profile profile, Product p) throws IOException {
        final Band[] bands = p.getBands();
        for (Band band : bands) {
            final FlagCoding flagCoding = readFlagCoding(band, profile.getFileInfo());
            if (flagCoding != null) {
                p.getFlagCodingGroup().add(flagCoding);
                band.setSampleCoding(flagCoding);
            }
        }
    }

    @Override
    public void define(Profile ctx, Product p, NetcdfFileWriteable ncFile) throws IOException {
        final Band[] bands = p.getBands();
        for (Band band : bands) {
            writeFlagCoding(ncFile, band);
        }
    }

    public static void writeFlagCoding(NetcdfFileWriteable ncFile, Band band) {
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
            ncFile.addVariableAttribute(band.getName(), new Attribute(FLAG_MEANINGS, meanings.toString()));
            ncFile.addVariableAttribute(band.getName(), new Attribute(FLAG_MASKS, Array.factory(flagValues)));
        }
    }


    public static FlagCoding readFlagCoding(Band band, FileInfo rp) throws ProductIOException {
        final Variable variable = rp.getGlobalVariablesMap().get(band.getName());
        final AttributeMap attMap = AttributeMap.create(variable);
        final String codingName = band.getName() + "_flag_coding";
        return createFlagCoding(attMap, codingName);
    }

    private static FlagCoding createFlagCoding(final AttributeMap attMap, final String codingName)
            throws ProductIOException {
        final Attribute flagMasks = attMap.get(FLAG_MASKS);
        final int[] maskValues;
        if (flagMasks != null) {
            maskValues = new int[flagMasks.getLength()];
            for (int i = 0; i < maskValues.length; i++) {
                maskValues[i] = flagMasks.getNumericValue(i).intValue();
            }
        } else {
            maskValues = null;
        }

        final String flagMeanings = attMap.getStringValue(FLAG_MEANINGS);
        final String[] flagNames;
        if (flagMeanings != null) {
            flagNames = flagMeanings.split(" ");
        } else {
            flagNames = null;
        }

        return createFlagCoding(codingName, maskValues, flagNames);
    }

    private static FlagCoding createFlagCoding(String codingName, int[] maskValues, String[] flagNames)
            throws ProductIOException {
        if (maskValues != null && flagNames != null) {
            if (maskValues.length != flagNames.length) {
                throw new ProductIOException(Constants.EM_INVALID_FLAG_CODING);
            }
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
