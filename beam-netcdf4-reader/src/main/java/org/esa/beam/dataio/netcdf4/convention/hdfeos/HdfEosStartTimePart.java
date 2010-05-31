package org.esa.beam.dataio.netcdf4.convention.hdfeos;

import org.esa.beam.dataio.netcdf4.Nc4ReaderParameters;
import org.esa.beam.dataio.netcdf4.Nc4ReaderUtils;
import org.esa.beam.dataio.netcdf4.convention.HeaderDataWriter;
import org.esa.beam.dataio.netcdf4.convention.ModelPart;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.jdom.Element;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;
import java.text.ParseException;


public class HdfEosStartTimePart implements ModelPart {
    
    @Override
    public void read(Product p, Nc4ReaderParameters rp) throws IOException {
        Element element = HdfEosUtils.getEosElement(HdfEosUtils.CORE_METADATA, rp.getNetcdfFile().getRootGroup());
        if (element != null) {
            String date = HdfEosUtils.getValue(element, "INVENTORYMETADATA", "MASTERGROUP", "RANGEDATETIME", "RANGEBEGINNINGDATE", "VALUE");
            String time = HdfEosUtils.getValue(element, "INVENTORYMETADATA", "MASTERGROUP", "RANGEDATETIME", "RANGEBEGINNINGTIME", "VALUE");
            if (date != null && !date.isEmpty() && time != null && !time.isEmpty()) {
                try {
                    p.setStartTime(Nc4ReaderUtils.parseDateTime(date + " " + time));
                } catch (ParseException ignore) {
                }
            }
        }
    }

    @Override
    public void write(Product p, NetcdfFileWriteable ncFile, HeaderDataWriter hdw) throws IOException {
        throw new IllegalStateException();
    }
}
