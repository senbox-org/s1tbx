package org.esa.beam.visat.toolviews.layermanager.layersrc.wms;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import com.bc.ceres.grender.Rendering;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.wms.WebMapServer;
import org.geotools.data.wms.request.GetMapRequest;
import org.geotools.data.wms.response.GetMapResponse;
import org.geotools.ows.ServiceException;
import org.opengis.layer.Style;

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

public class WmsLayer extends Layer {

    private final ImageLayer layerDelegate;

    protected WmsLayer(ValueContainer configuration) {
        super(LayerType.getLayerType(WmsLayerType.class.getName()), configuration);
        final WebMapServer mapServer;
        try {
            mapServer = getWmsServer(configuration);
        } catch (Exception e) {
            final String message = String.format("Not able to access Web Map Server: %s",
                                                 configuration.getValue(WmsLayerType.PROPERTY_WMS_URL));
            throw new RuntimeException(message, e);
        }
        final int layerIndex = (Integer) configuration.getValue(WmsLayerType.PROPERTY_WMS_LAYER_INDEX);
        final org.geotools.data.ows.Layer layer = getLayer(mapServer, layerIndex);
        final MultiLevelSource multiLevelSource = createMultiLevelSource(configuration, mapServer, layer);

        final ImageLayer.Type imageLayerType = (ImageLayer.Type) LayerType.getLayerType(
                ImageLayer.Type.class.getName());
        final ValueContainer template = imageLayerType.getConfigurationTemplate();
        try {
            template.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
            template.setValue(ImageLayer.PROPERTY_NAME_IMAGE_TO_MODEL_TRANSFORM,
                              multiLevelSource.getModel().getImageToModelTransform(0));
            template.setValue(ImageLayer.PROPERTY_NAME_BORDER_SHOWN, false);
        } catch (ValidationException e) {
            throw new IllegalStateException(e);
        }

        layerDelegate = new ImageLayer(imageLayerType, template);
        setName(layer.getName());

    }


    @Override
    protected void renderLayer(Rendering rendering) {
        layerDelegate.render(rendering);

    }

    @Override
    protected Rectangle2D getLayerModelBounds() {
        return layerDelegate.getModelBounds();
    }

    @Override
    protected void disposeLayer() {
        layerDelegate.dispose();
    }

    @Override
    public void regenerate() {
        layerDelegate.regenerate();
    }


    private static DefaultMultiLevelSource createMultiLevelSource(ValueContainer configuration,
                                                                  WebMapServer wmsServer,
                                                                  org.geotools.data.ows.Layer layer) {
        DefaultMultiLevelSource multiLevelSource;
        final String styleName = (String) configuration.getValue(WmsLayerType.PROPERTY_WMS_STYLE_NAME);
        final Dimension size = (Dimension) configuration.getValue(WmsLayerType.PROPERTY_WMS_IMAGE_SIZE);
        try {
            List<Style> styleList = layer.getStyles();
            Style style = null;
            if (!styleList.isEmpty()) {
                style = styleList.get(0);
                for (Style currentstyle : styleList) {
                    if (currentstyle.getName().equals(styleName)) {
                        style = currentstyle;
                    }
                }
            }
            CRSEnvelope crsEnvelope = (CRSEnvelope) configuration.getValue(WmsLayerType.PROPERTY_WMS_CRSENVELOPE);
            GetMapRequest mapRequest = wmsServer.createGetMapRequest();
            mapRequest.addLayer(layer, style);
            mapRequest.setTransparent(true);
            mapRequest.setDimensions(size.width, size.height);
            mapRequest.setSRS(crsEnvelope.getEPSGCode()); // e.g. "EPSG:4326" = Geographic CRS
            mapRequest.setBBox(crsEnvelope);
            mapRequest.setFormat("image/png");
            final PlanarImage image = PlanarImage.wrapRenderedImage(downloadWmsImage(mapRequest, wmsServer));
            RasterDataNode raster = (RasterDataNode) configuration.getValue(WmsLayerType.PROPERTY_WMS_RASTER);

            final int sceneWidth = raster.getSceneRasterWidth();
            final int sceneHeight = raster.getSceneRasterHeight();
            final AffineTransform g2mTransform = raster.getGeoCoding().getImageToModelTransform();
            AffineTransform i2mTransform = new AffineTransform(g2mTransform);
            i2mTransform.scale((double) sceneWidth / image.getWidth(), (double) sceneHeight / image.getHeight());
            final Rectangle2D bounds = DefaultMultiLevelModel.getModelBounds(i2mTransform, image);
            final DefaultMultiLevelModel multiLevelModel = new DefaultMultiLevelModel(1, i2mTransform, bounds);
            multiLevelSource = new DefaultMultiLevelSource(image, multiLevelModel);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Failed to access WMS: %s", configuration.getValue(
                    WmsLayerType.PROPERTY_WMS_URL)), e);
        }
        return multiLevelSource;
    }

    private static org.geotools.data.ows.Layer getLayer(WebMapServer server, int layerIndex) {
        return server.getCapabilities().getLayerList().get(layerIndex);
    }

    private static WebMapServer getWmsServer(ValueContainer configuration) throws IOException, ServiceException {
        return new WebMapServer((URL) configuration.getValue(WmsLayerType.PROPERTY_WMS_URL));
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
