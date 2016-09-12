/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.kompsat5;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import java.io.File;
import java.io.IOException;

/**
 * Created by luis on 12/08/2016.
 */
public class K5GeoTiff implements K5Format {

    private final ProductReaderPlugIn readerPlugIn;
    private final Kompsat5Reader reader;
    private Product product;
    private final ProductReader geoTiffReader;

    public K5GeoTiff(final ProductReaderPlugIn readerPlugIn, final Kompsat5Reader reader) {
        this.readerPlugIn = readerPlugIn;
        this.reader = reader;

        geoTiffReader = ProductIO.getProductReader("GeoTiff");
    }

    public Product open(final File inputFile) throws IOException {

        product = geoTiffReader.readProductNodes(inputFile, null);
        product.setFileLocation(inputFile);
        addAuxXML(product);
        addExtraBands(inputFile, product);

        return product;
    }

    private void addExtraBands(final File inputFile, final Product product) throws IOException {
        final String name = inputFile.getName().toLowerCase();

        if(name.contains("I_SCS") || name.contains("I_SCS")) {
            final File[] files = inputFile.getParentFile().listFiles();
            if(files != null) {
                for(File file : files) {
                    final String fname = inputFile.getName().toLowerCase();
                    if(fname.endsWith(".tif") && !fname.equals(name)) {
                        Product product2 = geoTiffReader.readProductNodes(file, null);
                        //todo
                    }
                }
            }
        }
    }

    public void close() throws IOException {
        if (product != null) {
            product = null;
        }
    }

    public void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                       int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                       int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                       ProgressMonitor pm) throws IOException {

        geoTiffReader.readBandRasterData(destBand, destOffsetX, destOffsetY, destWidth, destHeight, destBuffer, pm);
    }
}
