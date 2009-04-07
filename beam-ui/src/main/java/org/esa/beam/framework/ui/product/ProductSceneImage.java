package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.Style;
import com.bc.ceres.glayer.support.DefaultStyle;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.support.LayerStyleListener;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.BitmaskOverlayInfo;
import org.esa.beam.framework.datamodel.GcpDescriptor;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.glayer.BitmaskCollectionLayer;
import org.esa.beam.glayer.FigureLayer;
import org.esa.beam.glayer.GraticuleLayer;
import org.esa.beam.glayer.PlacemarkLayer;
import org.esa.beam.glevel.BandImageMultiLevelSource;
import org.esa.beam.glevel.MaskImageMultiLevelSource;
import org.esa.beam.glevel.RoiImageMultiLevelSource;
import org.esa.beam.util.PropertyMap;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;


public class ProductSceneImage implements LayerContext {

    private static final ImageLayerFilter IMAGE_LAYER_FILTER = new ImageLayerFilter();
    private final String name;
    private final PropertyMap configuration;
    private RasterDataNode[] rasters;
    private Layer rootLayer;
    private BandImageMultiLevelSource bandImageMultiLevelSource;

    /**
     * Creates a color indexed product scene for the given product raster.
     *
     * @param raster        the product raster, must not be null
     * @param configuration a configuration
     * @param pm            a monitor to inform the user about progress @return a color indexed product scene image
     */
    public ProductSceneImage(RasterDataNode raster, PropertyMap configuration, ProgressMonitor pm) {
        this("Image of " + raster.getName(),
             new RasterDataNode[]{raster},
             configuration);
        bandImageMultiLevelSource = BandImageMultiLevelSource.create(raster, pm);
        initRootLayer();
        initBitmaskLayer();
    }

    /**
     * Creates a new scene image for an existing view.
     *
     * @param raster The product raster.
     * @param view   An existing view.
     */
    public ProductSceneImage(RasterDataNode raster, ProductSceneView view) {
        this("Image of " + raster.getName(),
             new RasterDataNode[]{raster},
             view.getSceneImage().getConfiguration());
        bandImageMultiLevelSource = view.getSceneImage().getBandImageMultiLevelSource();
        initRootLayer();
    }

    /**
     * Creates an RGB product scene for the given raster datasets.
     *
     * @param name          the name of the scene view
     * @param redRaster     the product raster used for the red color component, must not be null
     * @param greenRaster   the product raster used for the green color component, must not be null
     * @param blueRaster    the product raster used for the blue color component, must not be null
     * @param configuration a configuration
     * @param pm            a monitor to inform the user about progress @return an RGB product scene image @throws java.io.IOException if the image creation failed due to an I/O problem
     */
    public ProductSceneImage(String name, RasterDataNode redRaster,
                             RasterDataNode greenRaster,
                             RasterDataNode blueRaster,
                             PropertyMap configuration,
                             ProgressMonitor pm) {
        this(name, new RasterDataNode[]{redRaster, greenRaster, blueRaster}, configuration);
        bandImageMultiLevelSource = BandImageMultiLevelSource.create(rasters, pm);
        initRootLayer();
    }

    private ProductSceneImage(String name, RasterDataNode[] rasters, PropertyMap configuration) {
        this.name = name;
        this.rasters = rasters;
        this.configuration = configuration;
    }

    public PropertyMap getConfiguration() {
        return configuration;
    }

    public String getName() {
        return name;
    }

    public ImageInfo getImageInfo() {
        return bandImageMultiLevelSource.getImageInfo();
    }

    public void setImageInfo(ImageInfo imageInfo) {
        bandImageMultiLevelSource.setImageInfo(imageInfo);
    }

    public RasterDataNode[] getRasters() {
        return rasters;
    }

    public void setRasters(RasterDataNode[] rasters) {
        this.rasters = rasters;
    }

