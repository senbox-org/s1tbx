package org.esa.beam.dataio.netcdf4.convention;

import com.bc.ceres.core.Assert;
import org.esa.beam.dataio.netcdf4.Nc4FileInfo;
import org.esa.beam.framework.datamodel.Product;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * No API.
 */
public final class ProfileImpl implements Profile {

    private ProfileInitPart profileInitPart;
    private final List<ProfilePart> profileParts = new ArrayList<ProfilePart>();
    private Nc4FileInfo fileInfo;
    private boolean yFlipped;


    public Product readProduct(final String productName) throws IOException {
        final Product product = profileInitPart.readProductBody(productName, fileInfo);
        for (ProfilePart profilePart : profileParts) {
            profilePart.read(this, product);
        }
        return product;
    }

    public void writeProduct(final NetcdfFileWriteable writeable, final Product product) throws IOException {
        profileInitPart.writeProductBody(writeable, product);
        for (ProfilePart profilePart : profileParts) {
            profilePart.define(this, product, writeable);
        }
        writeable.create();
        for (ProfilePart profilePart : profileParts) {
            profilePart.write(this, product, writeable);
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

    @Override
    public void setFileInfo(Nc4FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    @Override
    public Nc4FileInfo getFileInfo() {
        return fileInfo;
    }

    // todo - remove (e.g. prop in ctx)
    @Override
    public void setYFlipped(boolean yFlipped) {
        this.yFlipped = yFlipped;
    }

    // todo - remove (e.g. prop in ctx)
    @Override
    public boolean isYFlipped() {
        return yFlipped;
    }
}
