/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.insar.gpf.support;

import org.esa.s1tbx.commons.CRSGeoCodingHandler;
import org.esa.s1tbx.commons.SARGeocoding;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.dem.dataio.DEMFactory;
import org.esa.snap.dem.dataio.EarthGravitationalModel96;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.TileGeoreferencing;

import java.awt.*;

/**
 * Created by lveci on 12/15/2016.
 */
public class ProjectedDEM {

    private final Product sourceProduct;
    private Product targetProduct;

    private boolean isElevationModelAvailable = false;
    private ElevationModel dem = null;
    private Band elevationBand = null;
    private double demNoDataValue = 0.0f;
    private final String bandName;

    private final String demName = "SRTM 3Sec";
    private final String demResamplingMethod = ResamplingFactory.BILINEAR_INTERPOLATION_NAME;
    private final String mapProjection = "AUTO:42001";

    public ProjectedDEM(final String bandName, final Product sourceProduct) {
        this.bandName = bandName;
        this.sourceProduct = sourceProduct;

        createTargetProduct();
    }

    private void createTargetProduct() {
        try {
            double pixelSpacingInMeter = Math.max(SARGeocoding.getAzimuthPixelSpacing(sourceProduct),
                    SARGeocoding.getRangePixelSpacing(sourceProduct));
            double pixelSpacingInDegree = SARGeocoding.getPixelSpacingInDegree(pixelSpacingInMeter);

            final CRSGeoCodingHandler crsHandler = new CRSGeoCodingHandler(sourceProduct, mapProjection,
                    pixelSpacingInDegree, pixelSpacingInMeter);

            targetProduct = new Product(bandName,
                    sourceProduct.getProductType(), crsHandler.getTargetWidth(), crsHandler.getTargetHeight());
            targetProduct.setSceneGeoCoding(crsHandler.getCrsGeoCoding());

            elevationBand = new Band(bandName, ProductData.TYPE_FLOAT32,
                    crsHandler.getTargetWidth(), crsHandler.getTargetHeight());

            elevationBand.setUnit(Unit.METERS);
            elevationBand.setNoDataValue(0); //todo get dem no data value
            elevationBand.setNoDataValueUsed(true);
            targetProduct.addBand(elevationBand);

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private synchronized void getElevationModel() throws Exception {

        if (isElevationModelAvailable) return;

        dem = DEMFactory.createElevationModel(demName, demResamplingMethod);
        demNoDataValue = dem.getDescriptor().getNoDataValue();

        elevationBand.setNoDataValue(demNoDataValue);
        elevationBand.setNoDataValueUsed(true);

        isElevationModelAvailable = true;
    }

    public Product getTargetProduct() { return targetProduct; }

    public Band getElevationBand() {
        return elevationBand;
    }

    public void computeTile(final Rectangle targetRectangle) throws OperatorException {

        try {
            try {
                if (!isElevationModelAvailable) {
                    getElevationModel();
                }
            } catch (Exception e) {
                throw new OperatorException(e);
            }

            final int x0 = targetRectangle.x;
            final int y0 = targetRectangle.y;
            final int w = targetRectangle.width;
            final int h = targetRectangle.height;
            //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            final TileGeoreferencing tileGeoRef = new TileGeoreferencing(targetProduct, x0 - 1, y0 - 1, w + 2, h + 2);

            double[][] localDEM = new double[h + 2][w + 2];

            final boolean valid = DEMFactory.getLocalDEM(
                    dem, demNoDataValue, demResamplingMethod, tileGeoRef, x0, y0, w, h, sourceProduct,
                    false, localDEM);
            if (!valid) {
                return;
            }

            final GeoPos geoPos = new GeoPos();
            ProductData demBuffer = ProductData.createInstance(ProductData.TYPE_FLOAT32, elevationBand.getRasterWidth() * elevationBand.getRasterHeight());

            final EarthGravitationalModel96 egm = EarthGravitationalModel96.instance();

            final int maxY = y0 + h;
            final int maxX = x0 + w;
            for (int y = y0; y < maxY; y++) {
                final int yy = y - y0 + 1;
                for (int x = x0; x < maxX; x++) {
                    final int index = y * elevationBand.getRasterWidth() + x;

                    Double alt = localDEM[yy][x - x0 + 1];
                    if (alt.equals(demNoDataValue)) {
                        continue;
                    }

                    tileGeoRef.getGeoPos(x, y, geoPos);
                    final double lat = geoPos.lat;
                    double lon = geoPos.lon;
                    if (lon >= 180.0) {
                        lon -= 360.0;
                    }

                    if (alt.equals(demNoDataValue)) { // get corrected elevation for 0
                        alt = (double) egm.getEGM(lat, lon);
                    }

                    demBuffer.setElemDoubleAt(index, alt);
                }
            }
            elevationBand.setData(demBuffer);
            localDEM = null;

        } catch (Throwable e) {
            throw new OperatorException(e);
        }
    }

}
