/*
 * $Id: BandArithmetic.java,v 1.4 2007/03/23 08:56:20 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.dataop.barithm;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.jexp.EvalEnv;
import com.bc.jexp.EvalException;
import com.bc.jexp.Function;
import com.bc.jexp.Namespace;
import com.bc.jexp.ParseException;
import com.bc.jexp.Parser;
import com.bc.jexp.Symbol;
import com.bc.jexp.Term;
import com.bc.jexp.WritableNamespace;
import com.bc.jexp.impl.AbstractSymbol;
import com.bc.jexp.impl.DefaultNamespace;
import com.bc.jexp.impl.NamespaceImpl;
import com.bc.jexp.impl.ParserImpl;
import com.bc.jexp.impl.Tokenizer;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.Scaling;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.util.Guardian;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// todo - api doc!
/**
 * Provides band arithmetic utility methods.
 */
public class BandArithmetic {

    // todo - api doc!
    public static final String PIXEL_X_NAME = "X";
    // todo - api doc!
    public static final String PIXEL_Y_NAME = "Y";

    private static final Symbol PIXEL_X_SYMBOL = new AbstractSymbol.I(PIXEL_X_NAME) {
        public int evalI(EvalEnv env) throws EvalException {
            return ((RasterDataEvalEnv) env).getPixelX();
        }
    };

    private static final Symbol PIXEL_Y_SYMBOL = new AbstractSymbol.I(PIXEL_Y_NAME) {
        public int evalI(EvalEnv env) throws EvalException {
            return ((RasterDataEvalEnv) env).getPixelY();
        }
    };

    private static final WritableNamespace DEFAULT_NAMESPACE = new DefaultNamespace();

    private static final List<NamespaceExtender> _namespaceExtenderList = new ArrayList<NamespaceExtender>();

    static{
        MoreFuncs.registerExtraFunctions();
        MoreFuncs.registerExtraSymbols();
    }

    private BandArithmetic() {
    }

    // todo - api doc!
    public static void addNamespaceExtender(NamespaceExtender ne) {
        if (ne != null && !_namespaceExtenderList.contains(ne)) {
            _namespaceExtenderList.add(ne);
        }
    }

    // todo - api doc!
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

    // todo - api doc!
    public static int computeBand(final Product[] sourceProducts,
                                  final String expression,
                                  final boolean checkInvalids,
                                  final boolean noDataValueUsed,
                                  final double noDataValue,
                                  final int offsetX,
                                  final int offsetY,
                                  final int width,
                                  final int height,
                                  final ProductData targetRasterData,
                                  final Scaling scaling,
                                  ProgressMonitor pm) throws ParseException,
                                                             IOException {

        final Namespace namespace = createDefaultNamespace(sourceProducts);
        final Parser parser = new ParserImpl(namespace, false);
        final Term term = parser.parse(expression);

        return computeBand(term, offsetX, offsetY, width, height, checkInvalids, noDataValueUsed, noDataValue,
                           targetRasterData, scaling, pm);
    }


    // todo - api doc!
    public static int computeBand(final Term term,
                                  final int offsetX,
                                  final int offsetY,
                                  final int width,
                                  final int height,
                                  final boolean checkInvalids,
                                  final boolean noDataValueUsed,
                                  final double noDataValue,
                                  final ProductData targetRasterData,
                                  final Scaling scaling,
                                  ProgressMonitor pm) throws IOException {
        final boolean performInvalidCheck = checkInvalids || noDataValueUsed;
        final int[] numInvalidPixels = new int[]{0};

        final RasterDataLoop loop = new RasterDataLoop(offsetX, offsetY,
                                                       width, height,
                                                       new Term[]{term}, pm);
        loop.forEachPixel(new RasterDataLoop.Body() {
            public void eval(RasterDataEvalEnv env, int pixelIndex) {
                double pixelValue = term.evalD(env);
                if (performInvalidCheck && isInvalidValue(pixelValue)) {
                    numInvalidPixels[0]++;
                    if (noDataValueUsed) {
                        pixelValue = noDataValue;
                    }
                }
                if (scaling != null) {
                    targetRasterData.setElemDoubleAt(pixelIndex, scaling.scaleInverse(pixelValue));
                } else {
                    targetRasterData.setElemDoubleAt(pixelIndex, pixelValue);
                }
            }
        }, "Performing band arithmetic...");
        return numInvalidPixels[0];
    }

    /**
     * Creates a default namespace for the product(s) given in an array. The resulting namespace contains symbols for
     * all tie-point grids, bands and single flag values. if the array contains more then one product, the symbol's name
     * will have a prefix according to each product's reference number.
     *
     * @param products the array of input products
     *
     * @return a default namespace, never <code>null</code>
     */
    public static WritableNamespace createDefaultNamespace(Product[] products) {
        return createDefaultNamespace(products, 0);
    }

