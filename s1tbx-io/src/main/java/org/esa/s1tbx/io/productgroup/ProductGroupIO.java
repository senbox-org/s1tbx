/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.s1tbx.io.productgroup;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.snap.core.dataio.*;
import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.StopWatch;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.EngineConfig;

import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;

/**
 * The <code>ProductIO</code> class provides several utility methods concerning data I/O for remote sensing data
 * products.
 * <p> For example, a product can be read in using a single method call:
 * <pre>
 *      Product product =  ProductIO.readProduct("test.prd");
 * </pre>
 * and written out in a similar way:
 * <pre>
 *      ProductIO.writeProduct(product, "HDF5", "test.h5", null);
 * </pre>
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 */
public class ProductGroupIO {

    /**
     * The name of the default product format.
     */
    public static final String DEFAULT_FORMAT_NAME = DimapProductConstants.DIMAP_FORMAT_NAME;

    public static final String SYSTEM_PROPERTY_CONCURRENT = "snap.productio.concurrent";
    public static final boolean DEFAULT_WRITE_RASTER_CONCURRENT = true;

    /**
     * Writes a product with the specified format to the given file path.
     * <p>The method also writes all band data to the file. Therefore the band data must either
     * <ul>
     * <li>be completely loaded ({@link Band#getRasterData() Band.rasterData} is not <code>null</code>)</li>
     * <li>or the product must be associated with a product reader ({@link Product#getProductReader() Product.productReader} is not <code>null</code>)
     * so that unloaded data can be reloaded.</li>
     * </ul>.
     *
     * @param product    the product, must not be <code>null</code>
     * @param filePath   the file path
     * @param formatName the name of a supported product format, e.g. "HDF5". If <code>null</code>, the default format
     *                   "BEAM-DIMAP" will be used
     *
     * @throws IOException if an IOException occurs
     */
    public static void writeProduct(Product product,
                                    String filePath,
                                    String formatName) throws IOException {
        writeProduct(product, new File(filePath), formatName, false, ProgressMonitor.NULL);
    }

    /**
     * Writes a product with the specified format to the given file path.
     * <p>The method also writes all band data to the file. Therefore the band data must either
     * <ul>
     * <li>be completely loaded ({@link Band#getRasterData() Band.rasterData} is not <code>null</code>)</li>
     * <li>or the product must be associated with a product reader ({@link Product#getProductReader() Product.productReader} is not <code>null</code>)
     * so that unloaded data can be reloaded.</li>
     * </ul>.
     *
     * @param product    the product, must not be <code>null</code>
     * @param filePath   the file path
     * @param formatName the name of a supported product format, e.g. "HDF5". If <code>null</code>, the default format
     *                   "BEAM-DIMAP" will be used
     * @param pm         a monitor to inform the user about progress
     *
     * @throws IOException if an IOException occurs
     */
    public static void writeProduct(Product product,
                                    String filePath,
                                    String formatName,
                                    ProgressMonitor pm) throws IOException {
        writeProduct(product, new File(filePath), formatName, false, pm);
    }

    /**
     * Writes a product with the specified format to the given file.
     * <p>The method also writes all band data to the file. Therefore the band data must either
     * <ul>
     * <li>be completely loaded ({@link Band#getRasterData() Band.rasterData} is not <code>null</code>)</li>
     * <li>or the product must be associated with a product reader ({@link Product#getProductReader() Product.productReader} is not <code>null</code>)
     * so that unloaded data can be reloaded.</li>
     * </ul>.
     *
     * @param product     the product, must not be <code>null</code>
     * @param file        the product file , must not be <code>null</code>
     * @param formatName  the name of a supported product format, e.g. "HDF5". If <code>null</code>, the default format
     *                    "BEAM-DIMAP" will be used
     * @param incremental switch the product writer in incremental mode or not.
     *
     * @throws IOException if an IOException occurs
     */
    public static void writeProduct(Product product,
                                    File file,
                                    String formatName,
                                    boolean incremental) throws IOException {
        writeProduct(product, file, formatName, incremental, ProgressMonitor.NULL);
    }

