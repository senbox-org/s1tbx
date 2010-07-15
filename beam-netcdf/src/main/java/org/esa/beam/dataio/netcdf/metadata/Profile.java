package org.esa.beam.dataio.netcdf.metadata;

import org.esa.beam.framework.datamodel.Product;

import java.io.IOException;

/**
 * A metadata profile.
 * <p/>
 * This interface is NOT intended to be implemented by clients.
 */
public interface Profile {

    void addProfilePart(ProfilePart profilePart);

    void setInitialisationPart(ProfileInitPart initPart);

    Product readProduct(ProfileReadContext ctx) throws IOException;

    void writeProduct(ProfileWriteContext ctx, Product product) throws IOException;
}
