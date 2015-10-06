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

import java.awt.image.RenderedImage;

public abstract class KmlOverlay extends KmlFeature {

    private final RenderedImage overlay;
    private String iconName;
    private static final String ICON_EXTENSION = ".png";

    protected KmlOverlay(String kmlElementName, String name, RenderedImage overlay) {
        super(kmlElementName, name, null);
        this.overlay = overlay;
        iconName = name;
    }

    public RenderedImage getOverlay() {
        return overlay;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public String getIconFileName() {
        return getIconName() + ICON_EXTENSION;
    }

    @Override
    protected void createKmlSpecifics(StringBuilder sb) {
        sb.append("<Icon>");
        sb.append("<href>").append(getIconFileName()).append("</href>");
        sb.append("</Icon>");
    }
}
