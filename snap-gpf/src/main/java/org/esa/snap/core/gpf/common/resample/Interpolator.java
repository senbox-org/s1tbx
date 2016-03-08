package org.esa.snap.core.gpf.common.resample;

import javax.media.jai.RasterAccessor;
import java.awt.Rectangle;

/**
 * @author Tonio Fincke
 */
public interface Interpolator {

    void init(RasterAccessor srcAccessor, RasterAccessor dstAccessor, double nodataValue);

    void interpolate(Rectangle destRect, Rectangle srcRect, double scaleX, double scaleY, double offsetX, double offsetY);

}
