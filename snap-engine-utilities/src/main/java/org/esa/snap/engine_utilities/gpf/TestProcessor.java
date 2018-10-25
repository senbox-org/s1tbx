/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.engine_utilities.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.dataio.ProductSubsetBuilder;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.common.WriteOp;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.util.ProductFunctions;
import org.esa.snap.engine_utilities.util.TestUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Utilities for Operator unit tests
 * In order to test the datasets at Array, set the following to true in the nest.config
 * nest.test.ReadersOnAllProducts=true nest.test.ProcessingOnAllProducts=true
 */
public class TestProcessor {

    private final int subsetX;
    private final int subsetY;
    private final int subsetWidth;
    private final int subsetHeight;

    private final int maxIteration;

    private final boolean canTestReadersOnAllProducts;
    private final boolean canTestProcessingOnAllProducts;

    public TestProcessor() {
        this(100,100,100,100, 1, false, false);
    }

    public TestProcessor(final int subsetX, final int subsetY, final int subsetWidth, final int subsetHeight,
                         final int maxIteration, final boolean canTestReaders, final boolean canTestProcessing) {
        this.subsetX = subsetX;
        this.subsetY = subsetY;
        this.subsetWidth = subsetWidth;
        this.subsetHeight = subsetHeight;
        this.maxIteration = maxIteration;
        this.canTestReadersOnAllProducts = canTestReaders;
        this.canTestProcessingOnAllProducts = canTestProcessing;
    }

    public static void executeOperator(final Operator op) throws Exception {
        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        // readPixels: execute computeTiles()
        TestUtils.verifyProduct(targetProduct, true, true, true);
    }

    public Product createSubsetProduct(final Product sourceProduct) throws IOException {
        final int bandWidth = sourceProduct.getSceneRasterWidth();
        final int bandHeight = sourceProduct.getSceneRasterHeight();

        final ProductSubsetBuilder subsetReader = new ProductSubsetBuilder();
        final ProductSubsetDef subsetDef = new ProductSubsetDef();

        subsetDef.addNodeNames(sourceProduct.getTiePointGridNames());
        subsetDef.addNodeNames(sourceProduct.getBandNames());
        final int w = within(subsetWidth, bandWidth);
        final int h = within(subsetHeight, bandHeight);
        subsetDef.setRegion(within(subsetX, bandWidth - w), within(subsetY, bandHeight - h), w, h);
        subsetDef.setIgnoreMetadata(false);
        subsetDef.setTreatVirtualBandsAsRealBands(false);

        final Product subsetProduct = subsetReader.readProductNodes(sourceProduct, subsetDef);
        subsetProduct.setFileLocation(sourceProduct.getFileLocation());
        if (subsetProduct.getSceneRasterWidth() > subsetWidth || subsetProduct.getSceneRasterHeight() > subsetHeight) {
            throw new IOException("product size mismatch");
        }

        return subsetProduct;
    }

    public Product writeSubsetProduct(final Product sourceProduct) throws IOException {
        final int bandWidth = sourceProduct.getSceneRasterWidth();
        final int bandHeight = sourceProduct.getSceneRasterHeight();

        final ProductSubsetBuilder subsetReader = new ProductSubsetBuilder();
        final ProductSubsetDef subsetDef = new ProductSubsetDef();

        subsetDef.addNodeNames(sourceProduct.getTiePointGridNames());

        final String bandName = ProductUtils.findSuitableQuicklookBandName(sourceProduct);
        subsetDef.addNodeNames(new String[]{bandName});
        final int w = within(subsetWidth, bandWidth);
        final int h = within(subsetHeight, bandHeight);
        subsetDef.setRegion(within(subsetX, bandWidth - w), within(subsetY, bandHeight - h), w, h);
        subsetDef.setIgnoreMetadata(false);
        subsetDef.setTreatVirtualBandsAsRealBands(true);

        final Product subsetProduct = subsetReader.readProductNodes(sourceProduct, subsetDef);
        final File tmpFile = new File(SystemUtils.getCacheDir(), "tmp_subset.dim");
        final WriteOp writer = new WriteOp(subsetProduct, tmpFile, DimapProductConstants.DIMAP_FORMAT_NAME);
        writer.writeProduct(ProgressMonitor.NULL);

        return ProductIO.readProduct(tmpFile);
    }