    /**
     * Writes a product with the specified format to the given file.
     * <p>The method also writes all band data to the file. Therefore the band data must either
     * <ul>
     * <li>be completely loaded ({@link Band#getRasterData() Band.rasterData} is not <code>null</code>)</li>
     * <li>or the product must be associated with a product reader ({@link Product#getProductReader() Product.productReader} is not <code>null</code>)
     * so that unloaded data can be reloaded.</li>
     * </ul>.
     *
     * @param product     the product, must not be <code>null</code>
     * @param file        the product file , must not be <code>null</code>
     * @param formatName  the name of a supported product format, e.g. "HDF5". If <code>null</code>, the default format
     *                    "BEAM-DIMAP" will be used
     * @param incremental switch the product writer in incremental mode or not.
     * @param pm          a monitor to inform the user about progress
     *
     * @throws IOException if an IOException occurs
     */
    public static void writeProduct(Product product,
                                    File file,
                                    String formatName,
                                    boolean incremental,
                                    ProgressMonitor pm) throws IOException {
        Guardian.assertNotNull("product", product);
        Guardian.assertNotNull("file", file);
        if (formatName == null) {
            formatName = DEFAULT_FORMAT_NAME;
        }
        ProductWriter productWriter = ProductIO.getProductWriter(formatName);
        if (productWriter == null) {
            throw new ProductIOException("No product writer for the '" + formatName + "' format available.");
        }
        final EncodeQualification encodeQualification = productWriter.getWriterPlugIn().getEncodeQualification(product);
        if (encodeQualification.getPreservation() == EncodeQualification.Preservation.UNABLE) {
            throw new ProductIOException("Product writer is unable to write product:\n" + encodeQualification.getInfoString());
        }
        productWriter.setIncrementalMode(incremental);

        ProductWriter productWriterOld = product.getProductWriter();
        product.setProductWriter(productWriter);

        IOException ioException = null;
        try {
            long s;
            long e;
            s = System.currentTimeMillis();
            productWriter.writeProductNodes(product, file);
            e = System.currentTimeMillis();
            long t1 = e - s;
            SystemUtils.LOG.fine("write product nodes to " + file.getAbsolutePath() + " took " + StopWatch.getTimeString(t1));

            s = System.currentTimeMillis();
            writeAllBands(product, pm);
            e = System.currentTimeMillis();
            long t2 = e - s;
            SystemUtils.LOG.fine("write all bands of product " + file.getAbsolutePath() + " took " + StopWatch.getTimeString(t2));
            SystemUtils.LOG.fine("Write entire product " + file.getAbsolutePath() + " took " + StopWatch.getTimeString(t1 + t2));
        } catch (IOException e) {
            ioException = e;
        } finally {
            try {
                productWriter.flush();
                productWriter.close();
            } catch (IOException e) {
                if (ioException == null) {
                    ioException = e;
                }
            }
            product.setProductWriter(productWriterOld);
            product.setFileLocation(file);
        }

        if (ioException != null) {
            throw ioException;
        }
    }

    /*
     * This implementation helper methods writes all bands of the given product using the specified product writer. If a
     * band is entirely loaded its data is written out immediately, if not, a band's data raster is written out
     * line-by-line without producing any memory overhead.
     */
    private static void writeAllBands(Product product, ProgressMonitor pm) throws IOException {
        ProductWriter productWriter = product.getProductWriter();
        final boolean concurrent = Config.instance("snap").load().preferences().getBoolean(SYSTEM_PROPERTY_CONCURRENT, DEFAULT_WRITE_RASTER_CONCURRENT);

        // for correct progress indication we need to collect
        // all bands which shall be written to the output
        ArrayList<Band> bandsToWrite = new ArrayList<>();
        for (int i = 0; i < product.getNumBands(); i++) {
            Band band = product.getBandAt(i);
            if (productWriter.shouldWrite(band)) {
                bandsToWrite.add(band);
            }
        }

        if (!bandsToWrite.isEmpty()) {
            pm.beginTask("Writing bands of product '" + product.getName() + "'...", bandsToWrite.size());
            try {
                if (concurrent) {
                    writeBandsConcurrent(pm, bandsToWrite);
                } else {
                    writeBandsSequentially(pm, bandsToWrite);
                }
            } finally {
                pm.done();
            }
        }
    }

