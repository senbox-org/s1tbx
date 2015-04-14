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

package org.esa.snap.binning.support;


import org.junit.Test;

import static org.junit.Assert.*;

public class VectorImplTest {

    @Test
    public void testToString() {
        VectorImpl v = new VectorImpl(new float[]{2.1F, 1.9F, 3.4F, 5.6F});
        assertEquals("[2.1, 1.9, 3.4, 5.6]", v.toString());
        v.setOffsetAndSize(0, 0);
        assertEquals("[]", v.toString());
        v.setOffsetAndSize(0, 3);
        assertEquals("[2.1, 1.9, 3.4]", v.toString());
        v.setOffsetAndSize(2, 2);
        assertEquals("[3.4, 5.6]", v.toString());
    }
}
