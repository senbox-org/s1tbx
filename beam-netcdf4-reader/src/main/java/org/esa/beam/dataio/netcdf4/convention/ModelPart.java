package org.esa.beam.dataio.netcdf4.convention;

import org.esa.beam.framework.datamodel.Product;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;

public interface ModelPart {

    void read(Product p, Model rp) throws IOException;

    void write(Product p, NetcdfFileWriteable ncFile, HeaderDataWriter hdw, Model model) throws IOException;
}
