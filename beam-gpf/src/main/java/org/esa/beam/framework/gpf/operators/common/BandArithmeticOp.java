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
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.dataop.barithm.BandArithmetic.ProductPrefixProvider;
import org.esa.beam.framework.dataop.barithm.RasterDataEvalEnv;
import org.esa.beam.framework.dataop.barithm.RasterDataSymbol;
import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.StringUtils;

import java.awt.Rectangle;

/**
 * Created by marcoz.
 *
 * @author marcoz
 * @version $Revision: $ $Date: $
 */
public class BandArithmeticOp extends AbstractOperator implements ParameterConverter {

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

    @Parameter(defaultValue = "ExpressionProduct")
    private String productName = "ExpressionProduct";
    @Parameter
    private BandDescriptor[] bandDescriptors;
    @Parameter
    private Variable[] variables;
    @TargetProduct
    private Product targetProduct;
    @SourceProducts
    private Product[] sourceProducts;

    private WritableNamespace namespace;


    public void getParameterValues(Operator operator, Xpp3Dom configuration) throws OperatorException {
        // todo - implement        
    }

    public void setParameterValues(Operator operator, Xpp3Dom parameterDom) throws OperatorException {
        Xpp3Dom[] children = parameterDom.getChildren("bandDescriptor");
        bandDescriptors = new BandDescriptor[children.length];
        for (int i = 0; i < children.length; i++) {
            bandDescriptors[i] = new BandDescriptor();
            bandDescriptors[i].name = children[i].getChild("name").getValue();
            bandDescriptors[i].expression = children[i].getChild("expression").getValue();
            bandDescriptors[i].type = children[i].getChild("type").getValue();
        }
    }

    @Override
    protected Product initialize() throws OperatorException {
        int height = sourceProducts[0].getSceneRasterHeight();
        int width = sourceProducts[0].getSceneRasterWidth();
        targetProduct = new Product(productName, "EXP", width, height);

        namespace = BandArithmetic.createDefaultNamespace(sourceProducts, 0, new ProductPrefixProvider() {
            public String getPrefix(Product product) {
                String idForSourceProduct = getContext().getSourceProductId(product);
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
                } else if (ProductData.TYPESTRING_BOOLEAN.equals(variable.type)) {
                    Symbol symbol = SymbolFactory.createConstant(variable.name, Boolean.parseBoolean(variable.value));
                    namespace.registerSymbol(symbol);
                }
            }
        }
        for (BandDescriptor bandDescriptor : bandDescriptors) {
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
        }

        return targetProduct;
    }

    @Override
    public void computeTile(Band band, Tile targetTile) throws OperatorException {
        BandDescriptor bandDescriptor = getDesriptionForTile(targetTile);

        Rectangle rect = targetTile.getRectangle();
        final Term term;
        try {
            Parser parser = new ParserImpl(namespace, false);
            term = parser.parse(bandDescriptor.expression);
        } catch (ParseException e) {
            throw new OperatorException("Couldn't parse expression: " + bandDescriptor.expression, e);
        }

        RasterDataSymbol[] refRasterDataSymbols = BandArithmetic.getRefRasterDataSymbols(term);
        for (RasterDataSymbol symbol : refRasterDataSymbols) {
            Tile tile = getSourceTile(symbol.getRaster(), rect);
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
                ProductData dataBuffer = tile.getRawSampleData();
                symbol.setData(dataBuffer);
            }
        }

        final RasterDataEvalEnv env = new RasterDataEvalEnv(rect.x, rect.y, rect.width, rect.height);
        int pixelIndex = 0;
        ProgressMonitor pm = createProgressMonitor();
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

    private BandDescriptor getDesriptionForTile(Tile tile) {
        String rasterName = tile.getRasterDataNode().getName();
        for (BandDescriptor bandDescriptor : bandDescriptors) {
            if (bandDescriptor.name.equals(rasterName)) {
                return bandDescriptor;
            }
        }
        return null;
    }

    public static class Spi extends AbstractOperatorSpi {
        public Spi() {
            super(BandArithmeticOp.class, "BandArithmetic");
        }
    }
}
