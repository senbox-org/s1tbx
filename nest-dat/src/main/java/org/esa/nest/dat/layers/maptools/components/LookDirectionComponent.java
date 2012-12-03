package org.esa.nest.dat.layers.maptools.components;

import org.esa.beam.framework.datamodel.*;
import org.esa.nest.dat.layers.ScreenPixelConverter;
import org.esa.nest.datamodel.AbstractMetadata;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * map tools look direction component
 */
public class LookDirectionComponent implements MapToolsComponent {
    private boolean valid = true;
    private final List<Point> tails = new ArrayList<Point>();
    private final List<Point> heads = new ArrayList<Point>();

    public LookDirectionComponent(final RasterDataNode raster) {
        try {
            final Product product = raster.getProduct();
            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
            final MetadataElement lookDirectionListElem = absRoot.getElement("Look_Direction_List");
            if(lookDirectionListElem != null) {
                final GeoCoding geoCoding = raster.getGeoCoding();

                final MetadataElement[] dirDataElems = lookDirectionListElem.getElements();
                for(MetadataElement dirElem : dirDataElems) {
                    final float tailLat = (float)dirElem.getAttributeDouble("tail_lat");
                    final float tailLon = (float)dirElem.getAttributeDouble("tail_lon");
                    final float headLat = (float)dirElem.getAttributeDouble("head_lat");
                    final float headLon = (float)dirElem.getAttributeDouble("head_lon");

                    final PixelPos tailPix = geoCoding.getPixelPos(new GeoPos(tailLat, tailLon), null);
                    final PixelPos headPix = geoCoding.getPixelPos(new GeoPos(headLat, headLon), null);

                    final double m = (headPix.getY()-tailPix.getY()) / (headPix.getX()-tailPix.getX());
                    int length = 100;
                    if(tailPix.getX() > headPix.getX())
                        length = -length;
                    final int x = (int)tailPix.getX() + length;
                    final int y = (int)(m*x + (headPix.getY() - m*headPix.getX()));

                    tails.add(new Point((int)tailPix.getX(), (int)tailPix.getY()));
                    heads.add(new Point(x, y));
                }
            } else {
                final String pass = absRoot.getAttributeString(AbstractMetadata.PASS, null);
                final String antennaPointing = absRoot.getAttributeString(AbstractMetadata.antenna_pointing, null);

                int x = 0;
                int y = raster.getSceneRasterHeight() / 2;
                tails.add(new Point(x, y));
                heads.add(new Point(x+100, y));
            }
        } catch(Exception e) {
            valid = false;
        }
    }

    public void render(final Graphics2D graphics, final ScreenPixelConverter screenPixel) {
        if(!valid) return;

        graphics.setColor(Color.YELLOW);
        final double[] tailpts = new double[2];
        final double[] headpts = new double[2];
        for(int i=0; i < tails.size(); ++i) {
            screenPixel.pixelToScreen(tails.get(i), tailpts);
            screenPixel.pixelToScreen(heads.get(i), headpts);

            graphics.drawLine((int)tailpts[0], (int)tailpts[1], (int)headpts[0], (int)headpts[1]);
        }


    }
}
