package org.esa.nest.dat.layers.maptools.components;

import org.esa.beam.framework.datamodel.GeoCoding;
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


    }

    public void render(final Graphics2D graphics, final ScreenPixelConverter screenPixel) {
        final double[] pts = new double[] { 0, 0, 100, 0};
        final double[] vpts = new double[pts.length];
        screenPixel.pixelToScreen(pts, vpts);

        graphics.setColor(Color.YELLOW);
        graphics.drawLine((int)vpts[0], (int)vpts[1], (int)vpts[2], (int)vpts[3]);
    }
}
