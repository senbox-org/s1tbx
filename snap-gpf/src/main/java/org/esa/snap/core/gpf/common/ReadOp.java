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

package org.esa.snap.core.gpf.common;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;

/**
 * Reads the specified file as product. This operator may serve as a source node in processing graphs,
 * especially if multiple data products need to be read in.
 * <p>
 * Here is a sample of how the <code>Read</code> operator can be integrated as a node within a processing graph:
 * <pre>
 *    &lt;node id="readNode"&gt;
 *        &lt;operator&gt;Read&lt;/operator&gt;
 *        &lt;parameters&gt;
 *            &lt;file&gt;/eodata/SST.nc&lt;/file&gt;
 *            &lt;formatName&gt;GeoTIFF&lt;/formatName&gt;
 *        &lt;/parameters&gt;
 *    &lt;/node&gt;
 * </pre>
 *
 * @author Norman Fomferra
 * @author Marco Zuehlke
 * @since BEAM 4.2
 */
@OperatorMetadata(alias = "Read",
        category = "Input-Output",
        version = "1.2",
        authors = "Marco Zuehlke, Norman Fomferra",
        copyright = "(c) 2010 by Brockmann Consult",
        description = "Reads a data product from a given file location.")
public class ReadOp extends Operator {

    @Parameter(description = "The file from which the data product is read.", notNull = true, notEmpty = true)
    private File file;

    @Parameter(description = "An (optional) format name.", notNull = false, notEmpty = true)
    private String formatName;

    @TargetProduct
    private Product targetProduct;

    @Override
    public void initialize() throws OperatorException {
        if (file == null) {
            throw new OperatorException("The 'file' parameter is not set");
        }
        if (!file.exists()) {
            throw new OperatorException(String.format("Specified 'file' [%s] does not exist.", file));
        }

        try {
            Product openedProduct = getOpenedProduct();
            if (openedProduct != null) {
                //targetProduct = openedProduct;    // won't work. Product must be copied and use copySourceImage

                targetProduct = new Product(openedProduct.getName(), openedProduct.getProductType(),
                                            openedProduct.getSceneRasterWidth(), openedProduct.getSceneRasterHeight());
                for (Band srcband : openedProduct.getBands()) {
                    if (targetProduct.getBand(srcband.getName()) != null) {
                        continue;
                    }
                    if (srcband instanceof VirtualBand) {
                        ProductUtils.copyVirtualBand(targetProduct, (VirtualBand) srcband, srcband.getName());
                    } else {
                        ProductUtils.copyBand(srcband.getName(), openedProduct, targetProduct, true);
                    }
                }
                ProductUtils.copyProductNodes(openedProduct, targetProduct);

            }else {
                ProductReader productReader;
                if (formatName != null && !formatName.trim().isEmpty()) {
                    productReader = ProductIO.getProductReader(formatName);
                    if (productReader == null) {
                        throw new OperatorException("No product reader found for format '" + this.formatName + "'");
                    }
                } else {
                    productReader = ProductIO.getProductReaderForInput(file);
                    if (productReader == null) {
                        throw new OperatorException("No product reader found for file " + file);
                    }
                }
                targetProduct = productReader.readProductNodes(file, null);
                targetProduct.setFileLocation(file);
            }
        } catch (IOException e) {
            throw new OperatorException(e);
        }
    }

    private Product getOpenedProduct() {
        final Product[] openedProducts = getProductManager().getProducts();
        for (Product openedProduct : openedProducts) {
            if (file.equals(openedProduct.getFileLocation())) {
                return openedProduct;
            }
        }
        return null;
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        ProductData dataBuffer = targetTile.getRawSamples();
        Rectangle rectangle = targetTile.getRectangle();
        try {
            targetProduct.getProductReader().readBandRasterData(band, rectangle.x, rectangle.y, rectangle.width,
                                                                rectangle.height, dataBuffer, pm);
            targetTile.setRawSamples(dataBuffer);
        } catch (IOException e) {
            throw new OperatorException(e);
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ReadOp.class);
        }
    }
}
