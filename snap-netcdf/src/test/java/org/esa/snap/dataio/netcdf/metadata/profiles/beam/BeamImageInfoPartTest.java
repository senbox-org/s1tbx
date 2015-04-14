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
package org.esa.snap.dataio.netcdf.metadata.profiles.beam;

import junit.framework.TestCase;
import ucar.nc2.Attribute;

import static org.mockito.Mockito.*;

public class BeamImageInfoPartTest extends TestCase {

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

        assertEquals(true, BeamImageInfoPart.allAttributesAreNotNullAndHaveTheSameSize(attributes));
    }

    public void testReturnFalse_FirstAttributeIsNull() {
        when(a2.getLength()).thenReturn(4);
        when(a3.getLength()).thenReturn(4);
        when(a4.getLength()).thenReturn(4);
        final Attribute[] attributes = {null, a2, a3, a4};

        assertEquals(false, BeamImageInfoPart.allAttributesAreNotNullAndHaveTheSameSize(attributes));
    }

    public void testReturnFalse_OneOfTheAttributesIsNull() {
        when(a1.getLength()).thenReturn(4);
        when(a2.getLength()).thenReturn(4);
        when(a3.getLength()).thenReturn(4);
        when(a4.getLength()).thenReturn(4);
        final Attribute[] attributes = {a1, a2, null, a4};

        assertEquals(false, BeamImageInfoPart.allAttributesAreNotNullAndHaveTheSameSize(attributes));
    }

    public void testReturnFalse_TheAttributesHaveDiffenentSizes() {
        when(a1.getLength()).thenReturn(4);
        when(a2.getLength()).thenReturn(4);
        when(a3.getLength()).thenReturn(3);
        when(a4.getLength()).thenReturn(4);
        final Attribute[] attributes = {a1, a2, a3, a4};

        assertEquals(false, BeamImageInfoPart.allAttributesAreNotNullAndHaveTheSameSize(attributes));
    }
}
