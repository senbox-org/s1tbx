/*
 * $Id$
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
package org.esa.beam.dataio.netcdf.metadata.profiles.def;

import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.FileInfo;
import org.esa.beam.dataio.netcdf.metadata.Profile;
import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfFlagCodingPart;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.Product;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;

public class DefaultFlagCodingPart extends ProfilePart {

    public static final String FLAG_DESCRIPTIONS = "flag_descriptions";
    public static final String FLAG_CODING_NAME = "flag_coding_name";
    public static final String DESCRIPTION_SEPARATOR = "\t";

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

    private void writeFlagCoding(NetcdfFileWriteable ncFile, Band band) {
        CfFlagCodingPart.writeFlagCoding(ncFile, band);

        final FlagCoding flagCoding = band.getFlagCoding();
        if (flagCoding != null) {
            final String[] flagNames = flagCoding.getFlagNames();
            final StringBuffer descriptions = new StringBuffer();
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
            ncFile.addVariableAttribute(band.getName(), FLAG_CODING_NAME, flagCoding.getName());
            ncFile.addVariableAttribute(band.getName(), FLAG_DESCRIPTIONS, descriptions.toString().trim());
        }
    }

    public static FlagCoding readFlagCoding(Band band, FileInfo rp) throws ProductIOException {
        final FlagCoding flagCoding = CfFlagCodingPart.readFlagCoding(band, rp);

        if (flagCoding != null) {
            final Variable variable = rp.getGlobalVariablesMap().get(band.getName());

            final Attribute descriptionsAtt = variable.findAttributeIgnoreCase(FLAG_DESCRIPTIONS);
            if (descriptionsAtt != null) {
                final String[] descriptions = descriptionsAtt.getStringValue().split(DESCRIPTION_SEPARATOR);
                if (flagCoding.getNumAttributes() != descriptions.length) {
                    throw new ProductIOException(Constants.EM_INVALID_FLAG_CODING);
                }
                for (int i = 0; i < descriptions.length; i++) {
                    flagCoding.getAttributeAt(i).setDescription(descriptions[i]);
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
