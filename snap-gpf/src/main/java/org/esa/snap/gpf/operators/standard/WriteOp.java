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

package org.esa.snap.gpf.operators.standard;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.dataio.dimap.DimapProductWriter;
import org.esa.snap.framework.dataio.ProductIO;
import org.esa.snap.framework.dataio.ProductWriter;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.gpf.Operator;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.Tile;
import org.esa.snap.framework.gpf.annotations.OperatorMetadata;
import org.esa.snap.framework.gpf.annotations.Parameter;
import org.esa.snap.framework.gpf.annotations.SourceProduct;
import org.esa.snap.framework.gpf.annotations.TargetProduct;
import org.esa.snap.framework.gpf.internal.OperatorExecutor;
import org.esa.snap.framework.gpf.internal.OperatorExecutor.ExecutionOrder;
import org.esa.snap.util.Guardian;
import org.esa.snap.util.jai.JAIUtils;
import org.esa.snap.util.math.MathUtils;

import javax.media.jai.JAI;
import javax.media.jai.TileCache;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This standard operator is used to store a data product to a specified file location.
 * <p>
 * It is used by the framework, e.g. the {@code gpt} command line tool, to write target products.
 * <p>
 * It may also be used by clients to write out break-point product files. This is done by placing
 * a {@code WriteOp} node after any node in a processing graph:
 * <p>
 * <pre>
 * &lt;node id="anyNodeId"&gt;
 *     &lt;operator&gt;Write&lt;/operator&gt;
 *     &lt;sources&gt;
 *         &lt;source&gt;${anySourceNodeId}&lt;/source&gt;
 *     &lt;/sources&gt;
 *     &lt;parameters&gt;
 *         &lt;file&gt;/home/norman/eo-data/output/test.nc&lt;/file&gt;
 *         &lt;formatName&gt;NetCDF&lt;/formatName&gt;
 *         &lt;deleteOutputOnFailure&gt;true&lt;/deleteOutputOnFailure&gt;
 *         &lt;writeEntireTileRows&gt;true&lt;/writeEntireTileRows&gt;
 *         &lt;clearCacheAfterRowWrite&gt;true&lt;/clearCacheAfterRowWrite&gt;
 *     &lt;/parameters&gt;
 * &lt;/node&gt;
 * </pre>
 * <p>
 * Clients may also use this operator in a programmatic way:
 * <pre>
 *   WriteOp writeOp = new WriteOp(sourceProduct, file, formatName);
 *   writeOp.setDeleteOutputOnFailure(true);
 *   writeOp.setWriteEntireTileRows(true);
 *   writeOp.writeProduct(progressMonitor);
 * </pre>
 *
 * @author Norman Fomferra
 * @author Marco Zuehlke
 * @since BEAM 4.2
 */