    @Override
    public Object getCoordinateReferenceSystem() {
        return rasters[0].getGeoCoding().getModelCRS();
    }

    @Override
    public Layer getRootLayer() {
        return rootLayer;
    }

    Layer getLayer(String id) {
        return LayerUtils.getChildLayerById(getRootLayer(), id);
    }

    void addLayer(int index, Layer layer) {
        rootLayer.getChildren().add(index, layer);
    }

    int getFirstImageLayerIndex() {
        return LayerUtils.getChildLayerIndex(getRootLayer(), IMAGE_LAYER_FILTER, LayerUtils.SearchMode.DEEP, 0);
    }

    ImageLayer getBaseImageLayer() {
        return (ImageLayer) getLayer(ProductSceneView.BASE_IMAGE_LAYER_ID);
    }

    ImageLayer getNoDataLayer(boolean create) {
        ImageLayer layer = (ImageLayer) getLayer(ProductSceneView.NO_DATA_LAYER_ID);
        if (layer == null && create) {
            layer = createNoDataLayer(getImageToModelTransform());
            addLayer(getFirstImageLayerIndex(), layer);
        }
        return layer;
    }

    Layer getBitmaskLayer(boolean create) {
        Layer layer = getLayer(ProductSceneView.BITMASK_LAYER_ID);
        if (layer == null && create) {
            layer = createBitmaskCollectionLayer(getImageToModelTransform());
            addLayer(getFirstImageLayerIndex(), layer);
        }
        return layer;
    }

    ImageLayer getRoiLayer(boolean create) {
        ImageLayer layer = (ImageLayer) getLayer(ProductSceneView.ROI_LAYER_ID);
        if (layer == null && create) {
            layer = createRoiLayer(getImageToModelTransform());
            addLayer(getFirstImageLayerIndex(), layer);
        }
        return layer;
    }

    GraticuleLayer getGraticuleLayer(boolean create) {
        GraticuleLayer layer = (GraticuleLayer) getLayer(ProductSceneView.GRATICULE_LAYER_ID);
        if (layer == null && create) {
            layer = createGraticuleLayer(getImageToModelTransform());
            addLayer(getFirstImageLayerIndex(), layer);
        }
        return layer;
    }

    Layer getGcpLayer(boolean create) {
        Layer layer = getLayer(ProductSceneView.GCP_LAYER_ID);
        if (layer == null && create) {
            layer = createGcpLayer(getImageToModelTransform());
            addLayer(0, layer);
        }
        return layer;
    }

    Layer getPinLayer(boolean create) {
        Layer layer = getLayer(ProductSceneView.PIN_LAYER_ID);
        if (layer == null && create) {
            layer = createPinLayer(getImageToModelTransform());
            addLayer(0, layer);
        }
        return layer;
    }

    FigureLayer getFigureLayer(boolean create) {
        FigureLayer layer = (FigureLayer) getLayer(ProductSceneView.FIGURE_LAYER_ID);
        if (layer == null && create) {
            layer = createFigureLayer(getImageToModelTransform());
            addLayer(getFirstImageLayerIndex(), layer);
        }
        return layer;
    }

    private RasterDataNode getRaster() {
        return rasters[0];
    }

    private Product getProduct() {
        return getRaster().getProduct();
    }

    private void initRootLayer() {
        rootLayer = new CollectionLayer();
        addLayer(0, createBaseImageLayer());
    }

    private void initBitmaskLayer() {
        if (mustEnableBitmaskLayer()) {
            getBitmaskLayer(true);
        }
    }

    private boolean mustEnableBitmaskLayer() {
        final BitmaskOverlayInfo overlayInfo = getRaster().getBitmaskOverlayInfo();
        if (overlayInfo != null) {
            BitmaskDef[] defs = getRaster().getBitmaskDefs();
            for (BitmaskDef def : defs) {
                if (overlayInfo.containsBitmaskDef(def)) {
                    return true;
                }
            }
        }
        return false;
    }

