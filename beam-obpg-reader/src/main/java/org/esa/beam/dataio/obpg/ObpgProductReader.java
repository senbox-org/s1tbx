/*
 * $Id$
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
package org.esa.beam.dataio.obpg;

import com.bc.ceres.core.ProgressMonitor;
import ncsa.hdf.hdflib.HDFException;
import org.esa.beam.dataio.obpg.hdf.HdfAttribute;
import org.esa.beam.dataio.obpg.hdf.ObpgUtils;
import org.esa.beam.dataio.obpg.hdf.SdsInfo;
import org.esa.beam.dataio.obpg.bandreader.ObpgBandReader;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ObpgProductReader extends AbstractProductReader {

    private final Logger logger;
    ObpgUtils obpgUtils = new ObpgUtils();
    private int fileId;
    private int sdStart;
    private Map<Band,ObpgBandReader> readerMap;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    protected ObpgProductReader(ObpgProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        logger = BeamLogManager.getSystemLogger();
    }

    protected Product readProductNodesImpl() throws IOException {
        try {
            try {
                final File inFile = ObpgUtils.getInputFile(getInput());
                final String path = inFile.getPath();
                fileId = obpgUtils.openHdfFileReadOnly(path);
                sdStart = obpgUtils.openSdInterfaceReadOnly(path);

                final List<HdfAttribute> globalAttributes = obpgUtils.readGlobalAttributes(sdStart);
                final Product product = obpgUtils.createProductBody(globalAttributes);
                obpgUtils.addGlobalMetadata(product, globalAttributes);
                final SdsInfo[] sdsInfos = obpgUtils.extractSdsData(sdStart);
                obpgUtils.addScientificMetadata(product, sdsInfos);
                readerMap = obpgUtils.addBands(product, sdsInfos);
                obpgUtils.addGeocoding(product, sdsInfos);
                return product;
            } finally {
                obpgUtils.closeHdfFile(fileId);
            }
        } catch (HDFException e) {
            throw new ProductIOException(e.getMessage());
        }
    }

    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        final ObpgBandReader bandReader = readerMap.get(destBand);
        try {
            bandReader.readBandData(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight, sourceStepX, sourceStepY, destBuffer, pm);
        } catch (HDFException e) {
            final ProductIOException exception = new ProductIOException(e.getMessage());
            exception.setStackTrace(e.getStackTrace());
            throw exception;
        }
    }
}