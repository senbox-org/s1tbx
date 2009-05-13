package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;

import javax.media.jai.ImageLayout;
import java.awt.image.RenderedImage;

/**
 * Adapts a JAI {@link javax.media.jai.PlanarImage PlanarImage} to the
 * {@link com.bc.ceres.glevel.MultiLevelSource} interface.
 * The image data provided by this {@code PlanarImage} corresponds to the level zero image of the given
 * {@code MultiLevelSource}.
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class DefaultMultiLevelImage extends MultiLevelImage {

    private final MultiLevelSource source;

    /**
     * Constructs a new multi-level image from the given source.
     *
     * @param source The multi-level image source.
     */
    public DefaultMultiLevelImage(MultiLevelSource source) {
        super(new ImageLayout(source.getImage(0)), null, null);
        this.source = source;
    }

    /**
     * @return The multi-level image source.
     */
    public final MultiLevelSource getSource() {
        return source;
    }

    /////////////////////////////////////////////////////////////////////////
    // MultiLevelImage interface

    @Override
    public final MultiLevelModel getModel() {
        return source.getModel();
    }

    @Override
    public final RenderedImage getImage(int level) {
        return source.getImage(level);
    }

    @Override
    public void reset() {
        source.reset();
    }

    @Override
    public void dispose() {
        source.reset();
        super.dispose();
    }
}