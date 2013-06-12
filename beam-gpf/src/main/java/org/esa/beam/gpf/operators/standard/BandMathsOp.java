/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.gpf.operators.standard;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.jexp.Namespace;
import com.bc.jexp.ParseException;
import com.bc.jexp.Parser;
import com.bc.jexp.Symbol;
import com.bc.jexp.Term;
import com.bc.jexp.WritableNamespace;
import com.bc.jexp.impl.ParserImpl;
import com.bc.jexp.impl.SymbolFactory;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.dataop.barithm.BandArithmetic.ProductPrefixProvider;
import org.esa.beam.framework.dataop.barithm.RasterDataEvalEnv;
import org.esa.beam.framework.dataop.barithm.RasterDataSymbol;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StringUtils;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * This band maths operator can be used to create a product with multiple bands based on mathematical expression.<br/>
 * All products specified as source must have the same width and height, otherwise the operator will fail.
 * The geo-coding information and metadata for the target product is taken from the first source product.
 * </p>
 * <p>
 * To reference a band of one of the source products within an expression use the following syntax:<br/>
 * <br/>
 * <code>$sourceProducts<b>#</b>.bandName</code><br/>
 * <br/>
 * Where <b>#</b> means the index of the source product. The index is zero based.<br/>
 * The bands of the first source product (<code>$sourceProducts<b>0</b></code>) can be referenced without this
 * product identifier. The band name is sufficient.
 * </p>
 * <p>
 * When using this operator from the command-line Graph XML file must be provided in order to
 * specify all parameters. Here is some sample XML of how to use a <code>BandMaths</code> node within
 * a graph:
 * </p>
 * <pre>
 *      &lt;node id="bandMathsNode"&gt;
 *        &lt;operator&gt;BandMaths&lt;/operator&gt;
 *        &lt;sources&gt;
 *            &lt;sourceProducts&gt;readNode&lt;/sourceProducts&gt;
 *        &lt;/sources&gt;
 *        &lt;parameters&gt;
 *            &lt;targetBands&gt;
 *                &lt;targetBand&gt;
 *                    &lt;name&gt;reflec_13&lt;/name&gt;
 *                    &lt;expression&gt;radiance_13 / (PI * SOLAR_FLUX_13)&lt;/expression&gt;
 *                    &lt;description&gt;TOA reflectance in channel 13&lt;/description&gt;
 *                    &lt;type&gt;float&lt;/type&gt;
 *                    &lt;validExpression&gt;reflec_13 &gt;= 0&lt;/validExpression&gt;
 *                    &lt;noDataValue&gt;-999&lt;/noDataValue&gt;
 *                    &lt;spectralBandIndex&gt;13&lt;/spectralBandIndex&gt;
 *                &lt;/targetBand&gt;
 *                &lt;targetBand&gt;
 *                    &lt;name&gt;reflec_14&lt;/name&gt;
 *                    &lt;expression&gt;radiance_14 / (PI * SOLAR_FLUX_14)&lt;/expression&gt;
 *                    &lt;description&gt;TOA reflectance in channel 14&lt;/description&gt;
 *                    &lt;type&gt;float&lt;/type&gt;
 *                    &lt;validExpression&gt;reflec_14 &gt;= 0&lt;/validExpression&gt;
 *                    &lt;noDataValue&gt;-999&lt;/noDataValue&gt;
 *                    &lt;spectralBandIndex&gt;14&lt;/spectralBandIndex&gt;
 *                &lt;/targetBand&gt;
 *            &lt;/targetBands&gt;
 *            &lt;variables&gt;
 *                &lt;variable&gt;
 *                    &lt;name&gt;SOLAR_FLUX_13&lt;/name&gt;
 *                    &lt;type&gt;float&lt;/type&gt;
 *                    &lt;value&gt;914.18945&lt;/value&gt;
 *                &lt;/variable&gt;
 *                &lt;variable&gt;
 *                    &lt;name&gt;SOLAR_FLUX_14&lt;/name&gt;
 *                    &lt;type&gt;float&lt;/type&gt;
 *                    &lt;value&gt;882.8275&lt;/value&gt;
 *                &lt;/variable&gt;
 *                 &lt;variable&gt;
 *                    &lt;name&gt;PI&lt;/name&gt;
 *                    &lt;type&gt;double&lt;/type&gt;
 *                    &lt;value&gt;3.1415&lt;/value&gt;
 *                &lt;/variable&gt;
 *            &lt;/variables&gt;
 *        &lt;/parameters&gt;
 *    &lt;/node&gt;
 * </pre>
 *
 * @author Marco Zuehlke
 * @author Norman Fomferra
 * @author Marco Peters
 * @since BEAM 4.7
 */
