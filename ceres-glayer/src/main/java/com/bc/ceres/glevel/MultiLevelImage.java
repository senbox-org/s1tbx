package com.bc.ceres.glevel;

import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import java.util.Map;
import java.util.Vector;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.*;

/**
 * A {@link javax.media.jai.PlanarImage PlanarImage} which can act as a {@link MultiLevelSource}.
 * The image data provided by this image corresponds to the level zero image of the
 * {@code MultiLevelSource}.
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public abstract class MultiLevelImage extends PlanarImage implements MultiLevelSource {

    /**
     * Constructs a new {@code MultiLevelImage}.
     * Calls the
     *
     * @param layout     The layout of this image or null.
     * @param sources    The immediate sources of this image or null.
     * @param properties A Map containing the properties of this image or null.
     */
    protected MultiLevelImage(ImageLayout layout, Vector sources, Map properties) {
        super(layout, sources, properties);
    }

    /////////////////////////////////////////////////////////////////////////
    // PlanarImage interface

    @Override
    public final Raster getTile(int x, int y) {
        return getImage(0).getTile(x, y);
    }

    @Override
    public final Raster getData() {
        return getImage(0).getData();
    }

    @Override
    public final Raster getData(Rectangle rect) {
        return getImage(0).getData(rect);
    }

    @Override
    public final WritableRaster copyData(WritableRaster raster) {
        return getImage(0).copyData(raster);
    }

    /**
     * Provides a hint that an image will no longer be accessed from a
     * reference in user space.  The results are equivalent to those
     * that occur when the program loses its last reference to this
     * image, the garbage collector discovers this, and finalize is
     * called.  This can be used as a hint in situations where waiting
     * for garbage collection would be overly conservative.
     * <p/>
     * <p> The results of referencing an image after a call to
     * <code>dispose()</code> are undefined.</p>
     * <p/>
     * <p>Overrides shall call {@code super.dispose()} in a final step.</p>
     */
    @Override
    public void dispose() {
        super.dispose();
    }
}