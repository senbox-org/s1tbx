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

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.ProductData;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 * @since BEAM 4.7
 */
public class RangeTypeMaskPersistableSpiTest {
    private RangeTypeMaskPersistableSpi _persistableSpi;

    @Before
    public void setUp() {
        _persistableSpi = new RangeTypeMaskPersistableSpi();
    }

    @After
    public void tearDown() throws Exception {
        _persistableSpi = null;
    }

    @Test
    public void testCanDecode_GoodElement() throws JDOMException, IOException {
        final InputStream resourceStream = getClass().getResourceAsStream("RangeMask.xml");
        final Document document = new SAXBuilder().build(resourceStream);

        assertTrue(_persistableSpi.canDecode(document.getRootElement()));
    }

    @Test
    public void testCanDecode_NotDecodeableElement() {

        final Element element = new Element("SomeWhat");

        assertFalse(_persistableSpi.canDecode(element));
    }

    @Test
    public void testCanPersist() {
        final Mask mask = new Mask("b", 2, 2, Mask.RangeType.INSTANCE);

        assertTrue(_persistableSpi.canPersist(mask));

        assertFalse(_persistableSpi.canPersist(new ArrayList()));
        assertFalse(_persistableSpi.canPersist(new Object()));
        assertFalse(_persistableSpi.canPersist(new Band("b", ProductData.TYPE_INT8, 2, 2)));
    }

    @Test
    public void testCreatePersistable() {
        assertNotNull(_persistableSpi.createPersistable());
    }


}
