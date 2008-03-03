/*
 * $Id: $
 *
 * Copyright (C) 2007 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.framework.gpf.operators.common;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.jexp.*;
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

@OperatorMetadata(alias = "BandArithmetic", internal = true)
public class BandArithmeticOp extends Operator {

    public static class BandDescriptor {
        public String name;
        public String expression;
        public String description;
        public String type;
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

    @SourceProducts
    private Product[] sourceProducts;

    @Parameter(alias = "targetBands", itemAlias = "targetBand")
    private BandDescriptor[] targetBandDescriptors;
    @Parameter(alias = "variables", itemAlias = "variable")
    private Variable[] variables;

    private Map<Band, RasterDataSymbol[]> symbolMap;
    private Map<Band, Term> termMap;

    public static BandArithmeticOp createBooleanExpressionBand(String expression, Product sourceProduct) {
        BandDescriptor[] bandDescriptors = new BandDescriptor[1];
        bandDescriptors[0] = new BandDescriptor();
        bandDescriptors[0].name = "band1";
        bandDescriptors[0].expression = expression;
        bandDescriptors[0].type = ProductData.TYPESTRING_INT8;

        BandArithmeticOp bandArithmeticOp = new BandArithmeticOp();
        bandArithmeticOp.targetBandDescriptors = bandDescriptors;
        bandArithmeticOp.sourceProducts = new Product[]{sourceProduct};
        return bandArithmeticOp;
    }

    @Override
    public void initialize() throws OperatorException {
        if (targetBandDescriptors == null || targetBandDescriptors.length == 0) {
            throw new OperatorException("No target bands specified.");
        }

        int height = sourceProducts[0].getSceneRasterHeight();
        int width = sourceProducts[0].getSceneRasterWidth();
        targetProduct = new Product(sourceProducts[0].getName() + "_BandArithmetic", "BandArithmetic", width, height);

        WritableNamespace namespace = BandArithmetic.createDefaultNamespace(sourceProducts, 0, new ProductPrefixProvider() {
            public String getPrefix(Product product) {
                String idForSourceProduct = getSourceProductId(product);
                return "$" + idForSourceProduct + ".";
            }
        });
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
        symbolMap = new HashMap<Band, RasterDataSymbol[]>(targetBandDescriptors.length);
        termMap = new HashMap<Band, Term>(targetBandDescriptors.length);
        for (BandDescriptor bandDescriptor : targetBandDescriptors) {
            Band band = targetProduct.addBand(bandDescriptor.name, ProductData.getType(bandDescriptor.type));
            if (StringUtils.isNotNullAndNotEmpty(bandDescriptor.description)) {
                band.setDescription(bandDescriptor.description);
            }
            if (StringUtils.isNotNullAndNotEmpty(bandDescriptor.validExpression)) {
                band.setValidPixelExpression(bandDescriptor.validExpression);
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

            final Term term;
            try {
                Parser parser = new ParserImpl(namespace, false);
                term = parser.parse(bandDescriptor.expression);
            } catch (ParseException e) {
                throw new OperatorException("Could not parse expression: " + bandDescriptor.expression, e);
            }
            RasterDataSymbol[] refRasterDataSymbols = BandArithmetic.getRefRasterDataSymbols(term);

            symbolMap.put(band, refRasterDataSymbols);
            termMap.put(band, term);
        }

        if (sourceProducts.length == 1) {
            ProductUtils.copyMetadata(sourceProducts[0], targetProduct);
        }
        if (sourceProducts[0].getPreferredTileSize() != null) {
            targetProduct.setPreferredTileSize(sourceProducts[0].getPreferredTileSize());
        }
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle rect = targetTile.getRectangle();
        RasterDataSymbol[] refRasterDataSymbols = symbolMap.get(band);
        Term term = termMap.get(band);

        for (RasterDataSymbol symbol : refRasterDataSymbols) {
            Tile tile = getSourceTile(symbol.getRaster(), rect, pm);
            if (tile.getRasterDataNode().isScalingApplied()) {
                ProductData dataBuffer = ProductData.createInstance(ProductData.TYPE_FLOAT32, tile.getWidth() * tile.getHeight());
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
        int pixelIndex = 0;
        pm.beginTask("Evaluating expression", rect.height);
        try {
            for (int y = rect.y; y < rect.y + rect.height; y++) {
                if (pm.isCanceled()) {
                    break;
                }
                env.setPixelY(y);
                for (int x = rect.x; x < rect.x + rect.width; x++) {
                    env.setElemIndex(pixelIndex);
                    env.setPixelX(x);
                    targetTile.setSample(x, y, term.evalD(env));
                    pixelIndex++;
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(BandArithmeticOp.class);
        }
    }
}
