/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
import com.google.common.io.LittleEndianDataInputStream;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.dataio.orbits.*;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.OrbitStateVector;
import org.esa.snap.datamodel.Orbits;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.ReaderUtils;
import org.esa.snap.gpf.TileIndex;

import java.awt.*;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

/**
 * Rat file reader.
 */

@OperatorMetadata(alias = "Read-Rat-File",
        category = "SAR Processing/SENTINEL-1",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Read RAT file")
public final class ReadRatFileOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The RAT file.", label = "Rat File")
    private File ratFile = null;

    @Parameter(description = "The product width", interval = "(0, *]", defaultValue = "0", label = "Width")
    private int width = 0;

    @Parameter(description = "The product height", interval = "(0, *]", defaultValue = "0", label = "Height")
    private int height = 0;

    private String ratFileName = null; // XCA file for radiometric calibration
    private String ratFilePath = null; // absolute path for XCA file

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public ReadRatFileOp() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            if (ratFile != null && ratFile.exists()) {
                ratFileName = ratFile.getName();
                ratFilePath = ratFile.getAbsolutePath();

            } else {
                throw new OperatorException("No external auxiliary file is specified.");
            }

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Create target product.
     */
    void createTargetProduct() {

        targetProduct = new Product(ratFileName, "RAT", width, height);

        final Band tgtBandI = new Band("i_band", ProductData.TYPE_FLOAT32, width, height);
        tgtBandI.setUnit("real");
        targetProduct.addBand(tgtBandI);

        final Band tgtBandQ = new Band("q_band", ProductData.TYPE_FLOAT32, width, height);
        tgtBandQ.setUnit("imaginary");
        targetProduct.addBand(tgtBandQ);

        ReaderUtils.createVirtualIntensityBand(targetProduct, tgtBandI, tgtBandQ, "_band");

        targetProduct.setPreferredTileSize(width, 100);
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTileMap   The target tiles associated with all target bands to be computed.
     * @param targetRectangle The rectangle of target tile.
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override
    public synchronized void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int xMax = x0 + w;
        final int yMax = y0 + h;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final Band tgtBandI = targetProduct.getBand("i_band");
        final Band tgtBandQ = targetProduct.getBand("q_band");
        final Tile tgtTileI = targetTileMap.get(tgtBandI);
        final Tile tgtTileQ = targetTileMap.get(tgtBandQ);
        final ProductData tgtBufferI = tgtTileI.getDataBuffer();
        final ProductData tgtBufferQ = tgtTileQ.getDataBuffer();
        final TileIndex tgtIndex = new TileIndex(tgtTileI);

        try {
            LittleEndianDataInputStream in = new LittleEndianDataInputStream(new BufferedInputStream(new FileInputStream(ratFilePath)));
            in.skipBytes(1000 + y0*width*4*2);


            for (int y = y0; y < yMax; y++) {
                tgtIndex.calculateStride(y);
                for (int x = x0; x < xMax; x++) {
                    final int tgtIdx = tgtIndex.getIndex(x);
                    final float vI = in.readFloat();
                    final float vQ = in.readFloat();
                    tgtBufferI.setElemDoubleAt(tgtIdx, vI);
                    tgtBufferQ.setElemDoubleAt(tgtIdx, vQ);
                }
            }

            in.close();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("computeTileStack", e);
        }
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(ReadRatFileOp.class);
        }
    }
}