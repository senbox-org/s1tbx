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
package org.esa.beam.framework.dataop.barithm;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.jexp.*;
import com.bc.jexp.impl.*;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.Guardian;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides band arithmetic utility methods.
 */
public class BandArithmetic {

    public static final String PIXEL_X_NAME = "X";
    public static final String PIXEL_Y_NAME = "Y";

    private static final Symbol PIXEL_X_SYMBOL = new AbstractSymbol.D(PIXEL_X_NAME) {
        @Override
        public double evalD(EvalEnv env) throws EvalException {
            return ((RasterDataEvalEnv) env).getPixelX() + 0.5;
        }
    };

    private static final Symbol PIXEL_Y_SYMBOL = new AbstractSymbol.D(PIXEL_Y_NAME) {
        @Override
        public double evalD(EvalEnv env) throws EvalException {
            return ((RasterDataEvalEnv) env).getPixelY() + 0.5;
        }
    };

    private static final WritableNamespace DEFAULT_NAMESPACE = new DefaultNamespace();

    private static final List<NamespaceExtender> _namespaceExtenderList = new ArrayList<NamespaceExtender>();

    static {
        MoreFuncs.registerExtraFunctions();
        MoreFuncs.registerExtraSymbols();
    }

    private BandArithmetic() {
    }

    public static void addNamespaceExtender(NamespaceExtender ne) {
        if (ne != null && !_namespaceExtenderList.contains(ne)) {
            _namespaceExtenderList.add(ne);
        }
    }

    public static void removeNamespaceExtender(NamespaceExtender ne) {
        _namespaceExtenderList.remove(ne);
    }

    /**
     * Registers a new symbol
     *
     * @param s the symbol
     */
    public static void registerSymbol(Symbol s) {
        DEFAULT_NAMESPACE.registerSymbol(s);
    }

    /**
     * De-registers an existing symbol
     *
     * @param s the symbol
     */
    public static void deregisterSymbol(Symbol s) {
        DEFAULT_NAMESPACE.deregisterSymbol(s);
    }

    /**
     * Registers a new function
     *
     * @param f the function
     */
    public static void registerFunction(Function f) {
        DEFAULT_NAMESPACE.registerFunction(f);
    }

    /**
     * De-registers an existing function
     *
     * @param f the function
     */
    public static void deregisterFunction(Function f) {
        DEFAULT_NAMESPACE.deregisterFunction(f);
    }

    /**
     * Parses the given expression.
     *
     * @param expression          the expression
     * @param products            the array of input products
     * @param defaultProductIndex the index of the product for which also symbols without the
     *                            product prefix <code>$<i>ref-no</i></code> are registered in the namespace
     * @return the compiled expression
     * @throws ParseException if a parse error occurs
     */
    public static Term parseExpression(String expression, Product[] products, int defaultProductIndex) throws
            ParseException {
        Assert.notNull(expression, null);
        final Namespace namespace = createDefaultNamespace(products, defaultProductIndex);
        final Parser parser = new ParserImpl(namespace, false);
        return parser.parse(expression);
    }

    /**
     * Creates a default namespace for the product(s) given in an array. The resulting namespace contains symbols for
     * all tie-point grids, bands and single flag values. if the array contains more then one product, the symbol's name
     * will have a prefix according to each product's reference number.
     *
     * @param products            the array of input products
     * @param defaultProductIndex the index of the product for which also symbols without the
     *                            product prefix <code>$<i>ref-no</i></code> are registered in the namespace
     * @return a default namespace, never <code>null</code>
     */
    public static WritableNamespace createDefaultNamespace(Product[] products, int defaultProductIndex) {
        return createDefaultNamespace(products,
                                      defaultProductIndex,
                                      new ProductPrefixProvider() {
                                          @Override
                                          public String getPrefix(Product product) {
                                              return getProductNodeNamePrefix(product);
                                          }
                                      });
    }

