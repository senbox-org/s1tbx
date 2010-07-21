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

import com.bc.jexp.Term;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.MapProjection;
import org.esa.beam.framework.dataop.maptransf.MapTransform;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.RequestElementFactory;
import org.esa.beam.framework.processor.RequestElementFactoryException;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.math.MathUtils;

import javax.swing.filechooser.FileFilter;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @deprecated since BEAM 4.7, replaced by GPF operator 'Mosaic'
 */
@Deprecated
public class MosaicUtils {

    public static Rectangle2D createOutputProductBoundaries(final MapTransform mapTransform, final GeoPos geoPosUL,
                                                            final GeoPos geoPosLR, final float cellSizeX,
                                                            final float cellSizeY) {

        final Point2D upperLeftPt = mapTransform.forward(geoPosUL, null);
        final Point2D lowerRightPt;
        if (geoPosUL.lon < geoPosLR.lon) {
            lowerRightPt = mapTransform.forward(geoPosLR, null);
        } else {
            geoPosLR.lon += 360;
            lowerRightPt = mapTransform.forward(geoPosLR, null);
            geoPosLR.lon -= 360;
        }

        final double ulX = upperLeftPt.getX();
        final double ulY = upperLeftPt.getY();
        final double lrX = lowerRightPt.getX();
        final double lrY = lowerRightPt.getY();

        final int width;
        final double min_x = Math.min(ulX, lrX);
        final double max_x = Math.max(ulX, lrX);
        width = MathUtils.floorInt((max_x - min_x) / cellSizeX);

        final int height;
        final double min_y = Math.min(ulY, lrY);
        final double max_y = Math.max(ulY, lrY);
        height = MathUtils.floorInt((max_y - min_y) / cellSizeY);

        final Rectangle2D outputRect = new Rectangle2D.Float();
        if (geoPosUL.lat > geoPosLR.lat) {
            outputRect.setRect(ulX, ulY, width, height);
        } else {
            outputRect.setRect(ulX, lrY, width, height);
        }
        return outputRect;
    }

    public static Rectangle2D createOutputProductBoundaries(final MapProjection projection, final GeoPos geoPosUL,
                                                            final GeoPos geoPosLR, final float pixelSizeX,
                                                            final float pixelSizeY) {

        final MapTransform mapTransform = projection.getMapTransform();

        final Point2D upperLeftPt = mapTransform.forward(geoPosUL, null);
        final float ulX = (float) upperLeftPt.getX();
        final float ulY = (float) upperLeftPt.getY();

        // TODO - (nf) make Datum editable and ensure that all input products use the same datum (or provide datum transformation)
        final MapInfo mapInfo = new MapInfo(projection, 0.5f, 0.5f, ulX, ulY, pixelSizeX, pixelSizeY, Datum.WGS_84);
        final MapGeoCoding mapGeoCoding = new MapGeoCoding(mapInfo);

        final PixelPos pixelPos;
        if (geoPosUL.lon < geoPosLR.lon) {
            pixelPos = mapGeoCoding.getPixelPos(geoPosLR, null);
        } else {
            geoPosLR.lon += 360;
            pixelPos = mapGeoCoding.getPixelPos(geoPosLR, null);
            geoPosLR.lon -= 360;
        }

        final Rectangle2D outputRect = new Rectangle2D.Float();
        outputRect.setRect(ulX, ulY, Math.abs(pixelPos.getX()), Math.abs(pixelPos.getY()));
        return outputRect;
    }

