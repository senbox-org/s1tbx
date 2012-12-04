package org.esa.nest.dat.layers.maptools.components;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.nest.dat.layers.ScreenPixelConverter;

import java.awt.*;

/**
 * map tools lat lon grid component
 */
public class LatLonGridComponent implements MapToolsComponent {


    public LatLonGridComponent(final RasterDataNode raster) {
        final int width = raster.getRasterWidth();
        final int height = raster.getRasterHeight();
        final GeoCoding geoCoding = raster.getGeoCoding();

        final PixelPos tlPix = new PixelPos(0,0);
        final PixelPos trPix = new PixelPos(width, 0);
        final PixelPos blPix = new PixelPos(0,height);
        final PixelPos brPix = new PixelPos(width,height);
        final GeoPos tlGeo = geoCoding.getGeoPos(tlPix, null);
        final GeoPos trGeo = geoCoding.getGeoPos(trPix, null);


    }

    public void render(final Graphics2D graphics, final ScreenPixelConverter screenPixel) {
        final double[] pts = new double[] { 0, 0, 100, 0};
        final double[] vpts = new double[pts.length];
        screenPixel.pixelToScreen(pts, vpts);

        graphics.setColor(Color.YELLOW);
        graphics.drawLine((int)vpts[0], (int)vpts[1], (int)vpts[2], (int)vpts[3]);
    }
}
