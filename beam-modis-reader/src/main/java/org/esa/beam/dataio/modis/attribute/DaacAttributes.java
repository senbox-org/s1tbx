package org.esa.beam.dataio.modis.attribute;

import org.esa.beam.dataio.modis.ModisConstants;
import org.esa.beam.dataio.modis.ModisDaacUtils;
import org.esa.beam.dataio.modis.ModisGlobalAttributes;
import org.esa.beam.dataio.modis.ModisUtils;
import org.esa.beam.dataio.modis.hdf.HdfDataField;
import org.esa.beam.dataio.modis.netcdf.NetCDFVariables;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.FileUtils;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import ucar.nc2.Variable;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Date;

public class DaacAttributes implements ModisGlobalAttributes {

    private final NetCDFVariables netCDFVariables;

    public DaacAttributes(NetCDFVariables netCDFVariables) {
        this.netCDFVariables = netCDFVariables;
    }

    @Override
    public String getProductName() throws IOException {
        final Variable variable = netCDFVariables.get(ModisConstants.CORE_META_KEY);
        if (variable == null) {
            throw new ProductIOException("Unknown MODIS format: no ECSCore metadata available");
        }

        final String ecsCoreString = variable.readScalarString();
        final String productName = ModisUtils.extractValueForKey(ecsCoreString,
                                                                 ModisConstants.LOCAL_GRANULEID_KEY);
        if (StringUtils.isNullOrEmpty(productName)) {
            throw new ProductIOException("Unknown MODIS format: ECSCore metadata field '" +
                                                 ModisConstants.LOCAL_GRANULEID_KEY + "' missing");
        }
        return FileUtils.getFilenameWithoutExtension(new File(productName));
    }

    @Override
    public String getProductType() throws IOException {
        final String productName = getProductName();
        return ModisDaacUtils.extractProductType(productName);
    }

    @Override
    public Dimension getProductDimensions() {
        throw new NotImplementedException();
    }

    @Override
    public Dimension getProductDimensions(java.util.List<ucar.nc2.Dimension> netcdfFileDimensions) {
        int width = 0;
        int height = 0;
        for (int i = 0; i < netcdfFileDimensions.size(); i++) {
            ucar.nc2.Dimension dimension = netcdfFileDimensions.get(i);
            if (dimension.getName().contains("Max_EV_frames")) {
                width = dimension.getLength();
            }

            if (dimension.getName().contains("10*nscans")) {
                height = dimension.getLength();
            }
        }
        return new Dimension(width, height);
    }

    @Override
    public HdfDataField getDatafield(String name) throws ProductIOException {
        throw new NotImplementedException();
    }

    @Override
    public Date getSensingStart() {
        throw new NotImplementedException();
    }

    @Override
    public Date getSensingStop() {
        throw new NotImplementedException();
    }

    @Override
    public int[] getSubsamplingAndOffset(String dimensionName) {
        throw new NotImplementedException();
    }

    @Override
    public boolean isImappFormat() {
        return false;
    }

    @Override
    public String getEosType() {
        throw new NotImplementedException();
    }

    @Override
    public GeoCoding createGeocoding() {
        throw new NotImplementedException();
    }
}
