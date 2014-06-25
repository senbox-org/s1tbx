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
package org.esa.beam.dataio.avhrr.noaa;

import java.util.HashMap;
import java.util.Map;

public class FormatMetadata {

    private String type;
    private String description;
    private String units;
    private double scalingFactor = 1.0;
    private Map<Object, String> itemMap;

    public FormatMetadata setType(String typeString) {
        this.type = typeString;
        return this;
    }
    
    public String getType() {
        return type != null ? type : "";
    }

    public FormatMetadata setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getDescription() {
        return description != null ? description : "";
    }
    
    public FormatMetadata setUnits(String units) {
        this.units = units;
        return this;
    }
    
    public String getUnits() {
        return units != null ? units : "";
    }

    public FormatMetadata setScalingFactor(double scalingFactor) {
        this.scalingFactor = scalingFactor;
        return this;
    }
    
    public double getScalingFactor() {
        return scalingFactor;
    }

    public FormatMetadata addItem(Object key, String value) {
        if (itemMap == null) {
            itemMap = new HashMap<Object, String>();
        }
        itemMap.put(key, value);
        return this;
    }

    public Map<Object, String> getItemMap() {
        return itemMap;
    }
}
