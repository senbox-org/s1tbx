/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.landcover.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.dataop.barithm.ProductNamespacePrefixProvider;
import org.esa.snap.core.dataop.barithm.RasterDataEvalEnv;
import org.esa.snap.core.dataop.barithm.RasterDataSymbol;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.jexp.Parser;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.jexp.WritableNamespace;
import org.esa.snap.core.jexp.impl.ParserImpl;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

/**
 * Perform decision tree classification of a given product
 */

@OperatorMetadata(alias = "Land-Cover-Mask",
        category = "Raster/Masks",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2015 by Array Systems Computing Inc.",
        description = "Perform decision tree classification")
public final class LandCoverMaskOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames = null;

    @Parameter(description = "Land cover band", label = "Land Cover Band")
    private String landCoverBand = null;

    @Parameter(description = "Land cover classes to include", label = "Valid land cover classes")
    private int[] validLandCoverClasses = null;

    @Parameter(description = "Valid pixel expression", label = "Valid pixel expression")
    private String validPixelExpression = null;

    @Parameter(description = "Add other bands unmasked", defaultValue = "false", label = "Include all other bands")
    private boolean includeOtherBands = false;

    private Map<Band, Band> srcBandMap = new HashMap<>();
    private Map<Band, String> expressionMap = new HashMap<>();

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
            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {
        //ensureSingleRasterSize(sourceProduct);

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        if (includeOtherBands) {
            for (String srcBandName : sourceProduct.getBandNames()) {
                ProductUtils.copyBand(srcBandName, sourceProduct, srcBandName, targetProduct, true);
            }
        }

        final Band[] sourceBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames, false);
        for (Band srcBand : sourceBands) {
            if(srcBand.getName().startsWith("land_cover_")) {
                continue;
            }
            final String targetBandName = srcBand.getName() + "_masked";
            final Band targetBand = new Band(targetBandName,
                    srcBand.getDataType(),
                    srcBand.getRasterWidth(), //targetProduct.getSceneRasterWidth(),
                    srcBand.getRasterHeight());//targetProduct.getSceneRasterHeight());


            targetBand.setUnit(srcBand.getUnit());
            targetBand.setNoDataValue(srcBand.getNoDataValue());
            targetBand.setNoDataValueUsed(true);
            targetProduct.addBand(targetBand);

            srcBandMap.put(targetBand, srcBand);
            expressionMap.put(targetBand, createExpression(srcBand));
        }
    }

    private String createExpression(final Band srcBand) {
        final StringBuilder str = new StringBuilder("");
        if (validLandCoverClasses != null && validLandCoverClasses.length > 0) {
            for (int c : validLandCoverClasses) {
                if (str.length() == 0) {
                    str.append("( ");
                } else {
                    str.append(" || ");
                }
                str.append('\'').append(landCoverBand).append("'==").append(c);
            }
            str.append(" ) ? '");
            str.append(srcBand.getName());
            str.append("' : ");
            str.append(srcBand.getNoDataValue());
        } else if(validPixelExpression != null && !validPixelExpression.isEmpty()){
            return validPixelExpression;
        }
        return str.toString();
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

        try {
            final Rectangle rect = targetTile.getRectangle();
            final RasterDataEvalEnv env = new RasterDataEvalEnv(rect.x, rect.y, rect.width, rect.height);

            final String expression = expressionMap.get(targetBand);
            if(expression == null || expression.isEmpty()) {
                return;
            }
            final Term term = createTerm(expression);

            final RasterDataSymbol[] refRasterDataSymbols = BandArithmetic.getRefRasterDataSymbols(term);
            for (RasterDataSymbol symbol : refRasterDataSymbols) {
                final Tile tile = getSourceTile(symbol.getRaster(), rect);
                symbol.setData(tile.getRawSamples());
            }

            pm.beginTask("Evaluating expression", rect.height);
            int pixelIndex = 0;
            for (int y = rect.y; y < rect.y + rect.height; y++) {
                if (pm.isCanceled()) {
                    break;
                }
                for (int x = rect.x; x < rect.x + rect.width; x++) {
                    env.setElemIndex(pixelIndex);

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

    private Term createTerm(String expression) {
        WritableNamespace namespace = BandArithmetic.createDefaultNamespace(new Product[]{sourceProduct}, 0,
                new SourceProductPrefixProvider());
        final Term term;
        try {
            Parser parser = new ParserImpl(namespace, false);
            term = parser.parse(expression);
        } catch (ParseException e) {
            throw new OperatorException("Could not parse expression: " + expression, e);
        }
        return term;
    }

    private static class SourceProductPrefixProvider implements ProductNamespacePrefixProvider {

        @Override
        public String getPrefix(Product product) {
            //return "$" + getSourceProductId(product) + ".";
            return BandArithmetic.getProductNodeNamePrefix(product);
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
            super(LandCoverMaskOp.class);
        }
    }
}