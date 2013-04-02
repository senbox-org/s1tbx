package org.esa.beam.dataio.modis.attribute;

import org.esa.beam.dataio.modis.ModisConstants;
import org.esa.beam.dataio.modis.ModisDaacUtils;
import org.esa.beam.dataio.modis.ModisGlobalAttributes;
import org.esa.beam.dataio.modis.ModisUtils;
import org.esa.beam.dataio.modis.hdf.HdfDataField;
import org.esa.beam.dataio.modis.hdf.HdfEosStructMetadata;
import org.esa.beam.dataio.modis.netcdf.NetCDFVariables;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.FileUtils;
import ucar.nc2.Variable;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

public class DaacAttributes implements ModisGlobalAttributes {

    private final NetCDFVariables netCDFVariables;
    private String ecsCoreString;
    private HdfEosStructMetadata hdfEosStructMetadata;
    private Date sensingStart;
    private Date sensingStop;

    public DaacAttributes(NetCDFVariables netCDFVariables) {
        this.netCDFVariables = netCDFVariables;
    }

    @Override
    public String getProductName() throws IOException {
        if (StringUtils.isNullOrEmpty(ecsCoreString)) {
            readEcsCoreString();
        }
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
    public Dimension getProductDimensions(java.util.List<ucar.nc2.Dimension> netcdfFileDimensions) {
        int width = 0;
        int height = 0;
        for (ucar.nc2.Dimension dimension : netcdfFileDimensions) {
            if (isWidthDimension(dimension)) {
                final int dimWidth = dimension.getLength();
                if (dimWidth > width) {
                    width = dimWidth;
                }
            }

            if (isHeightDimension(dimension)) {
                final int dimHeight = dimension.getLength();
                if (dimHeight > height) {
                    height = dimHeight;
                }
            }
        }
        return new Dimension(width, height);
    }

    @Override
    public HdfDataField getDatafield(String name) throws ProductIOException {
        final Variable variable = netCDFVariables.get(name);
        final java.util.List<ucar.nc2.Dimension> dimensions = variable.getDimensions();

        final HdfDataField result = new HdfDataField();
        final String[] dimensionNames = new String[dimensions.size()];
        for (int i = 0; i < dimensions.size(); i++) {
            ucar.nc2.Dimension dimension = dimensions.get(i);
            dimensionNames[i] = dimension.getName();
        }
        result.setDimensionNames(dimensionNames);
        return result;
    }

    @Override
    public Date getSensingStart() throws ProductIOException {
        if (sensingStart == null) {
            parseSensingTimes();
        }
        return sensingStart;
    }

    @Override
    public Date getSensingStop() throws ProductIOException {
        if (sensingStop == null) {
            parseSensingTimes();
        }
        return sensingStop;
    }

    @Override
    public int[] getSubsamplingAndOffset(String dimensionName) throws IOException {
        if (hdfEosStructMetadata == null) {
            readHdfEosStructMeta();
        }
        return hdfEosStructMetadata.getSubsamplingAndOffset(dimensionName);
    }

    @Override
    public boolean isImappFormat() {
        return false;
    }

    @Override
    public String getEosType() throws IOException {
        if (hdfEosStructMetadata == null) {
            readHdfEosStructMeta();
        }
        return hdfEosStructMetadata.getEosType();
    }

    @Override
    public GeoCoding createGeocoding() {
        return hdfEosStructMetadata.createGeocoding();
    }

    // package access for testing only tb 2012-05-22
    static boolean isHeightDimension(ucar.nc2.Dimension dimension) {
        final String dimensionName = dimension.getName();
        return dimensionName.contains("10*nscans") ||
                dimensionName.contains("20*nscans") ||
                dimensionName.contains("YDim") ||
                dimensionName.contains("Number_of_records");
    }

    // package access for testing only tb 2012-05-22
    static boolean isWidthDimension(ucar.nc2.Dimension dimension) {
        final String dimensionName = dimension.getName();
        return dimensionName.contains("Max_EV_frames") ||
                dimensionName.contains("XDim") ||
                dimensionName.contains("Number_of_samples_per_record");
    }

    private void readEcsCoreString() throws IOException {
        ecsCoreString = ModisDaacUtils.extractCoreString(netCDFVariables);
    }

    private void readHdfEosStructMeta() throws IOException {
        final Variable structMetaVariable = netCDFVariables.get(ModisConstants.STRUCT_META_KEY);
        if (structMetaVariable == null) {
            throw new ProductIOException("Unknown MODIS format: no StructMetadata available");
        }

        final String structMetaString = structMetaVariable.readScalarString();
        if (StringUtils.isNullOrEmpty(structMetaString)) {
            throw new ProductIOException("Unknown MODIS format: no StructMetadata available");
        }
        hdfEosStructMetadata = new HdfEosStructMetadata(structMetaString);
    }

    private void parseSensingTimes() throws ProductIOException {
        try {
            final String startDate = ModisUtils.extractValueForKey(ecsCoreString, ModisConstants.RANGE_BEGIN_DATE_KEY);
            final String startTime = ModisUtils.extractValueForKey(ecsCoreString, ModisConstants.RANGE_BEGIN_TIME_KEY);
            final String endDate = ModisUtils.extractValueForKey(ecsCoreString, ModisConstants.RANGE_END_DATE_KEY);
            final String endTime = ModisUtils.extractValueForKey(ecsCoreString, ModisConstants.RANGE_END_TIME_KEY);

            if (startDate == null || startTime == null) {
                throw new ProductIOException("Unable to retrieve sensing start time from metadata");
            }
            sensingStart = ModisUtils.createDateFromStrings(startDate, startTime);

            if (endDate == null || endTime == null) {
                throw new ProductIOException("Unable to retrieve sensing stop time from metadata");
            }
            sensingStop = ModisUtils.createDateFromStrings(endDate, endTime);
        } catch (ParseException e) {
            throw new ProductIOException(e.getMessage());
        }
    }
}
