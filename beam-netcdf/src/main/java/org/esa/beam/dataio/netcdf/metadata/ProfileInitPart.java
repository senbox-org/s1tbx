package org.esa.beam.dataio.netcdf.metadata;

import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Product;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;

public interface ProfileInitPart {

    Product readProductBody(ProfileReadContext ctx) throws ProductIOException;

    void writeProductBody(NetcdfFileWriteable writeable, Product p) throws IOException;
}
