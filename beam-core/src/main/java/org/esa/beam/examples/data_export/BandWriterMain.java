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
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.FileImageOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * This is an example program which writes out an entire band of an ENVISAT data product. The image is written as a raw
 * data file - pixels are stored in the band's data type, which can be <code>byte</code>, <code>float</code> or others.
 * A message line is printed to the console containing some information about the image file beeing written.
 * <p/>
 * <p>The program expects three input arguments: <ol> <li><i>input-file</i> - the file path to an ENVISAT input data
 * product</li> <li><i>output-file</i> - the file path to the band image file to be written</li> <li><i>band-name</i> -
 * the name of the band to be extracted</li> </ol>
 * <p/>
 * <p>The valid band names for MERIS L1b reduced and full resolution products are:<ld>
 * <li><code>"radiance_</code><i>index</i>" type float in mW/(m^2*sr*nm) - TOA radiance, <i>index</i> ranges from
 * <code>1</code> to <code>15</code></li> <li><code>"l1_flags"        </code> type byte (8 flag bits) - quality
 * flags</li> <li><code>"ssi"             </code> type byte - spectral shift index</li> </ld></p>
 * <p/>
 * <p>The valid band names for MERIS L2 reduced and full resolution products are:<ld>
 * <li><code>"reflec_</code><i>index</i>" type float in 1 - Normalized surface reflectance, <i>index</i> = 1 to 13</li>
 * <li><code>"water_vapour"   </code> type float in g/cm^2 - Water vapour content               </li>
 * <li><code>"algal_1"        </code> type float in mg/m^3 - Chlorophyll 1 content              </li>
 * <li><code>"algal_2"        </code> type float in mg/m^3 - Chlorophyll 2 content              </li>
 * <li><code>"yellow_subs"    </code> type float in 1/m    - Yellow substance                   </li>
 * <li><code>"total_susp"     </code> type float in mg/m^3 - Total suspended matter             </li>
 * <li><code>"photosyn_rad"   </code> type float in myEinstein/m^2 - Photosynthetically active radiat
 * <li><code>"toa_veg"        </code> type float in 1  - TOA vegetation index                   </li>
 * <li><code>"boa_veg"        </code> type float in 1  - BOA vegetation index                   </li>
 * <li><code>"rect_refl"      </code> type float in dl - Rectified surface reflectances         </li>
 * <li><code>"surf_press"     </code> type float in hPa - Surface pressure                      </li>
 * <li><code>"aero_epsilon"   </code> type float in dl - Aerosol epsilon                        </li>
 * <li><code>"aero_opt_thick_443" </code> type float in dl - Aerosol optical thickness at 443 nm    </li>
 * <li><code>"aero_opt_thick_550" </code> type float in dl - Aerosol optical thickness at 550 nm    </li>
 * <li><code>"aero_opt_thick_865" </code> type float in dl - Aerosol optical thickness at 865 nm    </li>
 * <li><code>"cloud_albedo"   </code> type float in dl  - Cloud albedo                          </li>
 * <li><code>"cloud_opt_thick"</code> type float in dl - Cloud optical thickness                </li>
 * <li><code>"cloud_top_press"</code> type float in hPa  - Cloud top pressure                   </li>
 * <li><code>"cloud_type"     </code> type byte - Cloud type                                    </li>
 * <li><code>"l2_flags"       </code> type int (24 flag bits) - Classification and quality flags</li> </ld></p>
 * <p/>
 * <p>The valid band names for AATSR TOA L1b products are:<ld> <li><code>"btemp_nadir_1200"</code> type float in K -
 * Brightness temperature, nadir view (11500-12500 nm)</li> <li><code>"btemp_nadir_1100"</code> type float in K -
 * Brightness temperature, nadir view (10400-11300 nm)</li> <li><code>"btemp_nadir_0370"</code> type float in K -
 * Brightness temperature, nadir view (3505-3895 nm)</li> <li><code>"reflec_nadir_1600"</code> type float in % -
 * Refectance, nadir view (1580-1640 nm)</li> <li><code>"reflec_nadir_0870"</code> type float in % - Refectance, nadir
 * view (855-875 nm)</li> <li><code>"reflec_nadir_0670"</code> type float in % - Refectance, nadir view (649-669
 * nm)</li> <li><code>"reflec_nadir_0550"</code> type float in % - Refectance, nadir view (545-565 nm)</li>
 * <li><code>"btemp_fward_1200"</code> type float in K - Brightness temperature, forward view (11500-12500 nm)</li>
 * <li><code>"btemp_fward_1100"</code> type float in K - Brightness temperature, forward view (10400-11300 nm)</li>
 * <li><code>"btemp_fward_0370"</code> type float in K - Brightness temperature, forward view (3505-3895 nm)</li>
 * <li><code>"reflec_fward_1600"</code> type float in % - Refectance, forward view (1580-1640 nm)</li>
 * <li><code>"reflec_fward_0870"</code> type float in % - Refectance, forward view (855-875 nm)</li>
 * <li><code>"reflec_fward_0670"</code> type float in % - Refectance, forward view (649-669 nm)</li>
 * <li><code>"reflec_fward_0550"</code> type float in % - Refectance, forward view (545-565 nm)</li>
 * <li><code>"confid_flags_nadir"</code> type int (16 flag bits) - Confidence flags, nadir view</li>
 * <li><code>"confid_flags_fward"</code> type int (16 flag bits) - Confidence flags, forward view</li>
 * <li><code>"cloud_flags_nadir"</code> type int (16 flag bits) - Cloud flags, nadir view</li>
 * <li><code>"cloud_flags_fward"</code> type int (16 flag bits) - Cloud flags, forward view</li> </ld></p>
 * <p/>
 * <p>The valid band names for AATSR NR L2 products are:<ld> <li><code>"sst_nadir"       </code> type float in K - Sea
 * surface temperature nadir view      </li> <li><code>"sst_comb"        </code> type float in K - Sea surface
 * temperature combined views  </li> <li><code>"cloud_top_temp"  </code> type float in K - Cloud top temperature
 * </li> <li><code>"cloud_top_height"</code> type float in m - Cloud top height                        </li>
 * <li><code>"lst"             </code> type float in K - Land surface temperature                </li> <li><code>"ndvi"
 * </code> type float in * - Normalized difference vegetation index  </li> <li><code>"flags"           </code>
 * type int (16 flag bits) - Classification and quality flags</li> </ld></p>
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
public class BandWriterMain {

