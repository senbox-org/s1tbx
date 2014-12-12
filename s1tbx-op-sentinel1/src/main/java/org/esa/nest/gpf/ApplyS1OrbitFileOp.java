/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.dataio.orbits.*;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.OrbitStateVector;
import org.esa.snap.datamodel.Orbits;
import org.esa.snap.gpf.InputProductValidator;
import org.esa.snap.gpf.OperatorUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.text.ParseException;

/**
 * Apply Sentinel-1 orbit file to given Sentinel-1 product.
 */

@OperatorMetadata(alias = "Apply-S1-Orbit-File",
        category = "SAR Processing/SENTINEL-1",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Apply Sentinel-1 orbit file")
public final class ApplyS1OrbitFileOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(label = "Sentinel-1 Orbit File", defaultValue = "E:\\data\\S-1\\Sentinel-1_Orbits\\")
    private File orbitFileFolder;

    @Parameter(label = "Orbit Type", valueSet = {RESTITUTED, PRECISE}, defaultValue = PRECISE)
    private String orbitType = PRECISE;

    @Parameter(label = "Polynomial Degree", defaultValue = "3")
    private int polyDegree = 3;

    private File orbitFile = null;

    private MetadataElement absRoot = null;
    private SentinelPODOrbitFile podOrbitFile = null;

    private final static String RESTITUTED = "Restituded";
    private final static String PRECISE = "Precise";

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public ApplyS1OrbitFileOp() {
    }

    void setOrbitFileFolder(final File orbitFileFolder) {
        this.orbitFileFolder = orbitFileFolder;
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfSentinel1Product();

            absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

            orbitFile = findOrbitFile();
            if(orbitFile == null) {
                String timeStr = absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME).format();
                throw new OperatorException("No valid orbit file found for "+timeStr);
            }

            podOrbitFile = new SentinelPODOrbitFile(orbitFile);

            checkOrbitFileValidity();

            createTargetProduct();

            updateOrbitStateVectors();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private File findOrbitFile() {
        final String prefix;
        if(orbitType.endsWith(RESTITUTED)) {
            prefix = "S1A_OPER_AUX_RESORB";
        } else {
            prefix = "S1A_OPER_AUX_POEORB";
        }

        final File[] files = orbitFileFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                name = name.toUpperCase();
                return name.endsWith(".EOF") && name.startsWith(prefix);
            }
        });
        if(files == null || files.length == 0)
            return null;

        final double stateVectorTime = absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME).getMJD();
        for(File file : files) {
            try {
                final String filename = file.getName();
                final ProductData.UTC utcStart = SentinelPODOrbitFile.getValidityStartFromFilenameUTC(filename);
                final ProductData.UTC utcEnd = SentinelPODOrbitFile.getValidityStopFromFilenameUTC(filename);
                if(utcStart != null && utcEnd != null) {
                    if(stateVectorTime >= utcStart.getMJD() && stateVectorTime < utcEnd.getMJD()) {
                        return file;
                    }
                }
            } catch (ParseException ignored) {
            }
        }
        return null;
    }

    /**
     * Check if product acquisition time is within the validity period of the orbit file.
     * @throws Exception
     */
    private void checkOrbitFileValidity() throws Exception {

        final double stateVectorTime = absRoot.getAttributeUTC(AbstractMetadata.STATE_VECTOR_TIME).getMJD();
        final String validityStartTimeStr = podOrbitFile.getValidityStartFromHeader();
        final String validityStopTimeStr = podOrbitFile.getValidityStopFromHeader();
        final double validityStartTimeMJD = SentinelPODOrbitFile.toUTC(validityStartTimeStr).getMJD();
        final double validityStopTimeMJD = SentinelPODOrbitFile.toUTC(validityStopTimeStr).getMJD();

        if (stateVectorTime < validityStartTimeMJD || stateVectorTime > validityStopTimeMJD) {
            throw new OperatorException("Product acquisition time is not within the validity period of the orbit");
        }
    }

    /**
     * Create target product.
     */
    void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        for (Band srcBand : sourceProduct.getBands()) {
            if (srcBand instanceof VirtualBand) {
                OperatorUtils.copyVirtualBand(targetProduct, (VirtualBand) srcBand, srcBand.getName());
            } else {
                ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, true);
            }
        }
    }

    /**
     * Update orbit state vectors using data from the orbit file.
     *
     * @throws Exception The exceptions.
     */
    private void updateOrbitStateVectors() throws Exception {

        final MetadataElement tgtAbsRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);
        final OrbitStateVector[] orbitStateVectors = AbstractMetadata.getOrbitStateVectors(tgtAbsRoot);

        for (OrbitStateVector orbitStateVector : orbitStateVectors) {
            final double time = orbitStateVector.time_mjd;
            final Orbits.OrbitData orbitData = podOrbitFile.getOrbitData(time, polyDegree);
            orbitStateVector.x_pos = orbitData.xPos;
            orbitStateVector.y_pos = orbitData.yPos;
            orbitStateVector.z_pos = orbitData.zPos;
            orbitStateVector.x_vel = orbitData.xVel;
            orbitStateVector.y_vel = orbitData.yVel;
            orbitStateVector.z_vel = orbitData.zVel;
        }

        AbstractMetadata.setOrbitStateVectors(tgtAbsRoot, orbitStateVectors);
        tgtAbsRoot.setAttributeString(AbstractMetadata.orbit_state_vector_file, orbitFile.getName());
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs during computation of the target raster.
     */
   /* @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final Tile srcTile = getSourceTile(targetBand, targetTile.getRectangle());
        targetTile.setRawSamples(srcTile.getRawSamples());
    }*/

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(ApplyS1OrbitFileOp.class);
        }
    }
}