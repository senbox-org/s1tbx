/*
 * $Id: TileCachingStrategy.java,v 1.3 2007/05/07 07:53:11 marcoz Exp $
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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by marcoz.
 *
 * @author marcoz
 * @version $Revision: 1.3 $ $Date: 2007/05/07 07:53:11 $
 */
public class TileCachingStrategy implements CachingStrategy {

    private CachingOperator operator;
    private TileCache tileCache;
    private List<Rectangle> tileRects;

    public TileCachingStrategy(CachingOperator cachingOperator, List<Rectangle> tileRects, int maxSize) {
        this.operator = cachingOperator;
        this.tileRects = tileRects;
        tileCache = new TileCache(maxSize);
    }

    public void computeTargetRaster(Band band, Rectangle rectangle,
                                    ProductData destBuffer, ProgressMonitor pm)
            throws OperatorException {

        List<Rectangle> affectedTileRects = getAffectedTileRects(rectangle);
        for (Rectangle tileRectangle : affectedTileRects) {
            ProductDataCache cache = tileCache.getCacheFor(tileRectangle);
            if (cache.getData(band) == null) {
                if (operator.isComputingAllBandsAtOnce()) {
                    operator.computeTiles(tileRectangle, cache, pm);
                } else {
                    operator.computeTile(band, tileRectangle, cache, pm);
                }
            }
            ProductData cachedData = cache.getData(band);
            if (cachedData.getNumElems() != tileRectangle.width * tileRectangle.height) {
                throw new OperatorException(String.format("Operator [%s] returned an invalid data block.", operator.getSpi().getName()));
            }

            Rectangle intersection = rectangle.intersection(tileRectangle);
            final int tileOffsetY = intersection.y - tileRectangle.y;
            final int tileOffsetX = intersection.x - tileRectangle.x;
            final int targetOffsetY = intersection.y - rectangle.y;
            final int targetOffsetX = intersection.x - rectangle.x;

            int tileIndex = tileRectangle.width * tileOffsetY + tileOffsetX;
            int targetIndex = rectangle.width * targetOffsetY + targetOffsetX;
            if (intersection.width == rectangle.width) {
                System.arraycopy(cachedData.getElems(), tileIndex,
                                 destBuffer.getElems(), targetIndex,
                                 intersection.width * intersection.height);
            } else {
                for (int y = targetOffsetY; y < targetOffsetY + intersection.height; y++) {
                    System.arraycopy(cachedData.getElems(), tileIndex,
                                     destBuffer.getElems(), targetIndex,
                                     rectangle.width);
                    tileIndex += tileRectangle.width;
                    targetIndex += rectangle.width;
                }
            }
        }
    }

    /**
     * Returns all rectangles that intersect with the given rectangle
     *
     * @param rectangle the rectangle to intersect with
     * @return a list of rectangles
     */
    private List<Rectangle> getAffectedTileRects(Rectangle rectangle) {
        List<Rectangle> affectedRects = new LinkedList<Rectangle>();
        for (Rectangle rect : tileRects) {
            if (rectangle.intersects(rect)) {
                affectedRects.add(rect);
            }
        }
        return affectedRects;
    }


    public void dispose() {
        // TODO Auto-generated method stub
    }

    private class TileCache {

        private Map<Rectangle, ProductDataCache> cacheMap;

        public TileCache(final int maxSize) {
            cacheMap = new LinkedHashMap<Rectangle, ProductDataCache>(maxSize, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Rectangle, ProductDataCache> eldest) {
                    return size() > maxSize;
                }
            };
        }

        /**
         * @param tileRectangle the tile rectangle
         * @return the cache object
         */
        public ProductDataCache getCacheFor(Rectangle tileRectangle) {
            ProductDataCache productDataCache;
            if (cacheMap.containsKey(tileRectangle)) {
                productDataCache = cacheMap.get(tileRectangle);
            } else {
                productDataCache = new ProductDataCache(tileRectangle);
                cacheMap.put(tileRectangle, productDataCache);
            }
            return productDataCache;
        }

        /**
         * @param band
         * @param tileRectangle
         *
         */
//		public boolean containsDataFor(Rectangle tileRectangle, Band band) {
//			if (cacheMap.containsKey(tileRectangle)) {
//				ProductDataCache productDataCache = cacheMap.get(tileRectangle);
//				if (productDataCache.getValue(band) != null) {
//					return true;
//				}
//			}
//			return false;
//		}
    }
}