    private AffineTransform getImageToModelTransform() {
        return bandImageMultiLevelSource.getModel().getImageToModelTransform(0);
    }

    private ImageLayer createBaseImageLayer() {
        final ImageLayer imageLayer = new ImageLayer(bandImageMultiLevelSource);

        imageLayer.setName(getRaster().getDisplayName());
        imageLayer.setVisible(true);
        imageLayer.setId(ProductSceneView.BASE_IMAGE_LAYER_ID);

        setBaseImageLayerStyle(configuration, imageLayer);
        return imageLayer;
    }

    static void setBaseImageLayerStyle(PropertyMap configuration, Layer layer) {
        final boolean borderShown = configuration.getPropertyBool("image.border.shown",
                                                                  ImageLayer.DEFAULT_BORDER_SHOWN);
        final double borderWidth = configuration.getPropertyDouble("image.border.size",
                                                                   ImageLayer.DEFAULT_BORDER_WIDTH);
        final Color borderColor = configuration.getPropertyColor("image.border.color",
                                                                 ImageLayer.DEFAULT_BORDER_COLOR);

        final Style style = new DefaultStyle();
        style.setProperty(ImageLayer.PROPERTY_NAME_BORDER_SHOWN, borderShown);
        style.setProperty(ImageLayer.PROPERTY_NAME_BORDER_WIDTH, borderWidth);
        style.setProperty(ImageLayer.PROPERTY_NAME_BORDER_COLOR, borderColor);

        style.setComposite(layer.getStyle().getComposite());
        style.setDefaultStyle(layer.getStyle().getDefaultStyle());
        style.setOpacity(layer.getStyle().getOpacity());

        layer.setStyle(style);
    }

    private ImageLayer createNoDataLayer(AffineTransform imageToModelTransform) {
        final MultiLevelSource multiLevelSource;

        if (getRaster().getValidMaskExpression() != null) {
            final Color color = configuration.getPropertyColor("noDataOverlay.color", Color.ORANGE);
            multiLevelSource = MaskImageMultiLevelSource.create(getRaster().getProduct(), color,
                                                                getRaster().getValidMaskExpression(), true,
                                                                imageToModelTransform);
        } else {
            multiLevelSource = MultiLevelSource.NULL;
        }

        final ImageLayer noDataLayer = new ImageLayer(multiLevelSource);
        noDataLayer.setName("No-data mask");
        noDataLayer.setId(ProductSceneView.NO_DATA_LAYER_ID);
        noDataLayer.setVisible(false);
        setNoDataLayerStyle(configuration, noDataLayer);
        noDataLayer.addListener(new ColorStyleListener());

        return noDataLayer;
    }

    private Layer createBitmaskCollectionLayer(AffineTransform i2mTransform) {
        BitmaskCollectionLayer layer = new BitmaskCollectionLayer(getRaster(), i2mTransform);
        layer.setId(ProductSceneView.BITMASK_LAYER_ID);
        return layer;
    }

    static void setNoDataLayerStyle(PropertyMap configuration, Layer layer) {
        final Color color = configuration.getPropertyColor("noDataOverlay.color", Color.ORANGE);
        final double transparency = configuration.getPropertyDouble("noDataOverlay.transparency", 0.3);

        final Style style = new DefaultStyle();
        style.setProperty("color", color);
        style.setOpacity(1.0 - transparency);
        style.setProperty(ImageLayer.PROPERTY_NAME_BORDER_SHOWN, false);

        style.setComposite(layer.getStyle().getComposite());
        style.setDefaultStyle(layer.getStyle().getDefaultStyle());

        layer.setStyle(style);
    }

    private FigureLayer createFigureLayer(AffineTransform i2mTransform) {
        final FigureLayer figureLayer = new FigureLayer(i2mTransform, new Figure[0]);
        figureLayer.setName("Figures");
        figureLayer.setId(ProductSceneView.FIGURE_LAYER_ID);
        figureLayer.setVisible(true);
        setFigureLayerStyle(configuration, figureLayer);

        return figureLayer;
    }

