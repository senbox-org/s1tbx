package org.esa.nest.dat.layers.maptools.components;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.nest.dat.layers.ScreenPixelConverter;
import org.esa.nest.util.ResourceUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * map tools compass component
 */
public class CompassComponent implements MapToolsComponent {

    private static final ImageIcon roseIcon = ResourceUtils.LoadIcon("org/esa/nest/icons/compass_rose.png");
    private final BufferedImage image;
    private final PixelPos tail, head;
    private final PixelPos point1;
    private double angle;

    public CompassComponent(final RasterDataNode raster) {
        image = new BufferedImage(roseIcon.getIconWidth(), roseIcon.getIconHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        final Graphics2D g = image.createGraphics();
        g.drawImage(roseIcon.getImage(), null, null);

        final int width = raster.getRasterWidth();
        final int height = raster.getRasterHeight();
        final GeoCoding geoCoding = raster.getGeoCoding();

        point1 = new PixelPos(0, 0);
        final GeoPos point1Geo = geoCoding.getGeoPos(point1, null);
        final GeoPos point2Geo = geoCoding.getGeoPos(new PixelPos(width/2, height/2), null);
        final PixelPos point2 = geoCoding.getPixelPos(new GeoPos(point2Geo.getLat(), point1Geo.getLon()), null);

        final double op = point1.x-point2.x;
        final double hyp = Math.hypot(op, point1.y-point2.y);
        angle = Math.asin(op/hyp);

        if(point1Geo.getLat() < point2Geo.getLat()) {
            tail = point1;
            head = point2;
            angle += Math.PI;
        } else {
            tail = point2;
            head = point1;
        }
    }

    public void render(final Graphics2D graphics, final ScreenPixelConverter screenPixel) {

        final AffineTransform transformSave = graphics.getTransform();
        try {
            /*graphics.setColor(Color.RED);

            GraphicsUtils.drawArrow(graphics, screenPixel,
                    (int) tail.getX(), (int) tail.getY(),
                    (int) head.getX(), (int) head.getY());   */

            final AffineTransform transform = screenPixel.getImageTransform(transformSave);
            transform.translate(point1.x, point1.y);
            transform.rotate(angle);

            graphics.drawRenderedImage(image, transform);
        } finally {
            graphics.setTransform(transformSave);
        }
    }
}
