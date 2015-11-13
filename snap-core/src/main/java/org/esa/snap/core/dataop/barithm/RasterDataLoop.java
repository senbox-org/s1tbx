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
package org.esa.snap.core.dataop.barithm;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.util.Guardian;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A utility class which is used to iterate over all pixels within a region of a data raster and to evaluate
 * any number of {@link Term}s on each pixel.
 *
 * @author Sabine Embacher
 * @author Marco Peters
 * @author Norman Fomferra
 */
public class RasterDataLoop {

    private final RasterDataEvalEnv rasterDataEvalEnv;
    private final Term[] terms;
    private final RasterRegion[] rasterRegions;
    private ProgressMonitor pm;


    /**
     * Creates an instance of this class for the given region and terms.
     *
     * @param offsetX      the X-offset of the region.
     * @param offsetY      the Y-offset of the region.
     * @param regionWidth  the width of the region.
     * @param regionHeight the height of the region.
     * @param terms        an array of terms.
     * @param pm           a monitor to inform the user about progress
     */
    public RasterDataLoop(final int offsetX,
                          final int offsetY,
                          final int regionWidth,
                          final int regionHeight,
                          final Term[] terms,
                          final ProgressMonitor pm) {
        this(new RasterDataEvalEnv(offsetX, offsetY, regionWidth, regionHeight), terms, pm);
    }

    /**
     * Creates an instance of this class for the given region and terms.
     *
     * @param rasterDataEvalEnv the raster data evaluation environment passed to the term evaluation
     * @param terms             an array of terms.
     * @param pm                a monitor to inform the user about progress
     */
    public RasterDataLoop(final RasterDataEvalEnv rasterDataEvalEnv,
                          final Term[] terms,
                          final ProgressMonitor pm) {
        Guardian.assertNotNull("rasterDataEvalEnv", rasterDataEvalEnv);
        Guardian.assertNotNull("pm", pm);
        checkTerms(terms);
        checkRegion(rasterDataEvalEnv.getOffsetX(),
                    rasterDataEvalEnv.getOffsetY(),
                    rasterDataEvalEnv.getRegionWidth(),
                    rasterDataEvalEnv.getRegionHeight(),
                    terms);
        this.rasterDataEvalEnv = rasterDataEvalEnv;
        this.terms = terms;
        this.rasterRegions = createRasterRegions(terms, rasterDataEvalEnv.getRegionWidth(), 1);
        this.pm = pm;
    }

    public int getOffsetX() {
        return rasterDataEvalEnv.getOffsetX();
    }

    public int getOffsetY() {
        return rasterDataEvalEnv.getOffsetY();
    }

    public int getRegionWidth() {
        return rasterDataEvalEnv.getRegionWidth();
    }

    public int getRegionHeight() {
        return rasterDataEvalEnv.getRegionHeight();
    }

    public Term[] getTerms() {
        return terms;
    }

    /**
     * Evaluates the <code>body</code> by calling its {@link Body#eval(RasterDataEvalEnv,int) eval()} method.
     * This method just delegates to {@link #forEachPixel(Body, String) forEachPixel(body, null)}.
     *
     * @param body the object whose <code>eval</code> method is called.
     * @throws IOException if the raster data could not be read.
     */
    public void forEachPixel(final Body body) throws IOException {
        forEachPixel(body, "Performing raster data operation..."); /*I18N*/
    }

