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

@XStreamAlias("band")
class BandDescriptor {
    private String name;
    private String dataType;
    private String description;
    private double scalingOffset;
    private double scalingFactor;
    private String expression;

    BandDescriptor() {
    }

    BandDescriptor(String name, String dataType, double scalingOffset, double scalingFactor, String expression, String description) {
        this.name = name;
        this.dataType = dataType;
        this.description = description;
        this.scalingOffset = scalingOffset;
        this.scalingFactor = scalingFactor;
        this.expression = expression;
    }

    public String getName() {
        return name;
    }

    public String getDataType() {
        return dataType;
    }

    public String getDescription() {
        return description;
    }

    public double getScalingOffset() {
        return scalingOffset;
    }

    public double getScalingFactor() {
        return scalingFactor;
    }

    public String getExpression() {
        return expression;
    }
}
