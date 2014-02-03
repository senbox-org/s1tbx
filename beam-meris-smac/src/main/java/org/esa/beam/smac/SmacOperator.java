/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.smac;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProcessorUtils;
import org.esa.beam.processor.smac.SensorCoefficientManager;
import org.esa.beam.processor.smac.SmacAlgorithm;
import org.esa.beam.processor.smac.SmacConstants;
import org.esa.beam.processor.smac.SmacUtils;
import org.esa.beam.util.ObjectUtils;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Thomas Storm
 */
public class SmacOperator extends Operator {

    private static final int _merisSzaIndex = 6;    // DELETE
    private static final int _merisSaaIndex = 7;    // DELETE
    private static final int _merisVzaIndex = 8;    // DELETE
    private static final int _merisVaaIndex = 9;    // DELETE
    private static final int _merisPressIndex = 12; // DELETE
    private static final int _merisElevIndex = 2;
    private static final int _merisO3Index = 13;    // DELETE
    private static final int _merisWvIndex = 14;    // DELETE
    private static final int _aatsrSzaIndex = 7;    // DELETE
    private static final int _aatsrSzaFwdIndex = 11;    // DELETE
    private static final int _aatsrSaaIndex = 9;    // DELETE
    private static final int _aatsrSaaFwdIndex = 13;    // DELETE
    private static final int _aatsrVzaIndex = 8;    // DELETE
    private static final int _aatsrVzaFwdIndex = 12;    // DELETE
    private static final int _aatsrVaaIndex = 10;   // DELETE
    private static final int _aatsrVaaFwdIndex = 14;    // DELETE

    private final List<Band> _inputBandList;
    private final SmacAlgorithm _algorithm;
    private final Logger _logger;
    private String _sensorType;
    private TiePointGrid _szaBand;

    private TiePointGrid _saaBand;
    private TiePointGrid _vzaBand;
    private TiePointGrid _vaaBand;
    private TiePointGrid _wvBand;
    private TiePointGrid _o3Band;
    private TiePointGrid _pressBand;
    private TiePointGrid _elevBand;

    private TiePointGrid _szaFwdBand;
    private TiePointGrid _saaFwdBand;
    private TiePointGrid _vzaFwdBand;
    private TiePointGrid _vaaFwdBand;

    @Parameter(description = "Aerosol optical depth", notNull = true)
    private Float _tau_aero_550;

    @Parameter(description = "Relative humidity")
    private Float _u_h2o;

    @Parameter(description = "Ozone content")
    private Float _u_o3;

    @Parameter(description = "Surface pressure")
    private Float _surf_press;

    @Parameter(description = "Use MERIS ADS", notNull = true)
    private Boolean _useMerisADS;

    @Parameter(description = "Aerosol type", notNull = true, valueSet = {SensorCoefficientManager.AER_CONT_NAME,
            SensorCoefficientManager.AER_DES_NAME})
    private String _aerosolType;

    @Parameter(description = "Default reflectance for invalid pixel", defaultValue = "0.0F")
    Float _invalidPixel;

    @Parameter(description = "Mask expression for the whole view (MERIS) or the nadir view (AATSR)", defaultValue = "")
    private String _bitMaskExpression;

    @Parameter(description = "Mask expression for the forward view (AATSR only)", defaultValue = "")
    private String _bitMaskExpressionForward;

    @Parameter(description = "bands", notNull = true)
    private String[] bandNames;

    @SourceProduct(alias = "source")
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;


    public SmacOperator() {
        _inputBandList = new ArrayList<>();
        _algorithm = new SmacAlgorithm();
        _bitMaskExpression = null;
        _logger = getLogger();
    }

    @Override
    public void initialize() throws OperatorException {
        prepareProcessing();
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

    }

    private void prepareProcessing() throws ProcessorException, IOException {
        _logger.info("Preparing SMAC processing");

        // create a vector of input bands
        // ------------------------------
        loadInputProduct();

        // create a bitmask expression for input
        // -------------------------------------
        createMask();

        installAuxdata();
        File smacAuxDir = getAuxdataInstallDir();

        String auxPathString = smacAuxDir.toString();
        try {
            _coeffMgr = new SensorCoefficientManager(smacAuxDir.toURI().toURL());
            _logger.fine(SmacConstants.LOG_MSG_AUX_DIR + auxPathString);
        } catch (IOException e) {
            _logger.severe(SmacConstants.LOG_MSG_AUX_ERROR + auxPathString);
            _logger.severe(e.getMessage());
            _logger.log(Level.FINE, e.getMessage(), e);
        }
    }

    private void loadInputProduct() throws IOException, ProcessorException {
        // check what product type the input is and load the appropriate tie point ADS
        // ---------------------------------------------------------------------------
        _sensorType = SmacUtils.getSensorType(sourceProduct.getProductType());
        if (ObjectUtils.equalObjects(_sensorType, SensorCoefficientManager.MERIS_NAME)) {
            loadMERIS_ADS(sourceProduct);
        } else if (ObjectUtils.equalObjects(_sensorType, SensorCoefficientManager.AATSR_NAME)) {
            loadAATSR_ADS(sourceProduct);
            _useMerisADS = false;
        } else {
            throw new ProcessorException(SmacConstants.LOG_MSG_UNSUPPORTED_SENSOR);
        }

        // set up the bands we need for this request
        // -----------------------------------------
        if (bandNames.length == 0) {
            throw new ProcessorException(SmacConstants.LOG_MSG_NO_INPUT_BANDS);
        }

        for (String bandName : bandNames) {
            Band band = sourceProduct.getBand(bandName);
            if (band == null) {
                _logger.warning("The requested band '" + bandName + "' is not contained in the input product!");
            } else {
                if (band.getSpectralBandIndex() != -1) {
                    _inputBandList.add(band);
                } else {
                    _logger.warning(
                            "The requested band '" + bandName + "' is not a spectral band and will be excluded from processing");
                }
            }
        }
    }

