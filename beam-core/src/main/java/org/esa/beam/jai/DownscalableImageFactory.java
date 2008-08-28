package org.esa.beam.jai;

import com.bc.ceres.glevel.DownscalableImage;

/**
 * A factory for {@link com.bc.ceres.glevel.DownscalableImage}s.
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public interface DownscalableImageFactory {
    /**
     * Creates a new {@link com.bc.ceres.glevel.DownscalableImage}.
     *
     * @param level The resolution level. Level 0 is full resolution.
     * @return The new image.
     */
    DownscalableImage createDownscalableImage(int level);
}
