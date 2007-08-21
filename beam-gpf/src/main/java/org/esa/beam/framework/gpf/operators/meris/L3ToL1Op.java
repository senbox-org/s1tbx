/*
 * $Id: L3ToL1Op.java,v 1.1 2007/03/27 12:51:05 marcoz Exp $
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
package org.esa.beam.framework.gpf.operators.meris;

import java.awt.Rectangle;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.AbstractOperator;
import org.esa.beam.framework.gpf.AbstractOperatorSpi;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Raster;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import com.bc.ceres.core.ProgressMonitor;

/**
 * Created by marcoz.
 *
 * @author marcoz
 * @version $Revision: 1.1 $ $Date: 2007/03/27 12:51:05 $
 */
public class L3ToL1Op extends AbstractOperator {

    private GeoCoding l3GeoCoding;
    private GeoCoding l1GeoCoding;
    
    @SourceProduct(alias="l1")
    private Product l1Product;
    @SourceProduct(alias="l3")
    private Product l3Product;
    @TargetProduct
    private Product targetProduct;

    public L3ToL1Op(OperatorSpi spi) {
        super(spi);
    }

    @Override
	protected Product initialize(ProgressMonitor pm) throws OperatorException {
        l3GeoCoding = l3Product.getGeoCoding();
        l1GeoCoding = l1Product.getGeoCoding();

        final int width = l1Product.getSceneRasterWidth();
        final int height = l1Product.getSceneRasterHeight();
        targetProduct = new Product("L1", "L1", width, height);

        Band[] l3Bands = l3Product.getBands();
        for (Band sourceBand : l3Bands) {
            Band targetBand = targetProduct.addBand(sourceBand.getName(), sourceBand.getDataType());
            ProductUtils.copySpectralAttributes(sourceBand, targetBand);
            targetBand.setDescription(sourceBand.getDescription());
            targetBand.setUnit(sourceBand.getUnit());
            targetBand.setScalingFactor(sourceBand.getScalingFactor());
            targetBand.setScalingOffset(sourceBand.getScalingOffset());
            targetBand.setLog10Scaled(sourceBand.isLog10Scaled());
            targetBand.setNoDataValueUsed(sourceBand.isNoDataValueUsed());
            targetBand.setNoDataValue(sourceBand.getNoDataValue());
            if (sourceBand.getFlagCoding() != null) {
                FlagCoding srcFlagCoding = sourceBand.getFlagCoding();
                ProductUtils.copyFlagCoding(srcFlagCoding, targetProduct);
                targetBand.setFlagCoding(targetProduct.getFlagCoding(srcFlagCoding.getName()));
            }
        }
        return targetProduct;
    }

    @Override
    public void computeBand(Raster targetRaster, ProgressMonitor pm) throws OperatorException {
    	
    	Rectangle rectangle = targetRaster.getRectangle();
    	Band srcBand = l3Product.getBand(targetRaster.getRasterDataNode().getName());
    	
    	PixelPos l1PixelPos = new PixelPos();
        PixelPos l3PixelPos = new PixelPos();
        GeoPos geoPos = new GeoPos();
        
        Rectangle l3Rect = findL3Rectangle(rectangle, srcBand);
        Raster srcRaster = getRaster(srcBand, l3Rect);
        
    	pm.beginTask("compute", rectangle.height);
        try {
            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                l1PixelPos.y = y;
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    l1PixelPos.x = x;
                    l1GeoCoding.getGeoPos(l1PixelPos, geoPos);
                    l3GeoCoding.getPixelPos(geoPos, l3PixelPos);
                    targetRaster.setDouble(x, y, srcRaster.getDouble(Math.round(l3PixelPos.x), Math.round(l3PixelPos.y)));
                    
//                    double[] srcValue = srcBand.readPixels((int) l3PixelPos.x, (int) l3PixelPos.y, 1, 1, (double[])null, ProgressMonitor.NULL);
//					targetRaster.setDouble(x, y, srcValue[0]);
                }
                pm.worked(1);
            }
//        } catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
		} finally {
        	pm.done();
        }
    }
    
    private Rectangle findL3Rectangle(Rectangle l1Rectangle, Band srcBand) {
    	PixelPos bottomLeft = new PixelPos(l1Rectangle.x, l1Rectangle.y);
        PixelPos l3PixelPos = l3GeoCoding.getPixelPos(l1GeoCoding.getGeoPos(bottomLeft, null), null);
        Rectangle l3Rectangle = new Rectangle(Math.round(l3PixelPos.x), Math.round(l3PixelPos.y), 1, 1);
        
        PixelPos bottomRight = new PixelPos(l1Rectangle.x+l1Rectangle.width, l1Rectangle.y);
        l3PixelPos = l3GeoCoding.getPixelPos(l1GeoCoding.getGeoPos(bottomRight, null), l3PixelPos);
        l3Rectangle.add(l3PixelPos.x, l3PixelPos.y);
        
        PixelPos topRight = new PixelPos(l1Rectangle.x+l1Rectangle.width, l1Rectangle.y+l1Rectangle.height);
        l3PixelPos = l3GeoCoding.getPixelPos(l1GeoCoding.getGeoPos(topRight, null), l3PixelPos);
        l3Rectangle.add(l3PixelPos.x, l3PixelPos.y);
        
        PixelPos topLeft = new PixelPos(l1Rectangle.x, l1Rectangle.y+l1Rectangle.height);
        l3PixelPos = l3GeoCoding.getPixelPos(l1GeoCoding.getGeoPos(topLeft, null), l3PixelPos);
        l3Rectangle.add(l3PixelPos.x, l3PixelPos.y);
        
        l3Rectangle.grow(2, 2);
        Rectangle sceneRectangle = new Rectangle(srcBand.getSceneRasterWidth(), srcBand.getSceneRasterHeight());
        return l3Rectangle.intersection(sceneRectangle);
    }

    public static class Spi extends AbstractOperatorSpi {
        public Spi() {
            super(L3ToL1Op.class, "L3ToL1");
        }
    }
}