    private static void writeBandsConcurrent(ProgressMonitor pm, ArrayList<Band> bandsToWrite) throws IOException {
        final int numBands = bandsToWrite.size();
        final int numThreads = Runtime.getRuntime().availableProcessors();
        final int threadsPerBand = numThreads / numBands;
        final int executorSize = threadsPerBand == 0 ? 1 : threadsPerBand;
        Semaphore semaphore = new Semaphore(numThreads);
        List<IOException> ioExceptionCollector = Collections.unmodifiableList(new ArrayList<>());
        for (Band band : bandsToWrite) {
            if (pm.isCanceled()) {
                break;
            }
            ExecutorService executor = null;
            semaphore.acquireUninterruptibly();
            executor = Executors.newFixedThreadPool(executorSize);
            pm.setSubTaskName("Writing band '" + band.getName() + "'");
            ProgressMonitor subPM = SubProgressMonitor.create(pm, 1);
            writeRasterDataFully(subPM, band, executor, semaphore, ioExceptionCollector);
        }
        while (semaphore.availablePermits() < numThreads) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                EngineConfig.instance().logger().log(Level.WARNING,
                                                     "Method ProductIO.writeAllBands(...)' unexpected termination", e);
            }
        }
        for (IOException e : ioExceptionCollector) {
            SystemUtils.LOG.log(Level.SEVERE, e.getMessage(), e);
        }
        if (ioExceptionCollector.size() > 0) {
            IOException ioException = ioExceptionCollector.get(0);
            throw ioException;
        }
    }

    private static void writeBandsSequentially(ProgressMonitor pm, ArrayList<Band> bandsToWrite) throws IOException {
        for (Band band : bandsToWrite) {
            if (pm.isCanceled()) {
                break;
            }
            pm.setSubTaskName("Writing band '" + band.getName() + "'");
            ProgressMonitor subPM = SubProgressMonitor.create(pm, 1);
            writeRasterDataFully(subPM, band, null, null, null);
        }
    }

    /**
     * Constructor. Private, in order to prevent instantiation.
     */
    private ProductGroupIO() {
    }

    private static void writeRasterDataFully(ProgressMonitor pm, Band band, ExecutorService executor, Semaphore semaphore, List<IOException> ioExceptionCollector) throws IOException {
        if (band.hasRasterData()) {
            band.writeRasterData(0, 0, band.getRasterWidth(), band.getRasterHeight(), band.getRasterData(), pm);
            if (semaphore != null) {
                semaphore.release();
            }
        } else {
            final PlanarImage sourceImage = band.getSourceImage();
            final Point[] tileIndices = sourceImage.getTileIndices(
                    new Rectangle(0, 0, sourceImage.getWidth(), sourceImage.getHeight()));
            int numTiles = tileIndices.length;
            pm.beginTask("Writing raster data...", numTiles);
            if (executor != null) {
//                Finisher finisher = new Finisher(band.getName(), pm, semaphore, executor, numTiles);
                Finisher finisher = new Finisher(pm, semaphore, executor, numTiles);
                for (Point tileIndex : tileIndices) {
                    executor.execute(() -> {
                        try {
                            if (pm.isCanceled()) {
                                return;
                            }
                            writeTile(sourceImage, tileIndex, band);
                        } catch (IOException e) {
                            ioExceptionCollector.add(e);
                            pm.setCanceled(true);
                        } finally {
                            finisher.worked();
                        }
                    });
                }
            } else {
                for (final Point tileIndex : tileIndices) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    writeTile(sourceImage, tileIndex, band);
                    pm.worked(1);
                }
            }
        }
    }

    private static void writeTile(PlanarImage sourceImage, Point tileIndex, Band band) throws IOException {
        final Rectangle rect = sourceImage.getTileRect(tileIndex.x, tileIndex.y);
        if (!rect.isEmpty()) {
            final Raster data = sourceImage.getData(rect);
            final ProductData rasterData = band.createCompatibleRasterData(rect.width, rect.height);
            data.getDataElements(rect.x, rect.y, rect.width, rect.height, rasterData.getElems());
            band.writeRasterData(rect.x, rect.y, rect.width, rect.height, rasterData, ProgressMonitor.NULL);
            rasterData.dispose();
        }
    }

    private static class Finisher {

        private final ProgressMonitor pm;
        private final Semaphore semaphore;
        private final ExecutorService executor;
        private final int work;
        private int counter;

        public Finisher(ProgressMonitor pm, Semaphore semaphore, ExecutorService executor, int counter) {
            this.pm = pm;
            this.semaphore = semaphore;
            this.executor = executor;
            this.work = counter;

        }

        public synchronized void worked() {
            try {
                pm.worked(1);
            } finally {
                counter++;
                if (counter == work) {
                    semaphore.release();
                    executor.shutdown();
                    pm.done();
                }
            }
        }
    }
}
