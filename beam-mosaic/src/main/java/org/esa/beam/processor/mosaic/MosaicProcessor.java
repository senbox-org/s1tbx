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
package org.esa.beam.processor.mosaic;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.jexp.ParseException;
import com.bc.jexp.Parser;
import com.bc.jexp.Term;
import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductProjectionBuilder;
import org.esa.beam.framework.dataio.ProductSubsetBuilder;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.dataop.barithm.RasterDataEvalEnv;
import org.esa.beam.framework.dataop.barithm.RasterDataSymbol;
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModelRegistry;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.MapProjection;
import org.esa.beam.framework.dataop.maptransf.MapProjectionRegistry;
import org.esa.beam.framework.dataop.maptransf.MapTransform;
import org.esa.beam.framework.dataop.maptransf.MapTransformDescriptor;
import org.esa.beam.framework.dataop.maptransf.UTM;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.framework.dataop.resamp.ResamplingFactory;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.Processor;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProcessorUtils;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.RequestElementFactory;
import org.esa.beam.framework.processor.ui.ProcessorUI;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.math.MathUtils;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

/**
 * The <code>Mosaic Processor</code> class this class implemets the <code>Processor</code> interface so it can be used
 * by the processor framework.
 *
 * @deprecated since BEAM 4.7, replaced by GPF operator 'Mosaic'
 */
@Deprecated
public class MosaicProcessor extends Processor {

    public static final String HELP_ID = "mosaicScientificTool";

    private static final int PROJECTION_DEFAULT_NO_DATA_VALUE = 0;
    private static final MosaicUtils.MosaicVariable COUNT_VARIABLE = new MosaicUtils.MosaicVariable(
            MosaicConstants.BANDNAME_COUNT, "", false, true);

    private final Logger _logger;
    private ProcessorUI _processorUI;
    private boolean _updateMode;
    private CornerParams _cornerParams;
    private NorthEastMapParams _northEastMapParams;
    private ProjectionParams _projectionParams;
    private MosaicUtils.MosaicIoChannel[] _outputChannels;
    private MosaicUtils.MosaicIoChannel[] _testChannels;
    private MosaicUtils.MosaicIoChannel _countChannel;
    private ProductRef _outputProductRef;
    private ProductWriter _productWriter;
    private Product _outputProduct;
    private Area _outputProductArea;
    private MapProjection _outputProductMapProjection;
    private Product _currentInputProduct;
    private RasterDataNode[] _sourceBands;
    private Object[] _sourceLines;
    private boolean _testsOrCombined;
    private int _progressBarDepth;
    private boolean _projectionMode;
    private CenterLatLonMapParams _centerLatLonMapParams;
    private Parameter _fitOutputParam;
    private ProductSubsetDef _productSubsetDef;
    private boolean _orthorectifyInputProducts;
    private String _elevationModelName;
    private Product _projectedInputProduct;
    private PixelPos[] _sourcePixelCoords;
    private PixelGeoCodingParams _pixelGeoCodingParams;
    private boolean _includeTiePointGrids;

    public MosaicProcessor() {
        _logger = Logger.getLogger(MosaicConstants.LOGGER_NAME);
        _progressBarDepth = 3;
        setDefaultHelpId(HELP_ID);
    }

    @Override
    public void process(ProgressMonitor pm) throws ProcessorException {
        final Request request = getRequest();
        ProcessorUtils.setProcessorLoggingHandler(MosaicConstants.DEFAULT_LOG_PREFIX, request,
                                                  getName(), getVersion(), getCopyrightInformation());
        final String type = request.getType();
        _projectionMode = MosaicConstants.REQUEST_TYPE_MAP_PROJECTION.equalsIgnoreCase(type);
        if (!MosaicConstants.REQUEST_TYPE.equalsIgnoreCase(type) && !_projectionMode) {
            throw new ProcessorException("Illegal processing request type: '" + type + "'"); /*I18N*/
        }
        _logger.info("Mosaic processor version: " + MosaicConstants.VERSION_STRING); /*I18N*/

        evalRequest();
        if (isUpdateMode()) {
            pm = ProgressMonitor.NULL;
        }
        final boolean isUpdate = getRequest().getNumInputProducts() > 0;
        pm.beginTask("Initializing output product...", isUpdate ? 2 : 3); /*I18N*/
        try {
            loadInputProductAt(0, SubProgressMonitor.create(pm, 1));
            if (!isUpdateMode()) {
                tryToCreateOutputProductPhysically(SubProgressMonitor.create(pm, 1));
            }
            if (pm.isCanceled()) {
                return;
            }
            pm.setSubTaskName("Updating output product...");   /*I18N*/
            updateOutputProduct(SubProgressMonitor.create(pm, 1));
        } finally {
            if (pm.isCanceled()) {
                setCurrentStatus(ProcessorConstants.STATUS_ABORTED);
            }
            pm.done();
            cleanUp();
        }

    }

    private void tryToCreateOutputProductPhysically(ProgressMonitor pm) throws ProcessorException {
        IOException exception = null;
        boolean success = true;
        try {
            success = createOutputProductPhysically(pm);
            if (!success) {
                _logger.info("Creation of output product aborted by user."); /*I18N*/
            }
        } catch (IOException e) {
            exception = e;
        }
        final StringBuilder msgBuffer = new StringBuilder();
        if (exception != null || !success) {
            try {
                getProductWriter().deleteOutput(); // product writer already exists
            } catch (IOException e1) {
                _logger.severe("I/O error occurred while deleting the output product: " + e1.getMessage()); /*I18N*/
                msgBuffer.append(e1.getMessage());
            }
        }
        if (exception != null) {
            msgBuffer.insert(0, exception.getMessage() + "\n");
            throw new ProcessorException(msgBuffer.toString());
        }
    }

    @Override
    protected void cleanupAfterFailure() {
        cleanUp();
    }

    private void cleanUp() {
        cleanUpVariables(_outputChannels);
        _outputChannels = null;
        cleanUpVariables(_testChannels);
        _testChannels = null;
        _outputProductMapProjection = null;
        _outputProductRef = null;
        _projectionParams = null;
        _cornerParams = null;
        if (_outputProduct != null) {
            _outputProduct.dispose();
            _outputProduct = null;
        }
        _productWriter = null;
        if (_sourceBands != null) {
            for (RasterDataNode rasterDataNode : _sourceBands) {
                if (rasterDataNode != null) {
                    rasterDataNode.dispose();
                }
            }
            _sourceBands = null;
        }
        _sourceLines = null;
        cleanUpInput();
    }

    private static void cleanUpVariables(final MosaicUtils.MosaicIoChannel[] variables) {
        if (variables != null) {
            for (MosaicUtils.MosaicIoChannel outputVariable : variables) {
                final Band destBand = outputVariable.getDestBand();
                if (destBand != null) {
                    destBand.dispose();
                    outputVariable.setDestBand(null);
                }
                final ProductData destData = outputVariable.getDestData();
                if (destData != null) {
                    destData.dispose();
                    outputVariable.setDestData(null);
                }
                outputVariable.setTerm(null);
            }
        }
    }

    private void cleanUpInput() {
        if (_currentInputProduct != null) {
            _currentInputProduct.dispose();
            _currentInputProduct = null;
        }
    }

    @Override
    public String getName() {
        return MosaicConstants.PROCESSOR_NAME;
    }

    @Override
    public String getSymbolicName() {
        return MosaicConstants.PROCESSOR_SYMBOLIC_NAME;
    }

    @Override
    public String getVersion() {
        return MosaicConstants.VERSION_STRING;
    }

    @Override
    public String getCopyrightInformation() {
        return MosaicConstants.COPYRIGHT_INFO;
    }

    /**
     * Creates the graphical user interface of the processor and returns the base component to the framework.
     *
     * @return the graphical user interface of the processor.
     * @throws org.esa.beam.framework.processor.ProcessorException
     *
     */
    @Override
    public ProcessorUI createUI() throws ProcessorException {
        if (_processorUI == null) {
            _processorUI = new MosaicUi();
        }
        return _processorUI;
    }

    /**
     * Retrieves the title to be shown in the user interface. Override this method to set a processor specific title
     * string.
     */
    @Override
    public String getUITitle() {
        return MosaicConstants.UI_TITLE;
    }

