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
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.PropertyMap;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;

/**
 * TODO - Apidoc
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class ProductSceneImage {

    private final String name;
    private final PropertyMap configuration;
    private ImageInfo imageInfo;
    private RasterDataNode[] rasters;
    private Layer rootLayer;
    private MultiLevelSource imageMultiLevelSource;


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
                raster.getImageInfo(),
                configuration);
        imageMultiLevelSource = BandImageMultiLevelSource.create(raster, pm);
        setImageInfo(raster.getImageInfo());
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
                view.getImageInfo(),
                view.getSceneImage().getConfiguration());
        imageMultiLevelSource = view.getSceneImage().getImageMultiLevelSource();
        rootLayer = view.getRootLayer();
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
        this(name,
                new RasterDataNode[]{redRaster, greenRaster, blueRaster},
                null,
                configuration);
        imageMultiLevelSource = BandImageMultiLevelSource.create(rasters, pm);
        setImageInfo(ImageManager.getInstance().getImageInfo(rasters));
        initRootLayer();
    }

    private ProductSceneImage(String name, RasterDataNode[] rasters, ImageInfo imageInfo, PropertyMap configuration) {
        this.name = name;
        this.rasters = rasters;
        this.imageInfo = imageInfo;
        this.configuration = configuration;
    }

    public PropertyMap getConfiguration() {
        return configuration;
    }

    public String getName() {
        return name;
    }

    public ImageInfo getImageInfo() {
        return imageInfo;
    }

    public void setImageInfo(ImageInfo imageInfo) {
        this.imageInfo = imageInfo;
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

    private RasterDataNode getRaster() {
        return rasters[0];
    }

    private Product getProduct() {
        return getRaster().getProduct();
    }

    private void initRootLayer() {
        rootLayer = new Layer();
        final ImageLayer imageLayer = new ImageLayer(imageMultiLevelSource);
        imageLayer.setName(getName());
        imageLayer.setVisible(true);

        final ImageLayer noDataLayer = createNoDataLayer();
        final FigureLayer figureLayer = createFigureLayer();
        final ImageLayer roiLayer = createRoiLayer();
        final GraticuleLayer graticuleLayer = createGraticuleLayer();
        final PlacemarkLayer pinLayer = createPinLayer();
        final PlacemarkLayer gcpLayer = createGcpLayer();
        final Layer bitmaskLayer = createBitmaskCollectionLayer();

        rootLayer.getChildLayerList().add(figureLayer);
        rootLayer.getChildLayerList().add(pinLayer);
        rootLayer.getChildLayerList().add(gcpLayer);
        rootLayer.getChildLayerList().add(graticuleLayer);
        rootLayer.getChildLayerList().add(roiLayer);
        rootLayer.getChildLayerList().add(bitmaskLayer);
        rootLayer.getChildLayerList().add(noDataLayer);
        rootLayer.getChildLayerList().add(imageLayer);

        // TODO: remove this hack!!!
        if (getRaster().getProduct().getProductType().startsWith("MIR_")) {
            // SMOS
            Layer createWorldLayer = createWorldLayer();
            if (createWorldLayer != null) {
                rootLayer.getChildLayerList().add(createWorldLayer);
            }
        }
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

    private ImageLayer createNoDataLayer() {
        final MultiLevelSource multiLevelSource;

        if (getRaster().getValidMaskExpression() != null) {
            final Color color = configuration.getPropertyColor("noDataOverlay.color", Color.ORANGE);
            multiLevelSource = MaskImageMultiLevelSource.create(getRaster().getProduct(), color,
                    getRaster().getValidMaskExpression(), true, new AffineTransform());
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

        final Style style = layer.getStyle();
        style.setOpacity(1.0 - transparency);
        style.setProperty("color", color);
    }

    private FigureLayer createFigureLayer() {
        final FigureLayer figureLayer = new FigureLayer(new Figure[0]);
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

    private ImageLayer createRoiLayer() {
        final MultiLevelSource multiLevelSource;

        if (getRaster().getROIDefinition() != null && getRaster().getROIDefinition().isUsable()) {
            final Color color = configuration.getPropertyColor("roi.color", Color.RED);
            multiLevelSource = RoiImageMultiLevelSource.create(getRaster(), color, new AffineTransform());
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

        final Style style = layer.getStyle();
        style.setOpacity(1.0 - transparency);
        style.setProperty("color", color);
    }

    private GraticuleLayer createGraticuleLayer() {
        final GraticuleLayer graticuleLayer = new GraticuleLayer(getProduct(), getRaster());
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

    private PlacemarkLayer createPinLayer() {
        final PlacemarkLayer pinLayer = new PlacemarkLayer(getRaster().getProduct(), PinDescriptor.INSTANCE,
                new AffineTransform());
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

    private PlacemarkLayer createGcpLayer() {
        final PlacemarkLayer gcpLayer = new PlacemarkLayer(getRaster().getProduct(), GcpDescriptor.INSTANCE,
                new AffineTransform());
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

    private Layer createBitmaskCollectionLayer() {
        return new BitmaskCollectionLayer(getRaster());
    }

    private MultiLevelSource getImageMultiLevelSource() {
        return imageMultiLevelSource;
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
