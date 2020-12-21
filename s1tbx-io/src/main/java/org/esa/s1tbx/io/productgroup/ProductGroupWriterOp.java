
package org.esa.s1tbx.io.productgroup;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductSubsetBuilder;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.dataio.dimap.DimapProductWriter;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.io.FileUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Save a product as a Product Group
 */
@OperatorMetadata(alias = "ProductGroupWriter",
        authors = "Luis Veci",
        copyright = "Copyright (C) 2020 by SkyWatch Space Applications Inc.",
        version = "1.0",
        description = "Writes a stack as a product group",
        autoWriteDisabled = true,
        category = "Tools")
public class ProductGroupWriterOp extends Operator {

    @TargetProduct
    private Product targetProduct;

    @SourceProduct(alias = "source", description = "The source product to be written.")
    private Product sourceProduct;

    @Parameter(description = "The output file to which the data product is written.")
    private File file;

    @Parameter(defaultValue = ProductIO.DEFAULT_FORMAT_NAME,
            description = "The name of the output file format.")
    private String formatName;

    private final Map<MultiLevelImage, List<Point>> todoLists = new HashMap<>();

    private boolean productFileWritten;

    private SubsetInfo[] subsetInfo = null;

    public ProductGroupWriterOp() {
        setRequiresAllBands(true);
    }

    @Override
    public void initialize() throws OperatorException {
        try {
            targetProduct = sourceProduct;

            int numFiles =1, numRows, numCols, width, height;

            subsetInfo = new SubsetInfo[numFiles];
            int n = 0;
            for (int r = 0; r < numFiles; ++r) {
                    final ProductSubsetDef subsetDef = new ProductSubsetDef();
                    subsetDef.addNodeNames(sourceProduct.getTiePointGridNames());
                    subsetDef.addNodeNames(sourceProduct.getBandNames());
                    subsetDef.setSubSampling(1, 1);
                    subsetDef.setIgnoreMetadata(false);

                    subsetInfo[n] = new SubsetInfo();
                    subsetInfo[n].subsetBuilder = new ProductSubsetBuilder();
                    subsetInfo[n].product = subsetInfo[n].subsetBuilder.readProductNodes(sourceProduct, subsetDef);
                    subsetInfo[n].file = new File(file.getParentFile(), createName(file, n + 1));

                    subsetInfo[n].productWriter = ProductIO.getProductWriter(formatName);
                    if (subsetInfo[n].productWriter == null) {
                        throw new OperatorException("No data product writer for the '" + formatName + "' format available");
                    }
                    subsetInfo[n].productWriter.setIncrementalMode(false);
                    subsetInfo[n].productWriter.setFormatName(formatName);
                    subsetInfo[n].product.setProductWriter(subsetInfo[n].productWriter);

                    final Band[] bands = subsetInfo[n].product.getBands();
                    for (Band b : bands) {
                        // b.getSourceImage(); // trigger source image creation
                    }
                    ++n;
            }

        } catch (Throwable t) {
            throw new OperatorException(t);
        }
    }

    private static String createName(final File file, final int n) {
        return FileUtils.getFilenameWithoutExtension(file) + '_' + n + FileUtils.getExtension(file);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        try {
            synchronized (this) {
                if (!productFileWritten) {
                    for (SubsetInfo info : subsetInfo) {
                        info.productWriter.writeProductNodes(info.product, info.file);
                    }
                    productFileWritten = true;
                }
            }
            final Rectangle rect = targetTile.getRectangle();

            for (SubsetInfo info : subsetInfo) {
                final Rectangle trgRect = info.subsetBuilder.getSubsetDef().getRegion();
                if (rect.intersects(trgRect)) {
                    writeTile(info, targetBand.getName(), trgRect);
                }
            }
            markTileDone(targetBand, targetTile);
        } catch (Exception e) {
            if (e instanceof OperatorException) {
                throw (OperatorException) e;
            } else {
                throw new OperatorException(e);
            }
        }
    }

    private synchronized void writeTile(final SubsetInfo info, final String bandName, final Rectangle trgRect)
            throws IOException {

        final Tile sourceTile = getSourceTile(sourceProduct.getBand(bandName), trgRect);
        final ProductData rawSamples = sourceTile.getRawSamples();

        final Band trgBand = info.product.getBand(bandName);
        info.productWriter.writeBandRasterData(trgBand,
                0, 0, trgBand.getRasterWidth(), trgBand.getRasterHeight(), rawSamples, ProgressMonitor.NULL);
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
            for (SubsetInfo info : subsetInfo) {
                if (info.productWriter instanceof DimapProductWriter) {
                    // if we can update the header (only DIMAP) rewrite it!
                    synchronized (info.productWriter) {
                        info.productWriter.writeProductNodes(info.product, info.file);
                    }
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
            todoList = new ArrayList<>(numXTiles * numYTiles);
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
            for (SubsetInfo info : subsetInfo) {
                info.productWriter.close();
            }
        } catch (IOException ignore) {
        }
        todoLists.clear();
        super.dispose();
    }

    private static class SubsetInfo {
        Product product;
        ProductSubsetBuilder subsetBuilder;
        File file;
        ProductWriter productWriter;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ProductGroupWriterOp.class);
        }
    }
}
