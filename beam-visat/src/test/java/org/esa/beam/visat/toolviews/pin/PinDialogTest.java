/*
 * $Id: PinDialogTest.java,v 1.1 2007/04/19 10:41:39 norman Exp $
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

import junit.framework.TestCase;

import org.esa.beam.framework.datamodel.PinSymbol;

public class PinDialogTest extends TestCase {

    public void test() {
        PinDialog pinDialog = new PinDialog(null, null);

        pinDialog.setDescription("descrip");
        assertEquals("descrip", pinDialog.getDescription());

        pinDialog.setLat(3.6f);
        assertEquals(3.6f, pinDialog.getLat(), 1e-15);

        pinDialog.setLon(5.7f);
        assertEquals(5.7f, pinDialog.getLon(), 1e-15);

        pinDialog.setName("name");
        assertEquals("name", pinDialog.getName());

        pinDialog.setLabel("label");
        assertEquals("label", pinDialog.getLabel());

        pinDialog.setPixelX(2);
        assertEquals(2.5f, pinDialog.getPixelX(), 1e-15);

        pinDialog.setPixelY(14);
        assertEquals(14.5f, pinDialog.getPixelY(), 1e-15);

        assertNotNull(pinDialog.getPinSymbol());
        final PinSymbol defaultPinSymbol = PinSymbol.createDefaultPinSymbol();
        pinDialog.setPinSymbol(defaultPinSymbol);
        assertSame(defaultPinSymbol, pinDialog.getPinSymbol());
    }
}
