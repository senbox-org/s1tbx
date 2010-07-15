package org.esa.beam.dataio.netcdf.metadata;

import org.esa.beam.dataio.netcdf.util.FileInfo;

/**
 * A metadata profile.
 * <p/>
 * This interface is NOT intended to be implemented by clients.
 */
public interface Profile {
    void addProfilePart(ProfilePart profilePart);

    void setInitialisationPart(ProfileInitPart initPart);

    void setFileInfo(FileInfo fileInfo);

    FileInfo getFileInfo();

    // todo - remove (e.g. prop in ctx)

    void setYFlipped(boolean yFlipped);

    // todo - remove (e.g. prop in ctx)

    boolean isYFlipped();
}
