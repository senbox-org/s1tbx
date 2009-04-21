package org.esa.beam.glayer;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glevel.BandImageMultiLevelSource;

import java.awt.Color;

public class RasterImageLayerType extends ImageLayer.Type {

    public static final String PROPERTY_NAME_RASTERS = "rasters";

    @Override
    public String getName() {
        return "Raster Data Layer";
    }

    @Override
    public boolean isValidFor(LayerContext ctx) {
        return true;
    }

    @Override
    protected ImageLayer createLayerImpl(LayerContext ctx, ValueContainer configuration) {
        if (configuration.getValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE) == null) {
            final RasterDataNode[] rasters = (RasterDataNode[]) configuration.getValue(PROPERTY_NAME_RASTERS);
            MultiLevelSource levelSource = BandImageMultiLevelSource.create(rasters, ProgressMonitor.NULL);
            try {
                configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, levelSource);
            } catch (ValidationException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return new ImageLayer(this, configuration);
    }

    @Override
    public ValueContainer getConfigurationTemplate() {
        final ValueContainer template = super.getConfigurationTemplate();

        template.addModel(createDefaultValueModel(PROPERTY_NAME_RASTERS, RasterDataNode[].class));
        template.getDescriptor(PROPERTY_NAME_RASTERS).setNotNull(true);

        return template;
    }

    public Layer createLayer(RasterDataNode[] rasters, MultiLevelSource levelSource) {
        final ValueContainer configuration = getConfigurationTemplate();

        try {
            configuration.setValue(PROPERTY_NAME_RASTERS, rasters);
            configuration.setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, levelSource);
        } catch (ValidationException e) {
            throw new IllegalArgumentException(e);
        }

        return createLayer(null, configuration);
    }

    public static class Configuration {

        private RasterDataNode[] rasterDataNodes;
        private MultiLevelSource multiLevelSource;
        private boolean borderShown;
        private double borderWidth;
        private Color borderColor;

        private double transparency;
        private boolean visible;

        public Configuration(RasterDataNode[] rasterDataNodes) {
            this.rasterDataNodes = rasterDataNodes;
            borderShown = false;
            borderWidth = 1.0;
            borderColor = Color.ORANGE;
            transparency = 0.0;
            visible = true;
        }

        public RasterDataNode[] getRasterDataNodes() {
            return rasterDataNodes;
        }

        public MultiLevelSource getMultiLevelSource() {
            return multiLevelSource;
        }

        public boolean isBorderShown() {
            return borderShown;
        }

        public double getBorderWidth() {
            return borderWidth;
        }

        public Color getBorderColor() {
            return borderColor;
        }

        public double getTransparency() {
            return transparency;
        }

        public boolean isVisible() {
            return visible;
        }

        public void setRasterDataNodes(RasterDataNode[] rasterDataNodes) {
            this.rasterDataNodes = rasterDataNodes.clone();
        }

        public Configuration setMultiLevelSource(MultiLevelSource multiLevelSource) {
            this.multiLevelSource =  multiLevelSource;
            return this;
        }

        public Configuration setBorderShown(boolean borderShown) {
            this.borderShown = borderShown;
            return this;
        }

        public Configuration setBorderWidth(double borderWidth) {
            this.borderWidth = borderWidth;
            return this;
        }

        public Configuration setBorderColor(Color borderColor) {
            this.borderColor = borderColor;
            return this;
        }

        public void setTransparency(double transparency) {
            this.transparency = transparency;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }
    }
}
