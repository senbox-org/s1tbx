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

package org.esa.snap.core.util.kmz;

import com.bc.ceres.core.Assert;


public abstract class KmlFeature {

    private String kmlElementName;
    private final String name;
    private final String description;
    private ExtendedData extendedData;

    protected KmlFeature(String kmlElementName, String name, String description) {
        Assert.notNull(kmlElementName, "xmlTagName");
        Assert.notNull(name, "name");
        this.kmlElementName = kmlElementName;
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setExtendedData(ExtendedData extendedData) {
        this.extendedData = extendedData;
    }

    public final void createKml(StringBuilder sb) {
        sb.append("<").append(kmlElementName).append(">");
        sb.append("<name>");
        sb.append(getName());
        sb.append("</name>");
        String description = getDescription();
        if (description != null && !description.isEmpty()) {
            sb.append("<description>");
            sb.append(description);
            sb.append("</description>");
        }
        createKmlSpecifics(sb);
        addExtendedData(sb, extendedData);
        sb.append("</").append(kmlElementName).append(">");
    }

    private void addExtendedData(StringBuilder sb, ExtendedData extendedData) {
        if(extendedData != null) {
            extendedData.createKml(sb);
        }
    }

    protected abstract void createKmlSpecifics(StringBuilder sb);

}
