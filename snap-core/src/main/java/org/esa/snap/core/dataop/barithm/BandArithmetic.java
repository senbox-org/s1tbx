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
package org.esa.snap.core.dataop.barithm;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.jexp.Function;
import org.esa.snap.core.jexp.Namespace;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.jexp.Parser;
import org.esa.snap.core.jexp.Symbol;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.jexp.WritableNamespace;
import org.esa.snap.core.jexp.impl.DefaultNamespace;
import org.esa.snap.core.jexp.impl.NamespaceImpl;
import org.esa.snap.core.jexp.impl.ParserImpl;
import org.esa.snap.core.jexp.impl.Tokenizer;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides band arithmetic utility methods.
 */
public class BandArithmetic {

    private static final WritableNamespace DEFAULT_NAMESPACE = new DefaultNamespace();

    private BandArithmetic() {
    }

    /**
     * Registers a new global symbol.
     *
     * @param s the symbol
     */
    public static void registerSymbol(Symbol s) {
        DEFAULT_NAMESPACE.registerSymbol(s);
    }

    /**
     * De-registers an existing global symbol.
     *
     * @param s the symbol
     */
    public static void deregisterSymbol(Symbol s) {
        DEFAULT_NAMESPACE.deregisterSymbol(s);
    }

    /**
     * Registers a new global function.
     *
     * @param f the function
     */
    public static void registerFunction(Function f) {
        DEFAULT_NAMESPACE.registerFunction(f);
    }

    /**
     * De-registers an existing global function.
     *
     * @param f the function
     */
    public static void deregisterFunction(Function f) {
        DEFAULT_NAMESPACE.deregisterFunction(f);
    }

    /**
     * Parses the given expression.
     *
     * @param expression     the expression
     * @param contextProduct the context product
     * @return the compiled expression
     * @throws ParseException if a parse error occurs
     */
    public static Term parseExpression(String expression,
                                       Product contextProduct) throws ParseException {
        return parseExpression(expression, new Product[]{contextProduct}, 0);
    }

    /**
     * Parses the given expression.
     *
     * @param expression          the expression
     * @param products            the array of source products which form the valid namespace for the expression
     * @param contextProductIndex the index of the context product for which also symbols without the
     *                            product prefix <code>$<i>ref-no</i></code> are registered in the namespace
     * @return the compiled expression
     * @throws ParseException if a parse error occurs
     */
    public static Term parseExpression(String expression,
                                       Product[] products,
                                       int contextProductIndex) throws ParseException {
        Assert.notNull(expression, null);
        final Namespace namespace = createDefaultNamespace(products, contextProductIndex);
        final Parser parser = new ParserImpl(namespace, false);
        final Term term = parser.parse(expression);
        if (!areRastersEqualInSize(term)) {
            throw new ParseException("Referenced rasters must be of same size");
        }
        return term;
    }

    /**
     * Creates a default namespace for the product(s) given in an array. The resulting namespace contains symbols for
     * all tie-point grids, bands and single flag values. if the array contains more then one product, the symbol's name
     * will have a prefix according to each product's reference number.
     *
     * @param products            the array of source products which form the valid namespace for the expression
     * @param contextProductIndex the index of the context product for which also symbols without the
     *                            product prefix <code>$<i>ref-no</i></code> are registered in the namespace
     * @return a default namespace, never <code>null</code>
     */
    public static WritableNamespace createDefaultNamespace(Product[] products,
                                                           int contextProductIndex) {
        return createDefaultNamespace(products,
                                      contextProductIndex,
                                      BandArithmetic::getProductNodeNamePrefix);
    }

