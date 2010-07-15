package org.esa.beam.dataio.netcdf.metadata.profiles.cf;

import org.esa.beam.dataio.netcdf.util.AttributeMap;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.FileInfo;
import org.esa.beam.dataio.netcdf.metadata.Profile;
import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
import org.esa.beam.framework.datamodel.Product;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;

public class CfDescriptionPart extends ProfilePart {

    public static final String DESCRIPTION = "description";
    public static final String TITLE = "title";
    public static final String COMMENT = "comment";

    @Override
    public void read(Profile profile, Product p) throws IOException {
        p.setDescription(getProductDescription(profile.getFileInfo()));

    }

    @Override
    public void define(Profile ctx, Product p, NetcdfFileWriteable ncFile) throws IOException {
        final String description = p.getDescription();
        if (description != null && description.trim().length() > 0) {
            ncFile.addAttribute(null, new Attribute(TITLE, description));
        }
    }

    public static String getProductDescription(final FileInfo rv) {
        final AttributeMap attributesMap = rv.getGlobalAttributes();
        final String[] attribNames = new String[]{DESCRIPTION, TITLE, COMMENT};
        for (String attribName : attribNames) {
            final String description = attributesMap.getStringValue(attribName);
            if (description != null) {
                return description;
            }
        }
        return Constants.FORMAT_DESCRIPTION;
    }
}