    public static Product createGeocodedProduct(final Rectangle2D outputRect, final String productName,
                                                final MapProjection projection, final float cellSizeX,
                                                final float cellSizeY) {
        final int sceneRasterWidth = (int) outputRect.getWidth();
        final int sceneRasterHeight = (int) outputRect.getHeight();

        final Product outputProduct = new Product(productName, MosaicConstants.OUTPUT_PRODUCT_TYPE,
                                                  sceneRasterWidth, sceneRasterHeight);

        // TODO - (nf) make Datum editable and ensure that all input products use the same datum (or provide datum transformation)
        final MapInfo mapInfo = new MapInfo(projection,
                                            0.5F, 0.5F,
                                            (float) outputRect.getX(), (float) outputRect.getY(),
                                            cellSizeX, cellSizeY,
                                            Datum.WGS_84);
        mapInfo.setSceneWidth(sceneRasterWidth);
        mapInfo.setSceneHeight(sceneRasterHeight);

        outputProduct.setGeoCoding(new MapGeoCoding(mapInfo));
        return outputProduct;
    }

    public static GeoPos getCenterCoordinate(final GeoPos geoPosUL, final GeoPos geoPosLR) {
        final float ulLon = geoPosUL.getLon();
        final float ulLat = geoPosUL.getLat();
        float lrLon = geoPosLR.getLon();
        float lrLat = geoPosLR.getLat();
        if (lrLon < ulLon) {
            lrLon += 360;
        }
        if (lrLat > ulLat) {
            lrLat -= 180;
        }
        float centerLon = (lrLon - ulLon) / 2 + ulLon;
        if (centerLon > 180) {
            centerLon -= 360;
        }
        float centerLat = (lrLat - ulLat) / 2 + ulLat;
        if (centerLat < -90) {
            centerLat += 180;
        }
        return new GeoPos(centerLat, centerLon);
    }

    /**
     * extracts the zone number of an 'UTM Zone' projection name.
     *
     * @param projectionName the projection name.
     *
     * @return the zone number or -1 if it is not a valid 'UTM Zone' projection name
     */
    public static int extractUTMZoneNumber(String projectionName) {
        if (projectionName.matches("UTM Zone \\d.*")) {
            final int commaIndex = projectionName.indexOf(",");
            final String s;
            if (commaIndex == -1) {
                final int spaceIndex = projectionName.lastIndexOf(" ");
                s = projectionName.substring(spaceIndex + 1);
            } else {
                final int spaceIndex = projectionName.lastIndexOf(" ", commaIndex);
                s = projectionName.substring(spaceIndex + 1, commaIndex);
            }
            return Integer.parseInt(s);
        }
        return -1;
    }

    public static BeamFileChooser createBeamFileChooser(final int fileSelectionMode,
                                                        final boolean multiSelectionEnabled,
                                                        final String currentDirectoryPath,
                                                        final String defaultFormatName) {
        final BeamFileChooser beamFileChooser = new BeamFileChooser();
        beamFileChooser.setAcceptAllFileFilterUsed(true);
        beamFileChooser.setFileSelectionMode(fileSelectionMode);
        beamFileChooser.setMultiSelectionEnabled(multiSelectionEnabled);
        if (currentDirectoryPath != null) {
            beamFileChooser.setCurrentDirectory(new File(currentDirectoryPath));
        } else {
            beamFileChooser.setCurrentDirectory(SystemUtils.getUserHomeDir());
        }
        final Iterator allReaderPlugIns = ProductIOPlugInManager.getInstance().getAllReaderPlugIns();
        while (allReaderPlugIns.hasNext()) {
            ProductReaderPlugIn readerPlugIn = (ProductReaderPlugIn) allReaderPlugIns.next();
            final String[] formatNames = readerPlugIn.getFormatNames();

            final BeamFileFilter beamFileFilter = new BeamFileFilter(StringUtils.arrayToCsv(formatNames),
                                                                     readerPlugIn.getDefaultFileExtensions(),
                                                                     readerPlugIn.getDescription(null));
            beamFileChooser.addChoosableFileFilter(beamFileFilter);
        }
        if (defaultFormatName != null) {
            final FileFilter[] choosableFileFilters = beamFileChooser.getChoosableFileFilters();
            for (FileFilter fileFilter : choosableFileFilters) {
                if (fileFilter instanceof BeamFileFilter) {
                    BeamFileFilter filter = (BeamFileFilter) fileFilter;
                    final String formatName = filter.getFormatName();
                    if (formatName.indexOf(defaultFormatName) != -1) {
                        beamFileChooser.setFileFilter(filter);
                        break;
                    }
                }
            }
        }
        return beamFileChooser;
    }

