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
package org.esa.beam.processor.smac;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.IllegalProcessorStateException;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.util.logging.Logger;

/*
 * @deprecated since BEAM 4.11. No replacement.
 */
@Deprecated
public abstract class SmacAbstractProcessor {

    private Product _inputProduct;
    private Vector _inputBands;
    private Product _outputProduct;
    private Request _request;
    private float _tau_aero_550;
    private float _u_h2o;
    private float _u_o3;
    private float _surf_press;
    private float _invalidPixel;
    private String _aerosolType;

    private TiePointGrid _szaGrid;
    private TiePointGrid _saaGrid;
    private TiePointGrid _vzaGrid;
    private TiePointGrid _vaaGrid;

    private Logger _logger;

    /**
     * Constructs the object with default parameters.
     */
    public SmacAbstractProcessor() {
        _inputProduct = null;
        _request = null;
        _szaGrid = null;
        _saaGrid = null;
        _aerosolType = null;

        _inputBands = new Vector();

        _tau_aero_550 = 0.f;
        _u_h2o = 0.f;
        _u_o3 = 0.f;
        _surf_press = 0.f;
        _invalidPixel = 0.f;

        _logger = Logger.getLogger(SmacConstants.LOGGER_NAME);
    }

    /**
     * Sets the input product and the request for the current processing.
     *
     * @param input the input product to be processed
     */
    public void setInputProductAndRequest(Product input, String[] inputBandNames, Request request) {
        Guardian.assertNotNull("input product", input);
        Guardian.assertNotNull("input band names", inputBandNames);
        Guardian.assertNotNull("request", request);

        _inputProduct = input;
        _request = request;

        // extract the bands to process
        setUpInputBands(inputBandNames);
    }


    /**
     * Loads the processor specific annotation and tiepoint datasets
     */
    abstract public void loadTiePointsAndADS() throws IllegalProcessorStateException;

    /**
     * Scans the request for processor specific parameter values.
     */
    abstract public void loadRequestParameter() throws IllegalProcessorStateException,
                                                       ProcessorException;

