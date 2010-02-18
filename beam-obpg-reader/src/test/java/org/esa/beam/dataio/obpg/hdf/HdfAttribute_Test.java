/*
 * $Id$
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
package org.esa.beam.dataio.obpg.hdf;

import junit.framework.TestCase;

public class HdfAttribute_Test extends TestCase {

    public void testFail() {
        final int type = 5;
        final int elemCount = 6;

        final HdfAttribute attribute = new HdfAttribute("name", type, "abcdef", elemCount);

        assertEquals("name", attribute.getName());
        assertEquals(type, attribute.getHdfType());
        assertEquals("abcdef", attribute.getStringValue());
        assertEquals(elemCount, attribute.getElemCount());
    }
}
