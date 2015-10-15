/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.image;

import com.bc.ceres.jai.NoDataRaster;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.SourcelessOpImage;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Map;


/**
 * A base class for single-band {@code OpImages} retrieving data at a given pyramid level.
 */
public abstract class SingleBandedOpImage extends SourcelessOpImage {

    private LevelImageSupport levelImageSupport;

    /**
     * Constructor.
     *
     * @param dataBufferType The data type.
     * @param sourceWidth    The width of the level 0 image.
     * @param sourceHeight   The height of the level 0 image.
     * @param tileSize       The tile size for this image.
     * @param configuration  The configuration map. May be {@code null}.
     * @param level          The resolution level.
     */
    protected SingleBandedOpImage(int dataBufferType,
                                  int sourceWidth,
                                  int sourceHeight,
                                  Dimension tileSize,
                                  Map configuration,
                                  ResolutionLevel level) {
        this(dataBufferType,
             null,
             sourceWidth,
             sourceHeight,
             tileSize,
             configuration,
             level);
    }

    /**
     * Constructor.
     *
     * @param dataBufferType The data type.
     * @param sourcePos      The position of the level 0 image. May be {@code null}.
     * @param sourceWidth    The width of the level 0 image.
     * @param sourceHeight   The height of the level 0 image.
     * @param tileSize       The tile size for this image.
     * @param configuration  The configuration map. May be {@code null}.
     * @param level          The resolution level.
     */
    protected SingleBandedOpImage(int dataBufferType,
                                  Point sourcePos,
                                  int sourceWidth,
                                  int sourceHeight,
                                  Dimension tileSize,
                                  Map configuration,
                                  ResolutionLevel level) {
        this(ImageManager.createSingleBandedImageLayout(dataBufferType,
                                                        sourcePos,
                                                        sourceWidth,
                                                        sourceHeight,
                                                        tileSize,
                                                        level),
             sourceWidth,
             sourceHeight,
             configuration,
             level);
    }

    private SingleBandedOpImage(ImageLayout layout,
                                int sourceWidth,
                                int sourceHeight,
                                Map configuration,
                                ResolutionLevel level) {
        super(layout,
              configuration,
              layout.getSampleModel(null),
              layout.getMinX(null),
              layout.getMinY(null),
              layout.getWidth(null),
              layout.getHeight(null));
        levelImageSupport = new LevelImageSupport(sourceWidth,
                                                  sourceHeight,
                                                  level);
        if (getTileCache() == null) {
            setTileCache(JAI.getDefaultInstance().getTileCache());
        }
    }

    public final int getLevel() {
        return levelImageSupport.getLevel();
    }

    protected final double getScale() {
        return levelImageSupport.getScale();
    }

    protected final int getSourceX(int targetX) {
        return levelImageSupport.getSourceX(targetX);
    }

    protected final int getSourceY(int targetY) {
        return levelImageSupport.getSourceY(targetY);
    }

    protected final int getSourceWidth(int targetWidth) {
        return levelImageSupport.getSourceWidth(targetWidth);
    }

    protected final int getSourceHeight(int targetHeight) {
        return levelImageSupport.getSourceHeight(targetHeight);
    }

    protected final int getSourceCoord(double targetCoord, int min, int max) {
        return levelImageSupport.getSourceCoord(targetCoord, min, max);
    }

    protected LevelImageSupport getLevelImageSupport() {
        return levelImageSupport;
    }

    /**
     * Creates a new raster containing solely no-data (non-interpretable data, missing data) samples. The raster's
     * data buffer is filled with the given no-data value.
     * <p>
     * The raster's origin is (0, 0). In order to translate the raster,
     * use {@link Raster#createTranslatedChild(int x, int y)}.
     *
     * @param noDataValue The no-data value used to fill the data buffer
     *                    of the raster created.
     * @return the raster created.
     * @see  NoDataRaster
     */
    protected NoDataRaster createNoDataRaster(double noDataValue) {
        final Raster raster = createWritableRaster(getSampleModel(), new Point(0, 0));
        final DataBuffer buffer = raster.getDataBuffer();

        for (int i = 0; i < buffer.getSize(); i++) {
            buffer.setElemDouble(i, noDataValue);
        }

        return new NoDataRaster(raster);
    }

    /**
     * Empty implementation. Used to prevent clients from overriding it, since
     * they shall implement {@link #computeRect(javax.media.jai.PlanarImage[], java.awt.image.WritableRaster, java.awt.Rectangle)}.
     *
     * @param sources  The sources.
     * @param dest     The destination raster.
     * @param destRect The destination rectangle.
     */
    @Override
    protected final void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
    }
}
