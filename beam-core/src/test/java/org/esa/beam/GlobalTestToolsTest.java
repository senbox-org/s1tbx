/*
 * $Id: GlobalTestToolsTest.java,v 1.1.1.1 2006/09/11 08:16:50 norman Exp $
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
package org.esa.beam;

import junit.framework.TestCase;

public class GlobalTestToolsTest extends TestCase {


    public void testCreateBytes() {
        final byte[] bytes = GlobalTestTools.createBytes(270);

        assertNotNull(bytes);
        assertEquals(270, bytes.length);

        assertEquals(0, bytes[0]);
        assertEquals(1, bytes[1]);
        assertEquals(2, bytes[2]);

        assertEquals(125, bytes[125]);
        assertEquals(126, bytes[126]);
        assertEquals(127, bytes[127]);
        assertEquals(-128, bytes[128]);
        assertEquals(-127, bytes[129]);
        assertEquals(-126, bytes[130]);

        assertEquals(-3, bytes[253]);
        assertEquals(-2, bytes[254]);
        assertEquals(-1, bytes[255]);
        assertEquals(0, bytes[256]);
        assertEquals(1, bytes[257]);
        assertEquals(2, bytes[258]);

        assertEquals(11, bytes[267]);
        assertEquals(12, bytes[268]);
        assertEquals(13, bytes[269]);
    }
}
