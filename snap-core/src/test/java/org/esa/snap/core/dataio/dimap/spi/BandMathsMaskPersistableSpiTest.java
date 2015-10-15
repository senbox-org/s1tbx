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

package org.esa.snap.core.dataio.dimap.spi;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.datamodel.Mask;
import org.jdom.Element;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class BandMathsMaskPersistableSpiTest {

    private DimapPersistableSpi persistableSpi;

    @Before
    public void setup() {
        persistableSpi = new BandMathsMaskPersistableSpi();
    }

    @Test
    public void canPersistIntendedMaskType() {
        final Mask mask = new Mask("M", 10, 10, Mask.BandMathsType.INSTANCE);
        assertTrue(persistableSpi.canPersist(mask));
        assertTrue(DimapPersistence.getPersistable(mask) instanceof BandMathsMaskPersistable);
    }

    @Test
    public void canDecodeIntendedElement() {
        final Element element = new Element(DimapProductConstants.TAG_MASK);
        element.setAttribute(DimapProductConstants.ATTRIB_TYPE, "Maths");
        assertTrue(persistableSpi.canDecode(element));
        assertTrue(DimapPersistence.getPersistable(element) instanceof BandMathsMaskPersistable);
    }

    @Test
    public void cannotPersistOtherMaskType() {
        final Mask mask = new Mask("M", 10, 10, new Mask.ImageType("Other") {
            @Override
            public MultiLevelImage createImage(Mask mask) {
                return null;
            }
        });
        assertFalse(persistableSpi.canPersist(mask));
        assertFalse(DimapPersistence.getPersistable(mask) instanceof BandMathsMaskPersistable);
    }

    @Test
    public void cannotDecodeOtherElement() {
        final Element element = new Element(DimapProductConstants.TAG_MASK);
        element.setAttribute(DimapProductConstants.ATTRIB_TYPE, "Other");
        assertFalse(persistableSpi.canDecode(element));
        assertFalse(DimapPersistence.getPersistable(element) instanceof BandMathsMaskPersistable);
    }
}
