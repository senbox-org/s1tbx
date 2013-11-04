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

package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.gpf.ui.SourceUI;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Reads the specified files as products. This operator may serve as a source node in processing graphs,
 * especially if multiple data products need to be read in.
 * <p/>
 * Here is a sample of how the <code>Read</code> operator can be integrated as a node within a processing graph:
 * <pre>
 *    &lt;node id="readNode"&gt;
 *        &lt;operator&gt;Time-Series-Reader&lt;/operator&gt;
 *        &lt;parameters&gt;
 *            &lt;fileList&gt;/eodata/SST.nc&lt;/fileList&gt;
 *        &lt;/parameters&gt;
 *    &lt;/node&gt;
 * </pre>
 *
 */
@OperatorMetadata(alias = "Time-Series-Reader",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
        description = "Reads a list of products from disk and produces a stack.")
public class TimeSeriesReaderOp extends Operator {

    private ProductReader beamReader;

    @Parameter(description = "The file from which the data product is read.", notNull = true, notEmpty = true)
    private File file1;
    @Parameter(description = "The file from which the data product is read.", notNull = true, notEmpty = true)
    private File file2;
    @TargetProduct
    private Product targetProduct;

    private File file = file1;

    @Override
    public void initialize() throws OperatorException {
        try {
            targetProduct = ProductIO.readProduct(file);
            if (targetProduct == null) {
                throw new OperatorException("No product reader found for file " + file);
            }
            targetProduct.setFileLocation(file);
            beamReader = targetProduct.getProductReader();
        } catch (IOException e) {
            throw new OperatorException(e);
        }
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        ProductData dataBuffer = targetTile.getRawSamples();
        Rectangle rectangle = targetTile.getRectangle();
        try {
            beamReader.readBandRasterData(band, rectangle.x, rectangle.y, rectangle.width,
                                          rectangle.height, dataBuffer, pm);
            targetTile.setRawSamples(dataBuffer);
        } catch (IOException e) {
            throw new OperatorException(e);
        }
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(TimeSeriesReaderOp.class);
            setOperatorUI(SourceUI.class);
        }
    }
}