    /**
     * Evaluates the <code>body</code> by calling its {@link Body#eval(RasterDataEvalEnv,int) eval()} method.
     *
     * @param body    the object whose <code>eval</code> method is called.
     * @param message the progress message
     * @throws IOException if the raster data could not be read.
     */
    public void forEachPixel(final Body body, String message) throws IOException {
        Guardian.assertNotNull("body", body);
        final int offsetY = getOffsetY();
        final int width = getRegionWidth();
        final int height = getRegionHeight();
        final RasterDataEvalEnv env = rasterDataEvalEnv;
        message = (message == null) ? "Computing pixels..." : message;
        pm.beginTask(message, (height - offsetY) * 2);
        try {
            int pixelIndex = 0;
            for (int y = offsetY; y < offsetY + height; y++) {
                if (pm.isCanceled()) {
                    break;
                }
                readRegion(y, 1, SubProgressMonitor.create(pm, 1));
                for (int x = 0; x < width; x++) {
                    env.setElemIndex(x);
                    body.eval(env, pixelIndex);
                    pixelIndex++;
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    private void readRegion(final int offsetY, final int height, ProgressMonitor pm) throws IOException {
        for (RasterRegion rasterRegion : rasterRegions) {
            rasterRegion.readRegion(getOffsetX(), offsetY, getRegionWidth(), height, pm);
        }
    }

    private static void checkTerms(Term[] terms) {
        Guardian.assertNotNull("terms", terms);
        for (final Term term : terms) {
            Guardian.assertNotNull("term", term);
        }
    }

    private static void checkRegion(final int offsetX, final int offsetY,
                                    final int width, final int height,
                                    final Term[] terms) {
        for (final Term term : terms) {
            final RasterDataSymbol[] refRasterDataSymbols = BandArithmetic.getRefRasterDataSymbols(term);
            for (RasterDataSymbol refRasterDataSymbol : refRasterDataSymbols) {
                final RasterDataNode raster = refRasterDataSymbol.getRaster();
                final int rasterWidth = raster.getRasterWidth();
                final int rasterHeight = raster.getRasterHeight();
                if (rasterWidth < (offsetX + width) || rasterHeight < (offsetY + height)) {
                    throw new IllegalArgumentException("out of bounds.");
                }
            }
        }
    }

    private static RasterRegion[] createRasterRegions(final Term[] terms, final int width, final int height) {
        final Set<RasterDataSymbol> rasterSymbolSet = new HashSet<RasterDataSymbol>();
        for (final Term term : terms) {
            final RasterDataSymbol[] refRasterDataSymbols = BandArithmetic.getRefRasterDataSymbols(term);
            rasterSymbolSet.addAll(Arrays.asList(refRasterDataSymbols));
        }

        List<RasterRegion> rasterRegions = new ArrayList<RasterRegion>(rasterSymbolSet.size());
        for (RasterDataSymbol symbol : rasterSymbolSet) {
            RasterRegion rasterRegion = RasterRegion.createRasterRegion(symbol.getRaster(), width, height);
            rasterRegions.add(rasterRegion);
            symbol.setData(rasterRegion.getData().getElems());
        }
        return rasterRegions.toArray(new RasterRegion[rasterRegions.size()]);
    }

    /**
     * Represents the body to be evaluated for each pixel within the raster.
     */
    public interface Body {

        /**
         * This method is called for each pixel within the sub-raster.
         * The supplied pixel index is defined relative to the sub-rasters origin so that
         * the index 0 (zero) corresponds to the raster's origin at {@link RasterDataEvalEnv#getOffsetX()},{@link RasterDataEvalEnv#getOffsetY()}.
         *
         * @param env        the {@link RasterDataEvalEnv} which must be used by any term to be evaluated.
         * @param pixelIndex the current, relative pixel index
         * @see RasterDataLoop#forEachPixel(Body)
         */
        void eval(final RasterDataEvalEnv env, final int pixelIndex);
    }

    private static class RasterRegion {

        private final RasterDataNode _rasterNode;
        private final ProductData _regionData;


        /**
         * @param rasterNode the <code>RasterDataNode</code> for the created <code>RasterRegion</code>
         * @param width      the width of the line
         * @param height     the height of the line
         * @return an instance of <code>RasterRegion</code>
         */
        static RasterRegion createRasterRegion(final RasterDataNode rasterNode, final int width, final int height) {
            final ProductData data;
            if (rasterNode.isFloatingPointType()) {
                data = ProductData.createInstance(ProductData.TYPE_FLOAT32, width * height);
            } else {
                data = ProductData.createInstance(ProductData.TYPE_INT32, width * height);
            }
            return new RasterRegion(rasterNode, data);
        }

        /**
         * Creates an instance of this class.
         *
         * @param rasterNode the <code>RasterDataNode</code>
         * @param regionData the <code>RasterDataNode</code> associated with this object.
         */
        private RasterRegion(final RasterDataNode rasterNode, final ProductData regionData) {
            _rasterNode = rasterNode;
            _regionData = regionData;
        }

        RasterDataNode getRasterNode() {
            return _rasterNode;
        }

        ProductData getData() {
            return _regionData;
        }

        /**
         * Reads a region into the internal data array.
         *
         * @param x  the x-offset to start reading the region
         * @param y  the y-offset to start reading the region
         * @param w  the width of the region
         * @param h  the height of the region
         * @param pm progress monitor
         * @throws java.io.IOException is thrown if reading data fails.
         */
        void readRegion(final int x, final int y, final int w, final int h, ProgressMonitor pm) throws IOException {
            if (_rasterNode.isFloatingPointType()) {
                _rasterNode.readPixels(x, y, w, h, (float[]) _regionData.getElems(), pm);
            } else {
                _rasterNode.readPixels(x, y, w, h, (int[]) _regionData.getElems(), pm);
            }
        }
    }
}