    public static List<MosaicIoChannel> extractVariables(final Parameter[] parameters) {
        Guardian.assertNotNull("parameters", parameters);
        final String suffixExpression = MosaicConstants.PARAM_SUFFIX_EXPRESSION;
        final String suffixCondition = MosaicConstants.PARAM_SUFFIX_CONDITION;
        final String suffixOutput = MosaicConstants.PARAM_SUFFIX_OUTPUT;
        // Using a list for expressions instead of a Map to keep them ordered
        // as they are given by the parameters
        final List<Object[]> expressions = new ArrayList<Object[]>();
        final Map<String, Parameter> conditions = new HashMap<String, Parameter>();
        final Map<String, Parameter> output = new HashMap<String, Parameter>();

        for (Parameter parameter : parameters) {
            final String name = parameter.getName();
            if (name.endsWith(suffixExpression)) {
                final String key = extraktKey(name, suffixExpression);
                expressions.add(new Object[]{key, parameter});
            } else if (name.endsWith(suffixCondition)) {
                final String key = extraktKey(name, suffixCondition);
                conditions.put(key, parameter);
            } else if (name.endsWith(suffixOutput)) {
                final String key = extraktKey(name, suffixOutput);
                output.put(key, parameter);
            }
        }

        final List<MosaicIoChannel> variables = new ArrayList<MosaicIoChannel>();
        for (final Object[] exprEntry : expressions) {
            final String name = (String) exprEntry[0];
            final Parameter exprParam = (Parameter) exprEntry[1];
            final Parameter condParam = conditions.get(name);
            final Parameter outpParam = output.get(name);
            if (exprParam != null) {
                final String exprString = exprParam.getValueAsText();
                final boolean isCondition = isTrue(condParam);
                final boolean isOutput = !isCondition || isTrue(outpParam);
                final MosaicVariable variable = new MosaicVariable(name, exprString, isCondition, isOutput);
                final MosaicIoChannel variableChannel = new MosaicIoChannel(variable);
                variables.add(variableChannel);
            }
        }
        return variables;
    }

    public static String extraktKey(final String name, final String suffixExpression) {
        Guardian.assertNotNull("name", name);
        Guardian.assertNotNull("suffixExpression", suffixExpression);
        return name.substring(0, name.length() - suffixExpression.length());
    }

    public static float getFloat(Parameter parameter) {
        Guardian.assertNotNull("parameter", parameter);
        return (Float) parameter.getValue();
    }

    public static double getDouble(Parameter parameter) {
        Guardian.assertNotNull("parameter", parameter);
        return ((Double) parameter.getValue()).doubleValue();
    }

    public static int getInt(Parameter parameter) {
        Guardian.assertNotNull("parameter", parameter);
        return (Integer) parameter.getValue();
    }

    public static boolean isTrue(Parameter parameter) {
        return parameter != null && "true".equalsIgnoreCase(parameter.getValueAsText());
    }

