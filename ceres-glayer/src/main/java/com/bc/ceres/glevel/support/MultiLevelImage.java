package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.MultiLevelModel;

import javax.media.jai.RenderedImageAdapter;
import java.awt.image.RenderedImage;

/**
 * Adapts a JAI {@link javax.media.jai.PlanarImage PlanarImage} to the {@link com.bc.ceres.glevel.MultiLevelSource} interface.
 * The image data provided by this {@code PlanarImage} corresponds to the level zero image of the given
 * {@code LevelImageSource}.
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class MultiLevelImage extends RenderedImageAdapter implements MultiLevelSource {

    private final MultiLevelSource multiLevelSource;


    public MultiLevelImage(MultiLevelSource multiLevelSource) {
        super(multiLevelSource.getLevelImage(0));
        this.multiLevelSource = multiLevelSource;
    }

    public MultiLevelSource getMultiLevelSource() {
        return multiLevelSource;
    }

    @Override
    public MultiLevelModel getModel() {
        return multiLevelSource.getModel();
    }

    @Override
    public RenderedImage getLevelImage(int level) {
        return multiLevelSource.getLevelImage(level);
    }

    @Override
    public void reset() {
        multiLevelSource.reset();
    }

    @Override
    public synchronized void dispose() {
        reset();
        super.dispose();
    }
}