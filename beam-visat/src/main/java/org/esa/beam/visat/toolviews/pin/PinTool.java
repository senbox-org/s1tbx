/*
 * $Id: PinTool.java,v 1.1 2007/04/19 10:41:38 norman Exp $
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
package org.esa.beam.visat.toolviews.pin;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.ui.tool.ToolInputEvent;

/**
 * A tool used to create (single click), select (single click on a pin) or edit (double click on a pin) the pins
 * displayed in product scene view.
 */
public class PinTool extends PlacemarkTool {

    public PinTool() {
        super(PinDescriptor.INSTANCE);
    }

    @Override
    public void mouseReleased(ToolInputEvent e) {
        if (getDraggedPlacemark() != null && e.isPixelPosValid()) {
            GeoCoding geoCoding = getProductSceneView().getRaster().getGeoCoding();
            if (geoCoding != null && geoCoding.canGetGeoPos()) {
                getDraggedPlacemark().setGeoPos(geoCoding.getGeoPos(getDraggedPlacemark().getPixelPos(), null));
            }
            setDraggedPlacemark(null);
        }
    }
}
