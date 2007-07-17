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

import java.awt.Rectangle;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.dataop.barithm.RasterDataEvalEnv;
import org.esa.beam.framework.dataop.barithm.RasterDataSymbol;
import org.esa.beam.framework.dataop.barithm.BandArithmetic.ProductPrefixProvider;
import org.esa.beam.framework.gpf.AbstractOperator;
import org.esa.beam.framework.gpf.AbstractOperatorSpi;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.ParameterConverter;
import org.esa.beam.framework.gpf.Raster;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.StringUtils;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.jexp.Namespace;
import com.bc.jexp.ParseException;
import com.bc.jexp.Parser;
import com.bc.jexp.Term;
import com.bc.jexp.impl.ParserImpl;
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;

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
	}

	@Parameter
	private String productName = "ExpressionProduct";
	@Parameter
	private BandDescriptor[] bandDescriptors;
	@TargetProduct
	private Product targetProduct;
	@SourceProducts
	private Product[] sourceProducts;
	
	private Namespace namespace;
	
	public BandArithmeticOp(OperatorSpi spi) {
        super(spi);
    }
	
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
	protected Product initialize(ProgressMonitor pm) throws OperatorException {
		int height = sourceProducts[0].getSceneRasterHeight();
		int width = sourceProducts[0].getSceneRasterWidth();
		targetProduct = new Product(productName, "EXP", width, height);
		
		namespace = BandArithmetic.createDefaultNamespace(sourceProducts,0, new ProductPrefixProvider() {
			public String getPrefix(Product product) {
				String idForSourceProduct = getContext().getIdForSourceProduct(product);
				return "$" + idForSourceProduct + ".";
			}
		});
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
				}catch (NumberFormatException e) {
					throw new OperatorException("Bad value for NoDataValue given: " +  bandDescriptor.noDataValue, e);
				}
			}
		}
		
		return targetProduct;
	}
	
	@Override
    public void computeBand(Raster targetRaster, ProgressMonitor pm) throws OperatorException {
		BandDescriptor bandDescriptor = getDesriptionForRaster(targetRaster);

		Rectangle rect = targetRaster.getRectangle();
		final Term term;
		try {
			Parser parser = new ParserImpl(namespace, false);
			term = parser.parse(bandDescriptor.expression);
		} catch (ParseException e) {
			throw new OperatorException("Couldn't parse expression: "+bandDescriptor.expression, e);
		}
		
		RasterDataSymbol[] refRasterDataSymbols = BandArithmetic.getRefRasterDataSymbols(term);
		for (RasterDataSymbol symbol : refRasterDataSymbols) {
			Raster raster = getRaster(symbol.getRaster(), rect);
			ProductData dataBuffer = raster.getDataBuffer();
			symbol.setData(dataBuffer);
		}
		final ProductData targetData = targetRaster.getDataBuffer();

		final RasterDataEvalEnv env = new RasterDataEvalEnv(rect.x, rect.y, rect.width, rect.height);
		int pixelIndex = 0;
		pm.beginTask("Evaluating expression", rect.height);
		try { 
			for (int y = rect.y; y < rect.y + rect.height; y++) {
				if (pm.isCanceled()) {
					break;
				}
				env.setPixelY(y);
				for (int x = rect.x; x < rect.x+rect.width; x++) {
					env.setElemIndex(pixelIndex);
					env.setPixelX(x);
					targetData.setElemDoubleAt(pixelIndex, term.evalD(env));
					pixelIndex++;
				}
				pm.worked(1);
			}
		} finally {
			pm.done();
		}
	}
	
	private BandDescriptor getDesriptionForRaster(Raster raster) {
		String rasterName = raster.getRasterDataNode().getName();
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
