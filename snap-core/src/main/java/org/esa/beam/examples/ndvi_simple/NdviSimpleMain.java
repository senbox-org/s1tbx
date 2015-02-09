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
package org.esa.beam.examples.ndvi_simple;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;

import javax.imageio.stream.FileImageOutputStream;
import java.io.File;
import java.io.IOException;

import com.bc.ceres.core.PrintWriterProgressMonitor;

/**
 * This is an example program which writes out an NDVI value in the range 0 to 255 computed from the MERIS L1b bands
 * "radiance_6" and "radiance_10".
 * <p/>
 * <p>The program expects two input arguments: <ol> <li><i>input-file</i> - the file path to an input data product
 * containing the bands "radiance_6" and "radiance_10". The format can be either ENVISAT or BEAM-DIMAP</li>
 * <li><i>output-file</i> - the file path to the NDVI image file to be written</li> </ol>
 * <p/>
 * <i><b>Note:</b> If you want to work with product subsets, you can use the {@link
 * org.esa.beam.framework.dataio.ProductSubsetBuilder} class. It has a static method which lets you create a subset of a
 * given product and subset definition.</i>
 *
 * @see org.esa.beam.framework.dataio.ProductIO
 * @see org.esa.beam.framework.dataio.ProductSubsetBuilder
 * @see org.esa.beam.framework.dataio.ProductSubsetDef
 * @see org.esa.beam.framework.datamodel.Product
 * @see org.esa.beam.framework.datamodel.Band
 * @see org.esa.beam.framework.datamodel.TiePointGrid
 */
public class NdviSimpleMain {

    /**
     * The main method. Fetches the input arguments and delegates the call to the <code>run</code> method.
     *
     * @param args the program arguments
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("parameter usage: <input-file> <output-file>");
            return;
        }
        // Get arguments
        String inputPath = args[0];
        String outputPath = args[1];
        try {
            // Pass arguments to actual program code
            run(inputPath, outputPath);
        } catch (IOException e) {
            System.out.println("error: " + e.getMessage());
        }
    }

    /**
     * Runs this program with the given parameters.
     */
    private static void run(String inputPath, String outputPath)
            throws IOException {

        // Read the product (note that only 'nodes' are read, not the entire data!)
        Product product = ProductIO.readProduct(inputPath);
        // Get the scene width
        int w = product.getSceneRasterWidth();
        // Get the scene height
        int h = product.getSceneRasterHeight();

        // Get the "low" band
        Band lowBand = product.getBand("radiance_6");
        if (lowBand == null) {
            throw new IOException("low-band 'radiance_6' not found");
        }

        // Get the "high" band
        Band hiBand = product.getBand("radiance_10");
        if (hiBand == null) {
            throw new IOException("hi-band 'radiance_10' not found");
        }

        // Print out, what we are going to do...
        System.out.println("writing NDVI raw image file "
                           + outputPath
                           + " containing " + w + " x " + h
                           + " pixels of type byte (value range 0-255)...");

        // Create an output stream for the NDVI raw data
        FileImageOutputStream outputStream = new FileImageOutputStream(new File(outputPath));

        // Create a buffer for reading a single scan line of the low-band
        float[] lowBandPixels = new float[w];
        // Create a buffer for reading a single scan line of the hi-band
        float[] hiBandPixels = new float[w];

        // Hi/Low-band sum and difference of the NDVI quotient
        float sum, dif;
        // NDVI value
        float ndvi;
        // NDVI value in the range 0 to 255
        int ndviByte;

        // For all scan lines in the product...
        for (int y = 0; y < h; y++) {
            // Read low-band pixels for line y
            lowBand.readPixels(0, y, w, 1, lowBandPixels, new PrintWriterProgressMonitor(System.out));
            // Read hi-band pixels for line y
            hiBand.readPixels(0, y, w, 1, hiBandPixels, new PrintWriterProgressMonitor(System.out));

            // Compute NDVI for all x
            for (int x = 0; x < w; x++) {
                dif = lowBandPixels[x] - hiBandPixels[x];
                sum = lowBandPixels[x] + hiBandPixels[x];

                if (sum != 0.0F) {
                    ndvi = dif / sum;
                } else {
                    ndvi = 0.0F;
                }

                if (ndvi < 0.0F) {
                    ndvi = 0.0F;
                } else if (ndvi > 1.0F) {
                    ndvi = 1.0F;
                }

                // Convert NDVI to integer in the range 0 to 255
                ndviByte = (int) (255 * ndvi);

                // write NDVI byte to raw image file
                outputStream.writeByte(ndviByte);
            }
        }

        // close raw image file
        outputStream.close();

        // Done!
        System.out.println("OK");
    }
}
