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

package org.esa.beam.visat.toolviews.layermanager.layersrc.wms;

import com.bc.ceres.binding.*;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.annotations.LayerTypeMetadata;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ImageManager;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.StyleImpl;
import org.geotools.data.wms.WebMapServer;
import org.geotools.data.wms.request.GetMapRequest;
import org.geotools.data.wms.response.GetMapResponse;
import org.geotools.ows.ServiceException;

import javax.imageio.ImageIO;
import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * Layer type for layer that displays images coming from an OGC WMS.
 *
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
@LayerTypeMetadata(name = "WmsLayerType",
                   aliasNames = {"org.esa.beam.visat.toolviews.layermanager.layersrc.wms.WmsLayerType"})
public class WmsLayerType extends ImageLayer.Type {

    public static final String PROPERTY_NAME_RASTER = "raster";
    public static final String PROPERTY_NAME_URL = "serverUrl";
    public static final String PROPERTY_NAME_LAYER_INDEX = "layerIndex";
    public static final String PROPERTY_NAME_CRS_ENVELOPE = "crsEnvelope";
    public static final String PROPERTY_NAME_STYLE_NAME = "styleName";
    public static final String PROPERTY_NAME_IMAGE_SIZE = "imageSize";

    @Override
    public Layer createLayer(LayerContext ctx, PropertySet configuration) {
        final WebMapServer mapServer;
        try {
            mapServer = getWmsServer(configuration);
        } catch (Exception e) {
            final String message = String.format("Not able to access Web Mapping Server: %s",
                                                 configuration.getValue(WmsLayerType.PROPERTY_NAME_URL));
            throw new RuntimeException(message, e);
        }
        final int layerIndex = (Integer) configuration.getValue(WmsLayerType.PROPERTY_NAME_LAYER_INDEX);
        final org.geotools.data.ows.Layer wmsLayer = getLayer(mapServer, layerIndex);
        final MultiLevelSource multiLevelSource = createMultiLevelSource(configuration, mapServer, wmsLayer);

        final ImageLayer.Type imageLayerType = LayerTypeRegistry.getLayerType(ImageLayer.Type.class);
        final PropertySet config = imageLayerType.createLayerConfig(ctx);
        config.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
        config.setValue(ImageLayer.PROPERTY_NAME_BORDER_SHOWN, false);

        final ImageLayer wmsImageLayer = new ImageLayer(this, multiLevelSource, config);
        wmsImageLayer.setName(wmsLayer.getName());

        return wmsImageLayer;

    }

    @Override
    public PropertySet createLayerConfig(LayerContext ctx) {
        final PropertyContainer template = new PropertyContainer();

        template.addProperty(Property.create(PROPERTY_NAME_RASTER, RasterDataNode.class));
        template.addProperty(Property.create(PROPERTY_NAME_URL, URL.class));
        template.addProperty(Property.create(PROPERTY_NAME_LAYER_INDEX, Integer.class));
        template.addProperty(Property.create(PROPERTY_NAME_STYLE_NAME, String.class));
        template.addProperty(Property.create(PROPERTY_NAME_IMAGE_SIZE, Dimension.class));
        template.addProperty(Property.create(PROPERTY_NAME_CRS_ENVELOPE, CRSEnvelope.class));
        template.getDescriptor(PROPERTY_NAME_CRS_ENVELOPE).setDomConverter(new CRSEnvelopeDomConverter());

        return template;
    }

