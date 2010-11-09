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

package org.esa.beam.dataio.netcdf;

import org.esa.beam.dataio.netcdf.metadata.ProfileInitPartReader;
import org.esa.beam.dataio.netcdf.metadata.ProfilePartReader;
import org.esa.beam.framework.datamodel.Product;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A NetCdf metadata profile.
 * <p/>
 * This interface is NOT intended to be implemented by clients.
 */
class NetCdfReadProfile {

    private ProfileInitPartReader profileInitPart;
    private final List<ProfilePartReader> profileParts = new ArrayList<ProfilePartReader>();

    public void setInitialisationPartReader(ProfileInitPartReader initPart) {
        this.profileInitPart = initPart;
    }

    public void addProfilePartReader(ProfilePartReader profilePart) {
        this.profileParts.add(profilePart);
    }

    public Product readProduct(final ProfileReadContext ctx) throws IOException {

        final Product product = profileInitPart.readProductBody(ctx);
        for (ProfilePartReader profilePart : profileParts) {
            profilePart.preDecode(ctx, product);
        }
        for (ProfilePartReader profilePart : profileParts) {
            profilePart.decode(ctx, product);
        }
        return product;
    }
}
