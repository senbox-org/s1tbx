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

package org.esa.snap.core.gpf.pointop;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ConvolutionFilterBand;
import org.esa.snap.core.datamodel.GeneralFilterBand;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Kernel;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeFilter;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.SampleCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.image.VirtualBandOpImage;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.util.ProductUtils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
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
 * {@link GeoCoding GeoCoding}.
 *
 * @author Norman Fomferra
 * @since BEAM 4.9, revised in SNAP 2.0
 */
public abstract class PointOperator extends Operator {

    private transient RasterDataNode[] sourceRasters;
    private transient RasterDataNode[] computedRasters;
    private transient Mask validPixelMask;
    private transient Band[] targetBands;

    /**
     * Configures this {@code PointOperator} by performing a number of initialisation steps in the given order:
     * <ol>
     * <li>{@link #prepareInputs()}</li>
     * <li>{@link #createTargetProduct()}</li>
     * <li>{@link #configureTargetProduct(ProductConfigurer)}</li>
     * <li>{@link #configureSourceSamples(SourceSampleConfigurer)}</li>
     * <li>{@link #configureTargetSamples(TargetSampleConfigurer)}</li>
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
        SourceSampleConfigurerImpl sc = new SourceSampleConfigurerImpl();
        TargetSampleConfigurerImpl tc = new TargetSampleConfigurerImpl();
        configureSourceSamples(sc);
        configureTargetSamples(tc);
        sourceRasters = sc.getRasters();
        computedRasters = sc.getComputeNodes();
        targetBands = tc.getRasters();
    }

    /**
     * Prepares the inputs for this operator. Called by {@link #initialize()}.
     * <p>
     * Clients may override to perform some extra validation of
     * parameters and source products and/or to load external, auxiliary resources. External resources that may be opened
     * by this method and that must remain open during the {@code Operator}'s lifetime shall be closed
     * by a dedicated override of the {@link #dispose()} method.
     * <p>
     * Failures of input preparation shall be indicated by throwing an {@link OperatorException}.
     * <p>
     * The default implementation checks whether all source products have the same raster size.
     * Clients that require a similar behaviour in their operator shall first call the {@code super} method
     * in their implementation.
     *
     * @throws OperatorException If the validation of input fails.
     */
    protected void prepareInputs() throws OperatorException {
        checkRasterSize();
    }

    @Override
    public void dispose() {
        super.dispose();
        for (RasterDataNode node : computedRasters) {
            node.dispose();
        }
        sourceRasters = null;
        computedRasters = null;
        targetBands = null;
    }

    /**
     * Creates the target product instance. Called by {@link #initialize()}.
     * <p>
     * The default implementation creates a target product instance given the raster size of the (first) source product.
     *
     * @return A new target product instance.
     * @throws OperatorException If the target product cannot be created.
     */
    protected Product createTargetProduct() throws OperatorException {
        Product sourceProduct = getSourceProduct();
        Assert.state(sourceProduct != null, "source product not set");
        return new Product(getId(),
                           getClass().getName(),
                           sourceProduct.getSceneRasterWidth(),
                           sourceProduct.getSceneRasterHeight());
    }

