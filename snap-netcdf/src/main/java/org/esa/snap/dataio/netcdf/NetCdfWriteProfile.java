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

package org.esa.snap.dataio.netcdf;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.netcdf.metadata.ProfileInitPartWriter;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A NetCdf metadata profile.
 * <p>
 * This interface is NOT intended to be implemented by clients.
 */
class NetCdfWriteProfile {

    private ProfileInitPartWriter profileInitPart;
    private final List<ProfilePartWriter> profileParts = new ArrayList<ProfilePartWriter>();

    public void setInitialisationPartWriter(ProfileInitPartWriter initPart) {
        this.profileInitPart = initPart;
    }

    public void addProfilePartWriter(ProfilePartWriter profilePart) {
        this.profileParts.add(profilePart);
    }

    public void writeProduct(final ProfileWriteContext ctx, final Product product) throws IOException {
        profileInitPart.writeProductBody(ctx, product);
        for (ProfilePartWriter profilePart : profileParts) {
            profilePart.preEncode(ctx, product);
        }
        ctx.getNetcdfFileWriteable().create();
        for (ProfilePartWriter profilePart : profileParts) {
            profilePart.encode(ctx, product);
        }
    }
}
