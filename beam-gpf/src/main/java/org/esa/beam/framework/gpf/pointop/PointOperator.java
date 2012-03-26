/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.framework.gpf.pointop;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeFilter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.util.ProductUtils;

import java.awt.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The {@code PointOperator} class serves as a base class for operators
 * <ol>
 * <li>that compute single pixels independently of their neighbours and</li>
 * <li>whose target product and all source products share the same grid and coordinate reference system.</li>
 * </ol>
 * More specifically, the target product and all source products must share the same raster size and
 * {@link org.esa.beam.framework.datamodel.GeoCoding GeoCoding}.
 *
 * @author Norman Fomferra
 * @since BEAM 4.9
 */
public abstract class PointOperator extends Operator {

    private transient RasterDataNode[] sourceNodes;
    private transient Band[] targetNodes;

    /**
     * Configures this {@code PointOperator} by performing a number of initialisation steps in the given order:
     * <ol>
     * <li>{@link #prepareInputs()}</li>
     * <li>{@link #createTargetProduct()}</li>
     * <li>{@link #configureTargetProduct(ProductConfigurer)}</li>
     * <li>{@link #configureSourceSamples(SampleConfigurer)}</li>
     * <li>{@link #configureTargetSamples(SampleConfigurer)}</li>
     * </ol>
     * This method cannot be overridden by intention (<i>template method</i>). Instead clients may wish to
     * override the methods that are called during the initialisation sequence.
     *
     * @throws OperatorException If the configuration cannot be performed.
     */
    @Override
    public final void initialize() throws OperatorException {
        prepareInputs();
        Product targetProduct = createTargetProduct();
        setTargetProduct(targetProduct);
        configureTargetProduct(new ProductConfigurerImpl(getSourceProduct(), targetProduct));
        SourceSampleConfigurer sc = new SourceSampleConfigurer();
        TargetSampleConfigurer tc = new TargetSampleConfigurer();
        configureSourceSamples(sc);
        configureTargetSamples(tc);
        sourceNodes = sc.getNodes();
        targetNodes = tc.getNodes();
    }

    /**
     * Prepares the inputs for this operator. Called by {@link #initialize()}.
     * <p/>
     * Clients may override to perform some extra validation of
     * parameters and source products and/or to load external, auxiliary resources. External resources that may be opened
     * by this method and that must remain open during the {@code Operator}'s lifetime shall be closed
     * by a dedicated override of the {@link #dispose()} method.
     * <p/>
     * Failures of input preparation shall be indicated by throwing an {@link OperatorException}.
     * <p/>
     * The default implementation checks whether all source products have the same raster size.
     * Clients that require a similar behaviour in their operator shall first call the {@code super} method
     * in their implementation.
     *
     * @throws OperatorException If the validation of input fails.
     */
    protected void prepareInputs() throws OperatorException {
        checkRasterSize();
    }

    /**
     * Creates the target product instance. Called by {@link #initialize()}.
     * <p/>
     * The default implementation creates a target product instance given the raster size of the (first) source product.
     *
     * @return A new target product instance.
     * @throws OperatorException If the target product cannot be created.
     */
    protected Product createTargetProduct() throws OperatorException {
        Product sourceProduct = getSourceProduct();
        return new Product(getId(),
                           getClass().getName(),
                           sourceProduct.getSceneRasterWidth(),
                           sourceProduct.getSceneRasterHeight());
    }

