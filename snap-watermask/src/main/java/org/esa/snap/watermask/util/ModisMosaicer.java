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

package org.esa.snap.watermask.util;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.image.ImageHeader;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Properties;

/**
 * @author Thomas Storm
 */
public class ModisMosaicer {

    static final int MODIS_IMAGE_WIDTH = 155520;
    static final int MODIS_IMAGE_HEIGHT = 12960;
    private static final int MODIS_TILE_WIDTH = 576;
    private static final int MODIS_TILE_HEIGHT = 480;

    static final int NORTH_MODE = 0;

    public static void main(String[] args) throws IOException {
        final String pathname;
        int mode = Integer.parseInt(args[0]);
        if (mode == NORTH_MODE) {
            pathname ="C:\\dev\\projects\\beam-watermask\\MODIS\\north";
        } else {
            pathname = "C:\\dev\\projects\\beam-watermask\\MODIS\\south";
        }
        final File[] files = new File(pathname).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".dim");
            }
        });

        Product[] products = new Product[files.length];
        for (int i = 0, filesLength = files.length; i < filesLength; i++) {
            final Product product = ProductIO.readProduct(files[i]);
            products[i] = product;
        }

        int width = MODIS_IMAGE_WIDTH;
        int height = MODIS_IMAGE_HEIGHT;
        final Properties properties = new Properties();
        properties.setProperty("width", String.valueOf(width));
        properties.setProperty("dataType", "0");
        properties.setProperty("height", String.valueOf(height));
        properties.setProperty("tileWidth", String.valueOf(MODIS_TILE_WIDTH));
        properties.setProperty("tileHeight", String.valueOf(MODIS_TILE_HEIGHT));
        final ImageHeader imageHeader = ImageHeader.load(properties, null);
        final TemporaryMODISImage temporaryMODISImage = new TemporaryMODISImage(imageHeader, products, mode);
        final Product product = new Product("MODIS_lw", "lw", MODIS_IMAGE_WIDTH, MODIS_IMAGE_HEIGHT);
        final Band band = product.addBand("lw-mask", ProductData.TYPE_UINT8);
        band.setSourceImage(temporaryMODISImage);
        final String filePath;
        if(mode == NORTH_MODE) {
            filePath = "C:\\temp\\modis_north.dim";
        } else {
            filePath = "C:\\temp\\modis_south.dim";
        }
        ProductIO.writeProduct(product, filePath, "BEAM-DIMAP");
    }

}
