package com.bc.ceres.glevel;

import java.awt.image.RenderedImage;

/**
 * A {@link RenderedImage} which is also a {@link MultiLevelSource}.
 * The image data provided by this {@code RenderedImage} corresponds to the level zero image of the given
 * {@code MultiLevelSource}.
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public interface MultiLevelImage extends RenderedImage, MultiLevelSource {
    /**
     * Provides a hint that an image will no longer be accessed from a
     * reference in user space.  The results are equivalent to those
     * that occur when the program loses its last reference to this
     * image, the garbage collector discovers this, and finalize is
     * called.  This can be used as a hint in situations where waiting
     * for garbage collection would be overly conservative.
     *
     * <p> The results of referencing an image after a call to
     * <code>dispose()</code> are undefined.
     */
    void dispose();
}