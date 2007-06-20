package org.esa.beam.dataio.modis;

import ncsa.hdf.hdflib.HDFException;
import org.esa.beam.dataio.modis.hdf.HdfDataField;
import org.esa.beam.dataio.modis.hdf.HdfGlobalAttributes;
import org.esa.beam.dataio.modis.hdf.HdfStructMetadata;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.util.io.FileUtils;

import java.awt.Dimension;
import java.io.File;
import java.text.ParseException;
import java.util.Date;


class ModisDaacAttributes implements ModisGlobalAttributes {

    private String _productName;
    private String _productType;
    private Date _sensingStart;
    private Date _sensingStop;
    private final HdfStructMetadata _structMeta;

    public ModisDaacAttributes() {
        _structMeta = new HdfStructMetadata();
    }

    public String getProductName() {
        return _productName;
    }

    public String getProductType() {
        return _productType;
    }

    public boolean isImappFormat() {
        return false;
    }

    public Dimension getProductDimensions() {
        return _structMeta.getProductDimensions();
    }

    public HdfDataField getDatafield(String name) {
        return _structMeta.getDatafield(name);
    }

    public int[] getTiePointSubsAndOffset(String dimensionName) throws HDFException {
        return _structMeta.getTiePointSubsAndOffset(dimensionName);
    }

    public void decode(final HdfGlobalAttributes hdfAttributes) throws ProductIOException {
        decodeECSCore(hdfAttributes);

        final String structMetaString = hdfAttributes.getStringAttributeValue(ModisConstants.STRUCT_META_KEY);
        if (structMetaString == null) {
            throw new ProductIOException("Unknown MODIS format: no StructMetadata available");
        }
        _structMeta.parse(structMetaString);
    }

    public Date getSensingStart() {
        return _sensingStart;
    }

    public Date getSensingStop() {
        return _sensingStop;
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private void decodeECSCore(HdfGlobalAttributes hdfAttributes) throws ProductIOException {
        final String coreString = hdfAttributes.getStringAttributeValue(ModisConstants.CORE_META_KEY);
        if (coreString == null) {
            throw new ProductIOException("Unknown MODIS format: no ECSCore metadata available");
        }

        final String productName = ModisUtils.extractValueForKey(coreString, ModisConstants.LOCAL_GRANULEID_KEY);
        if (productName == null) {
            throw new ProductIOException(
                    "Unknown MODIS format: ECSCore metadata field '" + ModisConstants.LOCAL_GRANULEID_KEY + "' missing");
        }
        _productName = FileUtils.getFilenameWithoutExtension(new File(productName));

        // extract the type as shortname
        _productType = ModisUtils.extractValueForKey(coreString, ModisConstants.SHORT_NAME_KEY);

        extractStartAndStopTimes(coreString);
    }

    private void extractStartAndStopTimes(String coreString) throws ProductIOException {
        try {
            final String startDate = ModisUtils.extractValueForKey(coreString, ModisConstants.RANGE_BEGIN_DATE_KEY);
            final String startTime = ModisUtils.extractValueForKey(coreString, ModisConstants.RANGE_BEGIN_TIME_KEY);
            final String endDate = ModisUtils.extractValueForKey(coreString, ModisConstants.RANGE_END_DATE_KEY);
            final String endTime = ModisUtils.extractValueForKey(coreString, ModisConstants.RANGE_END_TIME_KEY);

            if (startDate == null || startTime == null) {
                throw new ProductIOException("Unable to retrieve sensing start time from metadata");
            }
            _sensingStart = ModisUtils.createDateFromStrings(startDate, startTime);

            if (endDate == null || endTime == null) {
                throw new ProductIOException("Unable to retrieve sensing start time from metadata");
            }
            _sensingStop = ModisUtils.createDateFromStrings(endDate, endTime);
        } catch (ParseException e) {
            throw new ProductIOException(e.getMessage());
        }
    }
}
