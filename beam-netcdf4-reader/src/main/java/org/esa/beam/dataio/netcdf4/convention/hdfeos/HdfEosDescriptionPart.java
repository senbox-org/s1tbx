package org.esa.beam.dataio.netcdf4.convention.hdfeos;

import org.esa.beam.dataio.netcdf4.Nc4ReaderParameters;
import org.esa.beam.dataio.netcdf4.convention.HeaderDataWriter;
import org.esa.beam.dataio.netcdf4.convention.ModelPart;
import org.esa.beam.framework.datamodel.Product;
import org.jdom.Element;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;
import java.util.List;


public class HdfEosDescriptionPart implements ModelPart {

    @Override
    public void read(Product p, Nc4ReaderParameters rp) throws IOException {
        Element element = HdfEosUtils.getEosElement(HdfEosUtils.ARCHIVE_METADATA, rp.getNetcdfFile().getRootGroup());
        if (element != null) {
            p.setDescription(HdfEosUtils.getValue(element, "ARCHIVEDMETADATA", "MASTERGROUP", "LONGNAME", "VALUE"));
        }
    }

    @Override
    public void write(Product p, NetcdfFileWriteable ncFile, HeaderDataWriter hdw) throws IOException {
        throw new IllegalStateException();
    }
}
