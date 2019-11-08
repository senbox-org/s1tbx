/*
 * Copyright (C) 2019 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.csa.rstb.soilmoisture.io.matlab;

import com.bc.ceres.core.ProgressMonitor;
import com.jmatio.io.MatFileHeader;
import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The product reader for Matlab data.
 */
public class MatlabReader extends AbstractProductReader {

    private Map<String, MLArray> content = null;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public MatlabReader(final ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code> interface method. Clients implementing this
     * method can be sure that the input object and eventually the subset information has already been set.
     * <p>
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {

        final Path inputPath = ReaderUtils.getPathFromInput(getInput());

        final MatFileReader mfr = new MatFileReader(inputPath.toFile());
        final MatFileHeader hdr = mfr.getMatFileHeader();

        content = mfr.getContent();

        int maxX = 0, maxY = 0;
        final List<Band> bandList = new ArrayList<>(2);
        final Set<String> keyset = content.keySet();
        for (String key : keyset) {
            final MLArray array = content.get(key);
            final String name = array.getName();
            final int[] dim = array.getDimensions();

            if (dim[0] > maxY)
                maxY = dim[0];
            if (dim.length > 1 && dim[1] > maxX)
                maxX = dim[1];

            final Band band = new Band(name, ProductData.TYPE_FLOAT64, dim.length > 1 ? dim[1] : 0, dim[0]);
            bandList.add(band);
        }

        final Product product = new Product(inputPath.getFileName().toString(), "Matlab", maxX, maxY);

        for (Band band : bandList) {
            product.addBand(band);
        }

        product.setProductReader(this);
        product.setModified(false);
        product.setFileLocation(inputPath.toFile());

        return product;
    }

    @Override
    public void close() throws IOException {
        super.close();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected synchronized void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                                       int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                                       int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                                       ProgressMonitor pm) throws IOException {
        final MLArray array = content.get(destBand.getName());
        if (array == null)
            return;

        final int type = array.getType();

        if (type == MLArray.mxDOUBLE_CLASS) {
            MLDouble dblArray = (MLDouble) array;

            for (int i = 0, x = sourceOffsetX; x < sourceOffsetX + sourceWidth; ++x, ++i) {
                for (int j = 0, y = sourceOffsetY; y < sourceOffsetY + sourceHeight; ++y, ++j) {
                    int index = j * destWidth + i;
                    destBuffer.setElemDoubleAt(index, dblArray.get(y, x));
                }
            }
        } else {
            throw new IOException("Matlab reader: Type " + MLArray.typeToString(type) + " not supported");
        }
    }
}