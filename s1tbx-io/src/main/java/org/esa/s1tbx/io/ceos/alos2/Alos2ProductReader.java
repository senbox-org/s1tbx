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
import org.esa.s1tbx.io.ceos.CEOSProductDirectory;
import org.esa.s1tbx.io.ceos.CEOSProductReader;
import org.esa.s1tbx.io.ceos.alos.AlosPalsarImageFile;
import org.esa.s1tbx.io.ceos.alos.AlosPalsarProductDirectory;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.framework.dataio.DecodeQualification;
import org.esa.snap.framework.dataio.ProductReaderPlugIn;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.ProductData;

import java.io.File;
import java.io.IOException;

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
    protected CEOSProductDirectory createProductDirectory(File inputFile) {
        return new Alos2ProductDirectory(inputFile.getParentFile());
    }

    DecodeQualification checkProductQualification(File file) {

        try {
            _dataDir = createProductDirectory(file);

            final Alos2ProductDirectory dataDir = (Alos2ProductDirectory) _dataDir;
            if (dataDir.isALOS2())
                return DecodeQualification.INTENDED;
            return DecodeQualification.UNABLE;

        } catch (Exception e) {
            return DecodeQualification.UNABLE;
        }
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
            final Alos2ProductDirectory dataDir = (Alos2ProductDirectory) _dataDir;
            final AlosPalsarImageFile imageFile = (AlosPalsarImageFile) dataDir.getImageFile(destBand);
            if (dataDir.isSLC()) {
                boolean oneOf2 = destBand.getUnit().equals(Unit.REAL) || !destBand.getName().startsWith("q");

                if (dataDir.getProductLevel() == Alos2Constants.LEVEL1_0) {
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

        } catch (Exception e) {
            handleReaderException(e);
        }
    }
}
