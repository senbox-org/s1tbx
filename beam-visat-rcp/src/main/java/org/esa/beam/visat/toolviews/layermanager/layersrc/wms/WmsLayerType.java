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

    public static final String PROPERTY_NAME_RASTER = "raster";
    public static final String PROPERTY_NAME_URL = "serverUrl";
    public static final String PROPERTY_NAME_LAYER_INDEX = "layerIndex";
    public static final String PROPERTY_NAME_CRS_ENVELOPE = "crsEnvelope";
    public static final String PROPERTY_NAME_STYLE_NAME = "styleName";
    public static final String PROPERTY_NAME_IMAGE_SIZE = "imageSize";


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

        template.addModel(createDefaultValueModel(PROPERTY_NAME_RASTER, RasterDataNode.class));
        template.addModel(createDefaultValueModel(PROPERTY_NAME_URL, URL.class));
        template.addModel(createDefaultValueModel(PROPERTY_NAME_LAYER_INDEX, Integer.class));
        template.addModel(createDefaultValueModel(PROPERTY_NAME_STYLE_NAME, String.class));
        template.addModel(createDefaultValueModel(PROPERTY_NAME_IMAGE_SIZE, Dimension.class));
        template.addModel(createDefaultValueModel(PROPERTY_NAME_CRS_ENVELOPE, CRSEnvelope.class));

        return template;
    }

}
