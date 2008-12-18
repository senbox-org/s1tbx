/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
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
package org.esa.beam.dataio.smos;

import junit.framework.TestCase;

/**
 * Tests logical expression for accepting the polarisation mode for calculating
 * browse data.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class PolarisationModeAcceptanceTest extends TestCase {

    public void testAcceptance() {
        assertTrue(accept(SmosFormats.L1C_POL_MODE_X, 0));
        assertTrue(accept(SmosFormats.L1C_POL_MODE_Y, 1));
        assertTrue(accept(SmosFormats.L1C_POL_MODE_XY1, 2));
        assertTrue(accept(SmosFormats.L1C_POL_MODE_XY1, 2));
        assertTrue(accept(SmosFormats.L1C_POL_MODE_XY2, 2));
        assertTrue(accept(SmosFormats.L1C_POL_MODE_XY2, 3));

        assertFalse(accept(SmosFormats.L1C_POL_MODE_X, 1));
        assertFalse(accept(SmosFormats.L1C_POL_MODE_Y, 0));
        assertFalse(accept(SmosFormats.L1C_POL_MODE_XY1, 0));
        assertFalse(accept(SmosFormats.L1C_POL_MODE_XY1, 1));
        assertFalse(accept(SmosFormats.L1C_POL_MODE_XY2, 0));
        assertFalse(accept(SmosFormats.L1C_POL_MODE_XY2, 1));
    }

    private boolean accept(int polMode, int flags) {
        return polMode == (flags & 3) || (polMode & flags & 2) != 0;
    }
}
