package org.esa.beam.dataio.netcdf.metadata.profiles.hdfeos;

import org.esa.beam.dataio.netcdf.AbstractNetCdfReaderPlugIn;
import org.esa.beam.dataio.netcdf.ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.ProfileInitPartReader;
import org.esa.beam.dataio.netcdf.metadata.ProfilePartReader;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfInitialisationPart;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.RasterDigest;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductIOException;
import org.jdom.Element;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;

import java.io.IOException;
import java.util.Locale;

public class HdfEosNetCdfReaderPlugIn extends AbstractNetCdfReaderPlugIn {

    @Override
    protected DecodeQualification getDecodeQualification(NetcdfFile netcdfFile) {
        try {
            Element eosElement = HdfEosUtils.getEosElement(HdfEosUtils.STRUCT_METADATA, netcdfFile.getRootGroup());
            // check for GRID
            String gridName = getGridName(eosElement);
            if (gridName == null || gridName.isEmpty()) {
                return DecodeQualification.UNABLE;
            }
            //check for projection
            Element gridStructure = eosElement.getChild("GridStructure");
            Element gridElem = (Element) gridStructure.getChildren().get(0);
            Element projectionElem = gridElem.getChild("Projection");
            if (projectionElem == null) {
                return DecodeQualification.UNABLE;
            }
            String projection = projectionElem.getValue();
            if (projection.equals("GCTP_GEO") || projection.equals("GCTP_SNSOID")) {
                return DecodeQualification.INTENDED;
            }
        } catch (Exception ignore) {
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    protected void initReadContext(ProfileReadContext ctx) throws IOException {
        Group eosGroup = ctx.getNetcdfFile().getRootGroup();
        Element eosStructElement;
        String gridName;
        eosStructElement = HdfEosUtils.getEosElement(HdfEosUtils.STRUCT_METADATA, eosGroup);
        gridName = getGridName(eosStructElement);
        if (gridName == null || gridName.isEmpty()) {
            throw new ProductIOException("Could not find grid.");
        }
        Group gridGroup = HdfEosUtils.findGroupNested(eosGroup, gridName);
        if (gridGroup == null) {
            throw new ProductIOException("Could not find grid group.");
        }
        RasterDigest rasterDigest = RasterDigest.createRasterDigest(gridGroup);
        ctx.setRasterDigest(rasterDigest);
        ctx.setProperty(HdfEosUtils.STRUCT_METADATA, eosStructElement);
        ctx.setProperty(HdfEosUtils.CORE_METADATA,
                        HdfEosUtils.getEosElement(HdfEosUtils.CORE_METADATA, eosGroup));
        ctx.setProperty(HdfEosUtils.ARCHIVE_METADATA,
                        HdfEosUtils.getEosElement(HdfEosUtils.ARCHIVE_METADATA, eosGroup));
    }

    @Override
    public ProfileInitPartReader createInitialisationPartReader() {
        return new CfInitialisationPart();
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{"HDF-EOS"};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{Constants.FILE_EXTENSION_HDF, Constants.FILE_EXTENSION_HDF_GZ};
    }

    @Override
    public String getDescription(Locale locale) {
        return "HDF-EOS products";
    }

    @Override
    public ProfilePartReader createBandPartReader() {
        return new HdfEosBandPart();
    }

    @Override
    public ProfilePartReader createGeoCodingPartReader() {
        return new HdfEosGeocodingPart();
    }

    @Override
    public ProfilePartReader createTimePartReader() {
        return new HdfEosTimePart();
    }

    @Override
    public ProfilePartReader createDescriptionPartReader() {
        return new HdfEosDescriptionPart();
    }

    @Override
    public ProfilePartReader createMetadataPartReader() {
        return new HdfEosMetadataPart();
    }

    private String getGridName(Element eosElement) throws IOException {
        if (eosElement != null) {
            Element gridStructure = eosElement.getChild("GridStructure");
            if (gridStructure != null && gridStructure.getChildren() != null && !gridStructure.getChildren().isEmpty()) {
                Element gridElem = (Element) gridStructure.getChildren().get(0);
                if (gridElem != null) {
                    Element gridNameElem = gridElem.getChild("GridName");
                    if (gridNameElem != null) {
                        return gridNameElem.getText();
                    }
                }
            }
        }
        return null;
    }

}