    /**
     * Configures the target product via the given {@link ProductConfigurer}. Called by {@link #initialize()}.
     * <p>
     * Client implementations of this method usually add product components to the given target product, such as
     * {@link Band bands} to be computed by this operator,
     * {@link VirtualBand virtual bands},
     * {@link Mask masks}
     * or {@link SampleCoding sample codings}.
     * <p>
     * The default implementation retrieves the (first) source product and copies to the target product
     * <ul>
     * <li>the start and stop time by calling {@link ProductConfigurer#copyTimeCoding()},</li>
     * <li>all tie-point grids by calling {@link ProductConfigurer#copyTiePointGrids(String...)},</li>
     * <li>the geo-coding by calling {@link ProductConfigurer#copyGeoCoding()}.</li>
     * </ul>
     * <p>
     * Clients that require a similar behaviour in their operator shall first call the {@code super} method
     * in their implementation.
     *
     * @param productConfigurer The target product configurer.
     * @throws OperatorException If the target product cannot be configured.
     * @see Product#addBand(Band)
     * @see Product#addBand(String, String)
     * @see Product#addTiePointGrid(TiePointGrid)
     * @see Product#getMaskGroup()
     */
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        productConfigurer.copyTimeCoding();
        productConfigurer.copyTiePointGrids();
        productConfigurer.copyGeoCoding();
    }


    /**
     * Configures all source samples that this operator requires for the computation of target samples.
     * Source sample are defined by using the provided {@link SourceSampleConfigurer}.
     * <p>
     * <p> The method is called by {@link #initialize()}.
     *
     * @param sampleConfigurer The configurer that defines the layout of a pixel.
     * @throws OperatorException If the source samples cannot be configured.
     */
    protected abstract void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException;

    /**
     * Configures all target samples computed by this operator.
     * Target samples are defined by using the provided {@link TargetSampleConfigurer}.
     * <p>
     * <p> The method is called by {@link #initialize()}.
     *
     * @param sampleConfigurer The configurer that defines the layout of a pixel.
     * @throws OperatorException If the target samples cannot be configured.
     */
    protected abstract void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) throws OperatorException;

    /**
     * Checks if all source products share the same raster size, otherwise throws an exception.
     * Called by {@link #initialize()}.
     *
     * @throws OperatorException If the source product's raster sizes are not equal.
     */
    protected final void checkRasterSize() throws OperatorException {
        Product[] sourceProducts = getSourceProducts();
        ensureSingleRasterSize(sourceProducts);
    }

    Sample[] createSourceSamples(Rectangle targetRectangle, Point location) {
        final Tile[] sourceTiles = getSourceTiles(targetRectangle);
        return createDefaultSamples(sourceRasters, sourceTiles, location);
    }

    Sample createSourceMaskSamples(Rectangle targetRectangle, Point location) {
        final Tile sourceMaskTile = getSourceMaskTile(targetRectangle);
        return sourceMaskTile != null ? new WritableSampleImpl(-1, sourceMaskTile, location) : null;
    }

    WritableSample[] createTargetSamples(Map<Band, Tile> targetTileStack, Point location) {
        final Tile[] targetTiles = getTargetTiles(targetTileStack);
        return createDefaultSamples(targetBands, targetTiles, location);
    }

    WritableSample createTargetSample(Tile targetTile, Point location) {
        final RasterDataNode targetRaster = targetTile.getRasterDataNode();
        for (int i = 0; i < targetBands.length; i++) {
            //noinspection ObjectEquality
            if (targetRaster == targetBands[i]) {
                return new WritableSampleImpl(i, targetTile, location);
            }
        }
        final String msgPattern = "Could not create target sample for band '%s'.";
        throw new IllegalStateException(String.format(msgPattern, targetRaster.getName()));
    }

    private Tile[] getSourceTiles(Rectangle region) {
        final Tile[] sourceTiles = new Tile[sourceRasters.length];
        for (int i = 0; i < sourceTiles.length; i++) {
            if (sourceRasters[i] != null) {
                sourceTiles[i] = getSourceTile(sourceRasters[i], region);
            }
        }
        return sourceTiles;
    }

    private Tile getSourceMaskTile(Rectangle region) {
        if (validPixelMask != null) {
            return getSourceTile(validPixelMask, region);
        }
        return null;
    }

    private Tile[] getTargetTiles(Map<Band, Tile> targetTileStack) {
        final Tile[] targetTiles = new Tile[targetBands.length];
        for (int i = 0; i < targetTiles.length; i++) {
            if (targetBands[i] != null) {
                Tile targetTile = targetTileStack.get(targetBands[i]);
                if (targetTile == null) {
                    final String msgPattern = "Could not find tile for defined target node '%s'.";
                    throw new IllegalStateException(String.format(msgPattern, targetBands[i].getName()));
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

    private abstract static class AbstractSampleConfigurer<T extends RasterDataNode> {

        final List<T> rasters = new ArrayList<>();

        void addRaster(int index, String name, Product product, boolean sourceless) throws OperatorException {
            T node = (T) product.getRasterDataNode(name);
            if (node == null) {
                String message = String.format(
                        "Product '%s' does not contain a raster with name '%s'",
                        product.getName(), name);
                throw new OperatorException(message);
            }
            addRaster(index, node, sourceless);
        }

        void addRaster(int index, T raster, boolean sourceless) throws OperatorException {
            String name = raster.getName();
            if (sourceless && raster.isSourceImageSet()) {
                String message = String.format(
                        "Raster '%s' must be sourceless, since it is a computed target",
                        name);
                throw new OperatorException(message);
            }
            addRaster(index, raster);
        }

        void addRaster(int index, T raster) {
            Assert.notNull(raster, "raster");
            Assert.argument(index >= 0, "index >= 0, was " + index);
            if (index < rasters.size()) {
                Assert.state(rasters.get(index) == null, String.format("raster at index %d already defined", index));
                rasters.set(index, raster);
            } else if (index == rasters.size()) {
                rasters.add(raster);
            } else {
                while (index > rasters.size()) {
                    rasters.add(null);
                }
                rasters.add(raster);
            }
        }

        RasterDataNode[] getRasters() {
            return rasters.toArray(new RasterDataNode[rasters.size()]);
        }
    }

    private final class SourceSampleConfigurerImpl extends AbstractSampleConfigurer<RasterDataNode> implements SourceSampleConfigurer {

        final List<RasterDataNode> computedRasters = new ArrayList<>();

        @Override
        public void defineSample(int index, String name) {
            addRaster(index, name, getSourceProduct(), false);
        }

        @Override
        public void defineSample(int index, String name, Product product) {
            super.addRaster(index, name, product, false);
        }

        @Override
        public void defineComputedSample(int index, int dataType, String expression, Product... sourceProducts) {
            VirtualBand virtualBand = new VirtualBand("__virtual_band_" + index,
                                                      dataType,
                                                      getSourceProduct().getSceneRasterWidth(),
                                                      getSourceProduct().getSceneRasterHeight(),
                                                      expression);
            // Special case:
            // If there are multiple source products, we must create the source image already now.
            // Otherwise virtualBand would create its source image on demand - but without the
            // multi-product context which is required here.
            if (sourceProducts.length > 1) {
                virtualBand.setSourceImage(createVirtualImage(dataType, expression, sourceProducts));
            }
            defineComputedSample(index, virtualBand);
        }

        private MultiLevelImage createVirtualImage(final int dataType, final String expression, final Product[] sourceProducts) {
            // check for RefNo
            for (Product sourceProduct : sourceProducts) {
                if (sourceProduct.getRefNo() == 0) {
                    throw new IllegalArgumentException(String.format("Product '%s' has no assigned reference number.", sourceProduct.getName()));
                }
            }
            Term term = VirtualBandOpImage.parseExpression(expression, 0, sourceProducts);
            Product contextProduct = sourceProducts[0];
            Dimension sourceSize = contextProduct.getSceneRasterSize();
            Dimension tileSize = contextProduct.getPreferredTileSize();
            MultiLevelModel multiLevelModel = contextProduct.createMultiLevelModel();
            MultiLevelSource multiLevelSource = new AbstractMultiLevelSource(multiLevelModel) {
                @Override
                public RenderedImage createImage(int level) {
                    return VirtualBandOpImage.builder(term)
                            .dataType(dataType)
                            .sourceSize(sourceSize)
                            .tileSize(tileSize)
                            .level(ResolutionLevel.create(getModel(), level))
                            .create();
                }
            };
            return new DefaultMultiLevelImage(multiLevelSource);
        }

        @Override
        public void defineComputedSample(int index, int sourceIndex, Kernel kernel) {
            RasterDataNode sourceNode = getSourceNode(sourceIndex);
            defineComputedSample(index, new ConvolutionFilterBand("__convolution_filter_band_" + index, sourceNode, kernel, 1));
        }

        @Override
        public void defineComputedSample(int index, int sourceIndex, GeneralFilterBand.OpType opType, Kernel structuringElement) {
            RasterDataNode sourceNode = getSourceNode(sourceIndex);
            defineComputedSample(index, new GeneralFilterBand("__general_filter_band_" + index, sourceNode, opType, structuringElement, 1));
        }

        @Override
        public void defineComputedSample(int index, RasterDataNode raster) {
            Assert.argument(raster != getTargetProduct().getRasterDataNode(raster.getName()), "raster must not be component of target product");
            if (raster.getOwner() == null) {
                raster.setOwner(getSourceProduct());
            }
            addRaster(index, raster);
            computedRasters.add(raster);
        }

        @Override
        public void setValidPixelMask(String maskExpression) {
            if (maskExpression == null || maskExpression.trim().isEmpty()) {
                return;
            }

            Assert.state(validPixelMask == null, "valid pixel mask already defined");

            if (!getSourceProduct().isCompatibleBandArithmeticExpression(maskExpression)) {
                String msg = String.format("The valid-pixel mask expression '%s' can not be used with the source product.", maskExpression);
                throw new OperatorException(msg);
            }

            validPixelMask = Mask.BandMathsType.create("__source_mask", null,
                                                       getSourceProduct().getSceneRasterWidth(),
                                                       getSourceProduct().getSceneRasterHeight(),
                                                       maskExpression,
                                                       Color.GREEN, 0.0);
            validPixelMask.setOwner(getSourceProduct());
            computedRasters.add(validPixelMask);
        }

        RasterDataNode[] getComputeNodes() {
            return computedRasters.toArray(new RasterDataNode[computedRasters.size()]);
        }

        private RasterDataNode getSourceNode(int index) {
            Assert.argument(rasters.get(index) != null, String.format("no source raster defined at index %s", index));
            return rasters.get(index);
        }
    }

    private final class TargetSampleConfigurerImpl extends AbstractSampleConfigurer<Band> implements TargetSampleConfigurer {

        @Override
        public void defineSample(int index, String name) {
            addRaster(index, name, getTargetProduct(), true);
        }

        @Override
        Band[] getRasters() {
            return rasters.toArray(new Band[rasters.size()]);
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
