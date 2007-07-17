/*
 * $Id: ProductDataCache.java,v 1.1 2007/03/27 12:51:05 marcoz Exp $
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

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.*;

/**
 * A specialized cache for ProductData covering a unique rectangle
 * for diferent bands.
 *
 * @author marcoz
 * @version $Revision: 1.1 $ $Date: 2007/03/27 12:51:05 $
 */
public class ProductDataCache {
    // todo - in which sense is this a cache? Rename!
    private final Cache<Rectangle, Band, ProductData> cache;

    public ProductDataCache(Rectangle rect) {
        cache = new Cache<Rectangle, Band, ProductData>(rect);
    }

    /**
     * Creates a {@link ProductData} instance for the given band. The required
     * size is taken from the actual covered rectangle. This object is put into
     * the cache.
     *
     * @param band the band for which data type the ProductData should be
     *             created.
     * @return a ProductData object.
     */
    public ProductData createData(Band band) {
        final Rectangle rectangle = getRectangle();
        final int size = rectangle.width * rectangle.height;
        ProductData data = band.createCompatibleProductData(size);
        setData(band, data);
        return data;
    }

    public Rectangle getRectangle() {
        return cache.getId();
    }

    public ProductData getData(Band key) {
        return cache.getValue(key);
    }

    public ProductData setData(Band key, ProductData value) {
        return cache.setValue(key, value);
    }

    public void dispose() {
        cache.dispose();
    }
}
