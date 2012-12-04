package org.esa.nest.dat.layers.maptools.components;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.nest.dat.layers.ScreenPixelConverter;
import org.esa.nest.util.ResourceUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * map tools logo component
 */
public class LogoComponent implements MapToolsComponent {

    private static final ImageIcon procNestIcon = ResourceUtils.LoadIcon("org/esa/nest/icons/proc_nest.png");
    private final BufferedImage image;
    private final Point point;

    public LogoComponent(final RasterDataNode raster) {

        image = new BufferedImage(procNestIcon.getIconWidth(), procNestIcon.getIconHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        final Graphics2D g = image.createGraphics();
        g.drawImage(procNestIcon.getImage(), null, null);

        point = new Point(raster.getRasterWidth()-image.getWidth(), raster.getRasterHeight()-image.getHeight());
    }

    public void render(final Graphics2D graphics, final ScreenPixelConverter screenPixel) {
        final AffineTransform transformSave = graphics.getTransform();
        try {
            final AffineTransform transform = screenPixel.getImageTransform(transformSave);
            transform.translate(point.x, point.y);

            graphics.drawRenderedImage(image, transform);
        } finally {
            graphics.setTransform(transformSave);
        }
    }
}