    /**
     * Configures the target product via the given {@link ProductConfigurer}. Called by {@link #initialize()}.
     * <p/>
     * Client implementations of this method usually add product components to the given target product, such as
     * {@link Band bands} to be computed by this operator,
     * {@link org.esa.beam.framework.datamodel.VirtualBand virtual bands},
     * {@link org.esa.beam.framework.datamodel.Mask masks}
     * or {@link org.esa.beam.framework.datamodel.SampleCoding sample codings}.
     * <p/>
     * The default implementation retrieves the (first) source product and copies to the target product
     * <ul>
     * <li>the start and stop time by calling {@link ProductConfigurer#copyTimeCoding()},</li>
     * <li>all tie-point grids by calling {@link ProductConfigurer#copyTiePointGrids(String...)},</li>
     * <li>the geo-coding by calling {@link ProductConfigurer#copyGeoCoding()}.</li>
     * </ul>
     * <p/>
     * Clients that require a similar behaviour in their operator shall first call the {@code super} method
     * in their implementation.
     *
     * @param productConfigurer The target product configurer.
     * @throws OperatorException If the target product cannot be configured.
     * @see Product#addBand(org.esa.beam.framework.datamodel.Band)
     * @see Product#addBand(String, String)
     * @see Product#addTiePointGrid(org.esa.beam.framework.datamodel.TiePointGrid)
     * @see org.esa.beam.framework.datamodel.Product#getMaskGroup()
     */
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        productConfigurer.copyTimeCoding();
        productConfigurer.copyTiePointGrids();
        productConfigurer.copyGeoCoding();
    }

    /**
     * Configures all source samples that this operator requires for the computation of target samples.
     * Source sample are defined by using the provided {@link SampleConfigurer}.
     * <p/>
     * <p/> The method is called by {@link #initialize()}.
     *
     * @param sampleConfigurer The configurer that defines the layout of a pixel.
     * @throws OperatorException If the source samples cannot be configured.
     */
    protected abstract void configureSourceSamples(SampleConfigurer sampleConfigurer) throws OperatorException;

    /**
     * Configures all target samples computed by this operator.
     * Target samples are defined by using the provided {@link SampleConfigurer}.
     * <p/>
     * <p/> The method is called by {@link #initialize()}.
     *
     * @param sampleConfigurer The configurer that defines the layout of a pixel.
     * @throws OperatorException If the target samples cannot be configured.
     */
    protected abstract void configureTargetSamples(SampleConfigurer sampleConfigurer) throws OperatorException;

    /**
     * Checks if all source products share the same raster size, otherwise throws an exception.
     * Called by {@link #initialize()}.
     *
     * @throws OperatorException If the source product's raster sizes are not equal.
     */
    protected void checkRasterSize() throws OperatorException {
        Product[] sourceProducts = getSourceProducts();
        int w = 0;
        int h = 0;
        for (int i = 0; i < sourceProducts.length; i++) {
            Product sourceProduct = sourceProducts[i];
            if (i == 0) {
                w = sourceProduct.getSceneRasterWidth();
                h = sourceProduct.getSceneRasterHeight();
            } else {
                if (sourceProduct.getSceneRasterWidth() != w || sourceProduct.getSceneRasterHeight() != h) {
                    throw new OperatorException("Source products must all have the same raster size.");
                }
            }
        }
    }

    Sample[] createSourceSamples(Rectangle targetRectangle, Point location) {
        final Tile[] sourceTiles = getSourceTiles(targetRectangle);
        return createDefaultSamples(sourceNodes, sourceTiles, location);
    }

    WritableSample[] createTargetSamples(Map<Band, Tile> targetTileStack, Point location) {
        final Tile[] targetTiles = getTargetTiles(targetTileStack);
        return createDefaultSamples(targetNodes, targetTiles, location);
    }

    WritableSample createTargetSample(Tile targetTile, Point location) {
        final RasterDataNode targetNode = targetTile.getRasterDataNode();
        for (int i = 0; i < targetNodes.length; i++) {
            //noinspection ObjectEquality
            if (targetNode == targetNodes[i]) {
                return new WritableSampleImpl(i, targetTile, location);
            }
        }
        final String msgPattern = "Could not create target sample for band '%s'.";
        throw new IllegalStateException(String.format(msgPattern, targetNode.getName()));
    }

    private Tile[] getSourceTiles(Rectangle region) {
        final Tile[] sourceTiles = new Tile[sourceNodes.length];
        for (int i = 0; i < sourceTiles.length; i++) {
            if (sourceNodes[i] != null) {
                sourceTiles[i] = getSourceTile(sourceNodes[i], region);
            }
        }
        return sourceTiles;
    }

    private Tile[] getTargetTiles(Map<Band, Tile> targetTileStack) {
        final Tile[] targetTiles = new Tile[targetNodes.length];
        for (int i = 0; i < targetTiles.length; i++) {
            if (targetNodes[i] != null) {
                Tile targetTile = targetTileStack.get(targetNodes[i]);
                if (targetTile == null) {
                    final String msgPattern = "Could not find tile for defined target node '%s'.";
                    throw new IllegalStateException(String.format(msgPattern, targetNodes[i].getName()));
                }
                targetTiles[i] = targetTile;
            }
        }
        return targetTiles;
    }

    private static WritableSampleImpl[] createDefaultSamples(RasterDataNode[] nodes, Tile[] tiles, Point location) {
        WritableSampleImpl[] samples = new WritableSampleImpl[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] != null) {
                samples[i] = new WritableSampleImpl(i, tiles[i], location);
            } else {
                samples[i] = WritableSampleImpl.NULL;
            }
        }
        return samples;
    }

    private static final class WritableSampleImpl implements WritableSample {

        static final WritableSampleImpl NULL = new WritableSampleImpl();

        private final int index;
        private final RasterDataNode node;
        private final int dataType;
        private final Tile tile;
        private final Point location;

        private WritableSampleImpl(int index, Tile tile, Point location) {
            this.index = index;
            this.node = tile.getRasterDataNode();
            this.dataType = this.node.getGeophysicalDataType();
            this.tile = tile;
            this.location = location;
        }

        private WritableSampleImpl() {
            this.index = -1;
            this.node = null;
            this.dataType = -1;
            this.tile = null;
            this.location = null;
        }

        @Override
        public RasterDataNode getNode() {
            return node;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public int getDataType() {
            return dataType;
        }

        @Override
        public boolean getBit(int bitIndex) {
            return tile.getSampleBit(location.x, location.y, bitIndex);
        }

        @Override
        public boolean getBoolean() {
            return tile.getSampleBoolean(location.x, location.y);
        }

        @Override
        public int getInt() {
            return tile.getSampleInt(location.x, location.y);
        }

        @Override
        public float getFloat() {
            return tile.getSampleFloat(location.x, location.y);
        }

        @Override
        public double getDouble() {
            return tile.getSampleDouble(location.x, location.y);
        }

        @Override
        public void set(int bitIndex, boolean v) {
            tile.setSample(location.x, location.y, bitIndex, v);
        }

        @Override
        public void set(boolean v) {
            tile.setSample(location.x, location.y, v);
        }

        @Override
        public void set(int v) {
            tile.setSample(location.x, location.y, v);
        }

        @Override
        public void set(float v) {
            tile.setSample(location.x, location.y, v);
        }

        @Override
        public void set(double v) {
            tile.setSample(location.x, location.y, v);
        }
    }

    private abstract static class AbstractSampleConfigurer<T extends RasterDataNode> implements SampleConfigurer {

        final List<T> nodes = new ArrayList<T>();

        void defineSample(int index, String name, Product product, boolean sourceless) throws OperatorException {
            T node = (T) product.getRasterDataNode(name);
            if (node == null) {
                String message = MessageFormat.format(
                        "Product ''{0}'' does not contain a raster data node with name ''{1}''",
                        product.getName(), name);
                throw new OperatorException(message);
            }
            if (sourceless && node.isSourceImageSet()) {
                String message = MessageFormat.format(
                        "Raster data node ''{0}'' must be sourceless, since it is a computed target",
                        name);
                throw new OperatorException(message);
            }
            if (index < nodes.size()) {
                nodes.set(index, node);
            } else if (index == nodes.size()) {
                nodes.add(node);
            } else {
                while (index > nodes.size()) {
                    nodes.add(null);
                }
                nodes.add(node);
            }
        }

        RasterDataNode[] getNodes() {
            return nodes.toArray(new RasterDataNode[nodes.size()]);
        }
    }

    private final class SourceSampleConfigurer extends AbstractSampleConfigurer<RasterDataNode> {

        @Override
        public void defineSample(int index, String name) {
            defineSample(index, name, getSourceProduct(), false);
        }

        @Override
        public void defineSample(int index, String name, Product product) {
            super.defineSample(index, name, product, false);
        }
    }

    private final class TargetSampleConfigurer extends AbstractSampleConfigurer<Band> {

        @Override
        public void defineSample(int index, String name) {
            defineSample(index, name, getTargetProduct(), true);
        }

        @Override
        public void defineSample(int index, String name, Product product) {
            super.defineSample(index, name, product, true);
        }

        @Override
        Band[] getNodes() {
            return nodes.toArray(new Band[nodes.size()]);
        }
    }

    private static final class ProductConfigurerImpl implements ProductConfigurer {

        private Product sourceProduct;
        private final Product targetProduct;

        private ProductConfigurerImpl(Product sourceProduct, Product targetProduct) {
            this.sourceProduct = sourceProduct;
            this.targetProduct = targetProduct;
        }

        @Override
        public Product getSourceProduct() {
            return sourceProduct;
        }

        @Override
        public void setSourceProduct(Product sourceProduct) {
            this.sourceProduct = sourceProduct;
        }

        @Override
        public Product getTargetProduct() {
            return targetProduct;
        }

        @Override
        public void copyMetadata() {
            ProductUtils.copyMetadata(getSourceProduct(), getTargetProduct());
        }

        @Override
        public void copyTimeCoding() {
            getTargetProduct().setStartTime(getSourceProduct().getStartTime());
            getTargetProduct().setEndTime(getSourceProduct().getEndTime());
        }

        @Override
        public void copyGeoCoding() {
            ProductUtils.copyGeoCoding(getSourceProduct(), getTargetProduct());
        }

        @Override
        public void copyMasks() {
            ProductUtils.copyMasks(getSourceProduct(), getTargetProduct());
        }

        @Override
        public void copyBands(String... names) {
            if (names.length == 0) {
                names = getSourceProduct().getBandNames();
            }
            for (String name : names) {
                copyBand(name);
            }
        }

        @Override
        public void copyBands(ProductNodeFilter<Band> filter) {
            Band[] sourceBands = getSourceProduct().getBands();
            for (Band sourceBand : sourceBands) {
                if (filter.accept(sourceBand)) {
                    copyBand(sourceBand.getName());
                }
            }
        }

        @Override
        public void copyTiePointGrids(String... names) {
            if (names.length == 0) {
                names = getSourceProduct().getTiePointGridNames();
            }
            for (String name : names) {
                ProductUtils.copyTiePointGrid(name, getSourceProduct(), getTargetProduct());
            }
        }

        @Override
        public void copyVectorData() {
            ProductUtils.copyVectorData(getSourceProduct(), getTargetProduct());
        }

        @Override
        public Band addBand(String name, int dataType) {
            return getTargetProduct().addBand(name, dataType);
        }

        @Override
        public Band addBand(String name, int dataType, double noDataValue) {
            Band band = addBand(name, dataType);
            band.setNoDataValue(noDataValue);
            band.setNoDataValueUsed(true);
            return band;
        }

        @Override
        public Band addBand(String name, String expression) {
            return getTargetProduct().addBand(name, expression);
        }

        @Override
        public Band addBand(String name, String expression, double noDataValue) {
            Band band = addBand(name, expression);
            band.setNoDataValue(noDataValue);
            band.setNoDataValueUsed(true);
            return band;
        }

        private void copyBand(String name) {
            ProductUtils.copyBand(name, getSourceProduct(), getTargetProduct(), true);
        }
    }
}
