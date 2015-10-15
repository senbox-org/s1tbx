package org.esa.snap.dataio.netcdf.metadata.profiles.hdfeos;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductIOException;
import org.esa.snap.dataio.netcdf.AbstractNetCdfReaderPlugIn;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.metadata.ProfileInitPartReader;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartReader;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfInitialisationPart;
import org.esa.snap.dataio.netcdf.util.Constants;
import org.esa.snap.dataio.netcdf.util.RasterDigest;
import org.jdom2.Element;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HdfEosNetCdfReaderPlugIn extends AbstractNetCdfReaderPlugIn {

    @Override
    protected DecodeQualification getDecodeQualification(NetcdfFile netcdfFile) {
        try {
            Element eosElement = HdfEosUtils.getEosElement(HdfEosUtils.STRUCT_METADATA, netcdfFile.getRootGroup());
            // check for GRID
            if (!HdfEosGridInfo.createGridInfos(eosElement).isEmpty()) {
                return DecodeQualification.INTENDED;
            }
        } catch (Exception ignore) {
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    protected void initReadContext(ProfileReadContext ctx) throws IOException {
        Group eosGroup = ctx.getNetcdfFile().getRootGroup();
        Element eosElement = HdfEosUtils.getEosElement(HdfEosUtils.STRUCT_METADATA, eosGroup);
        List<HdfEosGridInfo> gridInfos = HdfEosGridInfo.createGridInfos(eosElement);
        List<HdfEosGridInfo> compatibleGridInfos = HdfEosGridInfo.getCompatibleGridInfos(gridInfos);
        if (compatibleGridInfos.isEmpty()) {
            throw new ProductIOException("Could not find grids.");
        }
        List<Group> gridGroups = new ArrayList<Group>();
        for (HdfEosGridInfo gridInfo : compatibleGridInfos) {
            gridGroups.add(HdfEosUtils.findGroupNested(eosGroup, gridInfo.gridName));
        }
        if (gridGroups.isEmpty()) {
            throw new ProductIOException("Could not find grid group.");
        }
        Group[] groups = gridGroups.toArray(new Group[gridGroups.size()]);
        RasterDigest rasterDigest = RasterDigest.createRasterDigest(groups);
        ctx.setRasterDigest(rasterDigest);
        ctx.setProperty(HdfEosUtils.STRUCT_METADATA, eosElement);
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

}
