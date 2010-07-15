package org.esa.beam.dataio.netcdf.metadata;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.Product;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * No API.
 */
public final class ProfileImpl implements Profile {

    private ProfileInitPart profileInitPart;
    private final List<ProfilePart> profileParts = new ArrayList<ProfilePart>();


    @Override
    public Product readProduct(final ProfileReadContext ctx) throws IOException {
        final Product product = profileInitPart.readProductBody(ctx);
        for (ProfilePart profilePart : profileParts) {
            profilePart.read(ctx, product);
        }
        return product;
    }

    @Override
    public void writeProduct(final ProfileWriteContext ctx, final Product product) throws IOException {
        profileInitPart.writeProductBody(ctx.getNetcdfFileWriteable(), product);
        for (ProfilePart profilePart : profileParts) {
            profilePart.define(ctx, product);
        }
        ctx.getNetcdfFileWriteable().create();
        for (ProfilePart profilePart : profileParts) {
            profilePart.write(ctx, product);
        }
    }

    @Override
    public void addProfilePart(ProfilePart profilePart) {
        Assert.notNull(profilePart, "profilePart");
        this.profileParts.add(profilePart);
    }

    @Override
    public void setInitialisationPart(ProfileInitPart initPart) {
        this.profileInitPart = initPart;
    }

}
