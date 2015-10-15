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
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;

import javax.media.jai.JAI;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@OperatorMetadata(alias = "WriteRGB",
                  category = "Input-Output",
                  description = "Creates an RGB image from three source bands.",
                  internal = true)
public class WriteRGBOp extends Operator {

    @Parameter(description = "The zero-based index of the red band.")
    private int red;
    @Parameter(description = "The zero-based index of the green band.")
    private int green;
    @Parameter(description = "The zero-based index of the blue band.")
    private int blue;
    @Parameter(defaultValue = "png")
    private String formatName;
    @Parameter(description = "The file to which the image is written.")
    private File file;

    @SourceProduct(alias = "input")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    private transient RasterDataNode[] rgbChannelNodes;
    private transient Map<Band, Band> bandMap;
    private transient Map<Band, ProductData> dataMap;

    @Override
    public void initialize() throws OperatorException {
        final Band redBand = sourceProduct.getBandAt(red);
        final Band greenBand = sourceProduct.getBandAt(green);
        final Band blueBand = sourceProduct.getBandAt(blue);

        if (!ProductUtils.areRastersEqualInSize(new Band[]{redBand, greenBand, blueBand})) {
            throw new OperatorException("The red, green, and blue bands must be the same size");
        }

        bandMap = new HashMap<Band, Band>(3);
        dataMap = new HashMap<Band, ProductData>(3);
        rgbChannelNodes = new RasterDataNode[3];

        final int height = sourceProduct.getSceneRasterHeight();
        final int width = sourceProduct.getSceneRasterWidth();

        targetProduct = new Product("RGB", "RGB", width, height);
        prepareTargetBand(0, redBand, "red", width, height);
        prepareTargetBand(1, greenBand, "green", width, height);
        prepareTargetBand(2, blueBand, "blue", width, height);
    }

    private void prepareTargetBand(int rgbIndex, Band sourceBand, String bandName, int width, int height) {
        Band targetBand = new Band(bandName, sourceBand.getDataType(), width, height);
        targetProduct.addBand(targetBand);
        bandMap.put(targetBand, sourceBand);

        ProductData data = targetBand.createCompatibleRasterData();
        dataMap.put(targetBand, data);

        targetBand.setRasterData(data);
        rgbChannelNodes[rgbIndex] = targetBand;
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle rectangle = targetTile.getRectangle();

        Band sourceBand = bandMap.get(band);
        Tile sourceTile = getSourceTile(sourceBand, rectangle);

        ProductData rgbData = dataMap.get(band);
        System.arraycopy(sourceTile.getRawSamples().getElems(), 0, rgbData.getElems(), rectangle.x + rectangle.y * rectangle.width, rectangle.width * rectangle.height);
    }

    @Override
    public void dispose() {
        try {
            writeImage();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.dispose();
    }

    private void writeImage() throws IOException {
        ImageInfo imageInfo = ProductUtils.createImageInfo(rgbChannelNodes, true, ProgressMonitor.NULL);
        BufferedImage outputImage = ProductUtils.createRgbImage(rgbChannelNodes, imageInfo, ProgressMonitor.NULL);
        ParameterBlock storeParams = new ParameterBlock();
        storeParams.addSource(outputImage);
        storeParams.add(file);
        storeParams.add(formatName);
        JAI.create("filestore", storeParams);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(WriteRGBOp.class);
        }
    }
}
