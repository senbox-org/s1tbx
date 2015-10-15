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
package org.esa.snap.core.gpf.experimental;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.TileRequest;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.Raster;

/**
 * <p>
 * This executor triggers the computation of all tiles that the bands of the
 * target product of the given operator have. The computation of these tiles is
 * may be parallelized to use all available CPUs (cores) using the JAI
 * {@link javax.media.jai.TileScheduler}.
 *
 * <p>
 * Requirements:
 * <ol>
 * <li>Receive raster data for all bands for a requested tile size.</li>
 * <li>Deterministic vs. Non-deterministic Op/Graph Execution (asynchronous, synchronous)</li>
 * <li>Efficient computation of single pixels (may not be implemented here, but in MultiLevelImage)</li>
 * <li>Immediate Execution</li>
 * <li>Configurable with respect to e.g. parallelism, execution order</li>
 * </ol>
 * <p>
 * <i>Important Note: This class is not part of the official API, we may remove or rename it at any time.</i>
 *
 * @author Marco Zuehlke
 * @author Norman Fomferra
 * @since BEAM 4.9
 */
public class OperatorExecutor2 {

    private final Product product;
    private final Dimension frameSize;

    public OperatorExecutor2(Product product) {
        this.product = product;
        this.frameSize = product.getPreferredTileSize();
    }

    public Dimension getFrameSize() {
        return new Dimension(frameSize);
    }

    public void setFrameSize(int w, int h) {
        frameSize.setSize(w, h);
    }


    /**
     * Calls the given handler for all frames of the product the operator executor product.
     */
    public Object execute(Handler handler) throws Exception {
        int parallelism = Runtime.getRuntime().availableProcessors();
        System.out.println("parallelism = " + parallelism);
        JAI.getDefaultInstance().getTileScheduler().setParallelism(parallelism);
        Dimension frameSize = getFrameSize();
        int numXFrames = 1 + (product.getSceneRasterWidth() - 1) / frameSize.width;
        int numYFrames = 1 + (product.getSceneRasterHeight() - 1) / frameSize.height;

        Rectangle sceneRegion = new Rectangle(product.getSceneRasterWidth(), product.getSceneRasterHeight());

        for (int frameY = 0; frameY < numYFrames; frameY++) {
            for (int frameX = 0; frameX < numXFrames; frameX++) {

                Rectangle frameRegion = new Rectangle(frameX * frameSize.width,
                                                      frameY * frameSize.height,
                                                      frameSize.width,
                                                      frameSize.height).intersection(sceneRegion);

                int numBands = product.getNumBands();
                Band[] bandArray = new Band[numBands];
                ProductData[] dataArray = new ProductData[numBands];
                for (int b = 0; b < numBands; b++) {
                    Band band = product.getBandAt(b);
                    PlanarImage planarImage = band.getSourceImage();
                    Point[] indices = planarImage.getTileIndices(null);
                    System.out.println("indices = " + indices.length);
                    TileRequest tileRequest = planarImage.queueTiles(indices);
                    Raster raster = planarImage.getData();
                    System.out.println("raster = " + raster);
                    ProductData data = band.createCompatibleRasterData(frameRegion.width, frameRegion.height);
                    band.readRasterData(frameRegion.x, frameRegion.y, frameRegion.width, frameRegion.height, data);
                    bandArray[b] = band;
                    dataArray[b] = data;
                }

                MyFrame frame = new MyFrame(frameRegion, bandArray, dataArray);
                handler.frameComputed(frame);
            }
        }


        return new Object();
    }

    public interface Handler {
        void frameComputed(Frame frame);
    }

    public interface Frame {
        Rectangle getRegion();

        int getNumBands();

        Band getBand(int index);

        ProductData getData(int index);
    }

    private static class MyFrame implements Frame {
        private final Rectangle region;
        private final Band[] bandArray;
        private final ProductData[] dataArray;

        public MyFrame(Rectangle region, Band[] bandArray, ProductData[] dataArray) {
            this.region = region;
            this.bandArray = bandArray;
            this.dataArray = dataArray;
        }

        @Override
        public Rectangle getRegion() {
            return region;
        }

        @Override
        public int getNumBands() {
            return bandArray.length;
        }

        @Override
        public Band getBand(int index) {
            return bandArray[index];
        }

        @Override
        public ProductData getData(int index) {
            return dataArray[index];
        }
    }
}