    /**
     * Retrieves the request element factory for this processor.
     */
    @Override
    public RequestElementFactory getRequestElementFactory() {
        return MosaicRequestElementFactory.getInstance();
    }

    /**
     * Sets the progress bar depth, depends on update mode or not. The RequestValidator, which has been set from
     * MosaicUi to the ProcessorApp, detects if the updateMode is selected or not. If it is selected, the validator sets
     * the progress bar depth to 2.
     *
     * @param progessBarDepth The depth to indicate progress for.
     */
    public void setProgressBarDepth(int progessBarDepth) {
        _progressBarDepth = progessBarDepth;
    }

    /**
     * Retrieves the number of progressbars needed by the processor. Override this method if more than one progressbar
     * is needed, i.e. for multistage processes.
     *
     * @return the number og progressbars needed.
     */
    @Override
    public int getProgressDepth() {
        return _progressBarDepth;
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////


    private boolean createOutputProductPhysically(ProgressMonitor pm) throws IOException,
            ProcessorException {

        final String outputFile = _outputProductRef.getFilePath();
        final String productName = FileUtils.getFilenameWithoutExtension(_outputProductRef.getFile());
        final Rectangle2D outputRect = createOutputProductBoundaries();
        if (outputRect.getWidth() * outputRect.getHeight() < 0) {
            throw new ProcessorException("Size of output product exceeds the maximum size.\n" +
                                                 "Possibly caused by a small pixel size parameter.");
        }
        final float pixelSizeX = _projectionParams.getPixelSizeX();
        final float pixelSizeY = _projectionParams.getPixelSizeY();
        Product outputProduct = MosaicUtils.createGeocodedProduct(outputRect, productName, _outputProductMapProjection,
                                                                  pixelSizeX, pixelSizeY);

        final MapInfo mapInfo = ((MapGeoCoding) outputProduct.getGeoCoding()).getMapInfo();
        mapInfo.setResampling(_projectionParams.getResamplingMethod());
        mapInfo.setOrthorectified(_orthorectifyInputProducts);
        mapInfo.setElevationModelName(_elevationModelName);
        mapInfo.setNoDataValue(_projectionParams.getNoDataValue());

        addBandsToOutputProduct(outputProduct);

        final MetadataElement metadataElement = getRequest().convertToMetadata();
        final MetadataElement element = metadataElement.getElement(Request.METADATA_ELEM_NAME_INPUT_PRODUCTS);
        metadataElement.removeElement(element);
        outputProduct.getMetadataRoot().addElement(metadataElement);

        if (_projectionMode) {
            final String[] messages = ProductUtils.removeInvalidExpressions(outputProduct);
            for (String message : messages) {
                _logger.warning(message);
            }
        }

        final ProductWriter productWriter = getProductWriter();
        productWriter.writeProductNodes(outputProduct, outputFile);
        outputProduct.setProductWriter(productWriter);
        boolean initDiskSpaceSuccess = initDiscSpaceForBands(outputProduct, pm);
        setOutputProduct(outputProduct);
        return initDiskSpaceSuccess;
    }

    private void addBandsToOutputProduct(Product outputProduct) throws ProcessorException {
        if (_projectionMode) {
            final List<MosaicUtils.MosaicIoChannel> channels;
            Product inpProduct = null;
            try {
                inpProduct = getInputProductOrSubset();
                final MetadataElement[] elements = inpProduct.getMetadataRoot().getElements();
                for (MetadataElement element : elements) {
                    outputProduct.getMetadataRoot().addElement(element.createDeepClone());
                }
                final MetadataAttribute[] attributes = inpProduct.getMetadataRoot().getAttributes();
                for (MetadataAttribute attribute : attributes) {
                    outputProduct.getMetadataRoot().addAttribute(attribute.createDeepClone());
                }
                final String[] flagCodingNames = inpProduct.getFlagCodingGroup().getNodeNames();
                for (String flagCodingName : flagCodingNames) {
                    final FlagCoding flagCodingIn = inpProduct.getFlagCodingGroup().get(flagCodingName);
                    final FlagCoding flagCodingOut = new FlagCoding(flagCodingName);
                    outputProduct.getFlagCodingGroup().add(flagCodingOut);
                    final String[] flagNames = flagCodingIn.getFlagNames();
                    for (String flagName : flagNames) {
                        final MetadataAttribute flag = flagCodingIn.getFlag(flagName);
                        final int flagMask = flagCodingIn.getFlagMask(flagName);
                        flagCodingOut.addFlag(flagName, flagMask, flag.getDescription());
                    }
                }
                ProductUtils.copyMasks(inpProduct, outputProduct);
                ProductUtils.copyOverlayMasks(inpProduct, outputProduct);

                ProductUtils.copyBandsForGeomTransform(inpProduct, outputProduct, PROJECTION_DEFAULT_NO_DATA_VALUE,
                                                       null);
                // todo - (nf) add call to ProductUtils.copyBitmaskDefinitions
                final ArrayList<RasterDataNode> bands = new ArrayList<RasterDataNode>(20);
                bands.addAll(Arrays.asList(inpProduct.getBands()));
                if (_includeTiePointGrids) {
                    bands.addAll(Arrays.asList(inpProduct.getTiePointGrids()));
                }
                channels = new ArrayList<MosaicUtils.MosaicIoChannel>(bands.size());
                for (RasterDataNode dataNode : bands) {
                    if (dataNode instanceof VirtualBand) {
                        continue;
                    }
                    final String name = dataNode.getName();
                    final MosaicUtils.MosaicVariable variable = new MosaicUtils.MosaicVariable(name, name, false,
                                                                                               false);
                    final MosaicUtils.MosaicIoChannel channel = new MosaicUtils.MosaicIoChannel(variable);
                    channels.add(channel);
                    if (outputProduct.getRasterDataNode(name) == null) {
                        outputProduct.addBand(name, dataNode.getGeophysicalDataType());
                    }
                }
                _outputChannels = channels.toArray(new MosaicUtils.MosaicIoChannel[channels.size()]);
            } catch (IOException e) {
                throw new ProcessorException(e.getMessage());
            } finally {
                if (inpProduct != null && inpProduct != _currentInputProduct) {
                    inpProduct.dispose();
                }
            }
        } else {
            for (MosaicUtils.MosaicIoChannel channel : _outputChannels) {
                final String name = channel.getVariable().getName();
                if (channel == _countChannel) {
                    outputProduct.addBand(name, ProductData.TYPE_UINT16);
                } else {
                    outputProduct.addBand(name, ProductData.TYPE_FLOAT32);
                }
            }
        }
        setNoDataValueOfBands(outputProduct);
    }

    private void setNoDataValueOfBands(Product outputProduct) {
        MapGeoCoding mapGeoCoding = (MapGeoCoding) outputProduct.getGeoCoding();
        MapInfo mapInfo = mapGeoCoding.getMapInfo();
        for (MosaicUtils.MosaicIoChannel channel : _outputChannels) {
            final String name = channel.getVariable().getName();
            final RasterDataNode band = outputProduct.getRasterDataNode(name);
            if (channel != _countChannel) {
                band.setGeophysicalNoDataValue(mapInfo.getNoDataValue());
                band.setNoDataValueUsed(true);
                if (band.isLog10Scaled()) {
                    final double geophysicalNoDataValue = band.getGeophysicalNoDataValue();
                    double eps = Math.pow(10.0, -Math.log10(Math.abs(geophysicalNoDataValue)));
                    String externalName = BandArithmetic.createExternalName(band.getName());
                    final String expression = String.format("fneq(%s,%f,%f)",
                                                            externalName, geophysicalNoDataValue, eps);
                    band.setValidPixelExpression(expression);
                }
            }
        }
        for (MosaicUtils.MosaicIoChannel channel : _testChannels) {
            final String name = channel.getVariable().getName();
            final Band band = outputProduct.getBand(name);
            if (band != null) { // only if the channel is incl. in output product
                band.setNoDataValueUsed(false);
            }
        }
    }

    private Product getInputProductOrSubset() throws ProcessorException,
            IOException {
        final Product inputProduct = _currentInputProduct;
        final ProductSubsetBuilder productSubsetBuilder = new ProductSubsetBuilder();
        final Product subset = productSubsetBuilder.readProductNodes(inputProduct, _productSubsetDef);
        if (subset.getNumBands() == 0) {
            throw new ProcessorException("Unable to map-project the product '" +
                                                 getRequest().getInputProductAt(0).getFilePath() +
                                                 "' because the 'bands' parameter in the processing " +
                                                 "request results in a product without bands.");
        }
        return subset;
    }

    private static boolean initDiscSpaceForBands(final Product product, ProgressMonitor pm) throws IOException {
        final Band[] bands = product.getBands();
        final int sceneWidth = product.getSceneRasterWidth();
        final int sceneHeight = product.getSceneRasterHeight();

        pm.beginTask("Initializing output product...", bands.length);
        try {
            for (Band band : bands) {
                if (!(band instanceof VirtualBand)) {
                    ProgressMonitor subPm = SubProgressMonitor.create(pm, 1);
                    subPm.beginTask(MessageFormat.format("Initializing band ''{0}''...", band.getName()), sceneHeight);
                    try {
                        final ProductData rasterData = band.createCompatibleRasterData(sceneWidth, 1);
                        for (int x = 0; x < sceneWidth; x++) {
                            rasterData.setElemDoubleAt(x, band.getNoDataValue());
                        }
                        for (int y = 0; y < sceneHeight; y++) {
                            band.writeRasterData(0, y, sceneWidth, 1, rasterData, SubProgressMonitor.create(pm, 1));
                            if (subPm.isCanceled()) {
                                return false;
                            }
                        }
                    } finally {
                        subPm.done();
                    }
                } else {
                    pm.worked(1);
                }
                if (pm.isCanceled()) {
                    return false;
                }
            }
        } finally {
            pm.done();
        }
        return true;
    }


    private Rectangle2D createOutputProductBoundaries() throws ProcessorException {
        final float pixelSizeX;
        final float pixelSizeY;
        final MapProjection projection;
        String projectionParamsExceptionText = "Failed to create output product, projection parameters not " +
                "given or invalid";
        if (_projectionParams != null && _projectionParams.isValid()) {
            initProjection();
            projection = _outputProductMapProjection;
            pixelSizeX = _projectionParams.getPixelSizeX();
            pixelSizeY = _projectionParams.getPixelSizeY();
        } else {
            throw new ProcessorException(projectionParamsExceptionText);
        }
        if (MosaicUtils.isTrue(_fitOutputParam)) {
            if (_currentInputProduct == null) {
                throw new ProcessorException("Unable to create output product boundary");
            }
            final MapInfo suitableMapInfo = ProductUtils.createSuitableMapInfo(_currentInputProduct, null, projection);
            if (suitableMapInfo == null) {
                throw new ProcessorException(projectionParamsExceptionText);
            }
            final float easting = suitableMapInfo.getEasting();
            final float northing = suitableMapInfo.getNorthing();
            final int sceneWidth = suitableMapInfo.getSceneWidth();
            final int sceneHeight = suitableMapInfo.getSceneHeight();
            final int finalSceneWidth = (int) (sceneWidth * suitableMapInfo.getPixelSizeX() / pixelSizeX);
            final int finalSceneHeight = (int) (sceneHeight * suitableMapInfo.getPixelSizeY() / pixelSizeY);
            return new Rectangle2D.Float(easting, northing, finalSceneWidth, finalSceneHeight);
        } else if (_cornerParams != null && _cornerParams.isValid()) {
            final GeoPos geoPosUL = new GeoPos(_cornerParams.getNorthLat(), _cornerParams.getWestLon());
            final GeoPos geoPosLR = new GeoPos(_cornerParams.getSouthLat(), _cornerParams.getEastLon());
            return MosaicUtils.createOutputProductBoundaries(projection, geoPosUL, geoPosLR, pixelSizeX, pixelSizeY);
        } else if (_northEastMapParams != null && _northEastMapParams.isValid()) {
            final float easting = _northEastMapParams.getEasting();
            final float northing = _northEastMapParams.getNorthing();
            final float width = _northEastMapParams.getOutputWidth();
            final float height = _northEastMapParams.getOutputHeight();
            return new Rectangle2D.Double(easting, northing, width, height);
        } else if (_centerLatLonMapParams != null && _centerLatLonMapParams.isValid()) {
//            final GeoPos cp = _centerLatLonMapParams.getCenterPos();
//            final int width = _centerLatLonMapParams.getOutputWidth();
//            final int height = _centerLatLonMapParams.getOutputHeight();
//            return MosaicUtils.createOutputProductBoundaries(projection, cp, pixelSizeX, pixelSizeY, width, height);
            throw new ProcessorException("Map definition via center lat/lon is not implemented.");
        } else if (_projectionParams.getName().equals(UTM.AUTO_PROJECTION_NAME)) {
            float orientation = 0.0f;
            double defaultNoDataValue = MapInfo.DEFAULT_NO_DATA_VALUE;
            if (_currentInputProduct.getGeoCoding() instanceof MapGeoCoding) {
                final MapInfo mapInfo = ((MapGeoCoding) _currentInputProduct.getGeoCoding()).getMapInfo();
                orientation = mapInfo.getOrientation();
                defaultNoDataValue = mapInfo.getNoDataValue();
            }

            final MapInfo suitableMapInfo = ProductUtils.createSuitableMapInfo(_currentInputProduct,
                                                                               projection,
                                                                               orientation,
                                                                               defaultNoDataValue);
            if (suitableMapInfo == null) {
                throw new ProcessorException(projectionParamsExceptionText);
            }
            final float easting = suitableMapInfo.getEasting();
            final float northing = suitableMapInfo.getNorthing();
            final int sceneWidth = suitableMapInfo.getSceneWidth();
            final int sceneHeight = suitableMapInfo.getSceneHeight();
            final int finalSceneWidth = (int) (sceneWidth * suitableMapInfo.getPixelSizeX() / pixelSizeX);
            final int finalSceneHeight = (int) (sceneHeight * suitableMapInfo.getPixelSizeY() / pixelSizeY);
            return new Rectangle2D.Float(easting, northing, finalSceneWidth, finalSceneHeight);
        } else {
            throw new ProcessorException(projectionParamsExceptionText);
        }
    }

    private void initProjection() throws ProcessorException {
        final String projectionName = _projectionParams.getName();
        final MapProjection projection;
        if (UTM.AUTO_PROJECTION_NAME.equals(projectionName)) {
            final GeoPos centerGeoPos = ProductUtils.getCenterGeoPos(_currentInputProduct);
            projection = UTM.getSuitableProjection(centerGeoPos);
        } else {
            projection = MapProjectionRegistry.getProjection(projectionName);
        }
        if (projection == null) {
            final String message = "Unknown map projection '" + projectionName + "'."; /*I18N*/
            throw new ProcessorException(message);
        }
        final MapTransform mapTransform = projection.getMapTransform();
        final double[] defaultValues = mapTransform.getParameterValues();
        final MapTransformDescriptor descriptor = mapTransform.getDescriptor();
        final double[] values = _projectionParams.getParameters();
        final MapTransform transform;

        if (values == null) {
            transform = descriptor.createTransform(defaultValues);
        } else if (values.length < descriptor.getParameterDefaultValues().length) {
            final int expectedLength = descriptor.getParameterDefaultValues().length;
            final String paramName = MosaicConstants.PARAM_NAME_PROJECTION_PARAMETERS;
            throw new ProcessorException(
                    "The given parameter '" + paramName + "' contains less than the expected " + expectedLength + " values.");
        } else {
            transform = descriptor.createTransform(values);
        }
        _outputProductMapProjection = new MapProjection(projectionName, transform);
    }

    private void updateOutputProduct(ProgressMonitor pm) throws ProcessorException {
        if (getOutputProduct().getProductReader() == null) {
            getOutputProduct().dispose();
            setOutputProduct(null);
            loadOutputProduct();
        }
        if (getOutputProduct().getProductWriter() == null) {
            prepareOutputProductForUpdate();
        }
        try {
            processInputProducts(pm);
        } finally {
            updateProductNodes();
        }
    }

    private void processInputProducts(ProgressMonitor pm) throws ProcessorException {
        final GeoCoding geoCoding = getOutputProduct().getGeoCoding();
        if (geoCoding == null) {
            throw new ProcessorException("The output product is not geo-referenced."); /*I18N*/
        }
        if (!(geoCoding instanceof MapGeoCoding)) {
            throw new ProcessorException("The output product is not geo-referenced."); /*I18N*/
        }

        initDestBands();

        final MetadataElement rootElem = getOutputProduct().getMetadataRoot();
        final MetadataElement requestElem = rootElem.getElement(Request.METADATA_ELEM_NAME_PROCESSING_REQUEST);
        MetadataElement element = requestElem.getElement(Request.METADATA_ELEM_NAME_INPUT_PRODUCTS);
        if (element == null) {
            element = new MetadataElement(Request.METADATA_ELEM_NAME_INPUT_PRODUCTS);
            requestElem.addElement(element);
        }
        final MetadataElement inputProductElement = element;

        final Request request = getRequest();
        final int progressMax;
        if (_projectionMode) {
            progressMax = 1;
        } else {
            progressMax = request.getNumInputProducts();
        }
        pm.beginTask("Processing input products...", progressMax * 2);
        try {
            for (int i = 0; i < progressMax; i++) {
                boolean success = true;
                try {
                    String taskName = MessageFormat.format("Processing input product #{0} of {1}...",
                                                           i + 1, progressMax);
                    pm.setSubTaskName(taskName);
                    if (i == 0) {
                        if (_currentInputProduct == null) {
                            success = false;
                            continue;
                        }
                    } else if (!loadInputProductAt(i, SubProgressMonitor.create(pm, 1))) {
                        success = false;
                        continue;
                    }
                    if (pm.isCanceled()) {
                        success = false;
                        break;
                    }
                    try {
                        success = processInputProduct(SubProgressMonitor.create(pm, 1));
                    } catch (ProcessorException e) {
                        success = false;
                        throw e;
                    }
                } finally {
                    if (pm.isCanceled()) {
                        setCurrentStatus(ProcessorConstants.STATUS_ABORTED);
                    }
                    if (success) {
                        final int numInputs = inputProductElement.getNumAttributes();
                        final String inpProdAttribName = Request.METADATA_ATTRIB_NAME_PREFIX_INPUT_PRODUCT + (numInputs + 1);
                        ProductRef productRef = request.getInputProductAt(i);
                        Request.addProductAttribs(inputProductElement, inpProdAttribName, productRef);
                    }
                    cleanUpInput();
                }
            }
        } finally {
            pm.done();
        }
    }

    private void initDestBands() throws ProcessorException {
        for (final MosaicUtils.MosaicIoChannel channel : _outputChannels) {
            final String name = channel.getVariable().getName();
            final Band band = getOutputProduct().getBand(name);
            if (band == null) {
                final String message = createMissingBandMessage(name);
                _logger.severe(message);
                throw new ProcessorException(message);
            }
            channel.setDestBand(band);
        }
    }

    private static String createMissingBandMessage(final String name) {
        return "Missing band '" + name + "' in output product.";   /*I18N*/
    }

    private boolean processInputProduct(ProgressMonitor pm) throws ProcessorException {
        if (_orthorectifyInputProducts && !_currentInputProduct.canBeOrthorectified()) {
            _logger.warning("Unable to orthorectifiy the input product.");
            return false;
        }

        final MapInfo mapInfo = ((MapGeoCoding) getOutputProduct().getGeoCoding()).getMapInfo();
        final MapInfo inputMapInfo = mapInfo.createDeepClone();

        pm.beginTask(_currentInputProduct.getName(), _projectionMode ? 1 : _currentInputProduct.getSceneRasterHeight());
        try {

            if (_projectionMode) {
                return updateOutputProductWithIntersectingProduct(_currentInputProduct, inputMapInfo,
                                                                  SubProgressMonitor.create(pm, 1));
            } else {
                final int height = _currentInputProduct.getSceneRasterHeight();
                final int width = _currentInputProduct.getSceneRasterWidth();

                final int maxSubsetHeight = 16000;
                for (int startY = 0; startY < height; startY += maxSubsetHeight) {
                    final int subsetHeight;
                    if ((startY + maxSubsetHeight) > height) {
                        subsetHeight = height - startY;
                    } else {
                        subsetHeight = maxSubsetHeight;
                    }
                    final ProductSubsetDef subsetDef = new ProductSubsetDef();
                    subsetDef.setRegion(0, startY, width, subsetHeight);
                    final Product subset;
                    try {
                        subset = _currentInputProduct.createSubset(subsetDef, "s", "sd");
                    } catch (IOException e) {
                        _logger.warning("Unable to split the input product.");
                        return false;
                    }

                    final boolean success = updateOutputProductWithIntersectingProduct(subset, inputMapInfo,
                                                                                       SubProgressMonitor.create(pm,
                                                                                                                 1));
                    if (!success) {
                        return false;
                    }
                }
            }
        } finally {
            pm.done();
        }
        return true;
    }

    private boolean updateOutputProductWithIntersectingProduct(final Product subset, final MapInfo subsetMapInfo,
                                                               final ProgressMonitor pm) throws ProcessorException {
        if (isIntersectingOutputProduct(subset)) {
            try {
                _projectedInputProduct = ProductProjectionBuilder.createProductProjection(subset, false,
                                                                                          _includeTiePointGrids,
                                                                                          subsetMapInfo, "temp",
                                                                                          "temp-desc");
                disableLogScalingToPreventFromNoDataProblems(_projectedInputProduct);
            } catch (IOException e) {
                _logger.warning("Unable to project the input product according to the output product.");
                return false;
            }
            final Rectangle[] boundingRectangles = createInputProductBoundingRectangle(_projectedInputProduct);
            try {
                updateData(boundingRectangles, pm);
            } catch (IOException e) {
                _logger.severe(
                        "An I/O error occurred while reading data from input product '" + _currentInputProduct.getName() + "'"); /*I18N*/
                _logger.info("Continuing with next input product"); /*I18N*/
                return false;
            }
        }
        return true;
    }

    private void disableLogScalingToPreventFromNoDataProblems(Product projectedInputProduct) {
        Band[] bands = projectedInputProduct.getBands();
        for (Band band : bands) {
            if (band.getDataType() == ProductData.TYPE_FLOAT32
                    && band.isLog10Scaled()
                    && band.getScalingFactor() == 1.0
                    && band.getScalingOffset() == 0.0) {
                band.setLog10Scaled(false);
                MapGeoCoding mapGeoCoding = ((MapGeoCoding) projectedInputProduct.getGeoCoding());
                MapInfo mapInfo = mapGeoCoding.getMapInfo();
                band.setNoDataValue(mapInfo.getNoDataValue());
            }
        }
    }

    private boolean isIntersectingOutputProduct(Product subset) {
        final Area area = createArea(subset);
        area.intersect(_outputProductArea);
        return !area.isEmpty();
    }

    private boolean updateData(final Rectangle[] boundingRectangles, ProgressMonitor pm) throws ProcessorException,
            IOException {
        final int numIters = boundingRectangles.length;
        final int[] destX = new int[numIters];
        final int[] destY = new int[numIters];
        final int[] destWidth = new int[numIters];
        final int[] destHeight = new int[numIters];

        final int[] minY = new int[numIters];
        final int[] maxY = new int[numIters];
        for (int i = 0; i < numIters; i++) {
            Rectangle boundingRect = boundingRectangles[i];
            destX[i] = boundingRect.x;
            destY[i] = boundingRect.y;
            destWidth[i] = boundingRect.width;
            destHeight[i] = boundingRect.height;

            minY[i] = destY[i];
            maxY[i] = destY[i] + destHeight[i] - 1;
        }


        pm.beginTask("Processing " + _currentInputProduct.getName() + "...", numIters);
        try {
            for (int i = 0; i < numIters; i++) {
                if (!prepareVariablesForProcessing(destWidth[i])) {
                    _logger.warning("Unable to initialize (some) processing variables."); /*I18N*/
                    _logger.info("Continuing with the next input product.");/*I18N*/
                    return false;
                }

                final RasterDataEvalEnv env = new RasterDataEvalEnv(0, 0, 1, 1);

                for (MosaicUtils.MosaicIoChannel outputChannel : _outputChannels) {
                    outputChannel.setDestData(
                            ProductData.createInstance(outputChannel.getDestBand().getDataType(), destWidth[i]));
                }

                for (int y = minY[i]; y <= maxY[i]; y++) {
                    if (pm.isCanceled()) {
                        _logger.info("Processing terminated by user."); /*I18N*/
                        return false;
                    }

                    boolean containsValidPixels = readSourceLines(destX[i], y, destWidth[i]);
                    if (containsValidPixels) {

                        for (MosaicUtils.MosaicIoChannel outputChannel : _outputChannels) {
                            readDestLine(outputChannel, destX[i], y);
                        }

                        for (int x = 0; x < destWidth[i]; x++) {
                            env.setElemIndex(x);
                            if (isSourcePixelValid(x) && isSourceSampleValid(env)) {
                                updateDestLine(env, destX[i], y, x);
                            }
                        }

                        for (MosaicUtils.MosaicIoChannel outputChannel : _outputChannels) {
                            writeDestLine(outputChannel, destX[i], y);
                        }
                    }
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
        return true;
    }

    private boolean isSourcePixelValid(int x) {
        return _sourcePixelCoords[x] != null;
    }

    private boolean readSourceLines(final int x0, int y0, final int destWidth) throws IOException {
        _sourcePixelCoords = null;
        final ProductProjectionBuilder reader = (ProductProjectionBuilder) _projectedInputProduct.getProductReader();
        for (int i = 0; i < _sourceBands.length; i++) {
            final Object sourceLine = _sourceLines[i];
            final RasterDataNode sourceBand = _sourceBands[i];
            if (sourceLine instanceof int[]) {
                final int[] intLine = (int[]) sourceLine;
                sourceBand.readPixels(x0, y0, destWidth, 1, intLine, ProgressMonitor.NULL);
            } else {
                final float[] floatLine = (float[]) sourceLine;
                sourceBand.readPixels(x0, y0, destWidth, 1, floatLine, ProgressMonitor.NULL);
            }
            if (_sourcePixelCoords == null) {
                _sourcePixelCoords = new PixelPos[destWidth];
                reader.getSourceLinePixelCoords((Band) sourceBand, x0, y0, _sourcePixelCoords);
            }
        }
        for (PixelPos sourcePixelCoord : _sourcePixelCoords) {
            if (sourcePixelCoord != null) {
                return true;
            }
        }
        return false;
    }

    private void updateDestLine(final RasterDataEvalEnv env, int x0, int y0, final int x) {
        final int count;
        if (_projectionMode) {
            count = 1;
        } else {
            final ProductData destData = _countChannel.getDestData();
            count = destData.getElemIntAt(x) + 1;
            destData.setElemIntAt(x, count);
        }
        for (final MosaicUtils.MosaicIoChannel channel : _outputChannels) {
            if (channel != _countChannel) {
                boolean valid = isValidChannelPixel(channel, x0, y0, x);
                if (valid) {
                    final ProductData destData = channel.getDestData();
                    final Band destBand = channel.getDestBand();
                    final Term term = channel.getTerm();
                    final double noDataValue = destBand.getGeophysicalNoDataValue();
                    final double oldValue = destBand.scale(destData.getElemDoubleAt(x));
                    final double newValue;
                    if (term.isB()) {
                        // todo - why don't we average boolean values as well?
                        final boolean value = term.evalB(env);
                        if (oldValue == noDataValue) {
                            newValue = (value ? 1.0 : 0.0);
                        } else {
                            newValue = oldValue + (value ? 1.0 : 0.0);
                        }
                    } else {
                        final double value = term.evalD(env);
                        if (oldValue == noDataValue) {
                            newValue = value;
                        } else {
                            newValue = (oldValue * (count - 1) + value) / count;
                        }
                    }
                    destData.setElemDoubleAt(x, destBand.scaleInverse(newValue));
                }
            }
        }
    }

    private static boolean isValidChannelPixel(MosaicUtils.MosaicIoChannel channel, int x0, int y0, int x) {
        boolean validTerm = true;
        final RasterDataNode[] refRasters = channel.getRefRasters();
        for (RasterDataNode refRaster : refRasters) {
            if (!refRaster.isPixelValid(x0 + x, y0)) {
                validTerm = false;
            }
        }
        return validTerm;
    }

    private boolean isSourceSampleValid(RasterDataEvalEnv env) {
        if (_testChannels.length == 0) {
            return true;
        }
        if (_testsOrCombined) {
            for (MosaicUtils.MosaicIoChannel channel : _testChannels) {
                final boolean isTrue = channel.getTerm().evalB(env);
                if (isTrue) {
                    return true;
                }
            }
            return false;
        } else { // tests are AND combined
            for (MosaicUtils.MosaicIoChannel channel : _testChannels) {
                final boolean isFalse = !channel.getTerm().evalB(env);
                if (isFalse) {
                    return false;
                }
            }
            return true;
        }
    }


    private static void writeDestLine(MosaicUtils.MosaicIoChannel channel, int x, int y) throws ProcessorException {
        final Band destBand = channel.getDestBand();
        try {
            destBand.writeRasterData(x, y, channel.getDestData().getNumElems(), 1, channel.getDestData(),
                                     ProgressMonitor.NULL);
        } catch (IOException e) {
            throw new ProcessorException("Failed to write to output product: " + e.getMessage(), e); /*I18N*/
        }
    }

    private static void readDestLine(MosaicUtils.MosaicIoChannel channel, int x, int y) throws ProcessorException {
        final Band destBand = channel.getDestBand();
        try {
            destBand.readRasterData(x, y, channel.getDestData().getNumElems(), 1, channel.getDestData(),
                                    ProgressMonitor.NULL);
        } catch (IOException e) {
            throw new ProcessorException("Failed to read from output product: " + e.getMessage(), e); /*I18N*/
        }
    }


    private boolean prepareVariablesForProcessing(int lineWidth) {
        final List<MosaicUtils.MosaicIoChannel> allChannels = new ArrayList<MosaicUtils.MosaicIoChannel>(
                _outputChannels.length);
        for (final MosaicUtils.MosaicIoChannel outputChannel : _outputChannels) {
            if (outputChannel != _countChannel) {
                allChannels.add(outputChannel);
            }
        }
        allChannels.addAll(Arrays.asList(_testChannels));

        final HashSet<RasterDataSymbol> allBandSymbols = new HashSet<RasterDataSymbol>(10);
        final HashSet<RasterDataNode> allBands = new HashSet<RasterDataNode>(10);

        final Parser parser = _projectedInputProduct.createBandArithmeticParser();
        for (MosaicUtils.MosaicIoChannel channel : allChannels) {
            final MosaicUtils.MosaicVariable variable = channel.getVariable();
            final String expression = variable.getExpression();
            try {
                final Term term = parser.parse(expression);
                if (variable.isCondition() && !term.isB()) {
                    _logger.severe("Boolean expression expected for '" + variable.getName() + "' but was '" +
                                           expression + "'"); /*I18N*/
                    return false;
                }
                channel.setTerm(term);
                final RasterDataSymbol[] refRasterDataSymbols = BandArithmetic.getRefRasterDataSymbols(term);
                final RasterDataNode[] refRasters = BandArithmetic.getRefRasters(refRasterDataSymbols);
                channel.setRefRasters(refRasters);
                allBandSymbols.addAll(Arrays.asList(refRasterDataSymbols));
                for (final RasterDataNode refRaster : refRasters) {
                    if (refRaster instanceof Band || refRaster instanceof TiePointGrid) {
                        allBands.add(refRaster);
                    } else {
                        _logger.severe("Not a band: " + refRaster.getName());
                        return false;
                    }
                }
            } catch (ParseException e) {
                _logger.severe(e.getMessage());
                _logger.severe(MessageFormat.format(
                        "The expression\n''{0}''\ncould not be parsed by the expression parser.", expression));
                return false;
            }
        }

        final RasterDataSymbol[] rasterDataSymbols = allBandSymbols.toArray(
                new RasterDataSymbol[allBandSymbols.size()]);
        _sourceBands = allBands.toArray(new RasterDataNode[allBands.size()]);
        _sourceLines = new Object[_sourceBands.length];

        for (int i = 0; i < _sourceBands.length; i++) {
            final RasterDataNode band = _sourceBands[i];
            if (band.isFloatingPointType()) {
                _sourceLines[i] = new float[lineWidth];
            } else {
                _sourceLines[i] = new int[lineWidth];
            }
            for (RasterDataSymbol rasterDataSymbol : rasterDataSymbols) {
                if (rasterDataSymbol.getRaster() == band) {
                    rasterDataSymbol.setData(_sourceLines[i]);
                }
            }
        }
        return true;
    }

    private Rectangle[] createInputProductBoundingRectangle(final Product product) {
        final int step = Math.max(16, (product.getSceneRasterWidth() + product.getSceneRasterHeight()) / 250);
        final GeneralPath[] geoBoundaryPaths = ProductUtils.createGeoBoundaryPaths(product, null, step);
        final Rectangle[] boundingRectangles = new Rectangle[geoBoundaryPaths.length];
        final GeoCoding geoCoding = getOutputProduct().getGeoCoding();
        for (int i = 0; i < geoBoundaryPaths.length; i++) {
            GeneralPath geoBoundaryPath = geoBoundaryPaths[i];
            final int pixelXMax = getDestWidth() - 1;
            final int pixelYMax = getDestHeight() - 1;
            int xmin = pixelXMax;
            int ymin = pixelYMax;
            int xmax = 0;
            int ymax = 0;
            final GeoPos geoPos = new GeoPos();
            final float[] floats = new float[6];
            final PathIterator pathIterator = geoBoundaryPath.getPathIterator(null);
            while (!pathIterator.isDone()) {
                pathIterator.currentSegment(floats);
                geoPos.setLocation(floats[1], floats[0]);
                final PixelPos pixelPos = geoCoding.getPixelPos(geoPos, null);
                final int x = MathUtils.floorAndCrop(pixelPos.x, 0, pixelXMax);
                final int y = MathUtils.floorAndCrop(pixelPos.y, 0, pixelYMax);
                xmin = Math.min(xmin, x);
                ymin = Math.min(ymin, y);
                xmax = Math.max(xmax, x);
                ymax = Math.max(ymax, y);
                pathIterator.next();
            }
            boundingRectangles[i] = new Rectangle(xmin, ymin, xmax - xmin + 1,
                                                  ymax - ymin + 1);
        }
        return boundingRectangles;
    }

    private boolean loadInputProductAt(final int index, ProgressMonitor pm) {
        boolean success = true;
        try {
            _currentInputProduct = loadInputProduct(index);
            if (_pixelGeoCodingParams.isValid()) {
                initPixelGeoCoding(pm);
            }
        } catch (IOException e) {
            _logger.info("Continuing with next input product"); /*I18N*/
            success = false;
        } catch (ProcessorException e) {
            _logger.info("Continuing with next input product"); /*I18N*/
            success = false;
        }
        return success && _currentInputProduct != null;
    }

    private Band getExistingBand(String name) throws ProcessorException {
        Band band = _currentInputProduct.getBand(name);
        if (band == null) {
            String message = "Band not '" + name + "' found in input product";
            _logger.severe(message);
            throw new ProcessorException(message);
        }
        return band;
    }

    private void initPixelGeoCoding(ProgressMonitor pm) throws ProcessorException,
            IOException {
        String latName = _pixelGeoCodingParams.getSourceLatitudes();
        Band latBand = getExistingBand(latName);
        String lonName = _pixelGeoCodingParams.getSourceLongitudes();
        Band lonBand = getExistingBand(lonName);
        String validMask = _pixelGeoCodingParams.getSourceValidMask();
        int searchRadius = _pixelGeoCodingParams.getSourceSearchRadius();
        PixelGeoCoding geoCoding;
        try {
            _logger.info(
                    "Initializing geo-coding based on per-pixel latitudes and longitues from bands '" + latName + "' and '" + lonName + "'..."); /*I18N*/
            geoCoding = new PixelGeoCoding(latBand, lonBand, validMask, searchRadius, pm);
            _logger.info("Geo-coding initialized"); /*I18N*/
        } catch (IOException e) {
            _logger.severe("Failed to load data for pixel-based geo-coding: " + e.getMessage()); /*I18N*/
            throw e;
        }
        _currentInputProduct.setGeoCoding(geoCoding);
    }

    private void updateProductNodes() {
        final Product outputProduct = getOutputProduct();
        if (outputProduct == null) {
            return;
        }
        final ProductWriter productWriter = outputProduct.getProductWriter();
        if (productWriter == null) {
            return;
        }
        final File output = outputProduct.getFileLocation();
        if (output == null) {
            return;
        }
        try {
            productWriter.writeProductNodes(outputProduct, output);
        } catch (IOException e) {
            _logger.severe("Failed to write to output product '" + output + "'"); /*I18N*/
        }
    }

    private int getDestHeight() {
        return getOutputProduct().getSceneRasterHeight();
    }

    private int getDestWidth() {
        return getOutputProduct().getSceneRasterWidth();
    }

    private void prepareOutputProductForUpdate() throws ProcessorException {
        final String msgPrefix = "Failed to prepare output product for mosaic update process:\n"; /*I18N*/
        final Product outputProduct = getOutputProduct();
        final ProductWriter productWriter = ProcessorUtils.createProductWriter(_outputProductRef);
        if (productWriter == null) {
            final String message = msgPrefix +
                    "Unable to create a product writer for output format '" + _outputProductRef.getFileFormat() + "'"; /*I18N*/
            _logger.severe(message);
            throw new ProcessorException(message);
        }
        outputProduct.setProductWriter(productWriter);
        try {
            productWriter.writeProductNodes(outputProduct, outputProduct.getFileLocation());
        } catch (IOException e) {
            final String message = msgPrefix + e.getMessage();
            _logger.severe(message);
            throw new ProcessorException(message);
        }
    }

    private void setOutputProduct(final Product product) {
        _outputProduct = product;
        if (_outputProduct == null) {
            _outputProductArea = null;
        } else {
            _outputProductArea = createArea(_outputProduct);
        }
    }

    private static Area createArea(final Product product) {
        final GeneralPath[] paths = ProductUtils.createGeoBoundaryPaths(product);
        final Area area = new Area();
        for (GeneralPath path : paths) {
            area.add(new Area(path));
        }
        return area;
    }

    private boolean isUpdateMode() {
        return _updateMode;
    }

    private void evalRequest() throws ProcessorException {
        evalOutputProduct();
        evalProjectionMode();
        evalUpdateMode();
        evalProductWriter();
        if (!isUpdateMode()) {
            initProcessingParamsFromRequest();
        } else {
            initProcessingParamsFromOutputProduct();
        }
    }

    private Product getOutputProduct() {
        return _outputProduct;
    }

    private void loadOutputProduct() throws ProcessorException {
        final String outputProductPath = _outputProductRef.getFilePath();
        try {
            final Product product = ProductIO.readProduct(outputProductPath);
            if (product == null) {
                throw new ProcessorException("Unable to read the output product '" + outputProductPath + "'.");
            }
            setOutputProduct(product);
        } catch (IOException e) {
            throw new ProcessorException("An I/O error occurred while opening output product\n" + /*I18N*/
                                                 "'" + outputProductPath + "'\n" + e.getMessage());
        }
    }

    private void evalOutputProduct() throws ProcessorException {
        if (getRequest().getNumOutputProducts() != 1) {
            throw new ProcessorException("A single output product must be given."); /*I18N*/
        }
        final ProductRef outputProductRef = getRequest().getOutputProductAt(0);
        if (outputProductRef == null) {
            throw new ProcessorException("Output product is not given."); /*I18N*/
        }
        if (!(DimapProductConstants.DIMAP_FORMAT_NAME.equalsIgnoreCase(outputProductRef.getFileFormat()) ||
                "GeoTIFF".equalsIgnoreCase(outputProductRef.getFileFormat()))) {
            final String message = String.format("The output format '%s' is not supported.",
                                                 outputProductRef.getFileFormat());
            throw new ProcessorException(message);
        }
        final String productName = FileUtils.getFilenameWithoutExtension(new File(outputProductRef.getFilePath()));
        if (productName == null || productName.length() == 0) {
            throw new ProcessorException(ProcessorConstants.LOG_MSG_NO_OUTPUT_NAME);
        }
        _outputProductRef = outputProductRef;
    }

    private void evalProductWriter() throws ProcessorException {
        final String fileFormat = _outputProductRef.getFileFormat();
        final ProductWriter productWriter = ProductIO.getProductWriter(fileFormat);
        if (productWriter == null) {
            throw new ProcessorException(
                    "Failed to create a product writer for output format '" + fileFormat + "'"); /*I18N*/
        }
        _productWriter = productWriter;
    }

    private void evalUpdateMode() {
        final Parameter updateParameter = getRequest().getParameter(MosaicConstants.PARAM_NAME_UPDATE_MODE);
        if (updateParameter == null) {
            _updateMode = MosaicConstants.PARAM_DEFAULT_VALUE_UPDATE_MODE;
        } else {
            _updateMode = MosaicUtils.isTrue(updateParameter);
        }
    }

    private void evalProjectionMode() throws ProcessorException {
        if (_projectionMode) {
            if (getRequest().getNumInputProducts() < 1) {
                throw new ProcessorException("Input product not given."); /*I18N*/
            }

            final Parameter bandsParam = getRequest().getParameter(MosaicConstants.PARAM_NAME_BANDS);
            if (bandsParam == null) {
                return;
            }

            final String[] value = (String[]) bandsParam.getValue();
            if (value == null || value.length == 0) {
                return;
            }

            String[] gridNames = null;
            if (_orthorectifyInputProducts) {
                final Product currentInputProduct = _currentInputProduct;
                if (currentInputProduct != null && currentInputProduct.getNumTiePointGrids() > 0) {
                    gridNames = currentInputProduct.getTiePointGridNames();
                }
            }

            final String[] nodeNames;
            if (gridNames != null) {
                nodeNames = StringUtils.addArrays(value, gridNames);
            } else {
                nodeNames = value;
            }

            _productSubsetDef = new ProductSubsetDef();
            _productSubsetDef.setNodeNames(nodeNames);
        }
    }

    private void initProcessingParamsFromRequest() throws ProcessorException {
        initProcessingParams(getRequest().getAllParameters());
    }

    private void initProcessingParamsFromOutputProduct() throws ProcessorException {
        loadOutputProduct();
        initProcessingParams(MosaicUtils.extractProcessingParameters(getOutputProduct()));
    }

    private void initProcessingParams(final Parameter[] parameters) throws ProcessorException {
        evalCornerParameters(parameters);
        evalMapParameters(parameters);
        evalProjectionParameters(parameters);
        evalPixelGeoCodingParameters(parameters);
        evalOrthorectification(parameters);
        evalConditionsAndBands(parameters);
        evalConditionsOperator(parameters);
        evalLoggingParams(parameters);
        evalTiePointGridParameter();
    }

    private void evalOrthorectification(Parameter[] parameters) throws ProcessorException {
        final Parameter orthorectificationParam = MosaicUtils.askForParameter(parameters,
                                                                              MosaicConstants.PARAM_NAME_ORTHORECTIFY_INPUT_PRODUCTS);
        if (orthorectificationParam == null) {
            _orthorectifyInputProducts = MosaicConstants.PARAM_DEFAULT_ORTHORECTIFY_INPUT_PRODUCTS;
        } else {
            _orthorectifyInputProducts = MosaicUtils.isTrue(orthorectificationParam);
        }

        if (_orthorectifyInputProducts) {
            _elevationModelName = null;
            final Parameter elevationParam = MosaicUtils.askForParameter(parameters,
                                                                         MosaicConstants.PARAM_NAME_ELEVATION_MODEL_FOR_ORTHORECTIFICATION);

            if (elevationParam != null) {
                _elevationModelName = elevationParam.getValueAsText();
            }
            if (_elevationModelName == null || "".equals(_elevationModelName)) {
                _elevationModelName = MosaicConstants.PARAM_DEFAULT_ELEVATION_MODEL_FOR_ORTHORECTIFICATION;
            }
            final ElevationModelDescriptor demDescriptor = ElevationModelRegistry.getInstance().getDescriptor(
                    _elevationModelName);
            if (demDescriptor == null) {
                throw new ProcessorException("DEM not supported: " + _elevationModelName);
            }
            if (!demDescriptor.isDemInstalled()) {
                throw new ProcessorException("DEM not installed: " + demDescriptor.getName());
            }
        }
    }

    private void evalConditionsOperator(Parameter[] parameters) {
        final Parameter operatorParameter = MosaicUtils.askForParameter(parameters,
                                                                        MosaicConstants.PARAM_NAME_CONDITION_OPERATOR);
        if (operatorParameter == null) {
            _testsOrCombined = true;
        } else {
            _testsOrCombined = MosaicConstants.PARAM_DEFAULT_VALUE_CONDITION_OPERATOR.equalsIgnoreCase(
                    operatorParameter.getValueAsText());
        }
    }

    private void evalCornerParameters(final Parameter[] parameters) {
        final Parameter westLon = MosaicUtils.askForParameter(parameters, MosaicConstants.PARAM_NAME_WEST_LON);
        final Parameter eastLon = MosaicUtils.askForParameter(parameters, MosaicConstants.PARAM_NAME_EAST_LON);
        final Parameter northLat = MosaicUtils.askForParameter(parameters, MosaicConstants.PARAM_NAME_NORTH_LAT);
        final Parameter southLat = MosaicUtils.askForParameter(parameters, MosaicConstants.PARAM_NAME_SOUTH_LAT);
        _cornerParams = new CornerParams(westLon, eastLon, northLat, southLat);
    }

    private void evalMapParameters(final Parameter[] parameters) {
        final Parameter outputWidth = MosaicUtils.askForParameter(parameters, MosaicConstants.PARAM_NAME_OUTPUT_WIDTH);
        final Parameter outputHeight = MosaicUtils.askForParameter(parameters,
                                                                   MosaicConstants.PARAM_NAME_OUTPUT_HEIGHT);

        final Parameter northing = MosaicUtils.askForParameter(parameters, MosaicConstants.PARAM_NAME_NORTHING);
        final Parameter easting = MosaicUtils.askForParameter(parameters, MosaicConstants.PARAM_NAME_EASTING);
        _northEastMapParams = new NorthEastMapParams(northing, easting, outputWidth, outputHeight);

        final Parameter centerLat = MosaicUtils.askForParameter(parameters, MosaicConstants.PARAM_NAME_CENTER_LAT);
        final Parameter centerLon = MosaicUtils.askForParameter(parameters, MosaicConstants.PARAM_NAME_CENTER_LON);
        _centerLatLonMapParams = new CenterLatLonMapParams(centerLat, centerLon, outputWidth, outputHeight);

        _fitOutputParam = MosaicUtils.askForParameter(parameters, MosaicConstants.PARAM_NAME_FIT_OUTPUT);
    }

    private void evalProjectionParameters(final Parameter[] parameters) {
        _projectionParams = new ProjectionParams(
                MosaicUtils.askForParameter(parameters, MosaicConstants.PARAM_NAME_PROJECTION_NAME),
                MosaicUtils.askForParameter(parameters, MosaicConstants.PARAM_NAME_PROJECTION_PARAMETERS),
                MosaicUtils.askForParameter(parameters, MosaicConstants.PARAM_NAME_PIXEL_SIZE_X),
                MosaicUtils.askForParameter(parameters, MosaicConstants.PARAM_NAME_PIXEL_SIZE_Y),
                MosaicUtils.askForParameter(parameters, MosaicConstants.PARAM_NAME_RESAMPLING_METHOD),
                MosaicUtils.askForParameter(parameters, MosaicConstants.PARAM_NAME_NO_DATA_VALUE));
    }

    private void evalTiePointGridParameter() {
        if (_projectionMode) {
            Parameter tpgParameter = getRequest().getParameter(MosaicConstants.PARAM_NAME_INCLUDE_TIE_POINT_GRIDS);
            _includeTiePointGrids = tpgParameter == null || Boolean.parseBoolean(tpgParameter.getValueAsText());
        } else {
            _includeTiePointGrids = true;
        }
    }

    private void evalPixelGeoCodingParameters(Parameter[] parameters) {
        _pixelGeoCodingParams = new PixelGeoCodingParams(
                MosaicUtils.askForParameter(parameters, MosaicConstants.PARAM_NAME_GEOCODING_LATITUDES),
                MosaicUtils.askForParameter(parameters, MosaicConstants.PARAM_NAME_GEOCODING_LONGITUDES),
                MosaicUtils.askForParameter(parameters, MosaicConstants.PARAM_NAME_GEOCODING_VALID_MASK),
                MosaicUtils.askForParameter(parameters, MosaicConstants.PARAM_NAME_GEOCODING_SEARCH_RADIUS));
    }

    private void evalConditionsAndBands(final Parameter[] parameters) {
        final List<MosaicUtils.MosaicIoChannel> outputVariables = new ArrayList<MosaicUtils.MosaicIoChannel>(5);
        final List<MosaicUtils.MosaicIoChannel> testVariables = new ArrayList<MosaicUtils.MosaicIoChannel>(5);
        if (!_projectionMode) {
            final List<MosaicUtils.MosaicIoChannel> outputChannelList = MosaicUtils.extractVariables(parameters);
            for (MosaicUtils.MosaicIoChannel outputChannel : outputChannelList) {
                final MosaicUtils.MosaicVariable variable = outputChannel.getVariable();
                if (variable.isCondition()) {
                    testVariables.add(outputChannel);
                }
                if (variable.isOutput()) {
                    outputVariables.add(outputChannel);
                }
            }
            _countChannel = new MosaicUtils.MosaicIoChannel(COUNT_VARIABLE);
            outputVariables.add(_countChannel);
        }
        _outputChannels = outputVariables.toArray(new MosaicUtils.MosaicIoChannel[outputVariables.size()]);
        _testChannels = testVariables.toArray(new MosaicUtils.MosaicIoChannel[testVariables.size()]);
    }

    private void evalLoggingParams(final Parameter[] parameters) {
        // todo - implement
    }

///////////////////////////////////////
//////////  GENERAL PRIVATE  //////////
///////////////////////////////////////

    private ProductWriter getProductWriter() {
        return _productWriter;
    }

/////////////////////////////////////
//////////  INNER CLASSES  //////////
/////////////////////////////////////

    private static class CornerParams {

        private final Parameter westLon;
        private final Parameter eastLon;
        private final Parameter northLat;
        private final Parameter southLat;

        public CornerParams(Parameter westLong, Parameter eastLong, Parameter northLat, Parameter southLat) {
            this.westLon = westLong;
            this.eastLon = eastLong;
            this.northLat = northLat;
            this.southLat = southLat;
        }

        public boolean isValid() {
            return eastLon != null
                    && northLat != null
                    && southLat != null
                    && westLon != null;
        }

        public float getNorthLat() {
            return MosaicUtils.getFloat(northLat);
        }

        public float getWestLon() {
            return MosaicUtils.getFloat(westLon);
        }

        public float getSouthLat() {
            return MosaicUtils.getFloat(southLat);
        }

        public float getEastLon() {
            return MosaicUtils.getFloat(eastLon);
        }
    }

    private static class NorthEastMapParams {

        private final Parameter northing;
        private final Parameter easting;
        private final Parameter outputWidth;
        private final Parameter outputHeight;

        public NorthEastMapParams(Parameter northing, Parameter easting, Parameter outputWidth,
                                  Parameter outputHeight) {
            this.northing = northing;
            this.easting = easting;
            this.outputWidth = outputWidth;
            this.outputHeight = outputHeight;
        }

        public boolean isValid() {
            return northing != null
                    && easting != null
                    && outputWidth != null
                    && outputHeight != null;
        }

        public float getNorthing() {
            return MosaicUtils.getFloat(northing);
        }

        public float getEasting() {
            return MosaicUtils.getFloat(easting);
        }

        public float getOutputWidth() {
            return MosaicUtils.getFloat(outputWidth);
        }

        public float getOutputHeight() {
            return MosaicUtils.getFloat(outputHeight);
        }
    }

    private static class CenterLatLonMapParams {

        private final Parameter centerLat;
        private final Parameter centerLon;
        private final Parameter outputWidth;
        private final Parameter outputHeight;

        public CenterLatLonMapParams(Parameter centerLat, Parameter centerLon, Parameter outputWidth,
                                     Parameter outputHeight) {
            this.centerLat = centerLat;
            this.centerLon = centerLon;
            this.outputWidth = outputWidth;
            this.outputHeight = outputHeight;
        }

        public boolean isValid() {
            return centerLat != null
                    && centerLon != null
                    && outputWidth != null
                    && outputHeight != null;
        }

        public float getCenterLat() {
            return MosaicUtils.getFloat(centerLat);
        }

        public float getCenterLon() {
            return MosaicUtils.getFloat(centerLon);
        }

        public float getOutputWidth() {
            return MosaicUtils.getFloat(outputWidth);
        }

        public float getOutputHeight() {
            return MosaicUtils.getFloat(outputHeight);
        }

        public GeoPos getCenterPos() {
            return new GeoPos(getCenterLat(), getCenterLon());
        }
    }

    private static class ProjectionParams {

        private final Parameter name;
        private final Parameter parameters;
        private final Parameter pixelSizeX;
        private final Parameter pixelSizeY;
        private final Parameter resampling;
        private final Parameter noDataValue;

        public ProjectionParams(final Parameter name,
                                final Parameter parameters,
                                final Parameter pixelSizeX,
                                final Parameter pixelSizeY,
                                final Parameter resampling,
                                final Parameter noDataValue) {
            this.name = name;
            this.parameters = parameters;
            this.pixelSizeX = pixelSizeX;
            this.pixelSizeY = pixelSizeY;
            this.resampling = resampling;
            this.noDataValue = noDataValue;
        }

        public boolean isValid() {
            return name != null && pixelSizeX != null && pixelSizeY != null;
        }

        public String getName() {
            return name.getValueAsText();
        }

        public float getPixelSizeX() {
            return MosaicUtils.getFloat(pixelSizeX);
        }

        public float getPixelSizeY() {
            return MosaicUtils.getFloat(pixelSizeY);
        }

        public double getNoDataValue() {
            if (noDataValue == null) {
                return MapInfo.DEFAULT_NO_DATA_VALUE;
            }
            return MosaicUtils.getDouble(noDataValue);
        }

        public Resampling getResamplingMethod() {
            if (resampling != null) {
                final String resamplingName = resampling.getValueAsText();
                final Resampling resampling = ResamplingFactory.createResampling(resamplingName);
                if (resampling != null) {
                    return resampling;
                }
            }
            return Resampling.NEAREST_NEIGHBOUR;
        }

        private double[] getParameters() throws ProcessorException {
            if (parameters == null) {
                return null;
            }
            final String[] values = (String[]) parameters.getValue();
            final double[] doubles = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                try {
                    doubles[i] = Double.parseDouble(values[i]);
                } catch (NumberFormatException e) {
                    throw new ProcessorException(
                            "Illegal projection parameters, list of numbers expected but found '" + values[i] + "'."); /*I18N*/
                }
            }
            return doubles;
        }
    }

    private static class PixelGeoCodingParams {

        private final Parameter sourceLatitudes;
        private final Parameter sourceLongitudes;
        private final Parameter sourceValidMask;
        private final Parameter sourceSearchRadius;

        public PixelGeoCodingParams(final Parameter sourceLatitudes,
                                    final Parameter sourceLongitudes,
                                    final Parameter sourceValidMask,
                                    final Parameter sourceSearchRadius) {
            this.sourceLatitudes = sourceLatitudes;
            this.sourceLongitudes = sourceLongitudes;
            this.sourceValidMask = sourceValidMask;
            this.sourceSearchRadius = sourceSearchRadius;
        }

        public boolean isValid() {
            return getSourceLatitudes() != null && getSourceLongitudes() != null;
        }

        public String getSourceLatitudes() {
            return getValue(sourceLatitudes);
        }

        public String getSourceLongitudes() {
            return getValue(sourceLongitudes);
        }

        public String getSourceValidMask() {
            return getValue(sourceValidMask);
        }

        public int getSourceSearchRadius() {
            return sourceSearchRadius != null ? (Integer) sourceSearchRadius.getValue() : MosaicConstants.DEFAULT_GEOCODING_SEARCH_RADIUS;
        }

        private static String getValue(Parameter param) {
            if (param != null) {
                String valueAsText = param.getValueAsText().trim();
                return valueAsText.length() > 0 ? valueAsText : null;
            } else {
                return null;
            }
        }
    }
}
