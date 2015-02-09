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
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGrid;

import javax.imageio.stream.FileImageOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * This is an example program which writes out a tie-point grid of an ENVISAT data product. The tie-point grid data is
 * automatically interpolated to product scene pixels which are written as a raw data file - interpolated tie-point
 * pixels are always stored as 4-byte IEEE float. A message line is printed to the console containing some information
 * about the image file beeing written.
 * <p/>
 * <p>The program expects three input arguments: <ol> <li><i>input-file</i> - the file path to an ENVISAT input data
 * product</li> <li><i>output-file</i> - the file path to the band image file to be written</li> <li><i>grid-name</i> -
 * the name of the tie-point grid to be extracted</li> </ol>
 * <p/>
 * <p>The valid grid names for MERIS L1b and L2 reduced and full resolution products are:<ld> <li><code>"latitude"
 * </code> type float in deg - Latitude of the tie points (WGS-84), positive N, negative W</li> <li><code>"longitude"
 * </code> type float in deg - Longitude of the tie points (WGS-84), Greenwich</li> <li><code>"dem_alt"        </code>
 * type float in m   - Digital elevation model altitude               </li> <li><code>"dem_rough"      </code> type
 * float in m   - Digital elevation model roughness              </li> <li><code>"lat_corr"       </code> type float in
 * deg - Digital elevation model latitude corrections   </li> <li><code>"lon_corr"       </code> type float in deg -
 * Digital elevation model longitude corrections  </li> <li><code>"sun_zenith"     </code> type float in deg - Sun
 * zenith angle                               </li> <li><code>"sun_azimuth"    </code> type float in deg - Sun azimuth
 * angles                             </li> <li><code>"view_zenith"    </code> type float in deg - Viewing zenith angles
 * </li> <li><code>"view_azimuth"   </code> type float in deg - Viewing azimuth angles
 * </li> <li><code>"zonal_wind"     </code> type float in m/s - Zonal wind
 * </li> <li><code>"merid_wind"     </code> type float in m/s - Meridional wind
 * </li> <li><code>"atm_press"      </code> type float in hPa - Mean sea level pressure                        </li>
 * <li><code>"ozone"          </code> type float in DU  - Total ozone                                    </li>
 * <li><code>"rel_hum"        </code> type float in %   - Relative humidity                              </li>
 * </ld></p>
 * <p/>
 * <p>The valid grid names for AATSR TOA L1b products are:<ld> <li><code>"latitude"          </code> type float in deg -
 * Latitudes                           </li> <li><code>"longitude"         </code> type float in deg - Longitudes
 * </li> <li><code>"lat_corr_nadir"    </code> type float in deg - Latitude corrections, nadir view
 * </li> <li><code>"lon_corr_nadir"    </code> type float in deg - Longitude corrections, nadir view   </li>
 * <li><code>"lat_corr_fward"    </code> type float in deg - Latitude corrections, forward view  </li>
 * <li><code>"lon_corr_fward"    </code> type float in deg - Longitude corrections, forward view </li>
 * <li><code>"altitude"          </code> type float in m   - Topographic altitude                </li>
 * <li><code>"sun_elev_nadir"    </code> type float in deg - Solar elevation, nadir view         </li>
 * <li><code>"view_elev_nadir"   </code> type float in deg - Satellite elevation, nadir view     </li>
 * <li><code>"sun_azimuth_nadir" </code> type float in deg - Solar azimuth, nadir view           </li>
 * <li><code>"view_azimuth_nadir"</code> type float in deg - Satellite azimuth, nadir view       </li>
 * <li><code>"sun_elev_fward"    </code> type float in deg - Solar elevation, forward view       </li>
 * <li><code>"view_elev_fward"   </code> type float in deg - Satellite elevation, forward view   </li>
 * <li><code>"sun_azimuth_fward" </code> type float in deg - Solar azimuth, forward view         </li>
 * <li><code>"view_azimuth_fward"</code> type float in deg - Satellite azimuth, forward view     </li> </ld></p>
 * <p/>
 * <p>The valid grid names for AATSR NR L2 products are:<ld> <li><code>"latitude"          </code> type float in deg -
 * Latitudes                          </li> <li><code>"longitude"         </code> type float in deg - Longitudes
 * </li> <li><code>"lat_corr_nadir"    </code> type float in deg - Latitude corrections, nadir view
 * </li> <li><code>"lon_corr_nadir"    </code> type float in deg - Longitude corrections, nadir view  </li>
 * <li><code>"lat_corr_fward"    </code> type float in deg - Latitude corrections, forward view </li>
 * <li><code>"lon_corr_fward"    </code> type float in deg - Longitude corrections, forward view</li>
 * <li><code>"altitude"          </code> type float in m   - Topographic altitude               </li>
 * <li><code>"sun_elev_nadir"    </code> type float in deg - Solar elevation nadir view         </li>
 * <li><code>"view_elev_nadir"   </code> type float in deg - Satellite elevation nadir view     </li>
 * <li><code>"sun_azimuth_nadir" </code> type float in deg - Solar azimuth nadir view           </li>
 * <li><code>"view_azimuth_nadir"</code> type float in deg - Satellite azimuth nadir view       </li>
 * <li><code>"sun_elev_fward"    </code> type float in deg - Solar elevation forward view       </li>
 * <li><code>"view_elev_fward"   </code> type float in deg - Satellite elevation forward view   </li>
 * <li><code>"sun_azimuth_fward" </code> type float in deg - Solar azimuth forward view         </li>
 * <li><code>"view_azimuth_fward"</code> type float in deg - Satellite azimuth forward view     </li> </ld></p>
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
public class TiePointGridWriterMain {

