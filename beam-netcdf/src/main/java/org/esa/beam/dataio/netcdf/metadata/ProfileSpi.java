package org.esa.beam.dataio.netcdf.metadata;

import org.esa.beam.framework.dataio.DecodeQualification;
import ucar.nc2.NetcdfFile;

import java.io.IOException;

/**
 * The service provider interface (SPI) for a specific NetCDF metadata profile
 * capable of representing the BEAM product data model.
 * <p/>
 * This interface is intended to be implemented by clients.
 * But its should not be implemented directly. Instead, clients are asked to
 * derive from {@link AbstractProfileSpi}.
 */
public interface ProfileSpi {

    /**
     * Configures a metadata profile for the given NetcDF file.
     *
     * @param netcdfFile The NetcDF file.
     * @param profile    The profile.
     *
     * @throws IOException If an I/O error occurs.
     */
    void configureProfile(NetcdfFile netcdfFile, Profile profile) throws IOException;

    /**
     * Detects whether a profile can be generated for the given NetCDF file.
     *
     * @param netcdfFile The NetcDF file.
     *
     * @return A decode qualification.
     */
    DecodeQualification getDecodeQualification(NetcdfFile netcdfFile);

    ProfileReadContext createReadContext(NetcdfFile netcdfFile) throws IOException;
}
