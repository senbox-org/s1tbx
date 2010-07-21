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
package org.esa.beam.examples.data_export;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.jexp.ParseException;
import com.bc.jexp.Term;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This is an example program which writes out a bit-mask image of an ENVISAT data product. The image is written as a
 * raw byte stream and contains as much bytes as a the number of pixels a scene of the product has.
 * <p/>
 * <p>The program expects three input arguments: <ol> <li><i>input-file</i> - the input file path to an ENVISAT data
 * product</li> <li><i>output-file</i> - the file path to the bit-mask image file to be written<</li>
 * <li><i>bit-mask-expr</i> - the bit-mask expression</li> </ol>
 * <p/>
 * <p><i>bit-mask-expr</i> is a boolean expression. The logical operators you can use are "AND", "OR" and "NOT" (or the
 * characters "&", "|" and "!" respectively). You can also enclose sub-expressions in parentheses "(" and ")" to change
 * the evaluation priority.
 * <p/>
 * <p>A reference to a flag value in a data product comprises the flag dataset name followed by a dot "." and followed
 * by the flag name you are interested in. For example, "l1_flags.INVALID" references the INVALID-flag in the dataset
 * "l1_flags".
 * <p/>
 * <p>The following are examples for valid bit-mask expression strings for MERIS L1b products: <ld> <li><code>"NOT
 * l1_flags.INVALID AND NOT l1_flags.BRIGHT"</code></li> <li><code>"(l1_flags.COASTLINE OR l1_flags.LAND_OCEAN) AND NOT
 * l1_flags.GLINT_RISK"</code></li> <li><code>"!(l1_flags.BRIGHT | l1_flags.GLINT_RISK | l1_flags.INVALID |
 * l1_flags.SUSPECT)"</code></li> </ld></p>
 * <p/>
 * <p>The following are examples for valid bit-mask expression strings for AATSR TOA L1b products: <ld> <li><code>"NOT
 * confid_flags_nadir.SATURATION AND NOT confid_flags_nadir.OUT_OF_RANGE"</code> <li><code>"confid_flags_nadir.SATURATION
 * OR confid_flags_fward.SATURATION"</code> <li><code>"cloud_flags_nadir.LAND OR cloud_flags_fward.SUN_GLINT"</code>
 * <li><code>"cloud_flags_nadir.CLOUDY and not confid_flags_nadir.NO_SIGNAL"</code> <li><code>"cloud_flags_fward.CLOUDY
 * and not confid_flags_fward.NO_SIGNAL"</code> </ld></p>
 * <p/>
 * <p>You find all possible flag datasets in the <code><b>beam.jar</b>!/org/esa/beam/resources/dddb/bands/<i>product-type</i>.dd</code>
 * file, where <i>product-type</i> is an ENVISAT product type name, such as "MER_RR__1P" for a MERIS L1b reduced
 * resolution product. The corresponding flags you can find in a dataset are stored under
 * <code><b>beam.jar</b>!/org/esa/beam/resources/dddb/bands/<i>product-type</i>_<i>dataset_type</i><.dd</code>.
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
 * @see BitmaskWriterMain#main
 */
public class BitmaskWriterMain {

    /**
     * The main method. Fetches the input arguments and delgates the call to the <code>run</code> method.
     *
     * @param args the program arguments
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("parameter usage: <input-file> <output-file> <bit-mask-expr>");/*I18N*/
            return;
        }
        // Get arguments
        String inputPath = args[0];
        String outputPath = args[1];
        String bitmaskExpr = args[2];
        try {
            // Pass arguments to actual program code
            run(inputPath, outputPath, bitmaskExpr);
        } catch (IOException e) {
            System.out.println("I/O error: " + e.getMessage());
        } catch (ParseException e) {
            System.out.println("bit-mask syntax error: " + e.getMessage());
        }
    }

    /**
     * Runs this program with the given parameters.
     */
    private static void run(String inputPath, String outputPath, String bitmaskExpr)
            throws IOException,
                   ParseException {

        // Read the product (note that only 'nodes' are read, not the entire data!)
        Product product = ProductIO.readProduct(inputPath);

        // Parse the given bit-mask expression string to a term which can efficiently
        // be evaluated by the framework
        Term bitmaskTerm = product.parseExpression(bitmaskExpr);

        // Get the scene width
        int w = product.getSceneRasterWidth();
        // Get the scene height
        int h = product.getSceneRasterHeight();

        // Print out, what we are going to do...
        System.out.println("writing bit-mask image file "
                           + outputPath
                           + " containing " + w + " x " + h + " pixels of type byte...");

        // Open output stream for our bit-mask image
        FileOutputStream outputStream = new FileOutputStream(outputPath);

        // Create the bit-mask buffer for a single scan line
        byte[] bitmaskScanLine = new byte[w];

        // For all scan lines in the product...
        for (int y = 0; y < h; y++) {
            // Read the bit-mask scan line at y
            product.readBitmask(0, y, // x (=0) & y offsets
                                w, 1, // width (=w) & height (=1)
                                bitmaskTerm, // the parsed bit-mask expression
                                bitmaskScanLine, // the bit-mask scan-line buffer
                                (byte) 255, // the value for bitmaskScanLine(x,y)=TRUE
                                (byte) 0, // the value for bitmaskScanLine(x,y)=FALSE
                                ProgressMonitor.NULL);

            // write bit-mask scan line to image file
            outputStream.write(bitmaskScanLine);
        }

        // close bit-mask image file
        outputStream.close();

        // Done!
        System.out.println("OK");
    }
}