@OperatorMetadata(alias = "BandMaths",
                  version = "1.0",
                  copyright = "(c) 2010 by Brockmann Consult",
                  authors = "Marco Zuehlke, Norman Fomferra, Marco Peters",
                  description = "Create a product with one or more bands using mathematical expressions.")
public class BandMathsOp extends Operator {

    public static class BandDescriptor {

        public String name;
        public String expression;
        public String description;
        public String type;
        public String unit;
        public String validExpression;
        public String noDataValue;
        public Integer spectralBandIndex;
        public Float spectralWavelength;
        public Float spectralBandwidth;
    }

    public static class Variable {

        public String name;
        public String type;
        public String value;
    }

    @TargetProduct
    private Product targetProduct;

    @SourceProducts(description = "Any number of source products.")
    private Product[] sourceProducts;

    @Parameter(alias = "targetBands", itemAlias = "targetBand",
               description = "List of descriptors defining the target bands.")
    private BandDescriptor[] targetBandDescriptors;
    @Parameter(alias = "variables", itemAlias = "variable",
               description = "List of variables which can be used within the expressions.")
    private Variable[] variables;

    private Map<Band, BandDescriptor> descriptorMap;

    public static BandMathsOp createBooleanExpressionBand(String expression, Product sourceProduct) {
        BandDescriptor[] bandDescriptors = new BandDescriptor[1];
        bandDescriptors[0] = new BandDescriptor();
        bandDescriptors[0].name = "band1";
        bandDescriptors[0].expression = expression;
        bandDescriptors[0].type = ProductData.TYPESTRING_INT8;

        BandMathsOp bandMathsOp = new BandMathsOp();
        bandMathsOp.targetBandDescriptors = bandDescriptors;
        bandMathsOp.sourceProducts = new Product[]{sourceProduct};
        return bandMathsOp;
    }

