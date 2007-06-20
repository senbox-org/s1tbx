/*
 * $Id: XmlHelper.java,v 1.1.1.1 2006/09/11 08:16:47 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.util;

import java.awt.Color;

import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.jdom.Element;

//@todo 1 he/** - add (more) class documentation

public class XmlHelper {

    public static void printColorTag(int indent, Color color, XmlWriter pw) {
        if (color == null) {
            return;
        }
        if (pw == null) {
            return;
        }
        final String[][] attributes = new String[4][];
        attributes[0] = new String[]{DimapProductConstants.ATTRIB_RED, String.valueOf(color.getRed())};
        attributes[1] = new String[]{DimapProductConstants.ATTRIB_GREEN, String.valueOf(color.getGreen())};
        attributes[2] = new String[]{DimapProductConstants.ATTRIB_BLUE, String.valueOf(color.getBlue())};
        attributes[3] = new String[]{DimapProductConstants.ATTRIB_ALPHA, String.valueOf(color.getAlpha())};
        pw.printLine(indent, DimapProductConstants.TAG_COLOR, attributes, null);
    }

    public static Color createColor(Element colorElem) {
        int red = Integer.parseInt(colorElem.getAttributeValue(DimapProductConstants.ATTRIB_RED));
        int green = Integer.parseInt(colorElem.getAttributeValue(DimapProductConstants.ATTRIB_GREEN));
        int blue = Integer.parseInt(colorElem.getAttributeValue(DimapProductConstants.ATTRIB_BLUE));
        int alpha = Integer.parseInt(colorElem.getAttributeValue(DimapProductConstants.ATTRIB_ALPHA));
        Color color = new Color(red, green, blue, alpha);
        return color;
    }
}
