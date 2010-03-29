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
package org.esa.beam.dataio.netcdf4;

import junit.framework.TestCase;
import ucar.nc2.Attribute;

import static org.esa.beam.dataio.netcdf4.Nc4ReaderUtils.*;
import static org.mockito.Mockito.*;

public class Nc4ReaderUtils_hasSameSizes extends TestCase {

    private Attribute a1;
    private Attribute a2;
    private Attribute a3;
    private Attribute a4;

    protected void setUp() throws Exception {
        a1 = mock(Attribute.class);
        a2 = mock(Attribute.class);
        a3 = mock(Attribute.class);
        a4 = mock(Attribute.class);
    }

    protected void tearDown() throws Exception {
    }

    public void testReturnTrue_AllAttributesHaveTheSameSize() {
        when(a1.getLength()).thenReturn(4);
        when(a2.getLength()).thenReturn(4);
        when(a3.getLength()).thenReturn(4);
        when(a4.getLength()).thenReturn(4);
        final Attribute[] attributes = {a1, a2, a3, a4};

        assertEquals(true, allAttributesAreNotNullAndHaveTheSameSize(attributes));
    }

    public void testReturnFalse_FirstAttributeIsNull() {
        when(a2.getLength()).thenReturn(4);
        when(a3.getLength()).thenReturn(4);
        when(a4.getLength()).thenReturn(4);
        final Attribute[] attributes = {null, a2, a3, a4};

        assertEquals(false, allAttributesAreNotNullAndHaveTheSameSize(attributes));
    }

    public void testReturnFalse_OneOfTheAttributesIsNull() {
        when(a1.getLength()).thenReturn(4);
        when(a2.getLength()).thenReturn(4);
        when(a3.getLength()).thenReturn(4);
        when(a4.getLength()).thenReturn(4);
        final Attribute[] attributes = {a1, a2, null, a4};

        assertEquals(false, allAttributesAreNotNullAndHaveTheSameSize(attributes));
    }

    public void testReturnFalse_TheAttributesHaveDiffenentSizes() {
        when(a1.getLength()).thenReturn(4);
        when(a2.getLength()).thenReturn(4);
        when(a3.getLength()).thenReturn(3);
        when(a4.getLength()).thenReturn(4);
        final Attribute[] attributes = {a1, a2, a3, a4};

        assertEquals(false, allAttributesAreNotNullAndHaveTheSameSize(attributes));
    }
}