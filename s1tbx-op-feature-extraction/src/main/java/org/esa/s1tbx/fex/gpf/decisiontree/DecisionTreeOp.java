
package org.esa.s1tbx.fex.gpf.decisiontree;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConverterRegistry;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.dataop.barithm.RasterDataEvalEnv;
import org.esa.snap.core.dataop.barithm.RasterDataSymbol;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.jexp.Namespace;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.jexp.Parser;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.jexp.impl.ParserImpl;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Perform decision tree classification of a given product
 */

@OperatorMetadata(alias = "DecisionTree",
        category = "Raster/Classification",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2019 SkyWatch Space Applications Inc.",
        description = "Perform decision tree classification")
public final class DecisionTreeOp extends Operator {

    @SourceProducts
    private Product[] sourceProducts;
    @TargetProduct
    private Product targetProduct;

    @Parameter(converter = TreeNodeConverter.class, itemAlias = "treeNode")
    private DecisionTreeNode[] decisionTree;

    private Product[] availableProducts;
    private ProductSetNamespace namespaceManager;

    private String targetBandName = "classes";

    static {
        TreeNodeConverter.registerConverter();
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.snap.core.datamodel.Product} annotated with the
     * {@link org.esa.snap.core.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it  has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.snap.core.gpf.OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            final DecisionTreeNode treeRoot = decisionTree[0];

            if (!treeRoot.isConnected()) {
                DecisionTreeNode.connectNodes(decisionTree);
            }

            createTargetProduct();

            availableProducts = getAvailableProducts(sourceProducts);

            namespaceManager = new ProductSetNamespace(availableProducts);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    public static Product[] getAvailableProducts(final Product[] sourceProducts) {
        final java.util.List<Product> availableProductsList = new ArrayList<>();
        for (Product srcProduct : sourceProducts) {
            boolean found = false;
            for (Product prod : availableProductsList) {
                if (srcProduct.getName().equals(prod.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                availableProductsList.add(srcProduct);
            }
        }
        return availableProductsList.toArray(new Product[availableProductsList.size()]);
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product("Decision Tree Classification",
                sourceProducts[0].getProductType(),
                sourceProducts[0].getSceneRasterWidth(),
                sourceProducts[0].getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProducts[0], targetProduct);

        // add index coding
        final IndexCoding indexCoding = createIndexCoding();
        targetProduct.getIndexCodingGroup().add(indexCoding);

        final Band targetBand = new Band(targetBandName,
                ProductData.TYPE_INT8,
                targetProduct.getSceneRasterWidth(),
                targetProduct.getSceneRasterHeight());

        targetBand.setUnit("class");
        targetBand.setNoDataValue(0);
        targetBand.setNoDataValueUsed(true);
        targetProduct.addBand(targetBand);

        targetBand.setSampleCoding(indexCoding);
    }

    public IndexCoding createIndexCoding() {
        final IndexCoding indexCoding = new IndexCoding("Cluster_classes");
        indexCoding.addIndex("no data", 0, "no data");
        for (DecisionTreeNode n : decisionTree) {
            if (n.isLeaf()) {
                if (StringUtils.isIntegerString(n.getExpression())) {
                    try {
                        final Integer classVal = Integer.parseInt(n.getExpression());
                        final String className = "class_" + classVal;
                        if (indexCoding.getIndex(className) == null) {
                            indexCoding.addIndex(className, classVal, "Cluster " + classVal);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return indexCoding;
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.snap.core.gpf.OperatorException If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        final Rectangle rect = targetTile.getRectangle();
        final RasterDataEvalEnv env = new RasterDataEvalEnv(rect.x, rect.y, rect.width, rect.height);

        try {
            final Map<DecisionTreeNode, Term> termMap = new HashMap<>();
            for (DecisionTreeNode n : decisionTree) {
                if (n.getExpression().isEmpty())
                    throw new OperatorException("Decision node cannot be empty");

                final Term term = createTerm(n.getExpression());
                if (!n.isLeaf() && term.getRetType() != Term.TYPE_B) {
                    throw new Exception("Node " + n.getExpression() + " is not a boolean expression");
                }

                final RasterDataSymbol[] refRasterDataSymbols = BandArithmetic.getRefRasterDataSymbols(term);
                for (RasterDataSymbol symbol : refRasterDataSymbols) {
                    final Tile tile = getSourceTile(symbol.getRaster(), rect);
                    symbol.setData(tile.getRawSamples());
                }

                termMap.put(n, term);
            }
            final DecisionTreeNode treeRoot = decisionTree[0];

            pm.beginTask("Evaluating expression", rect.height);
            int pixelIndex = 0;
            for (int y = rect.y; y < rect.y + rect.height; y++) {
                if (pm.isCanceled()) {
                    break;
                }
                for (int x = rect.x; x < rect.x + rect.width; x++) {
                    env.setElemIndex(pixelIndex);

                    DecisionTreeNode node = treeRoot;
                    while (!node.isLeaf()) {
                        final Term term = termMap.get(node);

                        if (term.evalB(env)) {
                            node = node.getTrueNode();
                        } else {
                            node = node.getFalseNode();
                        }
                    }

                    final Term term = termMap.get(node);
                    double val = term.evalD(env);
                    targetTile.setSample(x, y, val);
                    pixelIndex++;
                }
                pm.worked(1);
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    private Term createTerm(final String expression) {
        final Namespace namespace = namespaceManager.createNamespace(0);
        final Term term;
        try {
            final Parser parser = new ParserImpl(namespace, false);
            term = parser.parse(expression);
        } catch (ParseException e) {
            throw new OperatorException("Could not parse expression: " + expression, e);
        }
        return term;
    }

    public static class TreeNodeConverter implements Converter<DecisionTreeNode> {

        @Override
        public Class<? extends DecisionTreeNode> getValueType() {
            return DecisionTreeNode.class;
        }

        @Override
        public DecisionTreeNode parse(String text) throws ConversionException {
            try {
                return DecisionTreeNode.parse(text);
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }

        @Override
        public String format(DecisionTreeNode value) {
            return value.toString();
        }

        public static void registerConverter() {
            TreeNodeConverter converter = new TreeNodeConverter();
            ConverterRegistry.getInstance().setConverter(DecisionTreeNode.class, converter);
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.snap.core.gpf.OperatorSpi#createOperator()
     * @see org.esa.snap.core.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(DecisionTreeOp.class);
        }
    }
}