    /**
     * Creates a default namespace for the product(s) given in an array. The resulting namespace contains symbols for
     * all tie-point grids, bands and single flag values. if the array contains more then one product, the symbol's name
     * will have a prefix according to each product's reference number.
     *
     * @param products            the array of source products which form the valid namespace for the expression
     * @param contextProductIndex the index of the context product for which also symbols without the
     *                            product prefix <code>$<i>ref-no</i></code> are registered in the namespace
     * @param prefixProviders     a list of product prefix providers
     * @return a default namespace, never <code>null</code>
     */
    public static WritableNamespace createDefaultNamespace(Product[] products,
                                                           int contextProductIndex,
                                                           ProductNamespacePrefixProvider... prefixProviders) {
        Assert.argument(products.length > 0, "products.length > 0");
        Assert.argument(contextProductIndex >= 0, "contextProductIndex >= 0");
        Assert.argument(contextProductIndex < products.length, "contextProductIndex < products.length");

        WritableNamespace namespace = new NamespaceImpl(DEFAULT_NAMESPACE);
        ProductNamespaceExtenderImpl namespaceExtender = new ProductNamespaceExtenderImpl();

        // Register symbols for default product without name prefix
        namespaceExtender.extendNamespace(products[contextProductIndex], "", namespace);

        // If the namespace comprises multiple products...
        if (products.length > 1) {
            boolean allSet = Arrays.stream(products).allMatch(p -> p.getRefNo() != 0);
            if (!allSet) {
                int cnt = 1;
                for (Product product : products) {
                    product.resetRefNo();
                    product.setRefNo(cnt++);
                }
            }
            // ... register symbols using a name prefix
            for (Product product : products) {
                for (ProductNamespacePrefixProvider prefixProvider : prefixProviders) {
                    namespaceExtender.extendNamespace(product, prefixProvider.getPrefix(product), namespace);
                }
            }
        }

        return namespace;
    }

    /**
     * Utility method which returns all raster data nodes referenced in the given
     * band math expressions.
     *
     * @param expression the expression
     * @param products   the array of source products which form the valid namespace for the expression
     * @return the array of raster data nodes, which may be empty
     */
    public static RasterDataNode[] getRefRasters(String expression, Product... products) throws ParseException {
        return getRefRasters(expression, products, 0);
    }

    /**
     * Utility method which returns all raster data nodes referenced in the given
     * band math expressions.
     *
     * @param expression          the expression
     * @param products            the array of source products which form the valid namespace for the expression
     * @param contextProductIndex the index of the context product for which also symbols without the
     *                            product prefix <code>$<i>ref-no</i></code> are registered in the namespace
     * @return the array of raster data nodes, which may be empty
     */
    public static RasterDataNode[] getRefRasters(String expression,
                                                 Product[] products,
                                                 int contextProductIndex) throws ParseException {
        Term term = parseExpression(expression, products, contextProductIndex);
        return getRefRasters(term);
    }

    /**
     * Utility method which returns all raster data nodes referenced in the given
     * compiled band math expressions.
     *
     * @param terms the array of terms to be analysed
     * @return the array of raster data nodes, which may be empty
     */
    public static RasterDataNode[] getRefRasters(Term... terms) {
        RasterDataSymbol[] symbols = getRefRasterDataSymbols(terms);
        return getRefRasters(symbols);
    }

    /**
     * Utility method which returns all raster data symbols referenced in a given compiled
     * band math expressions.
     * The order of the returned rasters is the order they appear in the given terms.
     *
     * @param terms the compiled band math expressions
     * @return the array of raster data symbols, never <code>null</code> but may be empty
     */
    public static RasterDataSymbol[] getRefRasterDataSymbols(Term... terms) {
        List<RasterDataSymbol> list = new ArrayList<>();
        Set<RasterDataSymbol> set = new HashSet<>();
        for (final Term term : terms) {
            if (term != null) {
                collectRefRasterDataSymbols(term, list, set);
            }
        }
        return list.toArray(new RasterDataSymbol[list.size()]);
    }

    public static String getValidMaskExpression(String expression,
                                                Product product,
                                                String validMaskExpression) throws ParseException {
        return getValidMaskExpression(expression, new Product[]{product}, 0, validMaskExpression);
    }

