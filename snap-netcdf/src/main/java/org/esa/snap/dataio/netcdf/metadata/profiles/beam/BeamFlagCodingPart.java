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
package org.esa.snap.dataio.netcdf.metadata.profiles.beam;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartIO;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfFlagCodingPart;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.util.ReaderUtils;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import java.io.IOException;

public class BeamFlagCodingPart extends ProfilePartIO {

    public static final String FLAG_DESCRIPTIONS = "flag_descriptions";
    public static final String FLAG_CODING_NAME = "flag_coding_name";
    public static final String DESCRIPTION_SEPARATOR = "\t";

    @Override
    public void decode(ProfileReadContext ctx, Product p) throws IOException {
        final Band[] bands = p.getBands();
        for (Band band : bands) {
            String variableName = ReaderUtils.getVariableName(band);
            final FlagCoding flagCoding = readFlagCoding(ctx, variableName);
            if (flagCoding != null) {
                p.getFlagCodingGroup().add(flagCoding);
                band.setSampleCoding(flagCoding);
            }
        }
    }

    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {
        NFileWriteable ncFile = ctx.getNetcdfFileWriteable();
        final Band[] bands = p.getBands();
        for (Band band : bands) {
            CfFlagCodingPart.writeFlagCoding(band, ncFile);
            writeFlagCoding(band, ncFile);
        }
    }

    public void writeFlagCoding(Band band, NFileWriteable ncFile) throws IOException {
        final FlagCoding flagCoding = band.getFlagCoding();
        if (flagCoding != null) {
            final String[] flagNames = flagCoding.getFlagNames();
            final StringBuilder descriptions = new StringBuilder();
            for (String flagName : flagNames) {
                final MetadataAttribute flag = flagCoding.getFlag(flagName);
                if (flag != null) {
                    final String description = flag.getDescription();
                    if (description != null) {
                        descriptions.append(description);
                    }
                }
                descriptions.append(DESCRIPTION_SEPARATOR);
            }
            String variableName = ReaderUtils.getVariableName(band);
            ncFile.findVariable(variableName).addAttribute(FLAG_CODING_NAME, flagCoding.getName());
            ncFile.findVariable(variableName).addAttribute(FLAG_DESCRIPTIONS, descriptions.toString().trim());
        }
    }

    public static FlagCoding readFlagCoding(ProfileReadContext ctx, String variableName) {
        final FlagCoding flagCoding = CfFlagCodingPart.readFlagCoding(ctx, variableName);

        if (flagCoding != null) {
            final Variable variable = ctx.getNetcdfFile().getRootGroup().findVariable(variableName);
            final Attribute descriptionsAtt = variable.findAttributeIgnoreCase(FLAG_DESCRIPTIONS);
            if (descriptionsAtt != null) {
                final String[] descriptions = descriptionsAtt.getStringValue().split(DESCRIPTION_SEPARATOR);
                if (flagCoding.getNumAttributes() == descriptions.length) {
                    for (int i = 0; i < descriptions.length; i++) {
                        flagCoding.getAttributeAt(i).setDescription(descriptions[i]);
                    }
                }
            }

            final Attribute nameAtt = variable.findAttributeIgnoreCase(FLAG_CODING_NAME);
            if (nameAtt != null) {
                flagCoding.setName(nameAtt.getStringValue());
            }
        }

        return flagCoding;
    }

}
