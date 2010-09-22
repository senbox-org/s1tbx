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

package org.esa.beam.gpf.operators.standard;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.dataio.dimap.DimapProductWriter;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.gpf.internal.OperatorExecutor;
import org.esa.beam.framework.gpf.internal.OperatorExecutor.ExecutionOrder;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.math.MathUtils;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This write operator stores the source product to the specified file location on the hard disc.
 * <br/>
 * When using GPT with a graph-file, instances of <code>WriteOp</code> are appended to leaf nodes.
 * This means, that it is not necessary to include nodes for writing in a graph.
 * But it can be used, to store results of non-leaf nodes.
 */
@OperatorMetadata(alias = "Write",
                  version="1.2",
                  authors = "Marco Zuehlke, Norman Fomferra",
                  copyright = "(c) 2010 by Brockmann Consult",
                  description = "Writes a data product to a file.")
public class WriteOp extends Operator {

    @TargetProduct
    private Product targetProduct;
    @SourceProduct(alias = "source", description = "The source product to be written.")
    private Product sourceProduct;

    @Parameter(description = "The output file to which the data product is written.")
    private File file;
    @Parameter(defaultValue = ProductIO.DEFAULT_FORMAT_NAME,
               description = "The name of the output file format.")
    private String formatName;
    @Parameter(defaultValue = "true",
               description = "If true, all output files are deleted after a failed write operation.")
    private boolean deleteOutputOnFailure;
    @Parameter(defaultValue = "true",
               description = "If true, the write operation waits until all tiles in a row have been computed before writing.")
    private boolean writeEntireTileRows;

    private final Map<MultiLevelImage, List<Point>> todoLists = new HashMap<MultiLevelImage, List<Point>>();
    private final Map<Row, Tile[]> writeCache = new HashMap<Row, Tile[]>();

    private ProductWriter productWriter;
    private List<Band> writableBands;
    private boolean productFileWritten;
    private Dimension tileSize;
    private int tileCountX;

    public WriteOp() {
        setRequiresAllBands(true);
    }

    public WriteOp(Product sourceProduct, File file, String formatName) {
        this(sourceProduct, file, formatName, true);
    }

    public WriteOp(Product sourceProduct, File file, String formatName, boolean deleteOutputOnFailure) {
        this();
        this.sourceProduct = sourceProduct;
        this.file = file;
        this.formatName = formatName;
        this.deleteOutputOnFailure = deleteOutputOnFailure;
    }

    @Override
    public void initialize() throws OperatorException {
        targetProduct = sourceProduct;
        productWriter = ProductIO.getProductWriter(formatName);
        if (productWriter == null) {
            throw new OperatorException("No data product writer for the '" + formatName + "' format available");
        }
        productWriter.setIncrementalMode(false);
        targetProduct.setProductWriter(productWriter);
        final Band[] bands = targetProduct.getBands();
        writableBands = new ArrayList<Band>(bands.length);
        for (final Band band : bands) {
            band.getSourceImage(); // trigger source image creation
            if (productWriter.shouldWrite(band)) {
                writableBands.add(band);
            }
        }

        tileSize = ImageManager.getPreferredTileSize(targetProduct);
        targetProduct.setPreferredTileSize(tileSize);
        tileCountX = MathUtils.ceilInt(targetProduct.getSceneRasterWidth() / (double) tileSize.width);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        if (!writableBands.contains(targetBand)) {
            return;
        }
        try {
            synchronized (this) {
                if (!productFileWritten) {
                    productWriter.writeProductNodes(targetProduct, file);
                    productFileWritten = true;
                }
            }
            final Rectangle rect = targetTile.getRectangle();
            if (writeEntireTileRows) {
                int tileX = MathUtils.floorInt(targetTile.getMinX() / (double) tileSize.width);
                int tileY = MathUtils.floorInt(targetTile.getMinY() / (double) tileSize.height);
                Row row = new Row(targetBand, tileY);
                Tile[] tileRow = updateTileRow(row, tileX, targetTile);
                if (tileRow != null) {
                    writeTileRow(targetBand, tileRow);
                }
            } else {
                final ProductData rawSamples = targetTile.getRawSamples();
                synchronized (productWriter) {
                    productWriter.writeBandRasterData(targetBand, rect.x, rect.y, rect.width, rect.height, rawSamples, pm);
                }
            }
            markTileDone(targetBand, targetTile);
        } catch (Exception e) {
            if (deleteOutputOnFailure) {
                try {
                    productWriter.deleteOutput();
                    productFileWritten = false;
                } catch (IOException ignored) {
                }
            }
            if (e instanceof OperatorException) {
                throw (OperatorException) e;
            } else {
                throw new OperatorException(e);
            }
        }
    }

    private Tile[] updateTileRow(Row key, int tileX, Tile currentTile) {
        synchronized (writeCache) {
            Tile[] tileRow;
            if (writeCache.containsKey(key)) {
                tileRow = writeCache.get(key);
            } else {
                tileRow = new Tile[tileCountX];
                writeCache.put(key, tileRow);
            }
            tileRow[tileX] = currentTile;
            for (Tile tile : tileRow) {
                if (tile == null) {
                    return null;
                }
            }
            writeCache.remove(key);
            return tileRow;
        }
    }

