package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import com.sun.media.jai.util.SunTileCache;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;

import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.util.Map;

import javax.media.jai.JAI;

import junit.framework.TestCase;

public class OperatorImageTileStackTest extends TestCase {

    public void testIsBandComputedByThisOperator() {
        // todo - MarcoP shall implement this test because it is very important to test OperatorImageTileStack.isBandComputedByThisOperator (nf - 20090514)
        assertTrue(true);
    }
    
    public void testTileStackImage() throws Exception {
        final SunTileCache tileCache = (SunTileCache) JAI.getDefaultInstance().getTileCache();
        tileCache.flush();
        long cacheTileCount = tileCache.getCacheTileCount();
        assertEquals(0, cacheTileCount);
        
        Operator operator = new Operator() {
            @Override
            public void initialize() throws OperatorException {
                Product product = new Product("name", "desc", 1, 1);
                product.addBand("a", ProductData.TYPE_INT32);
                product.addBand("b", ProductData.TYPE_INT32);
                product.addBand("c", ProductData.TYPE_INT32);
                setTargetProduct(product);
            }
            
            @Override
            public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm)
                                                                                                                    throws OperatorException {
                for (Tile tt : targetTiles.values()) {
                    ProductData dataBuffer = tt.getDataBuffer();
                    int numElems = dataBuffer.getNumElems();
                    for (int i = 0; i < numElems; i++) {
                        dataBuffer.setElemIntAt(i, i);
                    }
                }
                long cacheTileCount = tileCache.getCacheTileCount();
                assertEquals(0, cacheTileCount);
            }
        };
        
        assertNotNull(operator);
        Product targetProduct = operator.getTargetProduct();
        assertNotNull(targetProduct);
        assertEquals(3, targetProduct.getNumBands());
        MultiLevelImage sourceImage = targetProduct.getBandAt(0).getSourceImage();
        RenderedImage image = sourceImage.getImage(0);
        assertTrue(image instanceof OperatorImageTileStack);
        // pull data to trigger computation
        image.getData();
        
        cacheTileCount = tileCache.getCacheTileCount();
        assertEquals(3, cacheTileCount);
    }
}
