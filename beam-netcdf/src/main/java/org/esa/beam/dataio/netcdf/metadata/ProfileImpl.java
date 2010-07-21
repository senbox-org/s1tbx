/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
        Assert.notNull(initPart, "initPart");
        this.profileInitPart = initPart;
    }

}
