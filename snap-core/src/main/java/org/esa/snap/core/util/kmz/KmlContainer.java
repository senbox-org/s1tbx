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

import java.util.ArrayList;
import java.util.List;

public class KmlContainer extends KmlFeature {

    private final List<KmlFeature> children;

    protected KmlContainer(String kmlElementName, String name, String description) {
        super(kmlElementName, name, description);
        this.children = new ArrayList<KmlFeature>();
    }

    public List<KmlFeature> getChildren() {
        return children;
    }

    public void addChild(final KmlFeature child) {
        children.add(child);
    }

    @Override
    protected void createKmlSpecifics(StringBuilder sb) {
        for (KmlFeature container : getChildren()) {
            container.createKml(sb);
        }
    }
}