    /**
     * Creates a default namespace for the product(s) given in an array. The resulting namespace contains symbols for
     * all tie-point grids, bands and single flag values. if the array contains more then one product, the symbol's name
     * will have a prefix according to each product's reference number.
     *
     * @param products            the array of input products
     * @param defaultProductIndex the index of the product for which also symbols without the
     *                            product prefix <code>$<i>ref-no</i></code> are registered in the namespace
     *
     * @return a default namespace, never <code>null</code>
     */
    public static WritableNamespace createDefaultNamespace(Product[] products, int defaultProductIndex) {
        return createDefaultNamespace(products, defaultProductIndex,
        		new ProductPrefixProvider() {
					public String getPrefix(Product product) {
						return getProductNodeNamePrefix(product);
					}});
    }    

    public static WritableNamespace createDefaultNamespace(Product[] products, int defaultProductIndex, ProductPrefixProvider p) {
        Guardian.assertNotNullOrEmpty("products", products);
        Guardian.assertWithinRange("defaultProductIndex", defaultProductIndex, 0, products.length);

        WritableNamespace namespace = new NamespaceImpl(DEFAULT_NAMESPACE);

        // Register symbols for default product without name prefix
        registerProductSymbols(namespace, products[defaultProductIndex], "");

        // Register symbols for multiple products using a name prefix
        if (products.length > 1) {
            for (Product product : products) {
                registerProductSymbols(namespace, product, p.getPrefix(product));
            }
        }

        namespace.registerSymbol(PIXEL_X_SYMBOL);
        namespace.registerSymbol(PIXEL_Y_SYMBOL);

        return namespace;
    }

    /**
     * Utility method which returns all raster data symbols references in a given term.
     *
     * @param term the term to be analysed
     *
     * @return the array of raster data symbols, never <code>null</code> but may be empty
     */
    public static RasterDataSymbol[] getRefRasterDataSymbols(Term term) {
        return getRefRasterDataSymbols(new Term[]{term});
    }

    /**
     * Utility method which returns all raster data symbols references in a given term array.
     *
     * @param terms the term array to be analysed
     *
     * @return the array of raster data symbols, never <code>null</code> but may be empty
     */
    public static RasterDataSymbol[] getRefRasterDataSymbols(Term[] terms) {
        Set<RasterDataSymbol> set = new HashSet<RasterDataSymbol>();
        for (final Term term : terms) {
            if (term != null) {
                collectRefRasterDataSymbols(term, set);
            }
        }
        return set.toArray(new RasterDataSymbol[set.size()]);
    }

    /**
     * Utility method which returns all raster data nodes referenced in a given array of raster data symbols.
     * The given <code>rasterDataSymbols</code> argument can contain multiple references to the same raster data node,
     * e.g. if multilple {@link SingleFlagSymbol}s refer to the same raster.
     *
     * @param rasterDataSymbols the array to be analysed
     *
     * @return the array of raster data nodes, never <code>null</code> but may be empty
     */
    public static RasterDataNode[] getRefRasters(RasterDataSymbol[] rasterDataSymbols) {
        Set<RasterDataNode> set = new HashSet<RasterDataNode>();
        for (RasterDataSymbol symbol : rasterDataSymbols) {
            set.add(symbol.getRaster());
        }
        return set.toArray(new RasterDataNode[set.size()]);
    }

    /**
     * Create an external name from the given name.
     * If the given name contains character which are not valid in an external name
     * the name is escaped with single quotes.
     * <p>The method simply delgates to {@link Tokenizer#createExternalName(String)}.
     *
     * @param name the name
     *
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
     *
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
        registerSingleFlagSymbols(namespace, product, namePrefix);
        informNamespaceExtenders(namespace, product, namePrefix);
    }

    private static void registerTiePointGridSymbols(WritableNamespace namespace,
                                                    Product product,
                                                    String namePrefix) {
        for (int i = 0; i < product.getNumTiePointGrids(); i++) {
            final TiePointGrid grid = product.getTiePointGridAt(i);
            final String symbolName = namePrefix + grid.getName();
            final Symbol symbol = new RasterDataSymbol(symbolName, grid);
            namespace.registerSymbol(symbol);
        }
    }

    private static void registerBandSymbols(WritableNamespace namespace,
                                            Product product,
                                            String namePrefix) {
        for (int i = 0; i < product.getNumBands(); i++) {
            final Band band = product.getBandAt(i);
            final String symbolName = namePrefix + band.getName();
            final Symbol symbol = new RasterDataSymbol(symbolName, band);
            namespace.registerSymbol(symbol);
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

    private static void collectRefRasterDataSymbols(Term term, Set<RasterDataSymbol> set) {
        if (term == null) {
            return;
        }
        if (term instanceof Term.Ref) {
            Symbol symbol = ((Term.Ref) term).getSymbol();
            if (symbol instanceof RasterDataSymbol) {
                set.add((RasterDataSymbol) symbol);
            }
        } else {
            Term[] children = term.getChildren();
            for (Term child : children) {
                collectRefRasterDataSymbols(child, set);
            }
        }
    }

    private static boolean isInvalidValue(double pixelValue) {
        return Double.isNaN(pixelValue) || Double.isInfinite(pixelValue);
    }

    // todo - api doc!
    public static interface NamespaceExtender {

        void extendNamespace(WritableNamespace namespace,
                             Product product,
                             String namePrefix);
    }
    
    public static interface ProductPrefixProvider {
    	String getPrefix(Product product);
    }
}