    private static int within(final int val, final int max) {
        return Math.max(0, Math.min(val, max));
    }

    public static void recurseFindReadableProducts(final File origFolder, final ArrayList<File> productList, int maxCount) throws Exception {

        final File[] folderList = origFolder.listFiles(ProductFunctions.directoryFileFilter);
        if(folderList != null) {
            for (File folder : folderList) {
                if (!folder.getName().contains(TestUtils.SKIPTEST)) {
                    recurseFindReadableProducts(folder, productList, maxCount);
                }
            }
        }

        final File[] fileList = origFolder.listFiles(new ProductFunctions.ValidProductFileFilter());
        if(fileList != null) {
            for (File file : fileList) {
                if (maxCount > 0 && productList.size() >= maxCount)
                    return;

                try {
                    final ProductReader reader = ProductIO.getProductReaderForInput(file);
                    if (reader != null) {
                        productList.add(file);
                    } else {
                        SystemUtils.LOG.warning(file.getAbsolutePath() + " is non valid");
                    }
                } catch (Exception e) {
                    boolean ok = false;
               /* if(exceptionExemptions != null) {
                    for(String exemption : exceptionExemptions) {
                        if(e.getMessage().contains(exemption)) {
                            ok = true;
                            SystemUtils.LOG("Exemption for "+e.getMessage());
                            break;
                        }
                    }
                }    */
                    if (!ok) {
                        SystemUtils.LOG.severe("Failed to process " + file.toString());
                        throw e;
                    }
                }
            }
        }
    }

    private int recurseProcessFolder(final OperatorSpi spi, final File origFolder,
                                            final String format, int iterations,
                                            final String[] productTypeExemptions,
                                            final String[] exceptionExemptions) throws Exception {

        final File[] folderList = origFolder.listFiles(ProductFunctions.directoryFileFilter);
        if(folderList != null) {
            for (File folder : folderList) {
                if (maxIteration > 0 && iterations >= maxIteration)
                    break;
                if (!folder.getName().contains(TestUtils.SKIPTEST)) {
                    iterations = recurseProcessFolder(spi, folder, format, iterations, productTypeExemptions, exceptionExemptions);
                }
            }
        }

        ProductReader reader = null;
        if(format != null) {
            reader = ProductIO.getProductReader(format);
        }

        final File[] fileList = origFolder.listFiles(new ProductFunctions.ValidProductFileFilter());
        if(fileList != null) {
            for (File file : fileList) {
                if (maxIteration > 0 && iterations >= maxIteration)
                    break;

                try {
                    if (reader != null) {
                        if (reader.getReaderPlugIn().getDecodeQualification(file) != DecodeQualification.INTENDED) {
                            continue;
                        }
                    } else {
                        reader = CommonReaders.findCommonProductReader(file);
                        if (reader == null)
                            reader = ProductIO.getProductReaderForInput(file);
                    }
                    if (reader != null) {
                        final Product sourceProduct = reader.readProductNodes(file, null);
                        if (productTypeExemptions != null && containsProductType(productTypeExemptions, sourceProduct.getProductType()))
                            continue;

                        TestUtils.verifyProduct(sourceProduct, true, true, false);

                        final Product subsetProduct = createSubsetProduct(sourceProduct);

                        final Operator op = spi.createOperator();
                        op.setSourceProduct(subsetProduct);

                        SystemUtils.LOG.info(spi.getOperatorAlias() + " Processing [" + iterations + "] " + file.toString());
                        executeOperator(op);

                        SystemUtils.freeAllMemory();

                        ++iterations;
                    } else {
                        SystemUtils.LOG.warning(file.getAbsolutePath() + " is non valid");
                    }
                } catch (Exception e) {
                    boolean ok = false;
                    if (exceptionExemptions != null) {
                        for (String exemption : exceptionExemptions) {
                            if (e.getMessage() != null && e.getMessage().contains(exemption)) {
                                ok = true;
                                SystemUtils.LOG.info("Exemption for " + e.getMessage());
                                break;
                            }
                        }
                    }
                    if (!ok) {
                        SystemUtils.LOG.severe("Failed to process " + file.toString());
                        throw e;
                    }
                }
            }
        }
        return iterations;
    }

