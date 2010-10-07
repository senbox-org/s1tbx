package org.esa.beam.framework.gpf.experimental;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
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
        Product sourceProduct = getSourceProducts()[0]; // todo - using getSourceProduct() throws NPE 
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

    protected abstract void configureSourceSamples(Configurator c);

    protected abstract void configureTargetSamples(Configurator c);

    static void loadSourceSamples(int x, int y, Tile[] sourceTiles, WritableSample[] sourceSamples) {
        for (int i = 0; i < sourceSamples.length; i++) {
            final WritableSample sourceSample = sourceSamples[i];
            final Tile sourceTile = sourceTiles[i];
            sourceSample.set(sourceTile.getSampleDouble(x, y));
        }
    }

    static void storeTargetSamples(int x, int y, Sample[] targetSamples, Tile[] targetTiles) {
        for (int i = 0; i < targetSamples.length; i++) {
            final Sample targetSample = targetSamples[i];
            final Tile targetTile = targetTiles[i];
            targetTile.setSample(x, y, targetSample.getDouble());
        }
    }

    Tile[] getSourceTiles(Rectangle region) {
        final Tile[] sourceTiles = new Tile[sourceNodes.length];
        for (int i = 0; i < sourceTiles.length; i++) {
            sourceTiles[i] = getSourceTile(sourceNodes[i], region, ProgressMonitor.NULL);
        }
        return sourceTiles;
    }

    Tile[] getTargetTiles(Map<Band, Tile> targetTileStack) {
        final Tile[] targetTiles = new Tile[targetNodes.length];
        for (int i = 0; i < targetTiles.length; i++) {
            Tile targetTile = targetTileStack.get(targetNodes[i]);
            if (targetTile == null) {
                throw new IllegalStateException(); // todo - add message
            }
            targetTiles[i] = targetTile;
        }
        return targetTiles;
    }

    WritableSample[] createSourceSamples() {
        return createWritableSamples(sourceNodes);
    }

    WritableSample[] createTargetSamples() {
        return createWritableSamples(targetNodes);
    }

    WritableSample createTargetSample(Band targetBand) {
        for (int i = 0; i < targetNodes.length; i++) {
            if (targetBand == targetNodes[i]) {
                return createWritableSample(i, targetBand);
            }
        }
        throw new IllegalStateException(); // todo - add message
    }


    WritableSample[] createWritableSamples(RasterDataNode[] nodes) {
        WritableSample[] samples = new WritableSample[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            samples[i] = createWritableSample(i, nodes[i]);
        }
        return samples;
    }

    static WritableSample createWritableSample(int index, RasterDataNode node) {
        switch (node.getGeophysicalDataType()) {
            case ProductData.TYPE_UINT8:
                return new ByteSample(index, node);
            case ProductData.TYPE_INT16:
                return new ShortSample(index, node);
            case ProductData.TYPE_INT32:
                return new IntSample(index, node);
            case ProductData.TYPE_FLOAT32:
                return new FloatSample(index, node);
            case ProductData.TYPE_FLOAT64:
                return new DoubleSample(index, node);
            default:
                throw new IllegalStateException(); // todo - add message
        }
    }

    public static interface Configurator {

        void defineSample(int index, String name);

        void defineSample(int index, String name, Product sourceProduct);
    }

    public static interface Sample {

        RasterDataNode getNode();

        int getIndex();

        int getDataType();

        boolean getBoolean();

        int getInt();

        float getFloat();

        double getDouble();
    }

    public static interface WritableSample extends Sample {

        void set(boolean v);

        void set(int v);

        void set(float v);

        void set(double v);
    }

    private static abstract class AbstractWritableSample implements WritableSample {

        private final int index;
        private final RasterDataNode node;
        private final int dataType;

        protected AbstractWritableSample(int index, RasterDataNode node) {
            this.index = index;
            this.node = node;
            this.dataType = node.getGeophysicalDataType();
        }

        @Override
        public final int getIndex() {
            return index;
        }

        @Override
        public final RasterDataNode getNode() {
            return node;
        }

        @Override
        public final int getDataType() {
            return dataType;
        }

        @Override
        public final boolean getBoolean() {
            return getInt() != 0;
        }

        @Override
        public final void set(boolean v) {
            set(v ? 1 : 0);
        }
    }

    private static final class ByteSample extends AbstractWritableSample {

        private byte value;

        private ByteSample(int index, RasterDataNode node) {
            super(index, node);
        }

        @Override
        public int getInt() {
            return value & 0xff;
        }

        @Override
        public float getFloat() {
            return value & 0xff;
        }

        @Override
        public double getDouble() {
            return value & 0xff;
        }

        @Override
        public void set(int v) {
            value = (byte) v;
        }

        @Override
        public void set(float v) {
            value = (byte) Math.floor(v + 0.5f);
        }

        @Override
        public void set(double v) {
            value = (byte) Math.floor(v + 0.5);
        }
    }

    private static final class ShortSample extends AbstractWritableSample {

        private short value;

        private ShortSample(int index, RasterDataNode node) {
            super(index, node);
        }

        @Override
        public int getInt() {
            return value;
        }

        @Override
        public float getFloat() {
            return value;
        }

        @Override
        public double getDouble() {
            return value;
        }

        @Override
        public void set(int v) {
            value = (short) v;
        }

        @Override
        public void set(float v) {
            value = (short) Math.floor(v + 0.5f);
        }

        @Override
        public void set(double v) {
            value = (short) Math.floor(v + 0.5);
        }
    }

    private static final class IntSample extends AbstractWritableSample {

        private int value;

        private IntSample(int index, RasterDataNode node) {
            super(index, node);
        }

        @Override
        public int getInt() {
            return value;
        }

        @Override
        public float getFloat() {
            return value;
        }

        @Override
        public double getDouble() {
            return value;
        }

        @Override
        public void set(int v) {
            value = v;
        }

        @Override
        public void set(float v) {
            value = (int) Math.floor(v + 0.5f);
        }

        @Override
        public void set(double v) {
            value = (int) Math.floor(v + 0.5);
        }
    }

    private static final class FloatSample extends AbstractWritableSample {

        private float value;

        private FloatSample(int index, RasterDataNode node) {
            super(index, node);
        }

        @Override
        public int getInt() {
            return (int) Math.floor(value + 0.5f);
        }

        @Override
        public float getFloat() {
            return value;
        }

        @Override
        public double getDouble() {
            return value;
        }

        @Override
        public void set(int v) {
            value = v;
        }

        @Override
        public void set(float v) {
            value = v;
        }

        @Override
        public void set(double v) {
            value = (float) v;
        }
    }

    private static final class DoubleSample extends AbstractWritableSample {

        private double value;

        private DoubleSample(int index, RasterDataNode node) {
            super(index, node);
        }

        @Override
        public int getInt() {
            return (int) value;
        }

        @Override
        public float getFloat() {
            return (float) value;
        }

        @Override
        public double getDouble() {
            return value;
        }

        @Override
        public void set(int v) {
            value = v;
        }

        @Override
        public void set(float v) {
            value = v;
        }

        @Override
        public void set(double v) {
            value = v;
        }
    }

    private abstract class AbstractConfigurator implements Configurator {

        final List<RasterDataNode> nodes = new ArrayList<RasterDataNode>();

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
            defineSample(index, name, getSourceProducts()[0]); // todo - using getSourceProduct() throws NPE
        }
    }

    private class TargetConfigurator extends AbstractConfigurator {

        @Override
        public void defineSample(int index, String name) {
            defineSample(index, name, getTargetProduct());
        }
    }
}