    /**
     * Creates a default namespace for the product(s) given in an array. The resulting namespace contains symbols for
     * all tie-point grids, bands and single flag values. if the array contains more then one product, the symbol's name
     * will have a prefix according to each product's reference number.
     *
     * @param products            the array of input products
     * @param defaultProductIndex the index of the product for which also symbols without the
     *                            product prefix <code>$<i>ref-no</i></code> are registered in the namespace
     * @param prefixProvider      a product prefix provider
     * @return a default namespace, never <code>null</code>
     */
    public static WritableNamespace createDefaultNamespace(Product[] products, int defaultProductIndex,
                                                           ProductPrefixProvider prefixProvider) {
        Guardian.assertNotNullOrEmpty("products", products);
        Guardian.assertWithinRange("defaultProductIndex", defaultProductIndex, 0, products.length);

        WritableNamespace namespace = new NamespaceImpl(DEFAULT_NAMESPACE);

        // Register symbols for default product without name prefix
        registerProductSymbols(namespace, products[defaultProductIndex], "");

        // Register symbols for multiple products using a name prefix
        if (products.length > 1) {
            for (Product product : products) {
                registerProductSymbols(namespace, product, prefixProvider.getPrefix(product));
            }
        }

        namespace.registerSymbol(PIXEL_X_SYMBOL);
        namespace.registerSymbol(PIXEL_Y_SYMBOL);

        return namespace;
    }

    /**
     * @deprecated Since BEAM 4.10. Use {@link VirtualBand} or {@link org.esa.beam.jai.VirtualBandOpImage}.
     */
    @Deprecated
    public static int computeBand(final String expression,
                                  final String validMaskExpression,
                                  final Product[] sourceProducts,
                                  final int defaultProductIndex,
                                  final boolean checkInvalids,
                                  final boolean noDataValueUsed,
                                  final double noDataValue,
                                  final int offsetX,
                                  final int offsetY,
                                  final int width,
                                  final int height,
                                  final ProductData targetRasterData,
                                  final Scaling scaling,
                                  ProgressMonitor pm) throws ParseException, IOException {
        final Term term = parseExpression(expression, sourceProducts, defaultProductIndex);
        final Term validMaskTerm;
        if (validMaskExpression != null && !validMaskExpression.trim().isEmpty()) {
            validMaskTerm = parseExpression(validMaskExpression, sourceProducts, defaultProductIndex);
        } else {
            validMaskTerm = null;
        }
        return computeBand(term, validMaskTerm,
                           checkInvalids, noDataValueUsed, noDataValue,
                           offsetX, offsetY, width, height,
                           targetRasterData, scaling, pm);
    }

    /**
     * @deprecated Since BEAM 4.10. Use {@link VirtualBand} or {@link org.esa.beam.jai.VirtualBandOpImage}.
     */
    @Deprecated
    public static int computeBand(final Term term,
                                  final Term validMaskTerm,
                                  final boolean checkInvalids,
                                  final boolean noDataValueUsed,
                                  final double noDataValue,
                                  final int offsetX,
                                  final int offsetY,
                                  final int width,
                                  final int height,
                                  final ProductData targetRasterData,
                                  final Scaling scaling,
                                  ProgressMonitor pm) throws IOException {

        final boolean performInvalidCheck = checkInvalids || noDataValueUsed;
        final int[] numInvalidPixels = new int[]{0};

        final RasterDataLoop loop = new RasterDataLoop(offsetX, offsetY,
                                                       width, height,
                                                       validMaskTerm != null ? new Term[]{
                                                               term, validMaskTerm
                                                       } : new Term[]{term},
                                                       pm);
        loop.forEachPixel(new RasterDataLoop.Body() {
            @Override
            public void eval(RasterDataEvalEnv env, int pixelIndex) {
                double pixelValue = term.evalD(env);
                if (performInvalidCheck) {
                    if ((validMaskTerm != null && validMaskTerm.evalD(env) == 0.0) || isInvalidValue(pixelValue)) {
                        numInvalidPixels[0]++;
                        if (noDataValueUsed) {
                            pixelValue = noDataValue;
                        }
                    }
                }
                if (scaling != null) {
                    targetRasterData.setElemDoubleAt(pixelIndex, scaling.scaleInverse(pixelValue));
                } else {
                    targetRasterData.setElemDoubleAt(pixelIndex, pixelValue);
                }
            }
        }, "Performing band math...");
        return numInvalidPixels[0];
    }

