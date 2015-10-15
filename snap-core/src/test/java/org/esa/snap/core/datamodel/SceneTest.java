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

package org.esa.snap.core.datamodel;


import junit.framework.TestCase;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.util.math.FXYSum;

public class SceneTest extends TestCase {

    private Product _srcProduct;
    private Band _srcBand1;
    private Band _srcBand2;
    private Product _destProduct;
    private Band _destBand1;
    private Band _destBand2;
    private ProductSubsetDef _subsetDef;

    @Override
    public void setUp() throws Exception {
        _srcProduct = new Product("srcProduct", "pType", 100, 200);
        _srcBand1 = _srcProduct.addBand("Band1", ProductData.TYPE_INT8);
        _srcBand2 = _srcProduct.addBand("Band2", ProductData.TYPE_INT32);

        _destProduct = new Product("destProduct", "pType", 25, 50);
        _destBand1 = _destProduct.addBand("Band1", ProductData.TYPE_INT8);
        _destBand2 = _destProduct.addBand("Band2", ProductData.TYPE_INT32);

        _subsetDef = new ProductSubsetDef("subset");
        _subsetDef.setRegion(50, 100, 50, 100);
        _subsetDef.setSubSampling(2, 2);
    }

    public void testTransferGCFromProductToProduct() {

        final AbstractGeoCoding geoCoding = createFXYSumGeoCoding();
        _srcProduct.setSceneGeoCoding(geoCoding);
        final Scene srcScene = SceneFactory.createScene(_srcProduct);
        final Scene destScene = SceneFactory.createScene(_destProduct);
        srcScene.transferGeoCodingTo(destScene, _subsetDef);

        assertNotNull(_destProduct.getSceneGeoCoding());
        assertTrue(_destProduct.getSceneGeoCoding() instanceof FXYGeoCoding);
        assertNotSame(_destProduct.getSceneGeoCoding(), geoCoding);
        assertTrue(_destProduct.isUsingSingleGeoCoding());
        assertSame(_destBand1.getGeoCoding(), _destProduct.getSceneGeoCoding());
        assertSame(_destBand2.getGeoCoding(), _destProduct.getSceneGeoCoding());

    }

    public void testTransferGCFromBandToProduct() {

        final AbstractGeoCoding geoCoding = createFXYSumGeoCoding();
        _srcBand1.setGeoCoding(geoCoding);
        final Scene srcScene = SceneFactory.createScene(_srcBand1);
        final Scene destScene = SceneFactory.createScene(_destProduct);
        srcScene.transferGeoCodingTo(destScene, _subsetDef);

        assertNotNull(_destProduct.getSceneGeoCoding());
        assertTrue(_destProduct.getSceneGeoCoding() instanceof FXYGeoCoding);
        assertNotSame(_destProduct.getSceneGeoCoding(), geoCoding);
        assertTrue(_destProduct.isUsingSingleGeoCoding());
        assertSame(_destBand1.getGeoCoding(), _destProduct.getSceneGeoCoding());
        assertSame(_destBand2.getGeoCoding(), _destProduct.getSceneGeoCoding());

    }

    public void testTransferGCFromProductToBand() {

        final AbstractGeoCoding geoCoding = createFXYSumGeoCoding();
        _srcProduct.setSceneGeoCoding(geoCoding);
        _destBand2.setGeoCoding(createFXYSumGeoCoding());
        final Scene srcScene = SceneFactory.createScene(_srcProduct);
        final Scene destScene = SceneFactory.createScene(_destBand1);
        srcScene.transferGeoCodingTo(destScene, _subsetDef);

        assertNotNull(_destBand1.getGeoCoding());
        assertNotNull(_destBand2.getGeoCoding());
        assertFalse(_destProduct.isUsingSingleGeoCoding());
        assertTrue(_destBand1.getGeoCoding() instanceof FXYGeoCoding);
        assertTrue(_destBand2.getGeoCoding() instanceof FXYGeoCoding);
        assertNotSame(_destBand1.getGeoCoding(), geoCoding);
        assertNotSame(_destBand2.getGeoCoding(), geoCoding);
        assertNotSame(_destBand1.getGeoCoding(), _destBand2.getGeoCoding());
    }

    public void testTransferBandedGCFromProductToProduct() {

        final AbstractGeoCoding geoCoding1 = createFXYSumGeoCoding();
        _srcBand1.setGeoCoding(geoCoding1);
        final AbstractGeoCoding geoCoding2 = createFXYSumGeoCoding();
        _srcBand2.setGeoCoding(geoCoding2);
        final Scene srcScene = SceneFactory.createScene(_srcProduct);
        final Scene destScene = SceneFactory.createScene(_destProduct);
        srcScene.transferGeoCodingTo(destScene, _subsetDef);

        assertNotNull(_destProduct.getSceneGeoCoding());
        assertFalse(_destProduct.isUsingSingleGeoCoding());
        assertNotNull(_destBand1.getGeoCoding());
        assertNotNull(_destBand2.getGeoCoding());
        assertTrue(_destBand1.getGeoCoding() instanceof FXYGeoCoding);
        assertTrue(_destBand2.getGeoCoding() instanceof FXYGeoCoding);
        assertNotSame(_destBand1.getGeoCoding(), geoCoding1);
        assertNotSame(_destBand2.getGeoCoding(), geoCoding2);
    }

    public void testTransferBandedGCFromProductToBand() {

        final AbstractGeoCoding geoCoding1 = createFXYSumGeoCoding();
        _srcBand1.setGeoCoding(geoCoding1);
        final AbstractGeoCoding geoCoding2 = createFXYSumGeoCoding();
        _srcBand2.setGeoCoding(geoCoding2);
        _destBand2.setGeoCoding(createFXYSumGeoCoding());
        final Scene srcScene = SceneFactory.createScene(_srcProduct);
        final Scene destScene = SceneFactory.createScene(_destBand1);
        srcScene.transferGeoCodingTo(destScene, _subsetDef);

        assertNotNull(_destProduct.getSceneGeoCoding());
        assertFalse(_destProduct.isUsingSingleGeoCoding());
        assertNotNull(_destBand1.getGeoCoding());
        assertNotNull(_destBand2.getGeoCoding());
        assertTrue(_destBand1.getGeoCoding() instanceof FXYGeoCoding);
        assertNotSame(_destBand1.getGeoCoding(), geoCoding1);
        assertNotSame(_destBand1.getGeoCoding(), _destBand2.getGeoCoding());
    }


    private AbstractGeoCoding createFXYSumGeoCoding() {
        final AbstractGeoCoding geoCoding;
        final FXYSum.Linear xFunc = new FXYSum.Linear(new double[]{0, 0, 1});
        final FXYSum.Linear yFunc = new FXYSum.Linear(new double[]{0, 1, 0});
        final FXYSum.Linear latFunc = new FXYSum.Linear(new double[]{0, 0, 1});
        final FXYSum.Linear lonFunc = new FXYSum.Linear(new double[]{0, 1, 0});
        geoCoding = new FXYGeoCoding(0, 0, 1, 1,
                                     xFunc, yFunc,
                                     latFunc, lonFunc,
                                     Datum.WGS_84);
        return geoCoding;
    }

}
