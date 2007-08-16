/*
 * $Id: PrismProductReader.java,v 1.3 2007/03/19 15:52:28 marcop Exp $
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
package org.esa.beam.dataio.ceos.prism;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.IllegalFileFormatException;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.io.IOException;

/**
 * The product reader for Prism products.
 *
 * @author Sabine Embacher
 */
public class PrismProductReader extends AbstractProductReader {

    private PrismProductDirectory _prismDir;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public PrismProductReader(final ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    /**
     * Closes the access to all currently opened resources such as file input streams and all resources of this children
     * directly owned by this reader. Its primary use is to allow the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>close()</code> are undefined.
     * <p/>
     * <p>Overrides of this method should always call <code>super.close();</code> after disposing this instance.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (_prismDir != null) {
            _prismDir.close();
            _prismDir = null;
        }
        super.close();
    }

    /**
     * Returns a <code>File</code> if the given input is a <code>String</code> or <code>File</code>,
     * otherwise it returns null;
     *
     * @param input an input object of unknown type
     *
     * @return a <code>File</code> or <code>null</code> it the input can not be resolved to a <code>File</code>.
     */
    public static File getFileFromInput(final Object input) {
        if (input instanceof String) {
            return new File((String) input);
        } else if (input instanceof File) {
            return (File) input;
        }
        return null;
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code> interface method. Clients implementing this
     * method can be sure that the input object and eventually the subset information has already been set.
     * <p/>
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException,
                                                    IllegalFileFormatException {
        final ProductReaderPlugIn readerPlugIn = getReaderPlugIn();
        final Object input = getInput();
        if (readerPlugIn.getDecodeQualification(input) == DecodeQualification.UNABLE) {
            throw new IOException("Unsupported product format."); /*I18N*/
        }
        final File fileFromInput = PrismProductReader.getFileFromInput(getInput());
        final Product product;
        try {
            _prismDir = new PrismProductDirectory(fileFromInput.getParentFile());
            product = _prismDir.createProduct();
        } catch (IllegalCeosFormatException e) {
            final IOException ioException = new IOException(e.getMessage());
            ioException.initCause(e);
            throw ioException;
        }
        product.setProductReader(this);
        product.setModified(false);
        return product;
    }


    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int bufferX,
                                          int bufferY, int bufferWidth, int bufferHeight, ProductData buffer,
                                          ProgressMonitor pm) throws IOException {
        final DataBuffer destBuffer = new DataBuffer(buffer,
                                                     bufferX,
                                                     bufferY,
                                                     bufferWidth,
                                                     bufferHeight);

        final PrismImageFile[] imageFiles = _prismDir.getImageFiles();

        pm.beginTask("Reading band ...", imageFiles.length);
        try {
            for (int i = 0; i < imageFiles.length; i++) {
                if (pm.isCanceled()) {
                    break;
                }
                final PrismImageFile imageFile = imageFiles[i];
                final int overlap = imageFile.getOverlap();
                final int ccdWidth = imageFile.getWidth() - overlap;
                final int sceneOffsetX = ccdWidth * i;

                final int ccdSourceOffsetX;
                final int destOffsetX;
                if (sceneOffsetX < sourceOffsetX) {
                    ccdSourceOffsetX = sourceOffsetX - sceneOffsetX;

                    destOffsetX = 0;
                } else {
                    ccdSourceOffsetX = 0;

                    final int destSceneOffsetX = sceneOffsetX / sourceStepX;
                    final int destBufferPosX = sourceOffsetX / sourceStepX;
                    destOffsetX = destSceneOffsetX - destBufferPosX;
                }

                if (ccdSourceOffsetX > ccdWidth) {
                    continue;
                }

                final int scenePos = sourceOffsetX + sourceWidth;

                final int ccdSourceWidth;
                if (scenePos > sceneOffsetX + ccdWidth) {
                    ccdSourceWidth = ccdWidth - ccdSourceOffsetX;
                } else {
                    ccdSourceWidth = scenePos - sceneOffsetX - ccdSourceOffsetX;
                }

                if (ccdSourceWidth <= 0) {
                    continue;
                }

                final int destWidth = ccdSourceWidth / sourceStepX;

                imageFile.readBandRasterData(ccdSourceOffsetX, sourceOffsetY,
                                             ccdSourceWidth, sourceHeight,
                                             sourceStepX, sourceStepY,
                                             destBuffer,
                                             destOffsetX, destWidth, SubProgressMonitor.create(pm, 1));
            }
        } catch (IllegalCeosFormatException e) {
            final IOException ioException = new IOException(e.getMessage());
            ioException.initCause(e);
            throw ioException;
        } finally {
            pm.done();
        }

    }

    /**
     * The class is public for the benefit of the implementation of an other class
     * and the API may change in future releases of the software.
     */
    public static class DataBuffer {

        private final ProductData _buffer;
        private final Point _location;
        private final Dimension _dimension;

        public DataBuffer(final ProductData buffer, final int x, final int y, final int width, final int height) {
            _buffer = buffer;
            _location = new Point(x, y);
            _dimension = new Dimension(width, height);
        }

        public ProductData getBuffer() {
            return _buffer;
        }

        public Point getLocation() {
            return _location;
        }

        public Dimension getDimension() {
            return _dimension;
        }
    }
}


