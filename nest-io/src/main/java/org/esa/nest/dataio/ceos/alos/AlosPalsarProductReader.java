/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.ceos.alos;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.nest.dataio.ceos.CEOSProductDirectory;
import org.esa.nest.dataio.ceos.CEOSProductReader;
import org.esa.nest.datamodel.Unit;

import java.io.File;
import java.io.IOException;

/**
 * The product reader for AlosPalsar products.
 *
 */
public class AlosPalsarProductReader extends CEOSProductReader {

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public AlosPalsarProductReader(final ProductReaderPlugIn readerPlugIn) {
       super(readerPlugIn);
    }

    @Override
    protected CEOSProductDirectory createProductDirectory(File inputFile) {
        return new AlosPalsarProductDirectory(inputFile.getParentFile());
    }

    DecodeQualification checkProductQualification(File file) {

        try {
            _dataDir = createProductDirectory(file);

            final AlosPalsarProductDirectory dataDir = (AlosPalsarProductDirectory)_dataDir;
            if(dataDir.isALOS())
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
            final AlosPalsarProductDirectory dataDir = (AlosPalsarProductDirectory) _dataDir;
            final AlosPalsarImageFile imageFile = (AlosPalsarImageFile)dataDir.getImageFile(destBand);
            if(dataDir.isSLC()) {
                boolean oneOf2 = destBand.getUnit().equals(Unit.REAL) || !destBand.getName().startsWith("q");

                if(dataDir.getProductLevel() == AlosPalsarConstants.LEVEL1_0) {
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
            final IOException ioException = new IOException(e.getMessage());
            ioException.initCause(e);
            throw ioException;
        }

    }

}