package org.esa.beam.dataio.modis.attribute;

import org.esa.beam.dataio.modis.IncrementOffset;
import org.esa.beam.dataio.modis.ModisConstants;
import org.esa.beam.dataio.modis.ModisGlobalAttributes;
import org.esa.beam.dataio.modis.ModisUtils;
import org.esa.beam.dataio.modis.hdf.HdfDataField;
import org.esa.beam.dataio.modis.netcdf.NetCDFAttributes;
import org.esa.beam.dataio.modis.netcdf.NetCDFUtils;
import org.esa.beam.dataio.modis.netcdf.NetCDFVariables;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.logging.BeamLogManager;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import java.awt.*;
import java.io.File;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class ImappAttributes implements ModisGlobalAttributes {

    private final File inputFile;
    private final Logger logger;
    private final NetCDFVariables netCDFVariables;
    private final NetCDFAttributes netCDFAttributes;
    private HashMap<String, IncrementOffset> subsamplingMap;
    private Date sensingStart;
    private Date sensingStop;

    public ImappAttributes(File inputFile, NetCDFVariables netCDFVariables, NetCDFAttributes netCDFAttributes) {
        this.inputFile = inputFile;
        this.netCDFVariables = netCDFVariables;
        this.netCDFAttributes = netCDFAttributes;

        logger = BeamLogManager.getSystemLogger();
    }

    @Override
    public String getProductName() {
        return FileUtils.getFilenameWithoutExtension(inputFile);
    }

    @Override
    public String getProductType() {
        final String inputFileName = FileUtils.getFilenameWithoutExtension(inputFile);
        final int dotIndex = inputFileName.indexOf(".");
        if (dotIndex > 0) {
            return inputFileName.substring(0, dotIndex);
        } else {
            logger.warning("Unable to retrieve the product type from the file name.");
            return "unknown";
        }
    }

    @Override
    public Dimension getProductDimensions(java.util.List<ucar.nc2.Dimension> netcdfFileDimensions) {
        int width = 0;
        int height = 0;
        final Variable[] all = netCDFVariables.getAll();
        for (final Variable variable : all) {
            final int rank = variable.getRank();
            final int[] shape = variable.getShape();
            if (rank == 2) {
                if (width < shape[1]) {
                    width = shape[1];
                }

                if (height < shape[0]) {
                    height = shape[0];
                }
            } else if (rank == 3) {
                if (width < shape[2]) {
                    width = shape[2];
                }

                if (height < shape[1]) {
                    height = shape[1];
                }
            }
        }
        return new Dimension(width, height);
    }

    @Override
    public HdfDataField getDatafield(String name) throws ProductIOException {
        final Variable variable = netCDFVariables.get(name);
        final List<ucar.nc2.Dimension> dimensions = variable.getDimensions();

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
    public Date getSensingStart() {
        if (sensingStart == null) {
            parseSensingTimes();
        }
        return sensingStart;
    }

    @Override
    public Date getSensingStop() {
        if (sensingStop == null) {
            parseSensingTimes();
        }
        return sensingStop;
    }

    @Override
    public int[] getSubsamplingAndOffset(String dimensionName) {
        if (subsamplingMap == null) {
            parseTiePointSubsamplingAndOffset();
        }
        final IncrementOffset incrementOffset = subsamplingMap.get(dimensionName);
        if (incrementOffset != null) {
            int[] result = new int[2];
            result[0] = incrementOffset.increment;
            result[1] = incrementOffset.offset;

            return result;
        }
        return null;
    }

    @Override
    public boolean isImappFormat() {
        return true;
    }

    @Override
    public String getEosType() {
        return null;
    }

    @Override
    public GeoCoding createGeocoding() {
        return null;
    }

    private void parseTiePointSubsamplingAndOffset() {
        subsamplingMap = new HashMap<String, IncrementOffset>();
        final Variable[] all = netCDFVariables.getAll();
        for (final Variable variable : all) {
            final NetCDFAttributes netCDFAttributes = new NetCDFAttributes();
            netCDFAttributes.add(variable.getAttributes());

            final ucar.nc2.Dimension heightDimension = variable.getDimension(0);
            final String line_numbers = NetCDFUtils.getNamedStringAttribute("line_numbers", netCDFAttributes);
            if (StringUtils.isNotNullAndNotEmpty(line_numbers)) {
                subsamplingMap.put(heightDimension.getName(), ModisUtils.getIncrementOffset(line_numbers));
            }

            final ucar.nc2.Dimension widthDimension = variable.getDimension(1);
            final String frame_numbers = NetCDFUtils.getNamedStringAttribute("frame_numbers", netCDFAttributes);
            if (StringUtils.isNotNullAndNotEmpty(frame_numbers)) {
                subsamplingMap.put(widthDimension.getName(), ModisUtils.getIncrementOffset(frame_numbers));
            }
        }
    }

    private void parseSensingTimes() {
        final Attribute startDateAttribute = netCDFAttributes.get(ModisConstants.RANGE_BEGIN_DATE_KEY);
        final Attribute startTimeAttribute = netCDFAttributes.get(ModisConstants.RANGE_BEGIN_TIME_KEY);
        final Attribute stopDateAttribute = netCDFAttributes.get(ModisConstants.RANGE_END_DATE_KEY);
        final Attribute stopTimeAttribute = netCDFAttributes.get(ModisConstants.RANGE_END_TIME_KEY);

        try {
            if (startDateAttribute == null || startTimeAttribute == null) {
                logger.warning("Unable to retrieve sensing start time from metadata");
                sensingStart = null;
            } else {
                sensingStart = ModisUtils.createDateFromStrings(startDateAttribute.getStringValue(), startTimeAttribute.getStringValue());
            }

            if (stopDateAttribute == null || stopTimeAttribute == null) {
                logger.warning("Unable to retrieve sensing stop time from metadata");
                sensingStop = null;
            } else {
                sensingStop = ModisUtils.createDateFromStrings(stopDateAttribute.getStringValue(), stopTimeAttribute.getStringValue());
            }
        } catch (ParseException e) {
            logger.warning("Unable to parse sensing times: " + e.getMessage());
        }
    }
}
