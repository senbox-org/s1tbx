package org.esa.beam.framework.gpf.operators.common;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;

import org.esa.beam.dataio.dimap.DimapProductWriter;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.DataNode;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.math.MathUtils;

import javax.media.jai.JAI;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OperatorMetadata(alias = "Write",
                  description = "Writes a product to disk.")
public class WriteOp extends Operator {

    @TargetProduct
    private Product targetProduct;
    @SourceProduct(alias = "input")
    private Product sourceProduct;

    @Parameter(description = "The output file to which the data product is written.")
    private File file;
    @Parameter(defaultValue = ProductIO.DEFAULT_FORMAT_NAME,
               description = "The name of the output file format.")
    private String formatName;
    @Parameter(defaultValue = "true",
               description = "If true, all output files are deleted when the write operation has failed.")
    private boolean deleteOutputOnFailure;

    private ProductWriter productWriter;
    private List<Band> writableBands;
    private boolean productFileWritten;
    private Map<MultiLevelImage, List<Point>> notComputedTileIndexList;
    private boolean headerChanged;
    private ProductNodeChangeListener productNodeChangeListener;

    public WriteOp() {
    }

    public WriteOp(Product sourceProduct, File file, String formatName) {
        this(sourceProduct, file, formatName, true);
    }

    public WriteOp(Product sourceProduct, File file, String formatName, boolean deleteOutputOnFailure) {
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
            throw new OperatorException("No product writer for the '" + formatName + "' format available");
        }
        productWriter.setIncrementalMode(false);
        final Band[] bands = targetProduct.getBands();
        writableBands = new ArrayList<Band>(bands.length);
        for (final Band band : bands) {
            if (productWriter.shouldWrite(band)) {
                writableBands.add(band);
            }
        }
        notComputedTileIndexList = new HashMap<MultiLevelImage, List<Point>>(writableBands.size());
        headerChanged = false;
        productNodeChangeListener = new ProductNodeChangeListener();
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        if (writableBands.contains(targetBand)) {
            final ProductWriter oldProductWriter = targetProduct.getProductWriter();
            try {
                if (!productFileWritten) {
                    productWriter.writeProductNodes(targetProduct, file);
                    productFileWritten = true;
                    // rewrite of product header is only supported for DIMAP
                    if (productWriter instanceof DimapProductWriter) {
                        targetProduct.addProductNodeListener(productNodeChangeListener);
                    }
                }
                final Rectangle rectangle = targetTile.getRectangle();
                final Tile sourceTile = getSourceTile(targetBand, rectangle, pm);
                final ProductData rawSamples = sourceTile.getRawSamples();
                targetProduct.setProductWriter(productWriter);
                productWriter.writeBandRasterData(targetBand, rectangle.x, rectangle.y, rectangle.width, rectangle.height, rawSamples, pm);
                updateComputedTileMap(targetBand, targetTile);
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
            } finally {
                targetProduct.setProductWriter(oldProductWriter);
            }
        }
    }

    private void updateComputedTileMap(Band targetBand, Tile targetTile) throws IOException {
        synchronized (notComputedTileIndexList) {
            MultiLevelImage sourceImage = targetBand.getSourceImage();

            final List<Point> currentList = getTileList(sourceImage);
            currentList.remove(new Point(sourceImage.XToTileX(targetTile.getMinX()),
                                         sourceImage.YToTileY(targetTile.getMinY())));

            for (List<Point> points : notComputedTileIndexList.values()) {
                if (points.isEmpty()) {
                    targetProduct.removeProductNodeListener(productNodeChangeListener);
                } else { // not empty; there are more tiles to come
                    return;
                }
            }
            // If we get here all tiles are written
            if (headerChanged) {   // ask if we have to update the header
                getLogger().info("Product header changed. Overwriting " + file);
                productWriter.writeProductNodes(targetProduct, file);
            }
        }
    }


    private List<Point> getTileList(MultiLevelImage sourceImage) {
        List<Point> list = notComputedTileIndexList.get(sourceImage);
        if (list == null) {
            final int numXTiles = sourceImage.getNumXTiles();
            final int numYTiles = sourceImage.getNumYTiles();
            list = new ArrayList<Point>(numXTiles * numYTiles);
            for (int y = 0; y < numYTiles; y++) {
                for (int x = 0; x < numXTiles; x++) {
                    list.add(new Point(x, y));
                }
            }
            notComputedTileIndexList.put(sourceImage, list);
        }
        return list;
    }

    @Override
    public void dispose() {
        try {
            productWriter.close();
        } catch (IOException e) {
            // ignore
        }
        writableBands.clear();
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(WriteOp.class);
        }
    }

    public static void writeProduct(Product sourceProduct, File file, String formatName, ProgressMonitor pm) {
        writeProduct(sourceProduct, file, formatName, true, pm);
    }

    public static void writeProduct(Product sourceProduct, File file, String formatName, boolean deleteOutputOnFailure,
                                    ProgressMonitor pm) {
        // TODO experimental code by mz (2009.11.06)
        // use it or remove it
//        Spi spi = new WriteOp.Spi();
//        Map<String, Object> parameters = new HashMap<String, Object>();
//        parameters.put("file", file);
//        parameters.put("formatName", formatName);
//        parameters.put("deleteOutputOnFailure", deleteOutputOnFailure);
//        Map<String, Product> sourceProducts = new HashMap<String, Product>();
//        sourceProducts.put("sourceProduct", sourceProduct);
//        Dimension tileSize = ImageManager.getPreferredTileSize(sourceProduct);
//        tileSize = new Dimension(sourceProduct.getSceneRasterWidth(), (tileSize.height * tileSize.width / sourceProduct.getSceneRasterWidth()));
//        RenderingHints renderingHints = new RenderingHints(GPF.KEY_TILE_SIZE, tileSize);
//        final WriteOp writeOp = (WriteOp) spi.createOperator(parameters, sourceProducts, renderingHints);

        final WriteOp writeOp = new WriteOp(sourceProduct, file, formatName, deleteOutputOnFailure);
        final Product targetProduct = writeOp.getTargetProduct();
        Dimension tileSize = targetProduct.getPreferredTileSize();
        if (tileSize == null) {
            tileSize = JAI.getDefaultTileSize();
        }

        final int rasterHeight = targetProduct.getSceneRasterHeight();
        final int rasterWidth = targetProduct.getSceneRasterWidth();
        final Rectangle boundary = new Rectangle(rasterWidth, rasterHeight);
        final int tileCountX = MathUtils.ceilInt(boundary.width / (double) tileSize.width);
        final int tileCountY = MathUtils.ceilInt(boundary.height / (double) tileSize.height);
        final Band[] targetBands = targetProduct.getBands();

        try {
            pm.beginTask("Writing product...", tileCountX * tileCountY * targetBands.length * 2);
            long t1sum = 0;
            long t2sum = 0;
            for (int tileY = 0; tileY < tileCountY; tileY++) {
                for (int tileX = 0; tileX < tileCountX; tileX++) {
                    writeOp.checkForCancelation(pm);

                    final Rectangle tileRectangle = new Rectangle(tileX * tileSize.width,
                                                                  tileY * tileSize.height,
                                                                  tileSize.width,
                                                                  tileSize.height);
                    final Rectangle intersection = boundary.intersection(tileRectangle);

                    System.out.println("doing tile "+intersection + "   "+(tileY*tileCountX+tileX)+ " of "+(tileCountY*tileCountX));
                    for (final Band band : targetBands) {
                        long t1 = System.currentTimeMillis();
                        final Tile tile = writeOp.getSourceTile(band, intersection, new SubProgressMonitor(pm, 1));
                        long t2 = System.currentTimeMillis();
                        writeOp.computeTile(band, tile, new SubProgressMonitor(pm, 1));
                        long t3 = System.currentTimeMillis();
                        t1sum += (t2-t1);
                        t2sum += (t3-t2);
                    }
                    System.out.println("time get="+t1sum+"  save="+t2sum);
                }
            }
        } catch (OperatorException e) {
            if (deleteOutputOnFailure) {
                try {
                    writeOp.productWriter.deleteOutput();
                } catch (IOException ignored) {
                }
            }
            throw e;
        } finally {
            try {
                writeOp.productWriter.close();
            } catch (IOException ignored) {
            }
            pm.done();
        }

        writeOp.logPerformanceAnalysis();
    }

    private class ProductNodeChangeListener extends ProductNodeListenerAdapter {

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            if (!DataNode.PROPERTY_NAME_DATA.equalsIgnoreCase(event.getPropertyName())) {
                headerChanged = true;
            }
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            headerChanged = true;
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            headerChanged = true;
        }
    }
}
    