    public static void setFigureLayerStyle(PropertyMap configuration, Layer layer) {
        final Style style = new DefaultStyle();
        style.setProperty(FigureLayer.PROPERTY_NAME_SHAPE_OUTLINED,
                          configuration.getPropertyBool(FigureLayer.PROPERTY_NAME_SHAPE_OUTLINED,
                                                        FigureLayer.DEFAULT_SHAPE_OUTLINED));
        style.setProperty(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_COLOR,
                          configuration.getPropertyColor(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_COLOR,
                                                         FigureLayer.DEFAULT_SHAPE_OUTL_COLOR));
        style.setProperty(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_TRANSPARENCY,
                          configuration.getPropertyDouble(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_TRANSPARENCY,
                                                          FigureLayer.DEFAULT_SHAPE_OUTL_TRANSPARENCY));
        style.setProperty(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_WIDTH,
                          configuration.getPropertyDouble(FigureLayer.PROPERTY_NAME_SHAPE_OUTL_WIDTH,
                                                          FigureLayer.DEFAULT_SHAPE_OUTL_WIDTH));
        style.setProperty(FigureLayer.PROPERTY_NAME_SHAPE_FILLED,
                          configuration.getPropertyBool(FigureLayer.PROPERTY_NAME_SHAPE_FILLED,
                                                        FigureLayer.DEFAULT_SHAPE_FILLED));
        style.setProperty(FigureLayer.PROPERTY_NAME_SHAPE_FILL_COLOR,
                          configuration.getPropertyColor(FigureLayer.PROPERTY_NAME_SHAPE_FILL_COLOR,
                                                         FigureLayer.DEFAULT_SHAPE_FILL_COLOR));
        style.setProperty(FigureLayer.PROPERTY_NAME_SHAPE_FILL_TRANSPARENCY,
                          configuration.getPropertyDouble(FigureLayer.PROPERTY_NAME_SHAPE_FILL_TRANSPARENCY,
                                                          FigureLayer.DEFAULT_SHAPE_FILL_TRANSPARENCY));

        style.setComposite(layer.getStyle().getComposite());
        style.setDefaultStyle(layer.getStyle().getDefaultStyle());
        style.setOpacity(layer.getStyle().getOpacity());

        layer.setStyle(style);
    }

    private ImageLayer createRoiLayer(AffineTransform imageToModelTransform) {
        final MultiLevelSource multiLevelSource;

        if (getRaster().getROIDefinition() != null && getRaster().getROIDefinition().isUsable()) {
            final Color color = configuration.getPropertyColor("roi.color", Color.RED);
            multiLevelSource = RoiImageMultiLevelSource.create(getRaster(), color, imageToModelTransform);
        } else {
            multiLevelSource = MultiLevelSource.NULL;
        }

        final ImageLayer roiLayer = new ImageLayer(multiLevelSource);
        roiLayer.setName("ROI");
        roiLayer.setId(ProductSceneView.ROI_LAYER_ID);
        roiLayer.setVisible(false);
        setRoiLayerStyle(configuration, roiLayer);
        roiLayer.addListener(new ColorStyleListener());

        return roiLayer;
    }

    public static void setRoiLayerStyle(PropertyMap configuration, Layer layer) {
        final Color color = configuration.getPropertyColor("roi.color", Color.RED);
        final double transparency = configuration.getPropertyDouble("roi.transparency", 0.5);

        final Style style = new DefaultStyle();
        style.setProperty("color", color);
        style.setOpacity(1.0 - transparency);
        style.setProperty(ImageLayer.PROPERTY_NAME_BORDER_SHOWN, false);

        style.setComposite(layer.getStyle().getComposite());
        style.setDefaultStyle(layer.getStyle().getDefaultStyle());

        layer.setStyle(style);
    }