    /**
     * The main method. Fetches the input arguments and delgates the call to the <code>run</code> method.
     *
     * @param args the program arguments
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("parameter usage: <input-file> <output-file> <grid-name>");
            return;
        }
        // Get arguments
        String inputPath = args[0];
        String outputPath = args[1];
        String gridName = args[2];
        try {
            // Pass arguments to actual program code
            run(inputPath, outputPath, gridName);
        } catch (IOException e) {
            System.out.println("error: " + e.getMessage());
        }
    }

    /**
     * Runs this program with the given parameters.
     */
    private static void run(String inputPath, String outputPath, String gridName)
            throws IOException {

        // Read the product (note that only 'nodes' are read, not the entire data!)
        Product product = ProductIO.readProduct(inputPath);
        // Get the scene width
        int w = product.getSceneRasterWidth();
        // Get the scene height
        int h = product.getSceneRasterHeight();

        // Get the tie-point grid with the given name
        TiePointGrid grid = product.getTiePointGrid(gridName);
        if (grid == null) {
            throw new IOException("tie-point grid not found: " + gridName);
        }

        // Print out, what we are going to do...
        System.out.println("writing tie-point grid raw image file "
                           + outputPath
                           + " containing " + w + " x " + h + " interpolated pixels of type "
                           + ProductData.getTypeString(grid.getDataType()) + "...");

        // Create an output stream for the grid's raw data
        FileImageOutputStream outputStream = new FileImageOutputStream(new File(outputPath));

        // Create a buffer for reading a single scan line of the grid
        float[] gridScanLine = new float[w];

        // For all scan lines in the product...
        for (int y = 0; y < h; y++) {
            // Read the scan line at y
            grid.readPixels(0, y, // x (=0) & y offsets
                            w, 1, // width (=w) & height (=1)
                            gridScanLine,
                            ProgressMonitor.NULL);

            // write grid scan line to raw image file
            outputStream.writeFloats(gridScanLine, 0, w);
        }

        // close raw image file
        outputStream.close();

        // Done!
        System.out.println("OK");
    }
}
