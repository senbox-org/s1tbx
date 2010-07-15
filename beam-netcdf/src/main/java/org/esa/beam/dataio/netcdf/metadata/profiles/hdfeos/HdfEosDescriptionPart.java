package org.esa.beam.dataio.netcdf.metadata.profiles.hdfeos;

import org.esa.beam.dataio.netcdf.metadata.Profile;
import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
import org.esa.beam.framework.datamodel.Product;
import org.jdom.Element;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;


public class HdfEosDescriptionPart extends ProfilePart {

    @Override
    public void read(Profile profile, Product p) throws IOException {
        Element element = HdfEosUtils.getEosElement(HdfEosUtils.ARCHIVE_METADATA,
                                                    profile.getFileInfo().getNetcdfFile().getRootGroup());
        if (element != null) {
            p.setDescription(HdfEosUtils.getValue(element, "ARCHIVEDMETADATA", "MASTERGROUP", "LONGNAME", "VALUE"));
        }
    }

    @Override
    public void define(Profile ctx, Product p, NetcdfFileWriteable ncFile) throws IOException {
        throw new IllegalStateException();
    }
}