    public static String getValidMaskExpression(String expression,
                                                Product[] products,
                                                int defaultProductIndex,
                                                String validMaskExpression) throws ParseException {
        Assert.notNull(expression, "expression");
        Assert.notNull(products, "products");
        Assert.argument(products.length > 0, "products");
        Assert.argument(defaultProductIndex >= 0 && defaultProductIndex < products.length, "defaultProductIndex");

        final RasterDataNode[] rasters = getRefRasters(expression, products, defaultProductIndex);
        if (rasters.length == 0) {
            return validMaskExpression;
        }
        final Product contextProduct = products[defaultProductIndex];
        if (validMaskExpression == null && rasters.length == 1 && contextProduct == rasters[0].getProduct()) {
            return rasters[0].getValidMaskExpression();
        }

        final List<String> vmes = new ArrayList<String>(rasters.length);
        for (RasterDataNode raster : rasters) {
            String vme = raster.getValidMaskExpression();
            if (vme != null) {
                if (raster.getProduct() != contextProduct) {
                    final int productIndex = getProductIndex(products, raster);
                    Assert.state(productIndex >= 0, "productIndex >= 0");
                    vme = createUnambiguousExpression(vme, products, productIndex);
                }
                if (!vmes.contains(vme)) {
                    vmes.add(vme);
                }
            }
        }

        final StringBuilder sb = new StringBuilder();
        if (validMaskExpression != null) {
            sb.append("(");
            sb.append(validMaskExpression);
            sb.append(")");
        }
        for (String vme : vmes) {
            if (sb.length() > 0) {
                sb.append(" && ");
            }
            sb.append("(");
            sb.append(vme);
            sb.append(")");
        }
        return sb.toString();
    }

    private static String createUnambiguousExpression(String vme, Product[] products, int productIndex) throws
            ParseException {
        RasterDataNode[] rasters = getRefRasters(vme, products, productIndex);
        for (RasterDataNode raster : rasters) {
            String name = raster.getName();
            int namePos = 0;
            boolean changed;
            do {  // } while (changed)
                changed = false;
                namePos = vme.indexOf(name, namePos);
                if (namePos == 0) {
                    String prefix = getProductNodeNamePrefix(raster.getProduct());
                    vme = prefix + vme;
                    namePos += name.length() + prefix.length();
                    changed = true;
                } else if (namePos > 0) {
                    int i1 = namePos - 1;
                    int i2 = namePos + name.length();
                    char c1 = vme.charAt(i1);
                    char c2 = i2 < vme.length() ? vme.charAt(i2) : '\0';
                    if (c1 != '.' && !isNameChar(c1) && !isNameChar(c2)) {
                        String prefix = getProductNodeNamePrefix(raster.getProduct());
                        vme = vme.substring(0, namePos) + prefix + vme.substring(namePos);
                        namePos += name.length() + prefix.length();
                        changed = true;
                    }
                }
            } while (changed);
        }
        return vme;
    }

    private static boolean isNameChar(char c) {
        return Character.isJavaIdentifierStart(c) || Character.isJavaIdentifierPart(c);
    }

    private static int getProductIndex(Product[] products, RasterDataNode raster) {
        int productIndex = -1;
        for (int i = 0; i < products.length; i++) {
            Product product = products[i];
            if (product == raster.getProduct()) {
                productIndex = i;
                break;
            }
        }
        return productIndex;
    }

