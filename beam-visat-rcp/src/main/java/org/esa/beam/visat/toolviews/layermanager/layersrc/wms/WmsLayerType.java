package org.esa.beam.visat.toolviews.layermanager.layersrc.wms;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import java.awt.Dimension;
import java.net.URL;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
public class WmsLayerType extends LayerType {

    public static final String PROPERTY_WMS_RASTER = "wms.raster";
    public static final String PROPERTY_WMS_URL = "wms.serverUrl";
    public static final String PROPERTY_WMS_LAYER_INDEX = "wms.layerIndex";
    public static final String PROPERTY_WMS_CRSENVELOPE = "wms.crsEnvelope";
    public static final String PROPERTY_WMS_STYLE_NAME = "wms.styleName";
    public static final String PROPERTY_WMS_IMAGE_SIZE = "wms.imageSize";


    @Override
    public String getName() {
        return "Wms Layer";
    }

    @Override
    public boolean isValidFor(LayerContext ctx) {
        return DefaultGeographicCRS.WGS84.equals(ctx.getCoordinateReferenceSystem());
    }

    @Override
    protected com.bc.ceres.glayer.Layer createLayerImpl(LayerContext ctx, ValueContainer configuration) {
        return new WmsLayer(configuration);

    }

    @Override
    public ValueContainer getConfigurationTemplate() {
        final ValueContainer template = new ValueContainer();

        template.addModel(createDefaultValueModel(PROPERTY_WMS_RASTER, RasterDataNode.class));
        template.addModel(createDefaultValueModel(PROPERTY_WMS_URL, URL.class));
        template.addModel(createDefaultValueModel(PROPERTY_WMS_LAYER_INDEX, Integer.class));
        template.addModel(createDefaultValueModel(PROPERTY_WMS_STYLE_NAME, String.class));
        template.addModel(createDefaultValueModel(PROPERTY_WMS_IMAGE_SIZE, Dimension.class));
        template.addModel(createDefaultValueModel(PROPERTY_WMS_CRSENVELOPE, CRSEnvelope.class));

        return template;
    }

}