    private void loadMERIS_ADS(Product product) throws ProcessorException {
        _logger.info(SmacConstants.LOG_MSG_LOAD_MERIS_ADS);

        // sun zenith angle
        _szaBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisSzaIndex]);
        Assert.notNull(_szaBand);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisSzaIndex]);

        // sun azimuth angle
        _saaBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisSaaIndex]);
        Assert.notNull(_saaBand);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisSaaIndex]);

        // view zenith angle
        _vzaBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisVzaIndex]);
        Assert.notNull(_vzaBand);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisVzaIndex]);

        // view azimuth angle
        _vaaBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisVaaIndex]);
        Assert.notNull(_vaaBand);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisVaaIndex]);

        // if requested load the optional MERIS ADS
        // ----------------------------------------
        if (_useMerisADS) {
            // waterVapour
            _wvBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisWvIndex]);
            Assert.notNull(_wvBand);
            _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisWvIndex]);

            // ozone
            _o3Band = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisO3Index]);
            Assert.notNull(_o3Band);
            _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisO3Index]);

            // atmospheric pressure
            _pressBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisPressIndex]);
            Assert.notNull(_pressBand);
            _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisPressIndex]);

            // digital elevation
            _elevBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisElevIndex]);
            Assert.notNull(_elevBand);
            _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisElevIndex]);
        }
        _logger.info(ProcessorConstants.LOG_MSG_SUCCESS);
    }

    // Loads the AATSR ADS needed.
    private void loadAATSR_ADS(Product product) throws ProcessorException {

        _logger.info(SmacConstants.LOG_MSG_LOAD_AATSR_ADS);

        // sun elevation angle nadir
        _szaBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSzaIndex]);
        Assert.notNull(_szaBand);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSzaIndex]);

        // sun elevation angle forward
        _szaFwdBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSzaFwdIndex]);
        Assert.notNull(_szaFwdBand);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSzaFwdIndex]);

        // sun azimuth angle nadir
        _saaBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSaaIndex]);
        Assert.notNull(_saaBand);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSaaIndex]);

        // sun azimuth angle forward
        _saaFwdBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSaaFwdIndex]);
        Assert.notNull(_saaFwdBand);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSaaFwdIndex]);

        // view elevation angle nadir
        _vzaBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVzaIndex]);
        Assert.notNull(_vzaBand);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVzaIndex]);

        // view elevation angle forward
        _vzaFwdBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVzaFwdIndex]);
        Assert.notNull(_vzaFwdBand);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVzaFwdIndex]);

        // view azimuth angle nadir
        _vaaBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVaaIndex]);
        Assert.notNull(_vaaBand);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVaaIndex]);

        // view azimuth angle forward
        _vaaFwdBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVaaFwdIndex]);
        Assert.notNull(_vaaFwdBand);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVaaFwdIndex]);

        _logger.info(ProcessorConstants.LOG_MSG_SUCCESS);
    }

    /**
     * Replaces createBitmaskTerm()
     */
    private void createMask() {
        if (ObjectUtils.equalObjects(_sensorType, SensorCoefficientManager.MERIS_NAME)) {
            createMerisBitmaskTerm();
        } else {
            createAatsrBitmaskTerm();
        }
    }

        // Creates an AATSR bitmask term given the bitmask expression from the request. If no expression is set, it uses the
    // default expression
    private void createAatsrBitmaskTerm() throws ProcessorException {
        if ("".equalsIgnoreCase(_bitMaskExpression)) {
            sourceProduct.addMask("smac_mask", SmacConstants.DEFAULT_NADIR_FLAGS_VALUE, "", Color.BLACK, 0.0);
            sourceProduct.addMask("smac_mask_forward", SmacConstants.DEFAULT_FORWARD_FLAGS_VALUE, "", Color.BLACK, 0.0);

            _logger.warning(SmacConstants.LOG_MSG_NO_BITMASK);
            _logger.warning(SmacConstants.LOG_MSG_DEFAULT_NADIR_BITMASK + SmacConstants.DEFAULT_NADIR_FLAGS_VALUE);
            _logger.warning(SmacConstants.LOG_MSG_DEFAULT_FORWARD_BITMASK + SmacConstants.DEFAULT_FORWARD_FLAGS_VALUE);
        } else {
            _bitMaskTerm = ProcessorUtils.createTerm(_bitMaskExpression, _inputProduct);
            _bitMaskTermForward = ProcessorUtils.createTerm(_bitMaskExpressionForward, _inputProduct);

            _logger.info(SmacConstants.LOG_MSG_NADIR_BITMASK + _bitMaskExpression);
            _logger.info(SmacConstants.LOG_MSG_FORWARD_BITMASK + _bitMaskExpressionForward);
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SmacOperator.class);
        }

    }

}
