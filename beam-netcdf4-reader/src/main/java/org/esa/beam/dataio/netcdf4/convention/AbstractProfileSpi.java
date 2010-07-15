package org.esa.beam.dataio.netcdf4.convention;

import org.esa.beam.dataio.netcdf4.Nc4FileInfo;
import ucar.nc2.NetcdfFile;

import java.io.IOException;

/*
 * User: Thomas Storm
 * Date: 26.03.2010
 * Time: 14:48:07
 */

/**
 * Abstract base class for factories which are able to create profiles for NetCDF4-IO. Subclasses allow to
 * dynamically add generic {@link ProfilePart}s to the model.
 */
public abstract class AbstractProfileSpi implements ProfileSpi {

    public AbstractProfileSpi() {
    }

    public abstract ProfileInitPart getInitialisationPart();

    public abstract ProfilePart getMetadataPart();

    public abstract ProfilePart getBandPart();

    public abstract ProfilePart getFlagCodingPart();

    public abstract ProfilePart getGeocodingPart();

    public abstract ProfilePart getImageInfoPart();

    public abstract ProfilePart getIndexCodingPart();

    public abstract ProfilePart getMaskOverlayPart();

    public abstract ProfilePart getStxPart();

    public abstract ProfilePart getTiePointGridPart();

    public abstract ProfilePart getStartTimePart();

    public abstract ProfilePart getEndTimePart();

    public abstract ProfilePart getDescriptionPart();

    protected Nc4FileInfo createFileInfo(NetcdfFile netcdfFile) throws IOException {
        Nc4FileInfo fileInfo = null;
        if (netcdfFile != null) {
            fileInfo = new Nc4FileInfo(netcdfFile);
        }
        return fileInfo;
    }

    @Override
    public void configureProfile(NetcdfFile netcdfFile, Profile profile) throws IOException {
         ProfileInitPart initPart;
         ProfilePart bandPart;
         ProfilePart flagCodingPart;
         ProfilePart geocodingPart;
         ProfilePart imageInfoPart;
         ProfilePart indexCodingPart;
         ProfilePart maskOverlayPart;
         ProfilePart stxPart;
         ProfilePart tiePointGridPart;
         ProfilePart startTimePart;
         ProfilePart endTimePart;
         ProfilePart metaDataPart;
         ProfilePart descriptionPart;

        metaDataPart = getMetadataPart();
        bandPart = getBandPart();
        initPart = getInitialisationPart();
        flagCodingPart = getFlagCodingPart();
        geocodingPart = getGeocodingPart();
        imageInfoPart = getImageInfoPart();
        indexCodingPart = getIndexCodingPart();
        maskOverlayPart = getMaskOverlayPart();
        stxPart = getStxPart();
        tiePointGridPart = getTiePointGridPart();
        startTimePart = getStartTimePart();
        endTimePart = getEndTimePart();
        descriptionPart = getDescriptionPart();

        Nc4FileInfo fileInfo = createFileInfo(netcdfFile);
        if (fileInfo != null) {
            profile.setFileInfo(fileInfo);
        }

        if (initPart != null) {
            profile.setInitialisationPart(initPart);
        }
        if (metaDataPart != null) {
            profile.addProfilePart(metaDataPart);
        }
        if (bandPart != null) {
            profile.addProfilePart(bandPart);
        }
        if (tiePointGridPart != null) {
            profile.addProfilePart(tiePointGridPart);
        }
        if (flagCodingPart != null) {
            profile.addProfilePart(flagCodingPart);
        }
        if (geocodingPart != null) {
            profile.addProfilePart(geocodingPart);
        }
        if (imageInfoPart != null) {
            profile.addProfilePart(imageInfoPart);
        }
        if (indexCodingPart != null) {
            profile.addProfilePart(indexCodingPart);
        }
        if (maskOverlayPart != null) {
            profile.addProfilePart(maskOverlayPart);
        }
        if (stxPart != null) {
            profile.addProfilePart(stxPart);
        }
        if (startTimePart != null) {
            profile.addProfilePart(startTimePart);
        }
        if (endTimePart != null) {
            profile.addProfilePart(endTimePart);
        }
        if (descriptionPart != null) {
            profile.addProfilePart(descriptionPart);
        }
    }

}
