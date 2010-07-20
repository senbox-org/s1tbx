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
        profile.setInitialisationPart(createInitialisationPart());
        addProfilePart(profile, createMetadataPart());
        addProfilePart(profile, createBandPart());
        addProfilePart(profile, createTiePointGridPart());
        addProfilePart(profile, createFlagCodingPart());
        addProfilePart(profile, createGeocodingPart());
        addProfilePart(profile, createImageInfoPart());
        addProfilePart(profile, createIndexCodingPart());
        addProfilePart(profile, createMaskOverlayPart());
        addProfilePart(profile, createStxPart());
        addProfilePart(profile, createStartTimePart());
        addProfilePart(profile, createEndTimePart());
        addProfilePart(profile, createDescriptionPart());
    }

    protected void addProfilePart(Profile profile, ProfilePart profilePart) {
        if (profilePart != null) {
            profile.addProfilePart(profilePart);
        }
    }

}