    /**
     * Creates the output product as specified by the request
     */
    public void createOuputProduct() throws IllegalProcessorStateException,
                                            IOException,
                                            ProcessorException {
        Request request = getRequestSafe();
        ProductRef prod;
        ProductWriter writer;

        // take only the first output product. There might be more but we will ignore
        // these in SMAC.
        prod = request.getOutputProductAt(0);
        checkValueNotNull(prod, "output product");

        String productType = getInputProductSafe().getProductType() + "_SMAC";
        /* @todo 3 tb/tb - der Productname des Inputproductes soll zerlegt werden und namensrelevante
         *  Teile davon wiederverwertet */
        String productName = getInputProductSafe().getName() + "_SMAC";
        int sceneWith = getInputProductSafe().getSceneRasterWidth();
        int sceneHeight = getInputProductSafe().getSceneRasterHeight();

        _outputProduct = new Product(productName, productType, sceneWith, sceneHeight);
        /* @todo 3 tb/tb - der Übergebene Name "ENVI" eigentlich aus dem request */
        writer = ProductIO.getProductWriter("BEAM-DIMAP");
//        writer = ProductIO.getProductWriter("ENVI");
        _outputProduct.setProductWriter(writer);

        // call derived classes to add their specific bands
        addBandsToOutput();
        // call derived classes to copy their specific tie point datasets
        copyTiePointDatasets();

        // and initialize the disk represenation
        writer.writeProductNodes(_outputProduct, new File(prod.getFilePath()));
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Adds the needed bands to the output product. Must be overwritten by derived classes. This method is called from
     * the base class (this) during the output product construction process.
     */
    abstract protected void addBandsToOutput() throws IllegalProcessorStateException,
                                                      ProcessorException;

    /**
     * Copies the relevant tie point datasets from the input product to the output product. This method is called from
     * thebase class during the output product construction process.
     */
    abstract protected void copyTiePointDatasets() throws IllegalProcessorStateException,
                                                          IOException,
                                                          ProcessorException;

    /**
     * Loads the request parameter which are comon to all processor types. These are currently <ul> <li>aerosol optical
     * depth <li>aerosol type and <li>invalid pixel value </ul>
     */
    protected void loadRequestBaseParameter() throws IllegalProcessorStateException,
                                                     ProcessorException {
        Request request = getRequestSafe();
        Parameter param;

        // get aerosol optical depth
        param = request.getParameter(SmacConstants.AEROSOL_OPTICAL_DEPTH_PARAM_NAME);
        checkValueNotNull(param, SmacConstants.AEROSOL_OPTICAL_DEPTH_PARAM_NAME);
        setTauAero550(((Float) param.getValue()).floatValue());
        logInfo("aerosol optical depth = " + getTauAero550());

        // get aerosol type
        param = request.getParameter(SmacConstants.AEROSOL_TYPE_PARAM_NAME);
        checkValueNotNull(param, SmacConstants.AEROSOL_TYPE_PARAM_NAME);
        setAerosolType((String) param.getValue());
        logInfo("aerosol type = " + getAerosolType());

        // get invalid pixel value
        param = request.getParameter(SmacConstants.DEFAULT_REFLECT_FOR_INVALID_PIX_PARAM_NAME);
        if (param == null) {
            logWarning("no value for invalid pixels set - using 0.0");
            setInvalidPixelValue(0.f);
        } else {
            setInvalidPixelValue(((Float) param.getValue()).floatValue());
            logInfo("invalid pixel value = " + getInvalidPixelValue());
        }
    }

    /**
     * Retrieves a vector of <code>RsBands</code> which are to be processed
     */
    protected Vector getInputBands() {
        return _inputBands;
    }


    /**
     * Sets the sun zenith angle tie point dataset to be used.
     *
     * @throws java.lang.IllegalArgumentException
     *          on null argument
     */
    protected void setSZAGrid(TiePointGrid sza) {
        Guardian.assertNotNull("sun zenith angle tie point grid", sza);
        _szaGrid = sza;
    }

    /**
     * Returns the sun zenith angle tie point grid currently set.
     *
     * @throws org.esa.beam.framework.processor.IllegalProcessorStateException
     *          when no sza grid is set
     */
    protected TiePointGrid getSZAGridSafe() throws IllegalProcessorStateException {
        if (_szaGrid == null) {
            throw new IllegalProcessorStateException("no sza grid set");
        }

        return _szaGrid;
    }

    /**
     * Sets the sun azimuth angle tie point dataset to be used.
     *
     * @throws java.lang.IllegalArgumentException
     *          on null argument
     */
    protected void setSAAGrid(TiePointGrid saa) {
        Guardian.assertNotNull("sun azimuth angle tie point grid", saa);
        _saaGrid = saa;
    }

    /**
     * Returns the sun azimuth angle tie point grid currently set.
     *
     * @throws org.esa.beam.framework.processor.IllegalProcessorStateException
     *          when no sza grid is set
     */
    protected TiePointGrid getSAAGridSafe() throws IllegalProcessorStateException {
        if (_saaGrid == null) {
            throw new IllegalProcessorStateException("no saa grid set");
        }

        return _saaGrid;
    }

    /**
     * Sets the view zenith angle tie point dataset to be used.
     *
     * @throws java.lang.IllegalArgumentException
     *          on null argument
     */
    protected void setVZAGrid(TiePointGrid vza) {
        Guardian.assertNotNull("view zenith angle tie point grid", vza);
        _vzaGrid = vza;
    }

    /**
     * Returns the view zenith angle tie point grid currently set.
     *
     * @throws org.esa.beam.framework.processor.IllegalProcessorStateException
     *          when no sza grid is set
     */
    protected TiePointGrid getVZAGridSafe() throws IllegalProcessorStateException {
        if (_vzaGrid == null) {
            throw new IllegalProcessorStateException("no vza grid set");
        }

        return _vzaGrid;
    }

    /**
     * Sets the view azimuth angle tie point dataset to be used.
     *
     * @throws java.lang.IllegalArgumentException
     *          on null argument
     */
    protected void setVAAGrid(TiePointGrid vaa) {
        Guardian.assertNotNull("view azimuth angle tie point grid", vaa);
        _vaaGrid = vaa;
    }

    /**
     * Returns the view azimuth angle tie point grid currently set.
     *
     * @throws org.esa.beam.framework.processor.IllegalProcessorStateException
     *          when no sza grid is set
     */
    protected TiePointGrid getVAAGridSafe() throws IllegalProcessorStateException {
        if (_vaaGrid == null) {
            throw new IllegalProcessorStateException("no vaa grid set");
        }

        return _vaaGrid;
    }

    /**
     * Returns the input product currently set.
     *
     * @throws org.esa.beam.framework.processor.IllegalProcessorStateException
     *          when no input product is set
     */
    protected Product getInputProductSafe() throws IllegalProcessorStateException {
        if (_inputProduct == null) {
            throw new IllegalProcessorStateException("no input product set");
        }

        return _inputProduct;
    }

    /**
     * Returns the request currently set.
     *
     * @throws org.esa.beam.framework.processor.IllegalProcessorStateException
     *          when no request is set
     */
    protected Request getRequestSafe() throws IllegalProcessorStateException {
        if (_request == null) {
            throw new IllegalProcessorStateException("no request set");
        }

        return _request;
    }

    /**
     * Returns the output product currently set.
     *
     * @throws org.esa.beam.framework.processor.IllegalProcessorStateException
     *          when no output product is set
     */
    protected Product getOutputProductSafe() throws IllegalProcessorStateException {
        if (_outputProduct == null) {
            throw new IllegalProcessorStateException("no output product created");
        }

        return _outputProduct;
    }

    /**
     * Sets the value for the aerosol optical depth at 550 nm;
     */
    protected void setTauAero550(float val) {
        _tau_aero_550 = val;
    }

    /**
     * Retrieves the value for the aerosol optical depth at 550 nm;
     */
    protected float getTauAero550() {
        return _tau_aero_550;
    }

    /**
     * Converts the aerosol type given by the request to a string that can be understood by the
     * <code>SensorCoefficientManager</code>.
     *
     * @param type the request type string
     *
     * @throws java.lang.IllegalArgumentException
     *          on unknown aerosol types
     */
    protected void setAerosolType(String type) {
        if (ObjectUtils.equalObjects(SmacConstants.AER_TYPE_DESERT, type)) {
            _aerosolType = SensorCoefficientManager.AER_DES_NAME;
        } else if (ObjectUtils.equalObjects(SmacConstants.AER_TYPE_CONTINENTAL, type)) {
            _aerosolType = SensorCoefficientManager.AER_CONT_NAME;
        } else {
            throw new IllegalArgumentException("Invalid aerosol type");
        }
    }

    /**
     * Retrieves the aerosol type string ciurrently set.
     */
    protected String getAerosolType() {
        return _aerosolType;
    }

    /**
     * Sets the value for the relative humidity to be used.
     */
    protected void setUh2o(float val) {
        _u_h2o = val;
    }

    /**
     * Retrieves the value for the relative humidity to be used.
     */
    protected float getUh2o() {
        return _u_h2o;
    }

    /**
     * Sets the value for the ozone content to be used
     */
    protected void setUo3(float val) {
        _u_o3 = val;
    }

    /**
     * Retrieves the value for the ozone content to be used
     */
    protected float getUo3() {
        return _u_o3;
    }

    /**
     * Sets the value for the surface pressure to be used
     */
    protected void setSurfPress(float val) {
        _surf_press = val;
    }

    /**
     * Retrieves the value for the surface pressure to be used
     */
    protected float getSurfPress() {
        return _surf_press;
    }

    /**
     * Sets the value written for pixels set to invalid
     */
    protected void setInvalidPixelValue(float val) {
        _invalidPixel = val;
    }

    /**
     * Retrieves the value set for pixels set to iunvalid
     */
    protected float getInvalidPixelValue() {
        return _invalidPixel;
    }


    /**
     * Logs an information message to the parent processors' logging sink.
     *
     * @param message the message to be logged
     */
    protected void logInfo(String message) {
        _logger.info(message);
    }

    /**
     * Logs a warning message to the parent processors' logging sink.
     *
     * @param message the message to be logged
     */
    protected void logWarning(String message) {
        _logger.warning(message);
    }

    /**
     * Logs an error message to the parent processors' logging sink
     *
     * @param message th message to be logged
     */
    protected void logError(String message) {
        _logger.severe(message);
    }

    /**
     * Checks whether the given object reference is null. If so, throws ProcessorException.
     *
     * @param obj         the object to be checked
     * @param description a description string
     */
    protected void checkValueNotNull(Object obj, String description) throws ProcessorException {
        if (obj == null) {
            throw new ProcessorException("Object \"" + description + " is null");
        }
    }


    /**
     * Constructs a validated list of RsBands to be processed. Scans the input product for the band names passed in and
     * adds the appropriate <code>Band</code> to the vector of bands to be processed.
     *
     * @param inputBandNames the list of band names as written in the request
     */
    private void setUpInputBands(String[] inputBandNames) {
        Band band;
        _inputBands.clear();
        for (int i = 0; i < inputBandNames.length; i++) {
            band = _inputProduct.getBand(inputBandNames[i]);
            if (band == null) {
                logWarning("requested band \"" + inputBandNames[i] + "\" not found in product!");
            } else {
                _inputBands.add(band);
            }
        }
    }
}
