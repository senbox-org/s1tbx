package org.esa.beam.dataio.netcdf.metadata.profiles.hdfeos;

import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
import org.esa.beam.dataio.netcdf.metadata.ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.util.ReaderUtils;
import org.esa.beam.framework.datamodel.Product;
import org.jdom.Element;

import java.io.IOException;


public class HdfEosStartTimePart extends ProfilePart {

    @Override
    public void read(ProfileReadContext ctx, Product p) throws IOException {
        Element element = HdfEosUtils.getEosElement(HdfEosUtils.CORE_METADATA,
                                                    ctx.getNetcdfFile().getRootGroup());
        if (element != null) {
            String date = HdfEosUtils.getValue(element, "INVENTORYMETADATA", "MASTERGROUP", "RANGEDATETIME",
                                               "RANGEBEGINNINGDATE", "VALUE");
            String time = HdfEosUtils.getValue(element, "INVENTORYMETADATA", "MASTERGROUP", "RANGEDATETIME",
                                               "RANGEBEGINNINGTIME", "VALUE");
            if (date != null && !date.isEmpty() && time != null && !time.isEmpty()) {
                p.setStartTime(ReaderUtils.parseDateTime(date + " " + time));
            }
        }
    }

    @Override
    public void define(ProfileWriteContext ctx, Product p) throws IOException {
        throw new IllegalStateException();
    }
}
