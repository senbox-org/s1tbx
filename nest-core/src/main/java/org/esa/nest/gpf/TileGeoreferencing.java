package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;

/**
 * Handle getting the georefereing for the tile
 */
public class TileGeoreferencing {

    final TiePointGrid latTPG;
    final TiePointGrid lonTPG;
    final GeoCoding geocoding;
    final int width;
    final int x1;
    final int y1;
    final int size;

    boolean isCached;
    float[] latPixels = null;
    float[] lonPixels = null;

    public TileGeoreferencing(final Product product, final int x1, final int y1, final int w, final int h) {
        geocoding = product.getGeoCoding();
        latTPG = OperatorUtils.getLatitude(product);
        lonTPG = OperatorUtils.getLongitude(product);
        this.x1 = x1;
        this.y1 = y1;
        width = w;
        size = w*h;

        final boolean isCrsGeoCoding = geocoding instanceof CrsGeoCoding;
        isCached = !(latTPG == null || lonTPG == null) || isCrsGeoCoding;

        try {
            if(isCrsGeoCoding) {
                latPixels = new float[size];
                lonPixels = new float[size];
                ((CrsGeoCoding)geocoding).getPixels(x1, y1, w, h, latPixels, lonPixels);
            } else {
                if(latTPG != null) {
                    latPixels = new float[size];
                    latTPG.getPixels(x1, y1, w, h, latPixels, ProgressMonitor.NULL);
                }

                if(lonTPG != null) {
                    lonPixels = new float[size];
                    lonTPG.getPixels(x1, y1, w, h, lonPixels, ProgressMonitor.NULL);
                }
            }
        } catch(Exception e) {
            System.out.println("TileGeoreferencing tiepoint error "+e.getMessage());
            isCached = false;
        }
    }

    public void getGeoPos(final int x, final int y, final GeoPos geo) {

        if(isCached) {
            final int xx = x - x1;
            final int yy = y - y1;
            final int pos = yy*width+xx;
            if(xx >= 0 && yy >= 0 && pos < size) {
                geo.setLocation(latPixels[pos], lonPixels[pos]);
                return;
            }
        }
        geocoding.getGeoPos(new PixelPos(x+0.5f,y+0.5f), geo);
    }

    public void getGeoPos(final PixelPos pix, final GeoPos geo) {

        if(isCached) {
            final int xx = (int)pix.getX() - x1;
            final int yy = (int)pix.getY() - y1;
            final int pos = yy*width+xx;
            if(xx >= 0 && yy >= 0 && pos < size) {
                geo.setLocation(latPixels[pos], lonPixels[pos]);
                return;
            }
        }
        geocoding.getGeoPos(pix, geo);
    }

    public void getPixelPos(final GeoPos geo, final PixelPos pix) {
        if (geo.lon < 0) {
            geo.lon += 360;
        }
        geocoding.getPixelPos(geo, pix);
    }
}