@OperatorMetadata(alias = "Write",
                  category = "Input-Output",
                  version = "1.3",
                  authors = "Marco Zuehlke, Norman Fomferra",
                  copyright = "(c) 2010 by Brockmann Consult",
                  description = "Writes a data product to a file.",
                  autoWriteDisabled = true)
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
    private boolean deleteOutputOnFailure = true;

    @Parameter(defaultValue = "true",
               description = "If true, the write operation waits until an entire tile row is computed.")
    private boolean writeEntireTileRows;

    /**
     * @since BEAM 4.9
     */
    @Parameter(defaultValue = "false",
               description = "If true, the internal tile cache is cleared after a tile row has been written. Ignored if writeEntireTileRows=false.")
    private boolean clearCacheAfterRowWrite;

    private boolean[][][] tilesWritten;
    private final Map<Row, Tile[]> writeCache = new HashMap<>();
    private Dimension[] tileSizes;
    private int[] tileCountsX;

    private ProductWriter productWriter;
    private List<Band> writableBands;

    private boolean outputFileExists = false;
    private boolean incremental = false;

    public WriteOp() {
        setParameterDefaultValues();
        setRequiresAllBands(true);
    }

    public WriteOp(Product sourceProduct, File file, String formatName) {
        this();
        Guardian.assertNotNull("file", file);
        this.sourceProduct = sourceProduct;
        this.file = file;
        this.formatName = formatName;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getFormatName() {
        return formatName;
    }

    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    public void setFormatName(String formatName) {
        this.formatName = formatName;
    }

    public boolean isDeleteOutputOnFailure() {
        return deleteOutputOnFailure;
    }

    public void setDeleteOutputOnFailure(boolean deleteOutputOnFailure) {
        this.deleteOutputOnFailure = deleteOutputOnFailure;
    }

    public boolean isWriteEntireTileRows() {
        return writeEntireTileRows;
    }

    public void setWriteEntireTileRows(boolean writeEntireTileRows) {
        this.writeEntireTileRows = writeEntireTileRows;
    }

    public boolean isClearCacheAfterRowWrite() {
        return clearCacheAfterRowWrite;
    }

    public void setClearCacheAfterRowWrite(boolean clearCacheAfterRowWrite) {
        this.clearCacheAfterRowWrite = clearCacheAfterRowWrite;
    }

    /**
     * Writes the source product.
     *
     * @param pm A progress monitor.
     */
    public void writeProduct(ProgressMonitor pm) {
        long startNanos = System.nanoTime();
        getLogger().info("Start writing product " + getTargetProduct().getName() + " to " + getFile());
        OperatorExecutor operatorExecutor = OperatorExecutor.create(this);
        try {
            if (clearCacheAfterRowWrite && writeEntireTileRows) {
                operatorExecutor.setScheduleRowsSeparate(true);
            }
            operatorExecutor.execute(ExecutionOrder.SCHEDULE_ROW_COLUMN_BAND, "Writing...", pm);

            getLogger().info("End writing product " + getTargetProduct().getName() + " to " + getFile());

            double seconds = (System.nanoTime() - startNanos) / 1.0E9;
            int w = getTargetProduct().getSceneRasterWidth();
            int h = getTargetProduct().getSceneRasterHeight();

            getLogger().info(MessageFormat.format("Time: {0} sec. total, {1} sec. per line, {2} sec. per pixel",
                                                  seconds,
                                                  seconds / h,
                                                  seconds / h / w));

            stopTileComputationObservation();
        } catch (OperatorException e) {
            if (deleteOutputOnFailure && !outputFileExists) {
                try {
                    productWriter.deleteOutput();
                } catch (Exception e2) {
                    getLogger().warning("Failed to delete output after failure: " + e2.getMessage());
                }
            }
            throw e;
        } finally {
            dispose();
        }
    }

    @Override
    public void initialize() throws OperatorException {
        targetProduct = sourceProduct;
        outputFileExists = targetProduct.getFileLocation() != null && targetProduct.getFileLocation().exists();
        productWriter = ProductIO.getProductWriter(formatName);
        if (productWriter == null) {
            throw new OperatorException("No data product writer for the '" + formatName + "' format available");
        }
        productWriter.setIncrementalMode(incremental);
        targetProduct.setProductWriter(productWriter);

        final Band[] bands = targetProduct.getBands();
        writableBands = new ArrayList<>(bands.length);
        for (final Band band : bands) {
            band.getSourceImage(); // trigger source image creation
            if (productWriter.shouldWrite(band)) {
                writableBands.add(band);
            }
        }
        tileSizes = new Dimension[writableBands.size()];
        tileCountsX = new int[writableBands.size()];
        tilesWritten = new boolean[writableBands.size()][][];
        for (int i = 0; i < writableBands.size(); i++) {
            Band writableBand = writableBands.get(i);
            Dimension tileSize = determineTileSize(writableBand);
            tileSizes[i] = tileSize;
            int tileCountX = MathUtils.ceilInt(writableBand.getSceneRasterWidth() / (double) tileSize.width);
            tileCountsX[i] = tileCountX;
            int tileCountY = MathUtils.ceilInt(writableBand.getSceneRasterHeight() / (double) tileSize.height);
            tilesWritten[i] = new boolean[tileCountY][tileCountX];
        }
    }

    private Dimension determineTileSize(Band band) {
        Dimension tileSize = null;
        if (band.getRasterWidth() == targetProduct.getSceneRasterWidth() &&
                band.getRasterHeight() == targetProduct.getSceneRasterHeight()) {
            tileSize = targetProduct.getPreferredTileSize();
        }
        if (tileSize == null) {
            tileSize = JAIUtils.computePreferredTileSize(band.getSceneRasterWidth(),
                                                         band.getSceneRasterHeight(), 1);
        }
        return tileSize;
    }

    @Override
    public void doExecute(ProgressMonitor pm) {
        try {
            productWriter.writeProductNodes(targetProduct, file);
        } catch (IOException e) {
            throw new OperatorException("Not able to write product file: '" + file.getAbsolutePath() + "'", e);
        }
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        int bandIndex = writableBands.indexOf(targetBand);
        if (bandIndex == -1) {
            return;
        }
        try {
            final Rectangle rect = targetTile.getRectangle();
            final Dimension tileSize = tileSizes[bandIndex];
            int tileCountX = tileCountsX[bandIndex];
            int tileX = MathUtils.floorInt(targetTile.getMinX() / (double) tileSize.width);
            int tileY = MathUtils.floorInt(targetTile.getMinY() / (double) tileSize.height);
            if (writeEntireTileRows) {
                Row row = new Row(targetBand, tileY);
                Tile[] tileRowToWrite = updateTileRow(row, tileX, targetTile, tileCountX);
                if (tileRowToWrite != null) {
                    writeTileRow(targetBand, tileRowToWrite);
                }
                markTileAsHandled(targetBand, tileX, tileY);
                if (clearCacheAfterRowWrite && tileRowToWrite != null && isRowWrittenCompletely(tileY)) {
                    TileCache tileCache = JAI.getDefaultInstance().getTileCache();
                    if (tileCache != null) {
                        tileCache.flush();
                    }
                }
            } else {
                final ProductData rawSamples = targetTile.getRawSamples();
                synchronized (productWriter) {
                    productWriter.writeBandRasterData(targetBand, rect.x, rect.y, rect.width, rect.height, rawSamples,
                                                      pm);
                }
                markTileAsHandled(targetBand, tileX, tileY);
            }
            if (productWriter instanceof DimapProductWriter && isProductWrittenCompletely()) {
                // If we get here all tiles are written
                // we can update the header only for DIMAP, so rewrite it, to handle intermediate changes
                synchronized (productWriter) {
                    productWriter.writeProductNodes(targetProduct, file);
                }
            }
        } catch (Exception e) {
            if (deleteOutputOnFailure && !outputFileExists) {
                try {
                    productWriter.deleteOutput();
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

    private Tile[] updateTileRow(Row key, int tileX, Tile currentTile, int tileCountX) {
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
        int sceneWidth = band.getRasterWidth();
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

    private void markTileAsHandled(Band targetBand, int tileX, int tileY) {
        int bandIndex = writableBands.indexOf(targetBand);
        tilesWritten[bandIndex][tileY][tileX] = true;
    }

    private boolean isRowWrittenCompletely(int rowNumber) {
        for (int bandIndex = 0; bandIndex < writableBands.size(); bandIndex++) {
            for (boolean aYTileWritten : tilesWritten[bandIndex][rowNumber]) {
                if (!aYTileWritten) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isProductWrittenCompletely() {
        for (int bandIndex = 0; bandIndex < writableBands.size(); bandIndex++) {
            for (boolean[] aXTileWritten : tilesWritten[bandIndex]) {
                for (boolean aYTileWritten : aXTileWritten) {
                    if (!aYTileWritten) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void dispose() {
        try {
            productWriter.close();
        } catch (IOException ignore) {
        }
        writableBands.clear();
        writeCache.clear();
        super.dispose();
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(WriteOp.class);
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