    public static RasterDataNode[] getRefRasters(String expression, Product... products) throws ParseException {
        return getRefRasters(expression, products, 0);
    }

    public static RasterDataNode[] getRefRasters(String expression, Product[] products,
                                                 int defaultProductNamePrefix) throws ParseException {
        RasterDataSymbol[] symbols = getRefRasterDataSymbols(
                new Term[]{parseExpression(expression, products, defaultProductNamePrefix)});
        RasterDataNode[] rasters = new RasterDataNode[symbols.length];
        for (int i = 0; i < symbols.length; i++) {
            rasters[i] = symbols[i].getRaster();
        }
        return rasters;
    }

    /**
     * Utility method which returns all raster data nodes referenced in a given array of raster data symbols.
     * The given <code>rasterDataSymbols</code> argument can contain multiple references to the same raster data node,
     * e.g. if multilple {@link SingleFlagSymbol}s refer to the same raster.
     *
     * @param rasterDataSymbols the array to be analysed
     * @return the array of raster data nodes, never <code>null</code> but may be empty
     */
    public static RasterDataNode[] getRefRasters(RasterDataSymbol[] rasterDataSymbols) {
        Set<RasterDataNode> set = new HashSet<RasterDataNode>(rasterDataSymbols.length * 2);
        List<RasterDataNode> list = new ArrayList<RasterDataNode>(rasterDataSymbols.length);
        for (RasterDataSymbol symbol : rasterDataSymbols) {
            RasterDataNode raster = symbol.getRaster();
            if (!set.contains(raster)) {
                list.add(raster);
                set.add(raster);
            }
        }
        return list.toArray(new RasterDataNode[list.size()]);
    }

    /**
     * Utility method which returns all raster data symbols references in a given term.
     *
     * @param term the term to be analysed
     * @return the array of raster data symbols, never <code>null</code> but may be empty
     */
    public static RasterDataSymbol[] getRefRasterDataSymbols(Term term) {
        return getRefRasterDataSymbols(new Term[]{term});
    }

    /**
     * Utility method which returns all raster data symbols references in a given term array.
     * The order of the returned rasters is the order they appear in the given terms.
     *
     * @param terms the term array to be analysed
     * @return the array of raster data symbols, never <code>null</code> but may be empty
     */
    public static RasterDataSymbol[] getRefRasterDataSymbols(Term[] terms) {
        List<RasterDataSymbol> list = new ArrayList<RasterDataSymbol>();
        Set<RasterDataSymbol> set = new HashSet<RasterDataSymbol>();
        for (final Term term : terms) {
            if (term != null) {
                collectRefRasterDataSymbols(term, list, set);
            }
        }
        return list.toArray(new RasterDataSymbol[list.size()]);
    }

    /**
     * Create an external name from the given name.
     * If the given name contains character which are not valid in an external name
     * the name is escaped with single quotes.
     * <p>The method simply delgates to {@link Tokenizer#createExternalName(String)}.
     *
     * @param name the name
     * @return a valid external name
     */
    public static String createExternalName(final String name) {
        return Tokenizer.createExternalName(name);
    }

    /**
     * Gets a symbol name prefix for the names of bands, tie point grids, flags, etc. of the given product.
     * The prefix is of the general form <code>"$<i>refNo</i>."</code> where <i>refNo</i> is the product's reference
     * number returned by {@link org.esa.beam.framework.datamodel.Product#getRefNo()}.
     *
     * @param product the product, must not be <code>null</code>
     * @return a node name prefix, never null.
     */
    public static String getProductNodeNamePrefix(Product product) {
        Guardian.assertNotNull("product", product);
        StringBuffer sb = new StringBuffer();
        sb.append('$');
        sb.append(product.getRefNo());
        sb.append('.');
        return sb.toString();
    }

