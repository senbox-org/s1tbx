/*
 * $Id: PixelInterleavedRgbOpImage.java,v 1.2 2006/10/09 12:08:17 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.util.jai;

import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.TileCache;
import javax.media.jai.TileComputationListener;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.TIFFEncodeParam;

/**
 * By default this class does not use tile caching. Tile caching can be important to avaoid re-computation of same
 * tiles. In order to provide tile caching for this class either call <code>setTileCache</code> on an instance of this
 * class or pass a suitable configuration to its constructor:
 * <pre>
 * Map configuration = new HashMap();
 * TileCache tileCache = JAI.getDefaultInstance().createTileCache(64L * 1024 * 1024);
 * configuration.put(JAI.KEY_TILE_CACHE, tileCache);
 * </pre>
 * <p/>
 * <blockquote> <b>What is the best SampleModel to use for performance?</b> <i>Fastest processing performance will be
 * achieved when the Raster is composed of a PixelInterleavedSampleModel and a DataBuffer type that is supported by
 * native acceleration for the operations of interest. The BandOffsets arrays of all sources and the destinations should
 * match.</i> </blockquote>
 */
public abstract class PixelInterleavedRgbOpImage extends OpImage {

    /**
     * Default tile size for best JAI performance.
     */
    public static final int DEFAULT_TILE_SIZE = 512;

    private static final boolean _DEBUG = true;

    private int _numTilesComputed;

    //@todo 1 he/nf - don't remove this comments
//    private TileProducer _tileProducer;

    private boolean[] _continueTileComputing;

    public PixelInterleavedRgbOpImage(final int imageWidth,
                                      final int imageHeight,
                                      final boolean hasAlpha,
                                      final Map configuration) {
        this(imageWidth,
             imageHeight,
             imageWidth >= 2 * DEFAULT_TILE_SIZE ? DEFAULT_TILE_SIZE : imageWidth,
             imageHeight >= 2 * DEFAULT_TILE_SIZE ? DEFAULT_TILE_SIZE : imageHeight,
             hasAlpha,
             configuration);
    }

    public PixelInterleavedRgbOpImage(final int imageWidth,
                                      final int imageHeight,
                                      final int tileWidth,
                                      final int tileHeight,
                                      final boolean hasAlpha,
                                      final Map configuration) {
        super(null,
              createPixelInterleavedRgbImageLayout(imageWidth,
                                                   imageHeight,
                                                   tileWidth,
                                                   tileHeight,
                                                   hasAlpha),
              configuration,
              false);
        setTileCache(JAI.getDefaultInstance().getTileCache());
    }

    public int getNumTilesComputed() {
        return _numTilesComputed;
    }

    public int writeTIFF(String filePath) throws IOException {
        TIFFEncodeParam encodeParam = new TIFFEncodeParam();
        encodeParam.setTileSize(getTileWidth(), getTileHeight());
        encodeParam.setWriteTiled(true);
        final FileOutputStream ostream = new FileOutputStream(filePath);
        final ImageEncoder imageEncoder = ImageCodec.createImageEncoder("TIFF", ostream, encodeParam);
        imageEncoder.encode(this);
        ostream.close();
        return getNumTilesComputed();
    }

    /**
     * Returns false as this class returns Rasters via computeTile() that are internally cached. Some subclasses may
     * want to override this method and return true.
     *
     * @return always <code>false</code>
     */
    @Override
    public boolean computesUniqueTiles() {
        return false;
    }

    /**
     * This default implementation simply returns <code>null</code>, because this operation does not have source
     * images.
     *
     * @param destRectangle the destination rectangle
     * @param sourceIndex   the index of the source image
     *
     * @return always <code>null</code>
     */
    @Override
    public Rectangle mapDestRect(Rectangle destRectangle, int sourceIndex) {
        return null;
    }

    /**
     * This default implementation simply returns <code>null</code>, because this operation does not have source
     * images.
     *
     * @param sourceRectangle the destination rectangle
     * @param sourceIndex     the index of the source image
     *
     * @return always <code>null</code>
     */
    @Override
    public Rectangle mapSourceRect(Rectangle sourceRectangle, int sourceIndex) {
        return null;
    }

