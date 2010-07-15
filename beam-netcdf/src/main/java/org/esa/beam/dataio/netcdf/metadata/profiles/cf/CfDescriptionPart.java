package org.esa.beam.dataio.netcdf.metadata.profiles.cf;

import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
import org.esa.beam.dataio.netcdf.metadata.ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.util.AttributeMap;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.framework.datamodel.Product;
import ucar.nc2.Attribute;

import java.io.IOException;

public class CfDescriptionPart extends ProfilePart {

    public static final String DESCRIPTION = "description";
    public static final String TITLE = "title";
    public static final String COMMENT = "comment";

    @Override
    public void read(ProfileReadContext ctx, Product p) throws IOException {
        final AttributeMap attributesMap = ctx.getGlobalAttributes();
        final String[] attribNames = new String[]{DESCRIPTION, TITLE, COMMENT};
        for (String attribName : attribNames) {
            final String description = attributesMap.getStringValue(attribName);
            if (description != null) {
                p.setDescription(description);
                return;
            }
        }
        p.setDescription(Constants.FORMAT_DESCRIPTION);
    }

    @Override
    public void define(ProfileWriteContext ctx, Product p) throws IOException {
        final String description = p.getDescription();
        if (description != null && description.trim().length() > 0) {
            ctx.getNetcdfFileWriteable().addAttribute(null, new Attribute(TITLE, description));
        }
    }
}
