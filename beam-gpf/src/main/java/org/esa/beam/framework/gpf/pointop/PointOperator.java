package org.esa.beam.framework.gpf.pointop;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.util.ProductUtils;

import java.awt.Point;
import java.awt.Rectangle;
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
     * <li>{@link #createTargetProduct() product = createTargetProduct()}</li>
     * <li>{@link #configureTargetProduct(org.esa.beam.framework.datamodel.Product) configureTargetProduct(product)}</li>
     * <li>{@link #configureSourceSamples(SampleConfigurer)}</li>
     * <li>{@link #configureTargetSamples(SampleConfigurer)}</li>
     * </ol>
     * This method cannot be overridden by intention (<i>template method</i>). Instead override the methods that are called during the initialisation sequence.
     *
     * @throws OperatorException If the configuration cannot be performed.
     */
    @Override
    public final void initialize() throws OperatorException {
        Product product = createTargetProduct();
        configureTargetProduct(product);
        setTargetProduct(product);
        SourceSampleConfigurer sc = new SourceSampleConfigurer();
        TargetSampleConfigurer tc = new TargetSampleConfigurer();
        configureSourceSamples(sc);
        configureTargetSamples(tc);
        sourceNodes = sc.getNodes();
        targetNodes = tc.getNodes();
    }

    /**
     * Creates the target product instance.
     * The default implementation creates a target product instance given the raster size of the (first) source product.
     * Then it sets the target product' start and stop time and copies the tie-point grids and geo-coding.
     *
     * @return A new target product instance.
     * @throws OperatorException If the target product cannot be created.
     */
    protected Product createTargetProduct() throws OperatorException {
        Product sourceProduct = getSourceProduct();
        Product targetProduct = new Product(getId(),
                                            getClass().getName(),
                                            sourceProduct.getSceneRasterWidth(),
                                            sourceProduct.getSceneRasterHeight());
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        return targetProduct;
    }

    /**
     * Configures the given target product instance. Called by {@link #initialize()}.
     * Client implementations of this method usually add product components to the gibven target product, such as
     * {@link Band bands} to be computed by this operator, {@link org.esa.beam.framework.datamodel.VirtualBand virtual bands},
     * {@link org.esa.beam.framework.datamodel.Mask masks} or {@link org.esa.beam.framework.datamodel.SampleCoding sample codings}.
     *
     * @param targetProduct The target product to be configured.
     * @throws OperatorException If the target product cannot be configured.
     */
    protected abstract void configureTargetProduct(Product targetProduct) throws OperatorException;

    /**
     * Configures all source samples that this operator requires for the computation of target samples.
     * Source sample are defined by using the provided {@link SampleConfigurer}.
     *
     * @param sampleConfigurer The configurer that defines the layout of a pixel.
     * @throws OperatorException If the source samples cannot be configured.
     */
    protected abstract void configureSourceSamples(SampleConfigurer sampleConfigurer) throws OperatorException;

    /**
     * Configures all target samples computed by this operator.
     * Target samples are defined by using the provided {@link SampleConfigurer}.
     *
     * @param sampleConfigurer The configurer that defines the layout of a pixel.
     * @throws OperatorException If the target samples cannot be configured.
     */
    protected abstract void configureTargetSamples(SampleConfigurer sampleConfigurer) throws OperatorException;

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
                return new DefaultSample(i, targetTile, location);
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

    private static DefaultSample[] createDefaultSamples(RasterDataNode[] nodes, Tile[] tiles, Point location) {
        DefaultSample[] samples = new DefaultSample[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] != null) {
                samples[i] = new DefaultSample(i, tiles[i], location);
            } else {
                samples[i] = DefaultSample.NULL;
            }
        }
        return samples;
    }

    private static final class DefaultSample implements WritableSample {
        static final DefaultSample NULL = new DefaultSample();

        private final int index;
        private final RasterDataNode node;
        private final int dataType;
        private final Tile tile;
        private final Point location;

        private DefaultSample(int index, Tile tile, Point location) {
            this.index = index;
            this.node = tile.getRasterDataNode();
            this.dataType = this.node.getGeophysicalDataType();
            this.tile = tile;
            this.location = location;
        }

        private DefaultSample() {
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

        @Override
        public void defineSample(int index, String name, Product product) {
            T node = (T) product.getRasterDataNode(name);
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
            defineSample(index, name, getSourceProduct());
        }

    }

    private final class TargetSampleConfigurer extends AbstractSampleConfigurer<Band> {

        @Override
        public void defineSample(int index, String name) {
            defineSample(index, name, getTargetProduct());
        }

        @Override
        Band[] getNodes() {
            return nodes.toArray(new Band[nodes.size()]);
        }
    }
}
