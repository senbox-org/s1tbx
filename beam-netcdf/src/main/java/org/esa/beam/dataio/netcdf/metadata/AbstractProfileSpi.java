package org.esa.beam.dataio.netcdf.metadata;

import org.esa.beam.dataio.netcdf.util.RasterDigest;
import org.esa.beam.dataio.netcdf.util.VariableMap;
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

    public abstract ProfileInitPart createInitialisationPart();

    public abstract ProfilePart createMetadataPart();

    public abstract ProfilePart createBandPart();

    public abstract ProfilePart createFlagCodingPart();

    public abstract ProfilePart createGeocodingPart();

    public abstract ProfilePart createImageInfoPart();

    public abstract ProfilePart createIndexCodingPart();

    public abstract ProfilePart createMaskOverlayPart();

    public abstract ProfilePart createStxPart();

    public abstract ProfilePart createTiePointGridPart();

    public abstract ProfilePart createStartTimePart();

    public abstract ProfilePart createEndTimePart();

    public abstract ProfilePart createDescriptionPart();

    @Override
    public ProfileReadContext createReadContext(NetcdfFile netcdfFile) throws IOException {
        final RasterDigest rasterDigest = RasterDigest.createRasterDigest(netcdfFile.getRootGroup());
        final VariableMap variableMap = rasterDigest != null ? new VariableMap(
                rasterDigest.getRasterVariables()) : null;
        return new ProfileReadContextImpl(netcdfFile, rasterDigest, variableMap);
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

        metaDataPart = createMetadataPart();
        bandPart = createBandPart();
        initPart = createInitialisationPart();
        flagCodingPart = createFlagCodingPart();
        geocodingPart = createGeocodingPart();
        imageInfoPart = createImageInfoPart();
        indexCodingPart = createIndexCodingPart();
        maskOverlayPart = createMaskOverlayPart();
        stxPart = createStxPart();
        tiePointGridPart = createTiePointGridPart();
        startTimePart = createStartTimePart();
        endTimePart = createEndTimePart();
        descriptionPart = createDescriptionPart();

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
