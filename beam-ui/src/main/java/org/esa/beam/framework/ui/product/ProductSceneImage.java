package org.esa.beam.framework.ui.product;

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
import org.esa.beam.glevel.TiledFileMultiLevelSource;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.PropertyMap;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;

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
        initRootLayer(configuration);
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
        ProductSceneView45 view45 = (ProductSceneView45) view;
        imageMultiLevelSource = view45.getSceneImage45().getImageMultiLevelSource();
        rootLayer = view45.getRootLayer();
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
        initRootLayer(configuration);
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

    private void initRootLayer(PropertyMap configuration) {
        rootLayer = new Layer();
        final ImageLayer imageLayer = new ImageLayer(imageMultiLevelSource);
        imageLayer.setName(getName());
        imageLayer.setVisible(true);

        final ImageLayer noDataLayer = createNoDataLayer(configuration);
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

    private ImageLayer createNoDataLayer(PropertyMap configuration) {
        final MultiLevelSource multiLevelSource;

        final Color color = configuration.getPropertyColor("noDataOverlay.color", Color.ORANGE);
        final double transparency = configuration.getPropertyDouble("noDataOverlay.transparency", 0.3);

        if (getRaster().getValidMaskExpression() != null) {
            // todo - get color from style, set color
            multiLevelSource = MaskImageMultiLevelSource.create(getRaster().getProduct(), color,
                    getRaster().getValidMaskExpression(), true, new AffineTransform());
        } else {
            multiLevelSource = MultiLevelSource.NULL;
        }

        final ImageLayer noDataLayer = new ImageLayer(multiLevelSource);
        noDataLayer.setName("No-data mask of " + getRaster().getName());
        noDataLayer.setVisible(false);
        noDataLayer.getStyle().setOpacity(1.0 - transparency);

        return noDataLayer;
    }

    private FigureLayer createFigureLayer() {
        final FigureLayer figureLayer = new FigureLayer(new Figure[0]);
        figureLayer.setName("Figures");
        figureLayer.setVisible(true);
        return figureLayer;
    }

    private ImageLayer createRoiLayer() {
        final MultiLevelSource multiLevelSource;

        if (getRaster().getROIDefinition() != null && getRaster().getROIDefinition().isUsable()) {
            // todo - get color from style, set color
            multiLevelSource = RoiImageMultiLevelSource.create(getRaster(), Color.RED, new AffineTransform());
        } else {
            multiLevelSource = MultiLevelSource.NULL;
        }

        final ImageLayer roiLayer = new ImageLayer(multiLevelSource);
        roiLayer.setName("ROI of " + getRaster().getName());
        roiLayer.setVisible(false);
        roiLayer.getStyle().setOpacity(0.5);

        return roiLayer;
    }

    private GraticuleLayer createGraticuleLayer() {
        final GraticuleLayer graticuleLayer = new GraticuleLayer(getProduct(), getRaster());
        graticuleLayer.setName("Graticule of " + getRaster().getName());
        graticuleLayer.setVisible(false);
        graticuleLayer.getStyle().setOpacity(0.5);
        return graticuleLayer;
    }

    private PlacemarkLayer createPinLayer() {
        final PlacemarkLayer pinLayer = new PlacemarkLayer(getRaster().getProduct(), PinDescriptor.INSTANCE,
                new AffineTransform());
        pinLayer.setName("Pins");
        pinLayer.setVisible(false);
        pinLayer.setTextEnabled(true);
        return pinLayer;
    }

    private PlacemarkLayer createGcpLayer() {
        final PlacemarkLayer gcpLayer = new PlacemarkLayer(getRaster().getProduct(), GcpDescriptor.INSTANCE,
                new AffineTransform());
        gcpLayer.setName("GCPs");
        gcpLayer.setVisible(false);
        gcpLayer.setTextEnabled(false);
        return gcpLayer;
    }

    private Layer createBitmaskCollectionLayer() {
        return new BitmaskCollectionLayer(getRaster());
    }

    private MultiLevelSource getImageMultiLevelSource() {
        return imageMultiLevelSource;
    }
}
