package org.esa.beam.dataio.netcdf4.convention;

import org.esa.beam.dataio.netcdf4.Nc4FileInfo;

/**
 * A metadata profile.
 * <p/>
 * This interface is NOT intended to be implemented by clients.
 */
public interface Profile {
    void addProfilePart(ProfilePart profilePart);

    void setInitialisationPart(ProfileInitPart initPart);

    void setFileInfo(Nc4FileInfo fileInfo);

    Nc4FileInfo getFileInfo();

    // todo - remove (e.g. prop in ctx)

    void setYFlipped(boolean yFlipped);

    // todo - remove (e.g. prop in ctx)

    boolean isYFlipped();
}