    private GraticuleLayer createGraticuleLayer(AffineTransform i2mTransform) {
        final GraticuleLayer graticuleLayer = new GraticuleLayer(getRaster(), i2mTransform);
        graticuleLayer.setId(ProductSceneView.GRATICULE_LAYER_ID);
        graticuleLayer.setVisible(false);
        graticuleLayer.setName("Graticule");
        setGraticuleLayerStyle(configuration, graticuleLayer);
        return graticuleLayer;
    }

    public static void setGraticuleLayerStyle(PropertyMap configuration, Layer layer) {
        final Style style = new DefaultStyle();

        style.setProperty(GraticuleLayer.PROPERTY_NAME_RES_AUTO,
                          configuration.getPropertyBool(GraticuleLayer.PROPERTY_NAME_RES_AUTO,
                                                        GraticuleLayer.DEFAULT_RES_AUTO));
        style.setProperty(GraticuleLayer.PROPERTY_NAME_RES_PIXELS,
                          configuration.getPropertyInt(GraticuleLayer.PROPERTY_NAME_RES_PIXELS,
                                                       GraticuleLayer.DEFAULT_RES_PIXELS));
        style.setProperty(GraticuleLayer.PROPERTY_NAME_RES_LAT,
                          configuration.getPropertyDouble(GraticuleLayer.PROPERTY_NAME_RES_LAT,
                                                          GraticuleLayer.DEFAULT_RES_LAT));
        style.setProperty(GraticuleLayer.PROPERTY_NAME_RES_LON,
                          configuration.getPropertyDouble(GraticuleLayer.PROPERTY_NAME_RES_LON,
                                                          GraticuleLayer.DEFAULT_RES_LON));

        style.setProperty(GraticuleLayer.PROPERTY_NAME_LINE_COLOR,
                          configuration.getPropertyColor(GraticuleLayer.PROPERTY_NAME_LINE_COLOR,
                                                         GraticuleLayer.DEFAULT_LINE_COLOR));
        style.setProperty(GraticuleLayer.PROPERTY_NAME_LINE_WIDTH,
                          configuration.getPropertyDouble(GraticuleLayer.PROPERTY_NAME_LINE_WIDTH,
                                                          GraticuleLayer.DEFAULT_LINE_WIDTH));
        style.setProperty(GraticuleLayer.PROPERTY_NAME_LINE_TRANSPARENCY,
                          configuration.getPropertyDouble(GraticuleLayer.PROPERTY_NAME_LINE_TRANSPARENCY,
                                                          GraticuleLayer.DEFAULT_LINE_TRANSPARENCY));
        style.setProperty(GraticuleLayer.PROPERTY_NAME_TEXT_ENABLED,
                          configuration.getPropertyBool(GraticuleLayer.PROPERTY_NAME_TEXT_ENABLED,
                                                        GraticuleLayer.DEFAULT_TEXT_ENABLED));
        style.setProperty(GraticuleLayer.PROPERTY_NAME_TEXT_FG_COLOR,
                          configuration.getPropertyColor(GraticuleLayer.PROPERTY_NAME_TEXT_FG_COLOR,
                                                         GraticuleLayer.DEFAULT_TEXT_FG_COLOR));
        style.setProperty(GraticuleLayer.PROPERTY_NAME_TEXT_BG_COLOR,
                          configuration.getPropertyColor(GraticuleLayer.PROPERTY_NAME_TEXT_BG_COLOR,
                                                         GraticuleLayer.DEFAULT_TEXT_BG_COLOR));
        style.setProperty(GraticuleLayer.PROPERTY_NAME_TEXT_BG_TRANSPARENCY,
                          configuration.getPropertyDouble(GraticuleLayer.PROPERTY_NAME_TEXT_BG_TRANSPARENCY,
                                                          GraticuleLayer.DEFAULT_TEXT_BG_TRANSPARENCY));

        style.setComposite(layer.getStyle().getComposite());
        style.setDefaultStyle(layer.getStyle().getDefaultStyle());
        style.setOpacity(layer.getStyle().getOpacity());

        layer.setStyle(style);
    }

