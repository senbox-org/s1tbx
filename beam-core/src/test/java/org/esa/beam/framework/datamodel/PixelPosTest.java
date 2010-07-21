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

package org.esa.beam.framework.datamodel;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: Norman
 * Date: 13.01.2006
 * Time: 14:25:48
 * To change this template use File | Settings | File Templates.
 */
public class PixelPosTest extends TestCase {

    public void testInvalidState() {
        PixelPos pixelPos = new PixelPos();
        assertTrue(pixelPos.isValid());
        pixelPos.setInvalid();
        assertFalse(pixelPos.isValid());
    }
}
