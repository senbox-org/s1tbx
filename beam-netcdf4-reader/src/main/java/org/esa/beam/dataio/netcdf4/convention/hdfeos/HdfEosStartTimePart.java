package org.esa.beam.dataio.netcdf4.convention.hdfeos;

import org.esa.beam.dataio.netcdf4.Nc4ReaderUtils;
import org.esa.beam.dataio.netcdf4.convention.Profile;
import org.esa.beam.dataio.netcdf4.convention.ProfilePart;
import org.esa.beam.framework.datamodel.Product;
import org.jdom.Element;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;
import java.text.ParseException;


public class HdfEosStartTimePart extends ProfilePart {

    @Override
    public void read(Profile profile, Product p) throws IOException {
        Element element = HdfEosUtils.getEosElement(HdfEosUtils.CORE_METADATA,
                                                    profile.getFileInfo().getNetcdfFile().getRootGroup());
        if (element != null) {
            String date = HdfEosUtils.getValue(element, "INVENTORYMETADATA", "MASTERGROUP", "RANGEDATETIME",
                                               "RANGEBEGINNINGDATE", "VALUE");
            String time = HdfEosUtils.getValue(element, "INVENTORYMETADATA", "MASTERGROUP", "RANGEDATETIME",
                                               "RANGEBEGINNINGTIME", "VALUE");
            if (date != null && !date.isEmpty() && time != null && !time.isEmpty()) {
                try {
                    p.setStartTime(Nc4ReaderUtils.parseDateTime(date + " " + time));
                } catch (ParseException ignore) {
                }
            }
        }
    }

    @Override
    public void define(Profile ctx, Product p, NetcdfFileWriteable ncFile) throws IOException {
        throw new IllegalStateException();
    }
}
