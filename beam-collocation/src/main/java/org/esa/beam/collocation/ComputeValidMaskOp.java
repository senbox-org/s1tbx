package org.esa.beam.collocation;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.jexp.*;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.dataop.barithm.RasterDataSymbol;
import org.esa.beam.framework.dataop.barithm.SingleFlagSymbol;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import java.awt.*;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Operator for evaluating the valid pixel expression for all
 * bands in a product where a valid pixel expression is set.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
@OperatorMetadata(alias = "ComputeValidMask",
        version = "1.0",
        authors = "Ralf Quast",
        copyright = "(c) 2007 by Brockmann Consult",
        description = "Computes the valid mask.")
class ComputeValidMaskOp extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    private transient Map<Band, Term> termMap;
    private transient Map<Term, TileSymbol[]> tileSymbolsMap;

    public ComputeValidMaskOp(Product sourceProduct) {
        this.sourceProduct = sourceProduct;
    }

    public void initialize() throws OperatorException {
        termMap = new HashMap<Band, Term>();
        tileSymbolsMap = new HashMap<Term, TileSymbol[]>();

        targetProduct = new Product("ValidMask", "ValidMask", sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        final Namespace namespace = createNamespace(sourceProduct);

        for (final Band sourceBand : sourceProduct.getBands()) {
            final String expression = sourceBand.getDataMaskExpression();
            try {
                if (expression != null) {
                    final Term term = sourceProduct.createTerm(expression /*, namespace */); // method does not exist

                    if (!tileSymbolsMap.containsKey(term)) {
                        termMap.put(targetProduct.addBand(sourceBand.getName(), ProductData.TYPE_INT8), term);
                        tileSymbolsMap.put(term, getTileSymbols(term));
                    }
                }
            } catch (ParseException e) {
                throw new OperatorException(
                        MessageFormat.format("Error while parsing expression ''{0}''", expression), e);
            }
        }
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final Term term = termMap.get(targetBand);
        final Rectangle rectangle = targetTile.getRectangle();

        for (final TileSymbol symbol : tileSymbolsMap.get(term)) {
            symbol.setTile(getSourceTile(symbol.getRasterDataNode(), rectangle, pm));
        }

        pm.beginTask("Evaluating expression", rectangle.height);
        try {
            final TileEvalEnv env = new TileEvalEnv();
            final ProductData targetDataBuffer = targetTile.getDataBuffer();

            int offset = targetTile.getScanlineOffset();
            for (int y = 0; y < rectangle.height; ++y) {
                if (pm.isCanceled()) {
                    break;
                }
                int index = offset;
                for (int x = 0; x < rectangle.width; ++x) {
                    env.setIndex(index);
                    targetDataBuffer.setElemBooleanAt(index, term.evalB(env));
                    ++index;
                }
                offset += targetTile.getScanlineStride();
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    @Override
    public void dispose() {
        termMap = null;
        tileSymbolsMap = null;
    }

    private static Namespace createNamespace(Product product) {
        final WritableNamespace namespace = product.createBandArithmeticDefaultNamespace();
        final Set<Symbol> rasterDataSymbolSet = new HashSet<Symbol>();

        for (final Symbol symbol : namespace.getAllSymbols()) {
            if (symbol instanceof RasterDataSymbol) {
                rasterDataSymbolSet.add(symbol);
            }
        }
        for (final Symbol symbol : rasterDataSymbolSet) {
            namespace.deregisterSymbol(symbol);
            namespace.registerSymbol(createTileSymbol((RasterDataSymbol) symbol));
        }

        return namespace;
    }

    private static TileSymbol[] getTileSymbols(Term term) {
        final Set<TileSymbol> set = new HashSet<TileSymbol>();
        collectTileSymbols(term, set);

        return set.toArray(new TileSymbol[set.size()]);
    }

    private static void collectTileSymbols(Term term, Set<TileSymbol> set) {
        if (term instanceof Term.Ref) {
            final Symbol symbol = ((Term.Ref) term).getSymbol();
            if (symbol instanceof TileSymbol) {
                set.add((TileSymbol) symbol);
            }
        } else {
            for (final Term child : term.getChildren()) {
                collectTileSymbols(child, set);
            }
        }
    }

    private static TileSymbol createTileSymbol(RasterDataSymbol symbol) {
        if (symbol instanceof SingleFlagSymbol) {
            return new SingleFlagSymbolAdapter((SingleFlagSymbol) symbol);
        } else {
            return new RasterDataSymbolAdapter(symbol);
        }
    }

    private static class TileEvalEnv implements EvalEnv {

        private int x;
        private int y;
        private int index;

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public final int getPixelX() {
            return x;
        }

        public final int getPixelY() {
            return y;
        }

        public final void setPixelX(int x) {
            this.x = x;
        }

        public final void setPixelY(int y) {
            this.y = y;
        }
    }

    private interface TileSymbol extends Symbol {

        RasterDataNode getRasterDataNode();

        void setTile(Tile tile);
    }

    private static class RasterDataSymbolAdapter implements TileSymbol {

        private final String name;
        private final int type;
        private final RasterDataNode rasterDataNode;

        private ProductData dataBuffer;

        public RasterDataSymbolAdapter(RasterDataSymbol symbol) {
            this(symbol.getName(), symbol.getRetType(), symbol.getRaster());
        }

        private RasterDataSymbolAdapter(String name, int type, RasterDataNode rasterDataNode) {
            this.name = name;
            this.type = type;
            this.rasterDataNode = rasterDataNode;
        }

        public String getName() {
            return name;
        }

        public int getRetType() {
            return type;
        }

        public RasterDataNode getRasterDataNode() {
            return rasterDataNode;
        }

        public void setTile(Tile tile) {
            assert (tile.getRasterDataNode() == rasterDataNode);
            this.dataBuffer = tile.getDataBuffer();
        }

//        public boolean evalB(EvalEnv env) throws EvalException {
//            assert (dataBuffer != null);
//            final TileEvalEnv tileEvalEnv = (TileEvalEnv) env;
//            final int x = tileEvalEnv.getPixelX();
//            final int y = tileEvalEnv.getPixelY();
//
//            return tile.getSampleBoolean(x, y);
//        }

        public boolean evalB(EvalEnv env) throws EvalException {
            return evalI(env) != 0;
        }

//        public int evalI(EvalEnv env) throws EvalException {
//            assert (dataBuffer != null);
//            final TileEvalEnv tileEvalEnv = (TileEvalEnv) env;
//            final int x = tileEvalEnv.getPixelX();
//            final int y = tileEvalEnv.getPixelY();
//
//            return tile.getSampleInt(x, y);
//        }

        public int evalI(EvalEnv env) throws EvalException {
            assert (dataBuffer != null);
            final int index = ((TileEvalEnv) env).getIndex();

            int sample = dataBuffer.getElemIntAt(index);
            if (rasterDataNode.isScalingApplied()) {
                sample = (int) Math.floor(rasterDataNode.scale(sample) + 0.5);
            }
            return sample;
        }

//        public double evalD(EvalEnv env) throws EvalException {
//            assert (dataBuffer != null);
//            final TileEvalEnv tileEvalEnv = (TileEvalEnv) env;
//            final int x = tileEvalEnv.getPixelX();
//            final int y = tileEvalEnv.getPixelY();
//
//            return tile.getSampleDouble(x, y);
//        }

        public double evalD(EvalEnv env) throws EvalException {
            assert (dataBuffer != null);
            final int index = ((TileEvalEnv) env).getIndex();

            double sample = dataBuffer.getElemDoubleAt(index);
            if (rasterDataNode.isScalingApplied()) {
                sample = rasterDataNode.scale(sample);
            }
            return sample;
        }

        public String evalS(EvalEnv env) throws EvalException {
            return Double.toString(evalD(env));
        }
    }

    private static class SingleFlagSymbolAdapter extends RasterDataSymbolAdapter {

        private final int mask;

        public SingleFlagSymbolAdapter(SingleFlagSymbol symbol) {
            super(symbol);
            mask = symbol.getFlagMask();
        }

        @Override
        public boolean evalB(final EvalEnv env) throws EvalException {
            return (super.evalI(env) & mask) == mask;
        }

        @Override
        public int evalI(final EvalEnv env) throws EvalException {
            return (super.evalI(env) & mask) == mask ? 1 : 0;
        }

        @Override
        public double evalD(final EvalEnv env) throws EvalException {
            return (super.evalI(env) & mask) == mask ? 1.0 : 0.0;
        }
    }

    /**
     * Valid-pixel expression operator SPI.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ComputeValidMaskOp.class);
        }
    }
}
