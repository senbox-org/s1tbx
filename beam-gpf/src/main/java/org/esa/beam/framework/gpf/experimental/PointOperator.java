package org.esa.beam.framework.gpf.experimental;

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

public abstract class PointOperator extends Operator {

    private transient RasterDataNode[] sourceNodes;
    private transient Band[] targetNodes;

    @Override
    public final void initialize() throws OperatorException {

        setTargetProduct(createTargetProduct());

        SourceConfigurator sc = new SourceConfigurator();
        TargetConfigurator tc = new TargetConfigurator();
        configureSourceSamples(sc);
        configureTargetSamples(tc);
        sourceNodes = sc.nodes.toArray(new RasterDataNode[sc.nodes.size()]);
        targetNodes = tc.nodes.toArray(new Band[tc.nodes.size()]);
    }

    protected Product createTargetProduct() {
        Product sourceProduct = getSourceProduct();
        Product targetProduct = new Product(getId(),
                                            getClass().getName(),
                                            sourceProduct.getSceneRasterWidth(),
                                            sourceProduct.getSceneRasterHeight());
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        configureTargetProduct(targetProduct);
        return targetProduct;
    }

    protected abstract void configureTargetProduct(Product targetProduct);

    protected abstract void configureSourceSamples(Configurator configurator);

    protected abstract void configureTargetSamples(Configurator configurator);

    DefaultSample[] createSourceSamples(Rectangle targetRectangle, Point location) {
        final Tile[] sourceTiles = getSourceTiles(targetRectangle);
        return createDefaultSamples(sourceNodes, sourceTiles, location);
    }

    DefaultSample[] createTargetSamples(Map<Band, Tile> targetTileStack, Point location) {
        final Tile[] targetTiles = getTargetTiles(targetTileStack);
        return createDefaultSamples(targetNodes, targetTiles, location);
    }

    DefaultSample createTargetSample(Tile targetTile, Point location) {
        final RasterDataNode targetNode = targetTile.getRasterDataNode();
        for (int i = 0; i < targetNodes.length; i++) {
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

    public interface Configurator {

        void defineSample(int index, String name);

        void defineSample(int index, String name, Product sourceProduct);
    }

    public interface Sample {

        RasterDataNode getNode();

        int getIndex();

        int getDataType();

        boolean getBit(int bitIndex);

        boolean getBoolean();

        int getInt();

        float getFloat();

        double getDouble();
    }

    public interface WritableSample extends Sample {

        void set(int bitIndex, boolean v);

        void set(boolean v);

        void set(int v);

        void set(float v);

        void set(double v);
    }

    static final class DefaultSample implements WritableSample {
        static final DefaultSample NULL = new DefaultSample();

        private final int index;
        private final RasterDataNode node;
        private final int dataType;
        private final Tile tile;
        private final Point location;

        protected DefaultSample(int index, Tile tile, Point location) {
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

    private abstract static class AbstractConfigurator<T extends RasterDataNode> implements Configurator {

        final List<T> nodes = new ArrayList<T>();

        @Override
        public void defineSample(int index, String name, Product product) {
            addNode(index, (T) product.getRasterDataNode(name));
        }

        private void addNode(int index, T node) {
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

    }

    private class SourceConfigurator extends AbstractConfigurator<RasterDataNode> {

        @Override
        public void defineSample(int index, String name) {
            defineSample(index, name, getSourceProduct());
        }
    }

    private class TargetConfigurator extends AbstractConfigurator<Band> {

        @Override
        public void defineSample(int index, String name) {
            defineSample(index, name, getTargetProduct());
        }
    }
}
