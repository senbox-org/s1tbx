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

package org.esa.snap.core.datamodel;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.jexp.Term;

import java.util.AbstractSet;
import java.util.Iterator;
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

    private Term term;
    private final WeakHashSet<Product> referencedProducts;
    private final WeakHashSet<RasterDataNode> referencedRasters;

    /**
     * Creates a new {@link MultiLevelImage} computed from band math. The created
     * image resets itself whenever any referred rasters change.
     *
     * @param multiLevelSource A multi-level image source
     * @param term             A compiled band math expression.
     */
    VirtualBandMultiLevelImage(MultiLevelSource multiLevelSource, Term term) {
        super(multiLevelSource);
        Assert.notNull(multiLevelSource, "multiLevelSource");
        referencedProducts = new WeakHashSet<>();
        referencedRasters = new WeakHashSet<>();
        this.term = term;
        addTermNodes();
    }

    public Term getTerm() {
        return term;
    }

    public void setTerm(Term term) {
        Assert.notNull(term, "term");
        if (this.term != term) {
            removeTermNodes();
            this.term = term;
            addTermNodes();
        }
    }

    @Override
    public void dispose() {
        removeTermNodes();
        term = null;
        super.dispose();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (term != null) {
            //System.out.printf("%s.finalize(): term = %s%n", getClass().getSimpleName(), term.toString());
            dispose();
        }
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
    public void nodeChanged(ProductNodeEvent event) {
    }

    @Override
    public void nodeAdded(ProductNodeEvent event) {
    }

    @Override
    public void nodeRemoved(ProductNodeEvent event) {
    }

    // use for testing only
    Set<Product> getReferencedProducts() {
        return referencedProducts;
    }

    // use for testing only
    Set<RasterDataNode> getReferencedRasters() {
        return referencedRasters;
    }

    private void addTermNodes() {
        RasterDataNode[] refRasters = BandArithmetic.getRefRasters(term);
        for (RasterDataNode refRaster : refRasters) {
            referencedProducts.add(refRaster.getProduct());
            referencedRasters.add(refRaster);
        }
        for (Product referencedProduct : referencedProducts) {
            referencedProduct.addProductNodeListener(this);
        }
    }

    private void removeTermNodes() {
        for (Product referencedProduct : referencedProducts) {
            referencedProduct.removeProductNodeListener(this);
        }
        referencedProducts.clear();
        referencedRasters.clear();
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
