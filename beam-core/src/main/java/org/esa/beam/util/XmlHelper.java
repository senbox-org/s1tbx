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
