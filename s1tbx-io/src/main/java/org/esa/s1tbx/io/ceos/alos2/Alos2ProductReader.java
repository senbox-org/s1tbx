/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.ceos.alos2;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.VirtualDir;
import org.esa.s1tbx.io.ceos.CEOSProductDirectory;
import org.esa.s1tbx.io.ceos.CEOSProductReader;
import org.esa.s1tbx.io.ceos.alos.AlosPalsarConstants;
import org.esa.s1tbx.io.ceos.alos.AlosPalsarImageFile;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.quicklooks.Quicklook;
import org.esa.snap.engine_utilities.datamodel.Unit;

import java.awt.Rectangle;
import java.io.IOException;
import java.nio.file.Path;

/**
 * The product reader for AlosPalsar products.
 */
public class Alos2ProductReader extends CEOSProductReader {

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public Alos2ProductReader(final ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected CEOSProductDirectory createProductDirectory(final VirtualDir productDir) {
        return new Alos2ProductDirectory(productDir);
    }

    DecodeQualification checkProductQualification(final Path path) {

        try {
            dataDir = createProductDirectory(createProductDir(path));

            final Alos2ProductDirectory dataDir = (Alos2ProductDirectory) this.dataDir;
            if (dataDir.isALOS2()) {
                return DecodeQualification.INTENDED;
            }
            return DecodeQualification.UNABLE;

        } catch (Exception e) {
            return DecodeQualification.UNABLE;
        }
    }

    @Override
    protected void addQuicklooks(final Product product, final VirtualDir productDir) throws IOException {
        final String[] files = productDir.list("");
        if(files != null) {
            for (String file : files) {
                String name = file.toLowerCase();
                if (name.startsWith("brs") && name.endsWith(".jpg")) {
                    addQuicklook(product, Quicklook.DEFAULT_QUICKLOOK_NAME, productDir.getFile(file));
                    return;
                }
            }
        }
    }

    public void readTiePointGridRasterData(final TiePointGrid tpg,
                                           final int destOffsetX, final int destOffsetY,
                                           final int destWidth, final int destHeight,
                                           final ProductData destBuffer, final ProgressMonitor pm)
            throws IOException {
        final Alos2ProductDirectory alosDataDir = (Alos2ProductDirectory) this.dataDir;
        Rectangle destRect = new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight);
        alosDataDir.readTiePointGridRasterData(tpg, destRect, destBuffer, pm);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        try {
            final Alos2ProductDirectory dataDir = (Alos2ProductDirectory) this.dataDir;
            final AlosPalsarImageFile imageFile = (AlosPalsarImageFile) dataDir.getImageFile(destBand);
            if (dataDir.isSLC()) {
                boolean oneOf2 = destBand.getUnit().equals(Unit.REAL) || !destBand.getName().startsWith("q");

                if (dataDir.getProductLevel() == AlosPalsarConstants.LEVEL1_0) {
                    imageFile.readBandRasterDataSLCByte(sourceOffsetX, sourceOffsetY,
                            sourceWidth, sourceHeight,
                            sourceStepX, sourceStepY,
                            destWidth,
                            destBuffer, oneOf2, pm);
                } else {
                    imageFile.readBandRasterDataSLCFloat(sourceOffsetX, sourceOffsetY,
                            sourceWidth, sourceHeight,
                            sourceStepX, sourceStepY,
                            destWidth,
                            destBuffer, oneOf2, pm);
                }
            } else {
                imageFile.readBandRasterDataShort(sourceOffsetX, sourceOffsetY,
                        sourceWidth, sourceHeight,
                        sourceStepX, sourceStepY,
                        destWidth,
                        destBuffer, pm);
            }

        } catch (Throwable e) {
            //handleReaderException(e);
        }
    }
}
