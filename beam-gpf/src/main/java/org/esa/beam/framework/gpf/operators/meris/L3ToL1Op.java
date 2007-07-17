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
import java.io.IOException;

import org.esa.beam.framework.datamodel.Band;
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
        for (Band l3Band : l3Bands) {
            Band newBand = targetProduct.addBand(l3Band.getName(), l3Band.getDataType());
            ProductUtils.copySpectralAttributes(l3Band, newBand);
            newBand.setNoDataValueUsed(l3Band.isNoDataValueUsed());
            newBand.setNoDataValue(l3Band.getNoDataValue());
        }
        return targetProduct;
    }

    @Override
    public void computeBand(Raster targetRaster, ProgressMonitor pm) throws OperatorException {
    	
    	Rectangle rectangle = targetRaster.getRectangle();
    	Band srcBand = l3Product.getBand(targetRaster.getRasterDataNode().getName());
        try {
        	pm.beginTask("compute", rectangle.height);
            PixelPos l1PixelPos = new PixelPos();
            PixelPos l3PixelPos = new PixelPos();
            GeoPos geoPos = new GeoPos();
            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                l1PixelPos.y = y;
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    l1PixelPos.x = x;
                    l1GeoCoding.getGeoPos(l1PixelPos, geoPos);
                    l3GeoCoding.getPixelPos(geoPos, l3PixelPos);
                    double[] srcValue = srcBand.readPixels((int) l3PixelPos.x, (int) l3PixelPos.y, 1, 1, (double[])null, ProgressMonitor.NULL);
//                    Rectangle l3Rect = new Rectangle((int) l3PixelPos.x, (int) l3PixelPos.y, 1, 1);
//					Raster srcRaster = getRaster(srcBand, l3Rect);
//                    targetRaster.setDouble(x, y, srcRaster.getDouble((int) l3PixelPos.x, (int) l3PixelPos.y));
					targetRaster.setDouble(x, y, srcValue[0]);
                }
                pm.worked(1);
            }
        }catch (IOException e) {
        	throw new OperatorException(e);
        } finally {
        	pm.done();
        }
    }

    public static class Spi extends AbstractOperatorSpi {
        public Spi() {
            super(L3ToL1Op.class, "L3ToL1");
        }
    }
}