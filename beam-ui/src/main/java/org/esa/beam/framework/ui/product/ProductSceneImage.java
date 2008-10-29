package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.Style;
import com.bc.ceres.glayer.support.DefaultStyle;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.support.LayerStyleListener;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.glayer.BitmaskCollectionLayer;
import org.esa.beam.glayer.FigureLayer;
import org.esa.beam.glayer.GraticuleLayer;
import org.esa.beam.glayer.PlacemarkLayer;
import org.esa.beam.glevel.BandImageMultiLevelSource;
import org.esa.beam.glevel.MaskImageMultiLevelSource;
import org.esa.beam.glevel.RoiImageMultiLevelSource;
import org.esa.beam.glevel.TiledFileMultiLevelSource;
import org.esa.beam.util.PropertyMap;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO - Apidoc
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class ProductSceneImage {

    public static final int IMAGE_LAYER_ID = 0;
    public static final int NO_DATA_LAYER_ID = 1;
    public static final int BITMASK_LAYER_ID = 2;
    public static final int ROI_LAYER_ID = 3;
    public static final int GRATICULE_LAYER_ID = 4;
    public static final int GCP_LAYER_ID = 5;
    public static final int PIN_LAYER_ID = 6;
    public static final int FIGURE_LAYER_ID = 7;

    private final String name;
    private final PropertyMap configuration;
    private RasterDataNode[] rasters;
    private Layer rootLayer;
    private BandImageMultiLevelSource bandImageMultiLevelSource;
    private Map<Integer, Layer> layerMap;

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

    Layer getRootLayer() {
        return rootLayer;
    }

    ImageLayer getBaseImageLayer() {
        return (ImageLayer) layerMap.get(IMAGE_LAYER_ID);
    }

    ImageLayer getNoDataLayer() {
        return (ImageLayer) layerMap.get(NO_DATA_LAYER_ID);
    }

    Layer getBitmaskLayer() {
        return layerMap.get(BITMASK_LAYER_ID);
    }

    ImageLayer getRoiLayer() {
        return (ImageLayer) layerMap.get(ROI_LAYER_ID);
    }

    GraticuleLayer getGraticuleLayer() {
        return (GraticuleLayer) layerMap.get(GRATICULE_LAYER_ID);
    }

    Layer getGcpLayer() {
        return layerMap.get(GCP_LAYER_ID);
    }

    Layer getPinLayer() {
        return layerMap.get(PIN_LAYER_ID);
    }

    FigureLayer getFigureLayer() {
        return (FigureLayer) layerMap.get(FIGURE_LAYER_ID);
    }

    private RasterDataNode getRaster() {
        return rasters[0];
    }

    private Product getProduct() {
        return getRaster().getProduct();
    }

    private void initRootLayer() {
        final AffineTransform i2mTransform = bandImageMultiLevelSource.getModel().getImageToModelTransform(0);
        final ImageLayer baseImageLayer = createBaseImageLayer();
        final ImageLayer noDataLayer = createNoDataLayer(i2mTransform);
        final FigureLayer figureLayer = createFigureLayer(i2mTransform);
        final ImageLayer roiLayer = createRoiLayer(i2mTransform);
        final GraticuleLayer graticuleLayer = createGraticuleLayer(i2mTransform);
        final PlacemarkLayer pinLayer = createPinLayer(i2mTransform);
        final PlacemarkLayer gcpLayer = createGcpLayer(i2mTransform);
        final Layer bitmaskLayer = createBitmaskCollectionLayer(i2mTransform);

        rootLayer = new Layer();
        rootLayer.getChildren().add(figureLayer);
        rootLayer.getChildren().add(pinLayer);
        rootLayer.getChildren().add(gcpLayer);
        rootLayer.getChildren().add(graticuleLayer);
        rootLayer.getChildren().add(roiLayer);
        rootLayer.getChildren().add(bitmaskLayer);
        rootLayer.getChildren().add(noDataLayer);
        rootLayer.getChildren().add(baseImageLayer);

        // TODO: remove this hack!!!
        if (getRaster().getProduct().getProductType().startsWith("MIR_")) {
            // SMOS
            Layer createWorldLayer = createWorldLayer();
            if (createWorldLayer != null) {
                rootLayer.getChildren().add(createWorldLayer);
            }
        }

        layerMap = new HashMap<Integer, Layer>(12);

        layerMap.put(IMAGE_LAYER_ID, baseImageLayer);
        layerMap.put(NO_DATA_LAYER_ID, noDataLayer);
        layerMap.put(BITMASK_LAYER_ID, bitmaskLayer);
        layerMap.put(ROI_LAYER_ID, roiLayer);
        layerMap.put(GRATICULE_LAYER_ID, graticuleLayer);
        layerMap.put(GCP_LAYER_ID, gcpLayer);
        layerMap.put(PIN_LAYER_ID, pinLayer);
        layerMap.put(FIGURE_LAYER_ID, figureLayer);
    }

    private ImageLayer createBaseImageLayer() {
        final ImageLayer imageLayer = new ImageLayer(bandImageMultiLevelSource);

        imageLayer.setName(getName());
        imageLayer.setVisible(true);

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

    private Layer createWorldLayer() {
        final String WORLD_IMAGE_DIR_PROPERTY_NAME = "org.esa.beam.pview.worldImageDir";
        String dirPath = System.getProperty(WORLD_IMAGE_DIR_PROPERTY_NAME);
        if (dirPath == null || dirPath.isEmpty()) {
            return null;
        }
        MultiLevelSource multiLevelSource;
        try {
            multiLevelSource = TiledFileMultiLevelSource.create(new File(dirPath), false);
        } catch (IOException e) {
            return null;
        }

        final ImageLayer blueMarbleLayer = new ImageLayer(multiLevelSource);
        blueMarbleLayer.setName("Bluemarble " + getRaster().getName());
        blueMarbleLayer.setVisible(true);
        blueMarbleLayer.getStyle().setOpacity(1.0);
        return blueMarbleLayer;
    }

    private ImageLayer createNoDataLayer(AffineTransform imageToModelTransform) {
        final MultiLevelSource multiLevelSource;

        if (getRaster().getValidMaskExpression() != null) {
            final Color color = configuration.getPropertyColor("noDataOverlay.color", Color.ORANGE);
            multiLevelSource = MaskImageMultiLevelSource.create(getRaster().getProduct(), color,
                    getRaster().getValidMaskExpression(), true, imageToModelTransform);
        } else {
            multiLevelSource = MultiLevelSource.NULL;
        }

        final ImageLayer noDataLayer = new ImageLayer(multiLevelSource);
        noDataLayer.setName("No-data mask of " + getRaster().getName());
        noDataLayer.setVisible(false);
        setNoDataLayerStyle(configuration, noDataLayer);
        noDataLayer.addListener(new ColorStyleListener());

        return noDataLayer;
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
        roiLayer.setName("ROI of " + getRaster().getName());
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
        final GraticuleLayer graticuleLayer = new GraticuleLayer(getProduct(), getRaster(), i2mTransform);
        graticuleLayer.setName("Graticule of " + getRaster().getName());
        graticuleLayer.setVisible(false);
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
        gcpLayer.setName("GCPs");
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

    private Layer createBitmaskCollectionLayer(AffineTransform i2mTransform) {
        return new BitmaskCollectionLayer(getRaster(), i2mTransform);
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
                                getRaster().getValidMaskExpression(), true, imageLayer.getImageToModelTransform()));
            }
        }
    }
}
