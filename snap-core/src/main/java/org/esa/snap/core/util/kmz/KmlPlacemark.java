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

import java.awt.geom.Point2D;

public class KmlPlacemark extends KmlFeature {

    private final Point2D position;

    public KmlPlacemark(String name, String description, Point2D position) {
        super("Placemark", name, description);
        this.position = position;
    }

    public Point2D getPosition() {
        return position;
    }

    @Override
    protected void createKmlSpecifics(StringBuilder sb) {
        final Point2D position = getPosition();
        sb.append("<Point>");
        sb.append(String.format("<coordinates>%s,%s,0</coordinates>", position.getX(), position.getY()));
        sb.append("</Point>");
    }
}
