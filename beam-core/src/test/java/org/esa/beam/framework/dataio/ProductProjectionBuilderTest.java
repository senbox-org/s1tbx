/*
 * $Id: ProductProjectionBuilderTest.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
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
package org.esa.beam.framework.dataio;

import java.io.IOException;

import junit.framework.TestCase;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.MapProjection;
import org.esa.beam.framework.dataop.maptransf.MapTransform;
import org.esa.beam.framework.dataop.maptransf.MapTransformFactory;

public class ProductProjectionBuilderTest extends TestCase {

    public void testEnsureThatSubsetInfoAllwaysNull() {

        MapTransform transform = MapTransformFactory.createTransform("Affine", new double[]{1, 2, 3, 4, 5, 6});
        MapProjection projection = new MapProjection("mp", transform);
        MapInfo mapInfo = new MapInfo(projection, 3, 4, 5, 6, 7, 8, Datum.WGS_84);
        mapInfo.setSceneWidth(40);
        mapInfo.setSceneHeight(50);
        ProductProjectionBuilder projectionBuilder = new ProductProjectionBuilder(mapInfo);

        Product product = new Product("p", "t", 3, 3);
        TiePointGrid t1 = new TiePointGrid("t1", 3, 3, 0, 0, 1, 1, new float[]{0.6f, 0.3f, 0.4f, 0.8f, 0.9f, 0.4f, 0.3f, 0.2f, 0.4f});
        product.addTiePointGrid(t1);
        TiePointGrid t2 = new TiePointGrid("t2", 3, 3, 0, 0, 1, 1, new float[]{0.9f, 0.2f, 0.3f, 0.6f, 0.1f, 0.4f, 0.2f, 0.9f, 0.5f});
        product.addTiePointGrid(t2);
        product.setGeoCoding(new TiePointGeoCoding(t1, t2, Datum.WGS_84));

        try {
            ProductSubsetDef subsetDef = new ProductSubsetDef();
            assertNotNull(subsetDef);
            Product product2 = projectionBuilder.readProductNodes(product, subsetDef);
            assertEquals(product.getName(), product2.getName());
            assertNull(projectionBuilder.getSubsetDef());
            assertNotNull(subsetDef);
            projectionBuilder.setSubsetDef(subsetDef);
            assertNull(projectionBuilder.getSubsetDef());
        } catch (IOException e) {
            fail("IOException not expected");
        }
    }
}