    @SuppressWarnings({"unchecked"})
    private static DefaultMultiLevelSource createMultiLevelSource(PropertySet configuration,
                                                                  WebMapServer wmsServer,
                                                                  org.geotools.data.ows.Layer layer) {
        DefaultMultiLevelSource multiLevelSource;
        final String styleName = (String) configuration.getValue(WmsLayerType.PROPERTY_NAME_STYLE_NAME);
        final Dimension size = (Dimension) configuration.getValue(WmsLayerType.PROPERTY_NAME_IMAGE_SIZE);
        try {
            List<StyleImpl> styleList = layer.getStyles();
            StyleImpl style = null;
            if (!styleList.isEmpty()) {
                style = styleList.get(0);
                for (StyleImpl currentstyle : styleList) {
                    if (currentstyle.getName().equals(styleName)) {
                        style = currentstyle;
                    }
                }
            }
            CRSEnvelope crsEnvelope = (CRSEnvelope) configuration.getValue(WmsLayerType.PROPERTY_NAME_CRS_ENVELOPE);
            GetMapRequest mapRequest = wmsServer.createGetMapRequest();
            mapRequest.addLayer(layer, style);
            mapRequest.setTransparent(true);
            mapRequest.setDimensions(size.width, size.height);
            mapRequest.setSRS(crsEnvelope.getEPSGCode()); // e.g. "EPSG:4326" = Geographic CRS
            mapRequest.setBBox(crsEnvelope);
            mapRequest.setFormat("image/png");
            final PlanarImage image = PlanarImage.wrapRenderedImage(downloadWmsImage(mapRequest, wmsServer));
            RasterDataNode raster = (RasterDataNode) configuration.getValue(WmsLayerType.PROPERTY_NAME_RASTER);

            final int sceneWidth = raster.getSceneRasterWidth();
            final int sceneHeight = raster.getSceneRasterHeight();
            AffineTransform i2mTransform = ImageManager.getImageToModelTransform(raster.getGeoCoding());
            i2mTransform.scale((double) sceneWidth / image.getWidth(), (double) sceneHeight / image.getHeight());
            final Rectangle2D bounds = DefaultMultiLevelModel.getModelBounds(i2mTransform, image);
            final DefaultMultiLevelModel multiLevelModel = new DefaultMultiLevelModel(1, i2mTransform, bounds);
            multiLevelSource = new DefaultMultiLevelSource(image, multiLevelModel);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Failed to access WMS: %s", configuration.getValue(
                    WmsLayerType.PROPERTY_NAME_URL)), e);
        }
        return multiLevelSource;
    }

    private static org.geotools.data.ows.Layer getLayer(WebMapServer server, int layerIndex) {
        return server.getCapabilities().getLayerList().get(layerIndex);
    }

    private static WebMapServer getWmsServer(PropertySet configuration) throws IOException, ServiceException {
        return new WebMapServer((URL) configuration.getValue(WmsLayerType.PROPERTY_NAME_URL));
    }

    private static BufferedImage downloadWmsImage(GetMapRequest mapRequest, WebMapServer wms) throws IOException,
            ServiceException {
        GetMapResponse mapResponse = wms.issueRequest(mapRequest);
        InputStream inputStream = mapResponse.getInputStream();
        try {
            return ImageIO.read(inputStream);
        } finally {
            inputStream.close();
        }
    }

    private static class CRSEnvelopeDomConverter implements DomConverter {
        private static final String SRS_NAME = "srsName";
        private static final String MIN_X = "minX";
        private static final String MIN_Y = "minY";
        private static final String MAX_X = "maxX";
        private static final String MAX_Y = "maxY";

        @Override
        public Class<?> getValueType() {
            return CRSEnvelope.class;
        }

        @Override
        public Object convertDomToValue(DomElement parentElement, Object value) throws ConversionException,
                ValidationException {
            try {
                String srsName = parentElement.getChild(SRS_NAME).getValue();
                double minX = Double.parseDouble(parentElement.getChild(MIN_X).getValue());
                double minY = Double.parseDouble(parentElement.getChild(MIN_Y).getValue());
                double maxX = Double.parseDouble(parentElement.getChild(MAX_X).getValue());
                double maxY = Double.parseDouble(parentElement.getChild(MAX_Y).getValue());
                value = new CRSEnvelope(srsName, minX, minY, maxX, maxY);
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
            return value;
        }

        @Override
        public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
            CRSEnvelope crsEnvelope = (CRSEnvelope) value;
            DomElement srsName = parentElement.createChild(SRS_NAME);
            srsName.setValue(crsEnvelope.getSRSName());
            DomElement minX = parentElement.createChild(MIN_X);
            minX.setValue(Double.toString(crsEnvelope.getMinX()));
            DomElement minY = parentElement.createChild(MIN_Y);
            minY.setValue(Double.toString(crsEnvelope.getMinY()));
            DomElement maxX = parentElement.createChild(MAX_X);
            maxX.setValue(Double.toString(crsEnvelope.getMaxX()));
            DomElement maxY = parentElement.createChild(MAX_Y);
            maxY.setValue(Double.toString(crsEnvelope.getMaxY()));
        }
    }
}
