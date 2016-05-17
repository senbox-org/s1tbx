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

package org.esa.snap.watermask.util;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * @author Thomas Storm
 */
public class LandMaskRasterCreator {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            printUsage();
            System.exit(-1);
        }

        final LandMaskRasterCreator landMaskRasterCreator = new LandMaskRasterCreator(args[1]);
        final String sourcePath = args[0];
        landMaskRasterCreator.createRasterFile(sourcePath);
    }

    private static void printUsage() {
        System.out.println("Usage: ");
        System.out.println("    LandMaskRasterCreator $sourceFile $targetPath");
        System.out.println("    System will exit.");
    }

    private final String targetPath;

    public LandMaskRasterCreator(String targetPath) {
        this.targetPath = targetPath;
    }

    void createRasterFile(String sourcePath) throws IOException {
        validateSourcePath(sourcePath);
        final Product lwProduct = readLwProduct(sourcePath);
        final Band band = lwProduct.getBand("lw-mask");
        final MultiLevelImage sourceImage = band.getSourceImage();
        final int numXTiles = sourceImage.getNumXTiles();
        final int numYTiles = sourceImage.getNumYTiles();
        final int tileWidth = sourceImage.getTileWidth();
        final int tileHeight = sourceImage.getTileHeight();
        final BufferedImage image = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_BYTE_BINARY);
        int count = 0;
        for (int tileX = 0; tileX < numXTiles; tileX++) {
            for (int tileY = 0; tileY < numYTiles; tileY++) {
                count++;
                final Raster tile = sourceImage.getTile(tileX, tileY);
                final int minX = tile.getMinX();
                for (int x = minX; x < minX + tile.getWidth(); x++) {
                    final int minY = tile.getMinY();
                    for (int y = minY; y < minY + tile.getHeight(); y++) {
                        image.getRaster().setSample(x - minX, y - minY, 0, (byte) tile.getSample(x, y, 0));
                    }
                }
                image.setData(sourceImage.getTile(tileX, tileY));
                System.out.println("Writing image " + count + "/" + numXTiles * numYTiles + ".");
                ImageIO.write(image, "png", new File(targetPath, String.format("%d-%d.png", tileX, tileY)));
            }
        }
    }

    private Product readLwProduct(String sourcePath) {
        final Product lwProduct;
        try {
            lwProduct = ProductIO.readProduct(sourcePath);
        } catch (IOException e) {
            throw new IllegalArgumentException(MessageFormat.format("Unable to read from file ''{0}''.", sourcePath), e);
        }
        return lwProduct;
    }

    void validateSourcePath(String sourcePath) {
        final File path = new File(sourcePath);
        if (path.isDirectory()) {
            throw new IllegalArgumentException(MessageFormat.format("Source path ''''{0}'' points to a directory, but " +
                                                                    "must point to a file.", sourcePath));
        }
        if (!path.exists()) {
            throw new IllegalArgumentException(MessageFormat.format("Source path ''{0}'' does not exist.", sourcePath));
        }
    }
}
