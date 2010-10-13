package org.esa.beam.framework.gpf.experimental;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.util.ProductUtils;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class PointOperator extends Operator {

    private transient RasterDataNode[] sourceNodes;
    private transient RasterDataNode[] targetNodes;

    @Override
    public final void initialize() throws OperatorException {

        setTargetProduct(createTargetProduct());

        AbstractConfigurator sc = new SourceConfigurator();
        AbstractConfigurator tc = new TargetConfigurator();
        configureSourceSamples(sc);
        configureTargetSamples(tc);
        sourceNodes = sc.nodes.toArray(new RasterDataNode[sc.nodes.size()]);
        targetNodes = tc.nodes.toArray(new RasterDataNode[tc.nodes.size()]);
    }

    protected Product createTargetProduct() {
        Product sourceProduct = getSourceProduct();
        Product targetProduct = new Product(getId(),
                                            getClass().getName(),
                                            sourceProduct.getSceneRasterWidth(),
                                            sourceProduct.getSceneRasterHeight());
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        configureTargetProduct(targetProduct);
        return targetProduct;
    }

    protected abstract void configureTargetProduct(Product product);

    protected abstract void configureSourceSamples(Configurator configurator);

    protected abstract void configureTargetSamples(Configurator configurator);

    static void setSampleLocations(int x, int y, DefaultSample... sourceSamples) {
        for (final DefaultSample sourceSample : sourceSamples) {
            if (sourceSample != null) {
                sourceSample.setPixel(x, y);
            }
        }
    }

    DefaultSample[] createSourceSamples(Rectangle targetRectangle) {
        final Tile[] sourceTiles = getSourceTiles(targetRectangle);
        return createDefaultSamples(sourceNodes, sourceTiles);
    }

    DefaultSample[] createTargetSamples(Map<Band, Tile> targetTileStack) {
        final Tile[] targetTiles = getTargetTiles(targetTileStack);
        return createDefaultSamples(targetNodes, targetTiles);
    }

    DefaultSample createTargetSample(Tile targetTile) {
        final RasterDataNode targetNode = targetTile.getRasterDataNode();
        for (int i = 0; i < targetNodes.length; i++) {
            if (targetNode == targetNodes[i]) {
                return new DefaultSample(i, targetTile);
            }
        }
        final String msgPattern = "Could not create target sample for band '%s'.";
        throw new IllegalStateException(String.format(msgPattern, targetNode.getName()));
    }

    private Tile[] getSourceTiles(Rectangle region) {
        final Tile[] sourceTiles = new Tile[sourceNodes.length];
        for (int i = 0; i < sourceTiles.length; i++) {
            if (sourceNodes[i] != null) {
                sourceTiles[i] = getSourceTile(sourceNodes[i], region, ProgressMonitor.NULL);
            }
        }
        return sourceTiles;
    }

    private Tile[] getTargetTiles(Map<Band, Tile> targetTileStack) {
        final Tile[] targetTiles = new Tile[targetNodes.length];
        for (int i = 0; i < targetTiles.length; i++) {
            Tile targetTile = targetTileStack.get(targetNodes[i]);
            if (targetTile == null) {
                final String msgPattern = "Could not find tile for defined target node '%s'.";
                throw new IllegalStateException(String.format(msgPattern, targetNodes[i].getName()));
            }
            targetTiles[i] = targetTile;
        }
        return targetTiles;
    }

    private static DefaultSample[] createDefaultSamples(RasterDataNode[] nodes, Tile[] tiles) {
        DefaultSample[] samples = new DefaultSample[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] != null) {
                samples[i] = new DefaultSample(i, tiles[i]);
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

    public static class DefaultSample implements WritableSample {

        private int index;
        private RasterDataNode node;
        private int dataType;
        private int pixelX;
        private int pixelY;
        private Tile tile;

        protected DefaultSample(int index, Tile tile) {
            this.index = index;
            this.node = tile.getRasterDataNode();
            this.dataType = this.node.getGeophysicalDataType();
            this.tile = tile;
        }

        void setPixel(int x, int y) {
            pixelX = x;
            pixelY = y;
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
            return tile.getSampleBit(pixelX, pixelY, bitIndex);
        }

        @Override
        public boolean getBoolean() {
            return tile.getSampleBoolean(pixelX, pixelY);
        }

        @Override
        public int getInt() {
            return tile.getSampleInt(pixelX, pixelY);
        }

        @Override
        public float getFloat() {
            return tile.getSampleFloat(pixelX, pixelY);
        }

        @Override
        public double getDouble() {
            return tile.getSampleDouble(pixelX, pixelY);
        }

        @Override
        public void set(int bitIndex, boolean v) {
            tile.setSample(pixelX, pixelY, bitIndex, v);
        }

        @Override
        public void set(boolean v) {
            tile.setSample(pixelX, pixelY, v);
        }

        @Override
        public void set(int v) {
            tile.setSample(pixelX, pixelY, v);
        }

        @Override
        public void set(float v) {
            tile.setSample(pixelX, pixelY, v);
        }

        @Override
        public void set(double v) {
            tile.setSample(pixelX, pixelY, v);
        }
    }

    private abstract static class AbstractConfigurator implements Configurator {

        private final List<RasterDataNode> nodes = new ArrayList<RasterDataNode>();

        @Override
        public void defineSample(int index, String name, Product product) {
            addNode(index, product.getRasterDataNode(name), nodes);
        }

        private void addNode(int index, RasterDataNode node, List<RasterDataNode> nodes) {
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

    private class SourceConfigurator extends AbstractConfigurator {

        @Override
        public void defineSample(int index, String name) {
            defineSample(index, name, getSourceProduct());
        }
    }

    private class TargetConfigurator extends AbstractConfigurator {

        @Override
        public void defineSample(int index, String name) {
            defineSample(index, name, getTargetProduct());
        }
    }
}
