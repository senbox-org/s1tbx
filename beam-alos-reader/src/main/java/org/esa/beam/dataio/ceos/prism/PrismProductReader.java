/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.dataio.ceos.prism;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.dataio.ceos.CeosHelper;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.TreeNode;

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
     * Retrieves a set of TreeNode objects that represent the physical product structure as stored on the harddrive.
     * The tree consisty of:
     * - a root node (the one returned) pointing to the directory that CONTAINS the product
     * - any number of nested children that compose the product.
     * Each TreeNod is configured as follows:
     * - id: contains a string representation of the path. For the root node, this is the
     * absolute path to the parent of the file returned by Product.getFileLocation().
     * For all subsequent nodes, the node name.
     * - content: each node stores as content a java.io.File object that physically defines the node.
     * <p/>
     * The method returns null when a TreeNode can not be assembled (i.e. in-memory product, created from stream ...)
     *
     * @return the root TreeNode or null
     */
    @Override
    public TreeNode<File> getProductComponents() {
        final File input = CeosHelper.getFileFromInput(getInput());
        if (input == null) {
            return null;
        }

        return _prismDir.getProductComponents();
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
    protected Product readProductNodesImpl() throws IOException {
        final ProductReaderPlugIn readerPlugIn = getReaderPlugIn();
        final Object input = getInput();
        if (readerPlugIn.getDecodeQualification(input) == DecodeQualification.UNABLE) {
            throw new IOException("Unsupported product format."); /*I18N*/
        }
        final File fileFromInput = CeosHelper.getFileFromInput(getInput());
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
}


