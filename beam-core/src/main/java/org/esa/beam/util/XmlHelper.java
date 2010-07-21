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
package org.esa.beam.util;

import org.esa.beam.dataio.dimap.DimapProductHelpers;
import org.jdom.Element;

import java.awt.Color;

/**
 * @deprecated since BEAM 4.2
 */
@Deprecated
public class XmlHelper {

    @Deprecated
    public static void printColorTag(int indent, Color color, XmlWriter pw) {
        DimapProductHelpers.printColorTag(indent, color, pw);
    }

    @Deprecated
    public static Color createColor(Element colorElem) {
        return DimapProductHelpers.createColor(colorElem);
    }
}
