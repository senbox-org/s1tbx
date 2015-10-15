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

package org.esa.snap.core.datamodel;

public class GcpDescriptor extends PointPlacemarkDescriptor {

    public static GcpDescriptor getInstance() {
        return (GcpDescriptor) PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptor(
                GcpDescriptor.class.getName());
    }

    public GcpDescriptor() {
        super("org.esa.snap.GroundControlPoint");
    }

    /**
     * @deprecated since 4.10
     */
    @Override
    @Deprecated
    public String getShowLayerCommandId() {
        return "showGcpOverlay";
    }

    /**
     * @deprecated since 4.10
     */
    @Override
    @Deprecated
    public String getRoleName() {
        return "gcp";
    }

    /**
     * @deprecated since 4.10
     */
    @Override
    @Deprecated
    public String getRoleLabel() {
        return "GCP";
    }

    /**
     * @deprecated since 4.10
     */
    @Override
    @Deprecated
    public PlacemarkGroup getPlacemarkGroup(Product product) {
        return product.getGcpGroup();
    }
}
