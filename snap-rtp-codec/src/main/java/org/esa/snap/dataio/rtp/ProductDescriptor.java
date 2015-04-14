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

package org.esa.snap.dataio.rtp;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("product")
class ProductDescriptor {
    private String name;
    private String type;
    private String description;
    private int width;
    private int height;
    @XStreamAlias("bands")
    private BandDescriptor[] bandDescriptors;

    ProductDescriptor() {
    }

    ProductDescriptor(String name, String type, int width, int height, BandDescriptor[] bandDescriptors, String description) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.width = width;
        this.height = height;
        this.bandDescriptors = bandDescriptors;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public BandDescriptor[] getBandDescriptors() {
        return bandDescriptors;
    }

    public String getDescription() {
        return description;
    }
}
