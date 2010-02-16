package com.bc.ceres.glevel;

import com.bc.ceres.grender.Rendering;

/**
 * An {@code MultiLevelRenderer} is used to render images provided
 * by a {@link MultiLevelSource}s at a certain resolution level.
 */
public interface MultiLevelRenderer {
    /**
     * Renders the image provided by the given {@link MultiLevelSource} at
     * the specified resolution level onto
     * a {@link com.bc.ceres.grender.Rendering Rendering}.
     *
     * @param rendering        The rendering.
     * @param multiLevelSource The multi-resolution image source.
     * @param level            The resolution level.
     */
    void renderImage(Rendering rendering, MultiLevelSource multiLevelSource, int level);


    /**
     * Releases any allocated resources hold by this {@code MultiLevelRenderer},
     * e.g. releases images or image tiles cached over multiple calls to {@link #renderImage}.
     */
    void reset();
}