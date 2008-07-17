package org.esa.beam.jai;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import java.awt.*;
import java.util.Map;


/**
 * A base class for {@code OpImages} retrieving data at a given pyramid level.
 */
public abstract class SingleBandedOpImage extends LevelOpImage {

    protected SingleBandedOpImage(int dataBufferType,
                                  int sourceWidth,
                                  int sourceHeight,
                                  Dimension tileSize,
                                  Map configuration) {
        this(dataBufferType,
             sourceWidth,
             sourceHeight,
             tileSize,
             null,
             0,
             configuration);
    }

    protected SingleBandedOpImage(int dataBufferType,
                                  int sourceWidth,
                                  int sourceHeight,
                                  Dimension tileSize,
                                  LevelOpImage level0OpImage,
                                  int level,
                                  Map configuration) {
        this(ImageManager.createSingleBandedImageLayout(dataBufferType,
                                                        sourceWidth,
                                                        sourceHeight,
                                                        tileSize, level),
             configuration,
             sourceWidth,
             sourceHeight,
             level0OpImage,
             level);
    }

    private SingleBandedOpImage(ImageLayout layout,
                                Map configuration,
                                int sourceWidth,
                                int sourceHeight,
                                LevelOpImage level0OpImage,
                                int level) {
        super(layout,
              configuration,
              layout.getSampleModel(null),
              layout.getMinX(null),
              layout.getMinY(null),
              layout.getWidth(null),
              layout.getHeight(null),
              sourceWidth,
              sourceHeight,
              level0OpImage,
              level);
        setTileCache(JAI.getDefaultInstance().getTileCache());
    }


}
