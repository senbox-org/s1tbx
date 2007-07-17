/*
 * $Id: RectangleIteratorTest.java,v 1.1 2007/03/27 12:51:05 marcoz Exp $
 *
 * Copyright (C) 2007 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.framework.gpf.support;

import junit.framework.TestCase;

import java.awt.*;

/**
 * Created by marcoz.
 *
 * @author marcoz
 * @version $Revision: 1.1 $ $Date: 2007/03/27 12:51:05 $
 */
public class RectangleIteratorTest extends TestCase {

    public void testIterator() throws Exception {
        RectangleIterator iterator = new RectangleIterator(new Dimension(1, 1), 10, 10);
        assertTrue(iterator.hasNext());
        Rectangle rect = iterator.next();
        assertEquals(0, rect.x);
        assertEquals(0, rect.y);
        assertEquals(10, rect.width);
        assertEquals(1, rect.height);

        assertTrue(iterator.hasNext());
        rect = iterator.next();
        assertEquals(0, rect.x);
        assertEquals(1, rect.y);
        assertEquals(10, rect.width);
        assertEquals(1, rect.height);

        assertTrue(iterator.hasNext());
        rect = iterator.next();
        assertEquals(0, rect.x);
        assertEquals(2, rect.y);
        assertEquals(10, rect.width);
        assertEquals(1, rect.height);
    }

    public void testIterator2() throws Exception {
        RectangleIterator iterator = new RectangleIterator(new Dimension(4, 4), 2000, 10);
        assertTrue(iterator.hasNext());
        Rectangle rect = iterator.next();
        assertEquals(0, rect.x);
        assertEquals(0, rect.y);
        assertEquals(2000, rect.width);
        assertEquals(4, rect.height);

        assertTrue(iterator.hasNext());
        rect = iterator.next();
        assertEquals(0, rect.x);
        assertEquals(4, rect.y);
        assertEquals(2000, rect.width);
        assertEquals(4, rect.height);

        assertTrue(iterator.hasNext());
        rect = iterator.next();
        assertEquals(0, rect.x);
        assertEquals(8, rect.y);
        assertEquals(2000, rect.width);
        assertEquals(2, rect.height);

        assertFalse(iterator.hasNext());
    }
    
    public void testIterator3() throws Exception {
        RectangleIterator iterator = new RectangleIterator(new Dimension(200, 200), 300, 1);
        assertTrue(iterator.hasNext());
        Rectangle rect = iterator.next();
        assertEquals(0, rect.x);
        assertEquals(0, rect.y);
        assertEquals(300, rect.width);
        assertEquals(1, rect.height);
        
        assertFalse(iterator.hasNext());
    }
}
