package org.esa.beam.visat.toolviews.layermanager.layersrc.wms;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerTypeRegistry;
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
import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
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
        final AffineTransform i2mTransform = multiLevelSource.getModel().getImageToModelTransform(0);

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

}