    /**
     * The main method. Fetches the input arguments and delgates the call to the <code>run</code> method.
     *
     * @param args the program arguments
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("parameter usage: <input-file> <output-file> <band-name>"); /*I18N*/
            return;
        }
        // Get arguments
        String inputPath = args[0];
        String outputPath = args[1];
        String bandName = args[2];
        try {
            // Pass arguments to actual program code
            run(inputPath, outputPath, bandName);
        } catch (IOException e) {
            System.out.println("error: " + e.getMessage());
        }
    }

    /**
     * Runs this program with the given parameters.
     */
    private static void run(String inputPath, String outputPath, String bandName)
            throws IOException {

        // Read the product (note that only 'nodes' are read, not the entire data!)
        Product product = ProductIO.readProduct(inputPath);
        // Get the scene width
        int w = product.getSceneRasterWidth();
        // Get the scene height
        int h = product.getSceneRasterHeight();

        // Get the band with the given name
        Band band = product.getBand(bandName);
        if (band == null) {
            throw new IOException("band not found: " + bandName);
        }

        // Print out, what we are going to do...
        System.out.println("writing band raw image file "
                           + outputPath
                           + " containing " + w + " x " + h + " pixels of type "
                           + ProductData.getTypeString(band.getDataType()) + "...");

        // Create an output stream for the band's raw data
        FileImageOutputStream outputStream = new FileImageOutputStream(new File(outputPath));

        // Create a buffer for reading a single scan line of the band
        ProductData bandScanLine = band.createCompatibleRasterData(w, 1);

        // For all scan lines in the product...
        for (int y = 0; y < h; y++) {
            // Read the scan line at y
            band.readRasterData(0, y,
                                w, 1,
                                bandScanLine,
                                ProgressMonitor.NULL);

            // write band scan line to raw image file
            bandScanLine.writeTo(outputStream);
        }

        // close raw image file
        outputStream.close();

        // Done!
        System.out.println("OK");
    }
}