    /**
     * Extracts the processing parameters from the given product. The given product must be a product initialized by the
     * mosaicing processer, so the given product contains the initialize processing request as metadata elements.
     *
     * @param product must not be null
     *
     * @return a parameter array with all the parameters contained in the given product, never <code>null</code>.
     *
     * @throws ProcessorException if the given product contains no request information as metadata.
     */
    public static Parameter[] extractProcessingParameters(final Product product) throws ProcessorException {
        Guardian.assertNotNull("outputProduct", product);
        final MetadataElement metadataRoot = product.getMetadataRoot();
        final MetadataElement processingRequestRootElem = metadataRoot.getElement(
                Request.METADATA_ELEM_NAME_PROCESSING_REQUEST);
        if (processingRequestRootElem == null) {
            throw new ProcessorException("Missing mosaic initializing information in output product.");/*I18N*/
        }
        final MetadataElement requestParametersElement = processingRequestRootElem.getElement(
                Request.METADATA_ELEM_NAME_PARAMETERS);
        if (requestParametersElement == null) {
            throw new ProcessorException("Missing mosaic processing parameters in output product."); /*I18N*/
        }
        final MetadataAttribute[] attributes = requestParametersElement.getAttributes();
        final Parameter[] parameters = new Parameter[attributes.length];
        final RequestElementFactory factory = MosaicRequestElementFactory.getInstance();
        for (int i = 0; i < attributes.length; i++) {
            final MetadataAttribute attribute = attributes[i];
            final String name = attribute.getName();
            final String value = attribute.getData().getElemString();
            // >>>>>>  duplicated code in Request loader
            // >>>>>> @todo 1 se/** - when make a refactoring of processing framework
            // >>>>>>                 move this to the base class of all request element factories
            // >>>>>>                 to the method createParameter(name, value)
            if (name.equalsIgnoreCase(ProcessorConstants.LOG_PREFIX_PARAM_NAME)) {
                parameters[i] = factory.createDefaultLogPatternParameter(value);
            } else if (name.equalsIgnoreCase(ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME)) {
                try {
                    parameters[i] = factory.createLogToOutputParameter(value);
                } catch (ParamValidateException e) {
                    throw new RequestElementFactoryException(e.getMessage());
                }
                // <<<<<< duplicated code in Request loader
            } else {
                parameters[i] = factory.createParameter(name, value);
            }
        }
        return parameters;
    }

    /**
     * Returns the first parameter with the given name from the given parameter array.
     *
     * @param parameters the given parametr array.
     * @param name       the name for the parameter to find.
     *
     * @return the parameter with the given name or <code>null</code> if there is no one inside the given parameter
     *         array.
     */
    public static Parameter askForParameter(final Parameter[] parameters, String name) {
        if (parameters != null && name != null) {
            for (Parameter parameter : parameters) {
                if (parameter.getName().equals(name)) {
                    return parameter;
                }
            }
        }
        return null;
    }

    public static class MosaicVariable {

        private final String _name;
        private final String _expression;
        private final boolean _useAsCondition;
        private final boolean _outputAsCount;

        public MosaicVariable(String name, String expression, boolean useAsCondition, boolean outputAsCount) {
            _name = name;
            _expression = expression;
            _useAsCondition = useAsCondition;
            _outputAsCount = outputAsCount;
        }

        public String getName() {
            return _name;
        }

        public String getExpression() {
            return _expression;
        }

        public boolean isCondition() {
            return _useAsCondition;
        }

        public boolean isOutput() {
            return !isCondition() || _outputAsCount;
        }
    }

    public static class MosaicIoChannel {

        private MosaicVariable variable;
        private Band destBand;
        private ProductData destData;
        private Term term;
        private RasterDataNode[] refRasters;

        public MosaicIoChannel(MosaicVariable variable) {
            this.variable = variable;
        }

        public MosaicVariable getVariable() {
            return variable;
        }

        public Band getDestBand() {
            return destBand;
        }

        public void setDestBand(Band band) {
            destBand = band;
        }

        public ProductData getDestData() {
            return destData;
        }

        public void setDestData(ProductData destData) {
            this.destData = destData;
        }

        public Term getTerm() {
            return term;
        }

        public void setTerm(Term term) {
            this.term = term;
        }

        public RasterDataNode[] getRefRasters() {
            return refRasters;
        }

        public void setRefRasters(RasterDataNode[] refRasters) {
            this.refRasters = refRasters;
        }
    }
}