    /**
     * Computes a rectangle of output.
     *
     * @param planarImages   ignored, since this operation does not support source images
     * @param writableRaster the tile to be computed
     * @param rectangle      the tile rectangle
     */
    @Override
    protected void computeRect(final PlanarImage[] planarImages,
                               final WritableRaster writableRaster,
                               final Rectangle rectangle) {

        if (_DEBUG) {
            System.out.println("computeRect: ");
            if (planarImages != null) {
                for (int i = 0; i < planarImages.length; i++) {
                    System.out.println("   planarImages[" + i + "] = " + planarImages[i]);
                }
            }
            System.out.println("   writableRaster = " + writableRaster);
            System.out.println("   rectangle = " + rectangle);

            final TileCache tileCache = getTileCache();
            System.out.println("   tileCache = " + tileCache);
        }

        final DataBuffer dataBuffer = writableRaster.getDataBuffer();
        if (dataBuffer instanceof DataBufferByte) {
            final byte[] samples = ((DataBufferByte) dataBuffer).getData();
            if (samples.length < getNumBands() * rectangle.width * rectangle.height) {
                throw new IllegalStateException("illegal data buffer size");
            }
            computeTilePixels(rectangle, samples);
            _numTilesComputed++;
            fireTileComputed(rectangle, writableRaster);
        } else {
            throw new IllegalStateException("illegal data buffer type");
        }
    }

    //@todo 1 he/nf - don't remove this comments
//    protected void computeTilePixels(Rectangle sourceRectangle, byte[] samples, TiledTiffRequest request, int tileWidth) {
//        if (_tileProducer != null) {
//            _tileProducer.computeTilePixels(sourceRectangle, samples, request, tileWidth);
//        }
//    }

    private void fireTileComputed(final Rectangle rectangle, final WritableRaster writableRaster) {
        final TileComputationListener[] tileComputationListeners = getTileComputationListeners();
        if (tileComputationListeners != null) {
            for (int i = 0; i < tileComputationListeners.length; i++) {
                TileComputationListener tileComputationListener = tileComputationListeners[i];
                final int tileX = rectangle.x / getTileWidth();
                final int tileY = rectangle.y / getTileHeight();
                tileComputationListener.tileComputed(rectangle, null, this, tileX, tileY, writableRaster);
            }
        }
    }

    /**
     * Computes the RGB or RGBA pixels for a tile with the given tile rectangle in image coordinates. <p>The value of a
     * pixel at index
     * <pre>
     *   i = getNumBands() * (y * getWidth() + x);
     * </pre>
     * shall be stored in the specified <code>samples</code> array parameter as follows:
     * <pre>
     *   samples[i + 0] = red;
     *   samples[i + 1] = green;
     *   samples[i + 2] = blue;
     * </pre>
     * and if the image has alpha then also
     * <pre>
     *   samples[i + 3] = alpha;
     * </pre>
     * <p/>
     * <p>The length of the supplied <code>samples</code> array is always <code>getNumBands() * sourceRectangle.width *
     * sourceRectangle.height</code>.
     *
     * @param tileRectangle the tile rectangle
     * @param samples       the pixels to be computed given as byte interleaved sample values
     */
    protected abstract void computeTilePixels(Rectangle tileRectangle, byte[] samples);


    /**
     * Creates a suitable image layout for this image OP.
     *
     * @param imageWidth
     * @param imageHeight
     * @param tileWidth
     * @param tileHeight
     * @param hasAlpha
     *
     * @return a image layout
     */
    public static ImageLayout createPixelInterleavedRgbImageLayout(final int imageWidth,
                                                                   final int imageHeight,
                                                                   final int tileWidth,
                                                                   final int tileHeight,
                                                                   final boolean hasAlpha) {
        final ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                                                      hasAlpha,
                                                      false, //isAlphaPremultiplied,
                                                      hasAlpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE,
                                                      DataBuffer.TYPE_BYTE); //transferType
        final SampleModel sm = cm.createCompatibleSampleModel(imageWidth, imageHeight);
        final ImageLayout imageLayout = new ImageLayout();
        imageLayout.setWidth(imageWidth);
        imageLayout.setHeight(imageHeight);
        imageLayout.setTileWidth(tileWidth);
        imageLayout.setTileHeight(tileHeight);
        imageLayout.setSampleModel(sm);
        imageLayout.setColorModel(cm);
        return imageLayout;
    }

    //@todo 1 he/nf - don't remove this comments
//    public void setTileProducer(TileProducer tileProducer) {
//        _tileProducer = tileProducer;
//    }

//    public interface TileProducer {
//
//        void computeTilePixels(Rectangle sourceRectangle, byte[] samples, TiledTiffRequest request, int tileWidth);
//    }



    public void setContinueTileComputing(boolean[] continueTileComputing) {
        _continueTileComputing = continueTileComputing;
    }

    protected boolean continueTileComputing() {
        return _continueTileComputing == null || _continueTileComputing[0];
    }
}