    private void writeTileRow(Band band, Tile[] cacheLine) throws IOException {
        Tile firstTile = cacheLine[0];
        int sceneWidth = targetProduct.getSceneRasterWidth();
        Rectangle lineBounds = new Rectangle(0, firstTile.getMinY(), sceneWidth, firstTile.getHeight());
        ProductData[] rawSampleOFLine = new ProductData[cacheLine.length];
        int[] tileWidth = new int[cacheLine.length];
        for (int tileX = 0; tileX < cacheLine.length; tileX++) {
            Tile tile = cacheLine[tileX];
            rawSampleOFLine[tileX] = tile.getRawSamples();
            tileWidth[tileX] = tile.getRectangle().width;
        }
        ProductData sampleLine = ProductData.createInstance(rawSampleOFLine[0].getType(), sceneWidth);
        synchronized (productWriter) {
            for (int y = lineBounds.y; y < lineBounds.y + lineBounds.height; y++) {
                int targetPos = 0;
                for (int tileX = 0; tileX < cacheLine.length; tileX++) {
                    Object rawSamples = rawSampleOFLine[tileX].getElems();
                    int width = tileWidth[tileX];
                    int srcPos = (y - lineBounds.y) * width;
                    System.arraycopy(rawSamples, srcPos, sampleLine.getElems(), targetPos, width);
                    targetPos += width;
                }
                productWriter.writeBandRasterData(band, 0, y, sceneWidth, 1, sampleLine, ProgressMonitor.NULL);
            }
        }
    }

    private void markTileDone(Band targetBand, Tile targetTile) throws IOException {
        boolean done;
        synchronized (todoLists) {
            MultiLevelImage sourceImage = targetBand.getSourceImage();

            final List<Point> currentTodoList = getTodoList(sourceImage);
            currentTodoList.remove(new Point(sourceImage.XToTileX(targetTile.getMinX()),
                                             sourceImage.YToTileY(targetTile.getMinY())));

            done = isDone();
        }
        if (done) {
            // If we get here all tiles are written
            if (productWriter instanceof DimapProductWriter) {
                // if we can update the header (only DIMAP) rewrite it!
                synchronized (productWriter) {
                    productWriter.writeProductNodes(targetProduct, file);
                }
            }
        }
    }

    private boolean isDone() {
        for (List<Point> todoList : todoLists.values()) {
            if (!todoList.isEmpty()) {
                return false;
            }
        }
        return true;
    }


    private List<Point> getTodoList(MultiLevelImage sourceImage) {
        List<Point> todoList = todoLists.get(sourceImage);
        if (todoList == null) {
            final int numXTiles = sourceImage.getNumXTiles();
            final int numYTiles = sourceImage.getNumYTiles();
            todoList = new ArrayList<Point>(numXTiles * numYTiles);
            for (int y = 0; y < numYTiles; y++) {
                for (int x = 0; x < numXTiles; x++) {
                    todoList.add(new Point(x, y));
                }
            }
            todoLists.put(sourceImage, todoList);
        }
        return todoList;
    }

    @Override
    public void dispose() {
        try {
            productWriter.close();
        } catch (IOException ignore) {
        }
        writableBands.clear();
        todoLists.clear();
        writeCache.clear();
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(WriteOp.class);
        }
    }

    public static void writeProduct(Product sourceProduct, File file, String formatName, ProgressMonitor pm) {
        writeProduct(sourceProduct, file, formatName, true, pm);
    }

    public static void writeProduct(Product sourceProduct,
                                    File file,
                                    String formatName,
                                    boolean deleteOutputOnFailure,
                                    ProgressMonitor pm) {
        boolean writeEntireTileRows = true;
        ExecutionOrder executionOrder = ExecutionOrder.ROW_BAND_COLUMN;
        writeProduct(sourceProduct, file, formatName, deleteOutputOnFailure, writeEntireTileRows, executionOrder, pm);
    }
        
    public static void writeProduct(Product sourceProduct,
                                        File file,
                                        String formatName,
                                        boolean deleteOutputOnFailure,
                                        boolean writeEntireTileRows,
                                        ExecutionOrder executionOrder, ProgressMonitor pm) {

        final WriteOp writeOp = new WriteOp(sourceProduct, file, formatName, deleteOutputOnFailure);
        writeOp.writeEntireTileRows = writeEntireTileRows;
        OperatorExecutor operatorExecutor = OperatorExecutor.create(writeOp);
        try {
            operatorExecutor.execute(executionOrder, pm);
        } catch (OperatorException e) {
            if (deleteOutputOnFailure) {
                try {
                    writeOp.productWriter.deleteOutput();
                } catch (IOException ignored) {
                }
            }
            throw e;
        } finally {
            writeOp.logPerformanceAnalysis();
            writeOp.dispose();
        }
    }

    private static class Row {
        private final Band band;
        private final int tileY;

        private Row(Band band, int tileY) {
            this.band = band;
            this.tileY = tileY;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + band.hashCode();
            result = prime * result + tileY;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            Row other = (Row) obj;
            if (!band.equals(other.band)) {
                return false;
            }
            return tileY == other.tileY;
        }


    }
}