    public static String getValidMaskExpression(String expression,
                                                Product[] products,
                                                int contextProductIndex,
                                                String validMaskExpression) throws ParseException {

        final RasterDataNode[] rasters = getRefRasters(expression, products, contextProductIndex);
        if (rasters.length == 0) {
            return validMaskExpression;
        }
        final Product contextProduct = products[contextProductIndex];
        if (StringUtils.isNullOrEmpty(validMaskExpression) && rasters.length == 1 && contextProduct == rasters[0].getProduct()) {
            return rasters[0].getValidMaskExpression();
        }

        final List<String> vmes = new ArrayList<>(rasters.length);
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

        if (vmes.isEmpty()) {
            return validMaskExpression;
        }

        final StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotNullAndNotEmpty(validMaskExpression)) {
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
     * number returned by {@link Product#getRefNo()}.
     *
     * @param product the product, must not be <code>null</code>
     * @return a node name prefix, never null.
     */
    public static String getProductNodeNamePrefix(Product product) {
        Guardian.assertNotNull("product", product);
        return "$" + product.getRefNo() + '.';
    }

    /**
     * Determines whether all rasters which are referenced in a term are compatible.
     *
     * @param term The term in question
     * @return true if the rasters are compatible
     */
    public static boolean areRastersEqualInSize(Term term) {
        final RasterDataSymbol[] rasterDataSymbols = getRefRasterDataSymbols(term);
        if (rasterDataSymbols.length > 1) {
            int referenceWidth = rasterDataSymbols[0].getRaster().getRasterWidth();
            int referenceHeight = rasterDataSymbols[0].getRaster().getRasterHeight();
            for (int i = 1; i < rasterDataSymbols.length; i++) {
                if (rasterDataSymbols[i].getRaster().getRasterWidth() != referenceWidth ||
                        rasterDataSymbols[i].getRaster().getRasterHeight() != referenceHeight) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Determines whether all rasters which are referenced in the given expressions are compatible.
     *
     * @param product     The product to which the expressions can refer
     * @param expressions the expressions in question
     * @return true if all referenced rasters are compatible
     * @throws ParseException if a parse error occurs
     */
    public static boolean areRastersEqualInSize(Product product, String... expressions) throws ParseException {
        return areRastersEqualInSize(new Product[]{product}, 0, expressions);
    }

    /**
     * Determines whether all rasters which are referenced in the given expressions are compatible.
     *
     * @param products            The product to which the expressions can refer
     * @param defaultProductIndex The index of the default product
     * @param expressions         the expressions in question
     * @return true if all referenced rasters are compatible
     * @throws ParseException if a parse error occurs
     */
    public static boolean areRastersEqualInSize(Product[] products, int defaultProductIndex, String... expressions) throws ParseException {
        if (expressions.length == 0) {
            return true;
        }
        int referenceWidth = -1;
        int referenceHeight = -1;
        for (String expression : expressions) {
            final Namespace namespace = createDefaultNamespace(products, defaultProductIndex);
            final Parser parser = new ParserImpl(namespace, false);
            final Term term = parser.parse(expression);
            final RasterDataNode[] refRasters = getRefRasters(term);
            if (refRasters.length > 0) {
                if (!areRastersEqualInSize(term)) {
                    return false;
                }
                if (referenceWidth == -1) {
                    referenceWidth = refRasters[0].getRasterWidth();
                    referenceHeight = refRasters[0].getRasterHeight();
                } else if (refRasters[0].getRasterWidth() != referenceWidth ||
                        refRasters[0].getRasterHeight() != referenceHeight) {
                    return false;
                }
            }
        }
        return true;
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

    private static RasterDataNode[] getRefRasters(RasterDataSymbol[] rasterDataSymbols) {
        Set<RasterDataNode> set = new HashSet<>(rasterDataSymbols.length * 2);
        List<RasterDataNode> list = new ArrayList<>(rasterDataSymbols.length);
        for (RasterDataSymbol symbol : rasterDataSymbols) {
            RasterDataNode raster = symbol.getRaster();
            if (!set.contains(raster)) {
                list.add(raster);
                set.add(raster);
            }
        }
        return list.toArray(new RasterDataNode[list.size()]);
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

}
