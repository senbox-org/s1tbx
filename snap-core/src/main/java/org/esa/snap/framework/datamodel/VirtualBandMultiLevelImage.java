/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.framework.datamodel;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.jexp.Term;
import org.esa.snap.framework.dataop.barithm.BandArithmetic;
import org.esa.snap.jai.ImageManager;
import org.esa.snap.jai.ResolutionLevel;
import org.esa.snap.jai.VirtualBandOpImage;

import java.awt.Dimension;
import java.awt.image.RenderedImage;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * A {@link MultiLevelImage} computed from band maths. The {@link VirtualBandMultiLevelImage}
 * resets itself whenever any referred raster data have changed.
 *
 * @author Norman Fomferra
 * @author Ralf Quast
 * @since BEAM 4.7
 */
class VirtualBandMultiLevelImage extends DefaultMultiLevelImage implements ProductNodeListener {

    //private final Term term;
    private final WeakHashSet<Product> referencedProducts;
    private final WeakHashSet<RasterDataNode> referencedRasters;

    /**
     * Creates a new {@link MultiLevelImage} computed from band math. The created
     * image resets itself whenever any referred rasters change.
     *
     * @param term             A compiled band math expression.
     * @param multiLevelSource A multi-level image source
     */
    VirtualBandMultiLevelImage(Term term, MultiLevelSource multiLevelSource) {
        super(multiLevelSource);
        Assert.notNull(term, "term");
        Assert.notNull(multiLevelSource, "multiLevelSource");
        //this.term = term;
        RasterDataNode[] refRasters = BandArithmetic.getRefRasters(term);
        referencedProducts = new WeakHashSet<>();
        referencedRasters = new WeakHashSet<>();
        for (RasterDataNode refRaster : refRasters) {
            referencedProducts.add(refRaster.getProduct());
            referencedRasters.add(refRaster);
        }
        for (Product referencedProduct : referencedProducts) {
            referencedProduct.addProductNodeListener(this);
        }
    }

    @Override
    public void dispose() {
        for (Product referencedProduct : referencedProducts) {
            referencedProduct.removeProductNodeListener(this);
        }
        referencedProducts.clear();
        referencedRasters.clear();
        super.dispose();
    }

    @Override
    public void nodeChanged(ProductNodeEvent event) {
    }

    @Override
    public void nodeDataChanged(ProductNodeEvent event) {
        ProductNode sourceNode = event.getSourceNode();
        if (sourceNode instanceof RasterDataNode) {
            RasterDataNode rasterDataNode = (RasterDataNode) sourceNode;
            if (referencedRasters.contains(rasterDataNode)) {
                reset();
            }
        }
    }

    @Override
    public void nodeAdded(ProductNodeEvent event) {
    }

    @Override
    public void nodeRemoved(ProductNodeEvent event) {
    }

    // use for testing only
    public Set<Product> getReferencedProducts() {
        return referencedProducts;
    }

    // use for testing only
    public Set<RasterDataNode> getReferencedRasters() {
        return referencedRasters;
    }

    // implementation copied from {@code HashSet}
    private static class WeakHashSet<E> extends AbstractSet<E> {

        private static final Object PRESENT = new Object();
        private final WeakHashMap<E, Object> map;

        private WeakHashSet() {
            map = new WeakHashMap<>();
        }

        @Override
        public Iterator<E> iterator() {
            return map.keySet().iterator();
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            //noinspection SuspiciousMethodCalls
            return map.containsKey(o);
        }

        @Override
        public boolean add(E e) {
            return map.put(e, PRESENT) == null;
        }

        @Override
        public boolean remove(Object o) {
            return map.remove(o) == PRESENT;
        }

        @Override
        public void clear() {
            map.clear();
        }
    }
}