    @Override
    public void initialize() throws OperatorException {
        if (targetBandDescriptors == null || targetBandDescriptors.length == 0) {
            throw new OperatorException("No target bands specified.");
        }

        if (sourceProducts == null || sourceProducts.length == 0) {
            throw new OperatorException("No source products given.");
        }
        int width = sourceProducts[0].getSceneRasterWidth();
        int height = sourceProducts[0].getSceneRasterHeight();
        for (Product product : sourceProducts) {
            if (product.getSceneRasterWidth() != width ||
                product.getSceneRasterHeight() != height) {
                throw new OperatorException("Products must have the same raster dimension.");
            }
        }
        targetProduct = new Product(sourceProducts[0].getName() + "BandMath", "BandMath", width, height);

        descriptorMap = new HashMap<Band, BandDescriptor>(targetBandDescriptors.length);
        Namespace namespace = createNamespace();
        Parser verificationParser = new ParserImpl(namespace, true);
        for (BandDescriptor bandDescriptor : targetBandDescriptors) {
            createBand(bandDescriptor, verificationParser);
        }

        ProductUtils.copyMetadata(sourceProducts[0], targetProduct);
        ProductUtils.copyGeoCoding(sourceProducts[0], targetProduct);
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle rect = targetTile.getRectangle();
        Term term = createTerm(descriptorMap.get(band).expression);
        RasterDataSymbol[] refRasterDataSymbols = BandArithmetic.getRefRasterDataSymbols(term);

        for (RasterDataSymbol symbol : refRasterDataSymbols) {
            Tile tile = getSourceTile(symbol.getRaster(), rect);
            if (tile.getRasterDataNode().isScalingApplied()) {
                ProductData dataBuffer = ProductData.createInstance(ProductData.TYPE_FLOAT32,
                                                                    tile.getWidth() * tile.getHeight());
                int dataBufferIndex = 0;
                for (int y = rect.y; y < rect.y + rect.height; y++) {
                    for (int x = rect.x; x < rect.x + rect.width; x++) {
                        dataBuffer.setElemFloatAt(dataBufferIndex, tile.getSampleFloat(x, y));
                        dataBufferIndex++;
                    }
                }
                symbol.setData(dataBuffer);
            } else {
                ProductData dataBuffer = tile.getRawSamples();
                symbol.setData(dataBuffer);
            }
        }

        final RasterDataEvalEnv env = new RasterDataEvalEnv(rect.x, rect.y, rect.width, rect.height);
        pm.beginTask("Evaluating expression", rect.height);
        try {
            int pixelIndex = 0;
            for (int y = rect.y; y < rect.y + rect.height; y++) {
                if (pm.isCanceled()) {
                    break;
                }
                for (int x = rect.x; x < rect.x + rect.width; x++) {
                    env.setElemIndex(pixelIndex);
                    targetTile.setSample(x, y, term.evalD(env));
                    pixelIndex++;
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    private void createBand(BandDescriptor bandDescriptor, Parser verificationParser) {
        if (StringUtils.isNullOrEmpty(bandDescriptor.name)) {
            throw new OperatorException("Missing band name.");
        }
        if (StringUtils.isNullOrEmpty(bandDescriptor.type)) {
            throw new OperatorException(String.format("Missing data type for band %s.", bandDescriptor.name));
        }

        Band band = targetProduct.addBand(bandDescriptor.name, ProductData.getType(bandDescriptor.type.toLowerCase()));
        if (StringUtils.isNotNullAndNotEmpty(bandDescriptor.description)) {
            band.setDescription(bandDescriptor.description);
        }
        if (StringUtils.isNotNullAndNotEmpty(bandDescriptor.validExpression)) {
            band.setValidPixelExpression(bandDescriptor.validExpression);
        }
        if (StringUtils.isNotNullAndNotEmpty(bandDescriptor.unit)) {
            band.setUnit(bandDescriptor.unit);
        }
        if (StringUtils.isNotNullAndNotEmpty(bandDescriptor.noDataValue)) {
            try {
                double parseDouble = Double.parseDouble(bandDescriptor.noDataValue);
                band.setNoDataValue(parseDouble);
                band.setNoDataValueUsed(true);
            } catch (NumberFormatException e) {
                throw new OperatorException("Bad value for NoDataValue given: " + bandDescriptor.noDataValue, e);
            }
        }
        if (bandDescriptor.spectralBandIndex != null) {
            band.setSpectralBandIndex(bandDescriptor.spectralBandIndex);
        }
        if (bandDescriptor.spectralWavelength != null) {
            band.setSpectralWavelength(bandDescriptor.spectralWavelength);
        }
        if (bandDescriptor.spectralBandwidth != null) {
            band.setSpectralBandwidth(bandDescriptor.spectralBandwidth);
        }
        descriptorMap.put(band, bandDescriptor);
        try {
            Term testTerm = verificationParser.parse(bandDescriptor.expression);
        } catch (ParseException e) {
            throw new OperatorException("Could not parse expression: " + bandDescriptor.expression, e);
        }
    }

    private Namespace createNamespace() {
        WritableNamespace namespace = BandArithmetic.createDefaultNamespace(sourceProducts, 0,
                                                                            new SourceProductPrefixProvider());
        if (variables != null) {
            for (Variable variable : variables) {
                if (ProductData.isFloatingPointType(ProductData.getType(variable.type))) {
                    Symbol symbol = SymbolFactory.createConstant(variable.name, Double.parseDouble(variable.value));
                    namespace.registerSymbol(symbol);
                } else if (ProductData.isIntType(ProductData.getType(variable.type))) {
                    Symbol symbol = SymbolFactory.createConstant(variable.name, Long.parseLong(variable.value));
                    namespace.registerSymbol(symbol);
                } else if ("boolean".equals(variable.type)) {
                    Symbol symbol = SymbolFactory.createConstant(variable.name, Boolean.parseBoolean(variable.value));
                    namespace.registerSymbol(symbol);
                }
            }
        }
        return namespace;
    }

    private Term createTerm(String expression) {
        Namespace namespace = createNamespace();
        final Term term;
        try {
            Parser parser = new ParserImpl(namespace, false);
            term = parser.parse(expression);
        } catch (ParseException e) {
            throw new OperatorException("Could not parse expression: " + expression, e);
        }
        return term;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(BandMathsOp.class);
        }
    }

    private class SourceProductPrefixProvider implements ProductPrefixProvider {

        @Override
        public String getPrefix(Product product) {
            return "$" + getSourceProductId(product) + ".";
        }
    }
}
