package org.esa.nest.dat.layers.maptools.components;

import org.esa.nest.dat.layers.ScreenPixelConverter;

import java.awt.*;

/**
 * map tools component interface
 */
public interface MapToolsComponent {

    public void render(final Graphics2D graphics, final ScreenPixelConverter screenPixel);
}
