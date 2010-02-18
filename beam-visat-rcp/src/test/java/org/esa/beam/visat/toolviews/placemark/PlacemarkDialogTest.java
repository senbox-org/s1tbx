/*
 * $Id: PlacemarkDialogTest.java,v 1.1 2007/04/19 10:41:39 norman Exp $
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
package org.esa.beam.visat.toolviews.placemark;

import junit.framework.TestCase;

import org.esa.beam.framework.datamodel.PlacemarkSymbol;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.visat.toolviews.placemark.PlacemarkDialog;

import java.awt.HeadlessException;

public class PlacemarkDialogTest extends TestCase {

    public void test() {
        try {
            PlacemarkDialog pinDialog = new PlacemarkDialog(null, new Product("x", "y", 10, 10), PinDescriptor.INSTANCE, false);

            pinDialog.setDescription("descrip");
            assertEquals("descrip", pinDialog.getDescription());

            pinDialog.setLat(3.6f);
            assertEquals(3.6f, pinDialog.getLat(), 1e-15);

            pinDialog.setLon(5.7f);
            assertEquals(5.7f, pinDialog.getLon(), 1e-15);

            GeoPos geoPos = pinDialog.getGeoPos();
            assertNotNull(geoPos);
            assertEquals(3.6f, geoPos.lat, 1e-6f);
            assertEquals(5.7f, geoPos.lon, 1e-6f);

            pinDialog.setName("name");
            assertEquals("name", pinDialog.getName());

            pinDialog.setLabel("label");
            assertEquals("label", pinDialog.getLabel());

            pinDialog.setPixelX(2.3F);
            assertEquals(2.3F, pinDialog.getPixelX(), 1e-6F);

            pinDialog.setPixelY(14.1F);
            assertEquals(14.1F, pinDialog.getPixelY(), 1e-6F);

            PixelPos pixelPos = pinDialog.getPixelPos();
            assertNotNull(pixelPos);
            assertEquals(2.3F, pixelPos.x, 1e-6F);
            assertEquals(14.1F, pixelPos.y, 1e-6F);

            assertNotNull(pinDialog.getPlacemarkSymbol());
            final PlacemarkSymbol defaultPlacemarkSymbol = PlacemarkSymbol.createDefaultPinSymbol();
            pinDialog.setPlacemarkSymbol(defaultPlacemarkSymbol);
            assertSame(defaultPlacemarkSymbol, pinDialog.getPlacemarkSymbol());
        } catch (HeadlessException e) {
            System.out.println("A " + PlacemarkDialogTest.class + " test has not been performed: HeadlessException");
        }
    }
}
