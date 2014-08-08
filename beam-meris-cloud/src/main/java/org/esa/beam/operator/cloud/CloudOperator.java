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
package org.esa.beam.operator.cloud;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.jexp.ParseException;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.ResourceInstaller;
import org.esa.beam.util.SystemUtils;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The <code>CloudProcessor</code> implements all specific functionality to calculate a cloud probability.
 *
 */
@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@OperatorMetadata(alias = "CloudProb",
                  category = "Optical Processing/Pre-Processing",
                  version = "1.7",
                  authors = "Rene Preusker (Algorithm), Tom Block (BEAM Implementation), Thomas Storm (GPF conversion)",
                  copyright = "Copyright (C) 2004-2014 by ESA, FUB and Brockmann Consult",
                  description = "Applies a clear sky conservative cloud detection algorithm.")
public class CloudOperator extends Operator {

    public static final String AUXDATA_DIR = "beam-meris-cloud/auxdata";

    @SourceProduct(alias = "source", label = "Source product", description="The MERIS Level 1b source product.")
    private Product l1bProduct;

    @TargetProduct(label = "Cloud product")
    private Product targetProduct;

    private CloudPN cloudNode;
    private Product tempCloudProduct;

    @Override
    public void initialize()  {
        setLogger(Logger.getLogger(CloudConstants.LOGGER_NAME));
        getLogger().info("Starting request...");
        initCloudNode();
        try {
            initOutputProduct();
        } catch (IOException | ParseException e) {
            throw new OperatorException("Unable to initialise output product.", e);
        }
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        for (Map.Entry<Band, Tile> entry : targetTiles.entrySet()) {
            checkForCancellation();
            Band targetBand = entry.getKey();
            Tile targetTile = entry.getValue();
            Band sourceBand = tempCloudProduct.getBand(targetBand.getName());
            Rectangle targetRect = targetTile.getRectangle();
            ProductData rawSamples = getSourceTile(sourceBand, targetRect).getRawSamples();
            targetTile.setRawSamples(rawSamples);
        }
    }

    private void initCloudNode() {
        try {
            installAuxdata();
        } catch (IOException e) {
            throw new OperatorException("Unable to install auxiliary data.", e);
        }

        final Map<String, String> cloudConfig = new HashMap<>();
        cloudConfig.put(CloudPN.CONFIG_FILE_NAME, "cloud_config.txt");
        cloudConfig.put(CloudPN.INVALID_EXPRESSION, "l1_flags.INVALID");
        cloudNode = new CloudPN(getAuxdataInstallationPath());
        try {
            cloudNode.setUp(cloudConfig);
        } catch (IOException e) {
            throw new OperatorException("Failed to initialise cloud source: " + e.getMessage(), e);
        }
    }

    // package local for testing purposes
    void installAuxdata() throws IOException {
        String auxdataDirPath = getAuxdataInstallationPath();
        installAuxdata(ResourceInstaller.getSourceUrl(getClass()), "auxdata/", new File(auxdataDirPath));
    }

    // package local for testing purposes
    String getAuxdataInstallationPath() {
        File defaultAuxdataDir = new File(SystemUtils.getApplicationDataDir(), AUXDATA_DIR);
        return System.getProperty(CloudPN.CLOUD_AUXDATA_DIR_PROPERTY, defaultAuxdataDir.getAbsolutePath());
    }

    private void installAuxdata(URL sourceLocation, String sourceRelPath, File auxdataInstallDir) throws IOException {
        new ResourceInstaller(sourceLocation, sourceRelPath, auxdataInstallDir)
                .install(".*", ProgressMonitor.NULL);
    }

    /**
     * Creates the output product skeleton.
     */
    private void initOutputProduct() throws IOException, ParseException {
        if (!EnvisatConstants.MERIS_L1_TYPE_PATTERN.matcher(l1bProduct.getProductType()).matches()) {
            throw new OperatorException("Product type '" + l1bProduct.getProductType() + "' is not supported." +
                                                 "It must be a MERIS Level 1b product.");
        }
        tempCloudProduct = cloudNode.readProductNodes(l1bProduct, null);
        targetProduct = cloudNode.createTargetProductImpl();
        
        ProductUtils.copyFlagBands(l1bProduct, targetProduct, true);
        ProductUtils.copyTiePointGrids(l1bProduct, targetProduct);

        ProductUtils.copyGeoCoding(l1bProduct, targetProduct);
        ProductUtils.copyMetadata(l1bProduct, targetProduct);
        targetProduct.setStartTime(l1bProduct.getStartTime());
        targetProduct.setEndTime(l1bProduct.getEndTime());

        cloudNode.startProcessing();

        getLogger().info("Output product successfully initialised");
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(CloudOperator.class);
        }

    }

}
