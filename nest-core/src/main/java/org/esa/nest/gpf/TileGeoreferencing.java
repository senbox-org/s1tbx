package org.esa.nest.gpf;

import org.esa.beam.framework.datamodel.*;
import com.bc.ceres.core.ProgressMonitor;

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

    boolean hasTPG;
    float[] latPixels;
    float[] lonPixels;

    public TileGeoreferencing(final Product product, final int x1, final int y1, final int w, final int h) {
        geocoding = product.getGeoCoding();
        latTPG = OperatorUtils.getLatitude(product);
        lonTPG = OperatorUtils.getLongitude(product);
        this.x1 = x1;
        this.y1 = y1;
        width = w;
        size = w*h;

        hasTPG = !(latTPG == null || lonTPG == null);

        if(latTPG != null) {
            latPixels = new float[size];
            latTPG.getPixels(x1, y1, w, h, latPixels, ProgressMonitor.NULL);
        } else {
            latPixels = null;
        }

        try {
            if(lonTPG != null) {
                lonPixels = new float[size];
                lonTPG.getPixels(x1, y1, w, h, lonPixels, ProgressMonitor.NULL);
            } else {
                lonPixels = null;
            }
        } catch(Exception e) {
            System.out.println("TileGeoreferencing tiepoint error "+e.getMessage());
            hasTPG = false;
        }
    }

    public void getGeoPos(final int x, final int y, final GeoPos geo) {

        if(hasTPG) {
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

        if(hasTPG) {
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
}
