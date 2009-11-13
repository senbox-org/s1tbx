package org.esa.beam.framework.gpf.operators.common;

import com.bc.ceres.core.ProgressMonitor;
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
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.gpf.internal.TileImpl;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.math.MathUtils;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.TileComputationListener;
import javax.media.jai.TileRequest;
import javax.media.jai.TileScheduler;

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
    private volatile Tile[][][] oneLineOftiles;
    private Dimension tileSize;
    
    volatile int tilesInStore = 0;

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
        
        tileSize = ImageManager.getPreferredTileSize(targetProduct);
        targetProduct.setPreferredTileSize(tileSize);
        final int tileCountX = MathUtils.ceilInt(targetProduct.getSceneRasterWidth() / (double) tileSize.width);
        final int tileCountY = MathUtils.ceilInt(targetProduct.getSceneRasterHeight() / (double) tileSize.height);
        oneLineOftiles = new Tile[writableBands.size()][tileCountY][tileCountX];
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        if (writableBands.contains(targetBand)) {
            synchronized (this) {
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
                    targetProduct.setProductWriter(productWriter);
                    final Rectangle rectangle = targetTile.getRectangle();
                    
                    // TODO write single tiles, remove 
//                    long t1 = System.currentTimeMillis();
//                    System.out.println("writing "+targetBand.getName()+" "+rectangle);
//                    final Tile sourceTile = getSourceTile(targetBand, rectangle, pm);
//                    final ProductData rawSamples = sourceTile.getRawSamples();
//                    productWriter.writeBandRasterData(targetBand, rectangle.x, rectangle.y, rectangle.width,
//                                                      rectangle.height, rawSamples, pm);

                    final Tile sourceTile = getSourceTile(targetBand, rectangle, pm);
                    int tileX = MathUtils.floorInt(targetTile.getMinX() / (double) tileSize.width);
                    int tileY = MathUtils.floorInt(targetTile.getMinY() / (double) tileSize.height);
                    oneLineOftiles[writableBands.indexOf(targetBand)][tileY][tileX] = sourceTile;
                    tilesInStore++;
                    writeLineIfComplete(targetBand, tileY);

                    updateComputedTileMap(targetBand, targetTile);
//                    long t2 = System.currentTimeMillis();
//                    long savetime = t2-t1;
//                    if (savetime>3) {
//                        System.out.println("writing took="+savetime+" "+targetBand.getName()+" "+rectangle);
//                    }
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
    }

    // TODO should we handle out of order writing ? (mz, 2009.11.11)
    private void writeLineIfComplete(Band targetBand, int tileY) throws IOException {
        int bandIndex = writableBands.indexOf(targetBand);
        int tileXCount = oneLineOftiles[bandIndex][0].length;
        Tile[] thisTileLine = oneLineOftiles[bandIndex][tileY];
        for (int i = 0; i < tileXCount; i++) {
            if (thisTileLine[i] == null) {
                return;
            }
        }
        // all tiles for this line are available
        Tile firstTile = thisTileLine[0];
        int sceneWidth = targetProduct.getSceneRasterWidth();
        Rectangle lineBounds = new Rectangle(0, firstTile.getMinY(), sceneWidth, firstTile.getHeight());
        ProductData[] rawSampleOFLine = new ProductData[tileXCount];
        int[] tileWidth = new int[tileXCount];
        for (int tileX = 0; tileX < tileXCount; tileX++) {
            Tile tile = thisTileLine[tileX];
            rawSampleOFLine[tileX] = tile.getRawSamples();
            tileWidth[tileX] = tile.getRectangle().width; 
        }
        ProductData sampleLine = ProductData.createInstance(rawSampleOFLine[0].getType(), sceneWidth);
        for (int y = lineBounds.y; y < lineBounds.y + lineBounds.height; y++) {
            int targetPos = 0;
            for (int tileX = 0; tileX < tileXCount; tileX++) {
                ProductData productData = rawSampleOFLine[tileX];
                Object rawSamples = productData.getElems();
                int width = tileWidth[tileX];
                int srcPos = (y-lineBounds.y) * width;
                System.arraycopy(rawSamples, srcPos, sampleLine.getElems(), targetPos, width);
                targetPos += width;

            }
            productWriter.writeBandRasterData(targetBand, 0, y, sceneWidth, 1, sampleLine, ProgressMonitor.NULL);
        }
        for (int tileX = 0; tileX < tileXCount; tileX++) {
            thisTileLine[tileX] = null;
            tilesInStore--;
        }
        // TODO use this ? seems to not give any improvement (mz, 2009.11.11)
//        TileCache tileCache = JAI.getDefaultInstance().getTileCache();
//        RenderedImage image = targetBand.getSourceImage().getImage(0);
//        if (tileCache != null && image != null) {
//            if (image instanceof RenderedOp) {
//                RenderedOp renderedOp = (RenderedOp) image;
//                image = renderedOp.getRendering();
//            }
//            int tileY = MathUtils.floorInt(lineBounds.y / (double) tileSize.height);
//            for (int tileX = 0; tileX < tileXCount; tileX++) {
//                tileCache.remove(image, tileX, tileY);
//            }
//        }
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

        final WriteOp writeOp = new WriteOp(sourceProduct, file, formatName, deleteOutputOnFailure);
        final Product targetProduct = writeOp.getTargetProduct();
        Dimension tileSize = targetProduct.getPreferredTileSize();

        final int rasterHeight = targetProduct.getSceneRasterHeight();
        final int rasterWidth = targetProduct.getSceneRasterWidth();
        final Rectangle boundary = new Rectangle(rasterWidth, rasterHeight);
        final int tileCountX = MathUtils.ceilInt(boundary.width / (double) tileSize.width);
        final int tileCountY = MathUtils.ceilInt(boundary.height / (double) tileSize.height);
        final Band[] targetBands = targetProduct.getBands();

        Map<Band, PlanarImage> imageMap = new HashMap<Band, PlanarImage>(targetBands.length*2);
        for (final Band band : targetBands) {
          final RenderedImage image = band.getSourceImage().getImage(0);
          final PlanarImage planarImage = PlanarImage.wrapRenderedImage(image);
          imageMap.put(band, planarImage);
        }
        TileScheduler tileScheduler = JAI.getDefaultInstance().getTileScheduler();
        try {
            pm.beginTask("Writing product...", tileCountX * tileCountY * targetBands.length);
            for (int tileY = 0; tileY < tileCountY; tileY++) {
                for (final Band band : targetBands) {
                    writeOp.checkForCancelation(pm);
                    Point[] points = new Point[tileCountX];
                    for (int tileX = 0; tileX < tileCountX; tileX++) {
                        points[tileX] = new Point(tileX, tileY);
                    }
                    CountDownLatch latch = new CountDownLatch(tileCountX);
                    final PlanarImage planarImage = imageMap.get(band);
                    final TileComputationListener tcl = new MyTileComputationListener(band, writeOp, latch);
                    final TileComputationListener[] listeners = new TileComputationListener[] {tcl};
                    tileScheduler.scheduleTiles(planarImage, points, listeners);
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        throw new OperatorException(e);
                    }
                    pm.worked(tileCountX);
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
                while (writeOp.tilesInStore > 0) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        throw new OperatorException(e);
                    }
                }
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
    
    private static class MyTileComputationListener implements TileComputationListener {

        private final Band band;
        private final WriteOp writeOp;
        private CountDownLatch countDownLatch;

        MyTileComputationListener(Band band, WriteOp writeOp, CountDownLatch latch) {
            this.band = band;
            this.writeOp = writeOp;
            this.countDownLatch = latch;
        }

        @Override
            public void tileComputed(Object eventSource, TileRequest[] requests, PlanarImage image, int tileX,
                                 int tileY,
                                 Raster raster) {
            countDownLatch.countDown();
            
            final int rasterHeight = band.getSceneRasterHeight();
            final int rasterWidth = band.getSceneRasterWidth();
            final Rectangle boundary = new Rectangle(rasterWidth, rasterHeight);
            Rectangle rect = boundary.intersection(raster.getBounds());
            final TileImpl tile = new TileImpl(band, raster, rect, false);
            writeOp.computeTile(band, tile, ProgressMonitor.NULL);
        }

        @Override
            public void tileCancelled(Object eventSource, TileRequest[] requests, PlanarImage image, int tileX,
                                  int tileY) {
            System.out.println("tileCancelled");
        }

        @Override
            public void tileComputationFailure(Object eventSource, TileRequest[] requests, PlanarImage image, int tileX,
                                           int tileY, Throwable situation) {
            System.out.println("tileComputationFailure");
            situation.printStackTrace();
            System.out.println("writeOp.tilesInStore="+writeOp.tilesInStore);
            System.out.println("==========================");
        }
    }
}
    