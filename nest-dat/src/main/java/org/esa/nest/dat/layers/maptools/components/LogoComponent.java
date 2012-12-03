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

        point = new Point(100,100);
    }

    public void render(final Graphics2D graphics, final ScreenPixelConverter screenPixel) {

        final double[] pts = new double[2];
        screenPixel.pixelToScreen(point, pts);
        final double zoom = screenPixel.getZoomFactor();

        final AffineTransform at = new AffineTransform();
        at.setToTranslation((int)pts[0], (int)pts[1]);
        at.scale(zoom, zoom);
        graphics.drawRenderedImage(image, at);
    }
}