    private static void registerProductSymbols(WritableNamespace namespace,
                                               Product product,
                                               String namePrefix) {
        registerTiePointGridSymbols(namespace, product, namePrefix);
        registerBandSymbols(namespace, product, namePrefix);
        registerMaskSymbols(namespace, product, namePrefix);
        registerSingleFlagSymbols(namespace, product, namePrefix);
        informNamespaceExtenders(namespace, product, namePrefix);
    }

    private static void registerTiePointGridSymbols(WritableNamespace namespace,
                                                    Product product,
                                                    String namePrefix) {
        for (int i = 0; i < product.getNumTiePointGrids(); i++) {
            final TiePointGrid grid = product.getTiePointGridAt(i);
            final String symbolName = namePrefix + grid.getName();
            namespace.registerSymbol(new RasterDataSymbol(symbolName, grid, RasterDataSymbol.GEOPHYSICAL));
        }
    }

    private static void registerBandSymbols(WritableNamespace namespace,
                                            Product product,
                                            String namePrefix) {
        for (int i = 0; i < product.getNumBands(); i++) {
            final Band band = product.getBandAt(i);
            final String symbolName = namePrefix + band.getName();
            namespace.registerSymbol(new RasterDataSymbol(symbolName, band, RasterDataSymbol.GEOPHYSICAL));
            namespace.registerSymbol(new RasterDataSymbol(symbolName + ".raw", band, RasterDataSymbol.RAW));
        }
    }

    private static void registerMaskSymbols(WritableNamespace namespace,
                                            Product product,
                                            String namePrefix) {
        for (int i = 0; i < product.getMaskGroup().getNodeCount(); i++) {
            final Mask mask = product.getMaskGroup().get(i);
            final String symbolName = namePrefix + mask.getName();
            namespace.registerSymbol(new RasterDataSymbol(symbolName, mask));
        }
    }

    private static void registerSingleFlagSymbols(WritableNamespace namespace,
                                                  Product product,
                                                  String namePrefix) {
        for (int i = 0; i < product.getNumBands(); i++) {
            final Band band = product.getBandAt(i);
            if (band.getFlagCoding() != null) {
                for (int j = 0; j < band.getFlagCoding().getNumAttributes(); j++) {
                    final MetadataAttribute attribute = band.getFlagCoding().getAttributeAt(j);
                    final int flagMask = attribute.getData().getElemInt();
                    final String symbolName = namePrefix + band.getName() + "." + attribute.getName();
                    final Symbol symbol = new SingleFlagSymbol(symbolName, band, flagMask);
                    namespace.registerSymbol(symbol);
                }
            }
        }
    }

    private static void informNamespaceExtenders(WritableNamespace namespace,
                                                 Product product,
                                                 String namePrefix) {
        for (Object a_namespaceExtenderList : _namespaceExtenderList) {
            NamespaceExtender namespaceExtender = (NamespaceExtender) a_namespaceExtenderList;
            namespaceExtender.extendNamespace(namespace, product, namePrefix);
        }
    }

    private static void collectRefRasterDataSymbols(Term term, List<RasterDataSymbol> list, Set<RasterDataSymbol> set) {
        if (term == null) {
            return;
        }
        if (term instanceof Term.Ref) {
            Symbol symbol = ((Term.Ref) term).getSymbol();
            if (symbol instanceof RasterDataSymbol) {
                RasterDataSymbol rds = (RasterDataSymbol) symbol;
                if (!set.contains(rds)) {
                    list.add(rds);
                    set.add(rds);
                }
            }
        } else {
            Term[] children = term.getChildren();
            for (Term child : children) {
                collectRefRasterDataSymbols(child, list, set);
            }
        }
    }

    private static boolean isInvalidValue(double pixelValue) {
        return Double.isNaN(pixelValue) || Double.isInfinite(pixelValue);
    }

    public static interface NamespaceExtender {

        void extendNamespace(WritableNamespace namespace,
                             Product product,
                             String namePrefix);
    }

    public static interface ProductPrefixProvider {

        String getPrefix(Product product);
    }
}