    public static boolean containsProductType(final String[] productTypeExemptions, final String productType) {
        if (productTypeExemptions != null) {
            for (String str : productTypeExemptions) {
                if (productType.contains(str)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Processes all products in a folder
     *
     * @param spi                   the OperatorSpi to create the operator
     * @param folderPaths           list of paths to recurse through
     * @param productTypeExemptions product types to ignore
     * @param exceptionExemptions   exceptions that are ok and can be ignored for the test
     * @throws Exception general exception
     */
    public void testProcessAllInPath(final OperatorSpi spi, final File[] folderPaths,
                                            final String[] productTypeExemptions,
                                            final String[] exceptionExemptions) throws Exception {
        testProcessAllInPath(spi, folderPaths, null, productTypeExemptions, exceptionExemptions);
    }

    /**
     * Processes all products in a folder
     *
     * @param spi                   the OperatorSpi to create the operator
     * @param folderPaths           list of paths to recurse through
     * @param format                only use a reader for this format
     * @param productTypeExemptions product types to ignore
     * @param exceptionExemptions   exceptions that are ok and can be ignored for the test
     * @throws Exception general exception
     */
    public void testProcessAllInPath(final OperatorSpi spi, final File[] folderPaths,
                                            final String format,
                                            final String[] productTypeExemptions,
                                            final String[] exceptionExemptions) throws Exception {
        if (canTestProcessingOnAllProducts) {
            for (File folder : folderPaths) {
                if (!folder.exists()) {
                    TestUtils.skipTest(spi, folder + " not found");
                    continue;
                }

                int iterations = 0;
                recurseProcessFolder(spi, folder, format, iterations, productTypeExemptions, exceptionExemptions);
            }
        }
    }

    private final static ProductFunctions.ValidProductFileFilter fileFilter = new ProductFunctions.ValidProductFileFilter(false);

    public void recurseReadFolder(final Object callingClass, final File[] folderPaths,
                                         final ProductReaderPlugIn readerPlugin,
                                         final ProductReader reader,
                                         final String[] productTypeExemptions,
                                         final String[] exceptionExemptions) throws Exception {
        if (!canTestReadersOnAllProducts)
            return;
        for (File folderPath : folderPaths) {
            if (!folderPath.exists()) {
                TestUtils.skipTest(callingClass, "Folder " + folderPath + " not found");
                continue;
            }
            recurseReadFolder(folderPath, readerPlugin, reader, productTypeExemptions, exceptionExemptions, 0);
        }
    }

    private int recurseReadFolder(final File origFolder,
                                         final ProductReaderPlugIn readerPlugin,
                                         final ProductReader reader,
                                         final String[] productTypeExemptions,
                                         final String[] exceptionExemptions,
                                         int iterations) throws Exception {
        final File[] folderList = origFolder.listFiles(ProductFunctions.directoryFileFilter);
        if(folderList != null) {
            for (File folder : folderList) {
                if (!folder.getName().contains(TestUtils.SKIPTEST)) {
                    iterations = recurseReadFolder(folder, readerPlugin, reader, productTypeExemptions, exceptionExemptions, iterations);
                    if (maxIteration > 0 && iterations >= maxIteration)
                        return iterations;
                }
            }
        }

        final File[] files = origFolder.listFiles(fileFilter);
        if(files != null) {
            for (File file : files) {
                if (readerPlugin.getDecodeQualification(file) == DecodeQualification.INTENDED) {

                    try {
                        SystemUtils.LOG.info("Reading [" + iterations + "] " + file.toString());

                        final Product product = reader.readProductNodes(file, null);
                        if (productTypeExemptions != null && containsProductType(productTypeExemptions, product.getProductType()))
                            continue;
                        TestUtils.verifyProduct(product, true, true, false);
                        ++iterations;

                        if (maxIteration > 0 && iterations >= maxIteration)
                            break;
                    } catch (Exception e) {
                        boolean ok = false;
                        if (exceptionExemptions != null) {
                            for (String exemption : exceptionExemptions) {
                                if (e.getMessage() != null && e.getMessage().contains(exemption)) {
                                    ok = true;
                                    SystemUtils.LOG.info("Exemption for " + e.getMessage());
                                    break;
                                }
                            }
                        }
                        if (!ok) {
                            SystemUtils.LOG.severe("Failed to read " + file.toString());
                            throw e;
                        }
                    }
                }
            }
        }
        return iterations;
    }
}
