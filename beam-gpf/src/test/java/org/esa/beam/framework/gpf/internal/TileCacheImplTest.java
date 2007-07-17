package org.esa.beam.framework.gpf.internal;

import java.awt.Rectangle;

import junit.framework.TestCase;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Raster;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.TileCache;


public class TileCacheImplTest extends TestCase {

    public void testCacheMemoryCapacity() {
        TileCache tileCache = new TileCacheImpl();
        long tileSize = (64 * 64) * 2; // FLOAT32!
        tileCache.setMemoryLoadFactor(1.0);
        tileCache.setMemoryCapacity(3 * tileSize);

        Product product = createTestProduct();
        Band band1 = product.getBand("band_1");

        Tile tile1, tile2;

        tile1 = tileCache.createTile(band1, new Rectangle(0, 0, 64, 64), null);
        assertNotNull(tile1);
        tile2 = tileCache.getTile(band1, new Rectangle(0, 0, 64, 64));
        assertSame(tile1, tile2);

        tile1 = tileCache.createTile(band1, new Rectangle(64, 0, 64, 64), null);
        assertNotNull(tile1);
        tile2 = tileCache.getTile(band1, new Rectangle(64, 0, 64, 64));
        assertSame(tile1, tile2);

        tile1 = tileCache.createTile(band1, new Rectangle(0, 64, 64, 64), null);
        assertNotNull(tile1);
        tile2 = tileCache.getTile(band1, new Rectangle(0, 64, 64, 64));
        assertSame(tile1, tile2);

        // next one will not be cached anymore
        tile1 = tileCache.createTile(band1, new Rectangle(64, 64, 64, 64), null);
        assertNotNull(tile1);
        tile2 = tileCache.getTile(band1, new Rectangle(64, 64, 64, 64));
        assertNull(tile2); // cache full!
    }


    public void testCaching() {
        TileCache tileCache = new TileCacheImpl();

        Product product = createTestProduct();
        Band band1 = product.getBand("band_1");
        Band band2 = product.getBand("band_2");

        Tile tile;
        tile = tileCache.getTile(band1, new Rectangle(0, 0, 10, 12));
        assertNull(tile);

        tile = tileCache.createTile(band1, new Rectangle(0, 0, 10, 12), null);
        assertNotNull(tile);
        
        Raster raster = tile.getRaster();
        assertNotNull(raster);
        
        assertEquals(0, raster.getOffsetX());
        assertEquals(0, raster.getOffsetY());
        assertEquals(10, raster.getWidth());
        assertEquals(12, raster.getHeight());
        assertEquals(new Rectangle(0, 0, 10, 12), tile.getRectangle());
        assertEquals(Tile.State.NOT_COMPUTED, tile.getState());
        assertSame(band1, raster.getRasterDataNode());
        assertNotNull(raster.getDataBuffer());
        assertEquals(ProductData.TYPE_INT16, raster.getDataBuffer().getType());
        assertEquals(10 * 12, raster.getDataBuffer().getNumElems());


        assertSame(tile, tileCache.getTile(band1, new Rectangle(0, 0, 10, 12)));
        assertNotSame(tile, tileCache.getTile(band1, new Rectangle(0, 99, 10, 12)));

        tile = tileCache.createTile(band2, new Rectangle(5, 2, 5, 12), null);
        assertNotNull(tile);
        
        raster = tile.getRaster();
        assertNotNull(raster);
        
        assertEquals(5, raster.getOffsetX());
        assertEquals(2, raster.getOffsetY());
        assertEquals(5, raster.getWidth());
        assertEquals(12, raster.getHeight());
        assertEquals(new Rectangle(5, 2, 5, 12), tile.getRectangle());
        assertEquals(Tile.State.NOT_COMPUTED, tile.getState());
        assertSame(band2, raster.getRasterDataNode());
        assertNotNull(raster.getDataBuffer());
        assertEquals(ProductData.TYPE_INT16, raster.getDataBuffer().getType());
        assertEquals(5 * 12, raster.getDataBuffer().getNumElems());
    }

    private Product createTestProduct() {
        Product product = new Product("prod_1", "test1", 128, 128);
        Band band1 = new Band("band_1", ProductData.TYPE_INT16, 128, 128);
        band1.setScalingFactor(0.1);
        Band band2 = new Band("band_2", ProductData.TYPE_INT16, 128, 128);
        product.addBand(band1);
        product.addBand(band2);
        return product;
    }
}