    private PlacemarkLayer createPinLayer(AffineTransform i2mTransform) {
        final PlacemarkLayer pinLayer = new PlacemarkLayer(getRaster().getProduct(), PinDescriptor.INSTANCE,
                                                           i2mTransform);
        pinLayer.setName("Pins");
        pinLayer.setId(ProductSceneView.PIN_LAYER_ID);
        pinLayer.setVisible(false);
        setPinLayerStyle(configuration, pinLayer);

        return pinLayer;
    }

    public static void setPinLayerStyle(PropertyMap configuration, Layer layer) {
        final DefaultStyle style = new DefaultStyle();

        style.setProperty(PlacemarkLayer.PROPERTY_NAME_TEXT_ENABLED,
                          configuration.getPropertyBool("pin.text.enabled", Boolean.TRUE));
        style.setProperty(PlacemarkLayer.PROPERTY_NAME_TEXT_FG_COLOR,
                          configuration.getPropertyColor("pin.text.fg.color", Color.WHITE));
        style.setProperty(PlacemarkLayer.PROPERTY_NAME_TEXT_BG_COLOR,
                          configuration.getPropertyColor("pin.text.bg.color", Color.BLACK));

        style.setComposite(layer.getStyle().getComposite());
        style.setDefaultStyle(layer.getStyle().getDefaultStyle());
        style.setOpacity(layer.getStyle().getOpacity());

        layer.setStyle(style);
    }

    private PlacemarkLayer createGcpLayer(AffineTransform i2mTransform) {
        final PlacemarkLayer gcpLayer = new PlacemarkLayer(getRaster().getProduct(), GcpDescriptor.INSTANCE,
                                                           i2mTransform);
        gcpLayer.setName("Ground Control Points");
        gcpLayer.setId(ProductSceneView.GCP_LAYER_ID);
        gcpLayer.setVisible(false);
        setGcpLayerStyle(configuration, gcpLayer);

        return gcpLayer;
    }

    public static void setGcpLayerStyle(PropertyMap configuration, Layer layer) {
        final DefaultStyle style = new DefaultStyle();

        style.setProperty(PlacemarkLayer.PROPERTY_NAME_TEXT_ENABLED,
                          configuration.getPropertyBool("gcp.text.enabled", Boolean.TRUE));
        style.setProperty(PlacemarkLayer.PROPERTY_NAME_TEXT_FG_COLOR,
                          configuration.getPropertyColor("gcp.text.fg.color", Color.WHITE));
        style.setProperty(PlacemarkLayer.PROPERTY_NAME_TEXT_BG_COLOR,
                          configuration.getPropertyColor("gcp.text.bg.color", Color.BLACK));

        style.setComposite(style.getComposite());
        style.setDefaultStyle(layer.getStyle().getDefaultStyle());
        style.setOpacity(layer.getStyle().getOpacity());

        layer.setStyle(style);
    }

    private BandImageMultiLevelSource getBandImageMultiLevelSource() {
        return bandImageMultiLevelSource;
    }

    private class ColorStyleListener extends LayerStyleListener {

        @Override
        public void handleLayerStylePropertyChanged(Layer layer, PropertyChangeEvent event) {
            if ("color".equals(event.getPropertyName())) {
                final Color color = (Color) layer.getStyle().getProperty("color");
                final ImageLayer imageLayer = (ImageLayer) layer;
                imageLayer.setMultiLevelSource(
                        MaskImageMultiLevelSource.create(getRaster().getProduct(), color,
                                                         getRaster().getValidMaskExpression(), true,
                                                         imageLayer.getImageToModelTransform()));
            }
        }
    }

    private static class ImageLayerFilter implements LayerFilter {

        @Override
        public boolean accept(Layer layer) {
            return layer instanceof ImageLayer;
        }
    }
}
