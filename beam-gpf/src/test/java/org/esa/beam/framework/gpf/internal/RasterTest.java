/*
 * $Id: $
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
package org.esa.beam.framework.gpf.internal;

import java.awt.Rectangle;

import junit.framework.TestCase;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Tile;

/**
 * Created by marcoz.
 *
 * @author marcoz
 * @version $Revision: $ $Date: $
 */
public class RasterTest extends TestCase {
	
	private int[] elems = {1,2,3,4,5,6,7,8};
	private ProductData dataBuffer;
	private Band band;
	
	@Override
	protected void setUp() throws Exception {
		dataBuffer = ProductData.createInstance(elems);
		band = new Band("name", ProductData.TYPE_INT16, 1, 1);
	}
	
	public void testRectangleAt00() throws Exception {
		Rectangle rectangle = new Rectangle(0, 0, 4, 2);
		Tile tile = new RasterImpl(band, rectangle, dataBuffer);
		
		assertEquals("width", 4, tile.getWidth());
		assertEquals("height", 2, tile.getHeight());
		assertEquals("0,0", 1, tile.getSampleInt(0, 0));
		assertEquals("3,1", 8, tile.getSampleInt(3, 1));
		try {
			tile.getSampleInt(3, 2);	
		} catch (ArrayIndexOutOfBoundsException e) {
			assertEquals("11", e.getMessage());
		}
	}
	
	public void testRectangleAt55() throws Exception {
		Rectangle rectangle = new Rectangle(5, 5, 4, 2);
		Tile tile = new RasterImpl(band, rectangle, dataBuffer);
		
		assertEquals("width", 4, tile.getWidth());
		assertEquals("height", 2, tile.getHeight());
		assertEquals("5,5", 1, tile.getSampleInt(5, 5));
		assertEquals("8,6", 8, tile.getSampleInt(8, 6));
		try {
			tile.getSampleInt(3, 2);	
		} catch (ArrayIndexOutOfBoundsException e) {
			assertEquals("-14", e.getMessage());
		}
	}

}
