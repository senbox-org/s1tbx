package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.glayer.FigureLayer;
import org.esa.beam.glayer.GraticuleLayer;
import org.esa.beam.glayer.PlacemarkLayer;
import org.esa.beam.glevel.BandImageMultiLevelSource;
import org.esa.beam.glevel.MaskImageMultiLevelSource;
import org.esa.beam.glevel.RoiImageMultiLevelSource;
import org.esa.beam.glevel.TiledFileMultiLevelSource;
import org.esa.beam.util.ProductUtils;

import java.awt.*;
import java.awt.geom.AffineTransform;
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
class ProductSceneImage45 extends ProductSceneImage {

    private Layer rootLayer;
    private MultiLevelSource multiLevelSource;

    ProductSceneImage45(RasterDataNode raster, ProductSceneView45 view) {
        super(raster.getDisplayName(), new RasterDataNode[]{raster}, view.getImageInfo());
        multiLevelSource = view.getSceneImage45().getMultiLevelSource();
        rootLayer = view.getRootLayer();
    }

    ProductSceneImage45(RasterDataNode raster) {
        super(raster.getDisplayName(), new RasterDataNode[]{raster}, raster.getImageInfo());
        multiLevelSource = BandImageMultiLevelSource.create(raster);
        setImageInfo(raster.getImageInfo());
        initRootLayer();
    }

    ProductSceneImage45(String name, RasterDataNode[] rasters) throws IOException {
        super(name, rasters, null);
        multiLevelSource = BandImageMultiLevelSource.create(rasters);
        setImageInfo(ProductUtils.createImageInfo(rasters, false, ProgressMonitor.NULL));
        initRootLayer();
    }

    private void initRootLayer() {
        rootLayer = new Layer();
        final ImageLayer imageLayer = new ImageLayer(multiLevelSource);
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
        rootLayer.getChildLayerList().add(bitmaskLayer);
        rootLayer.getChildLayerList().add(roiLayer);
        rootLayer.getChildLayerList().add(noDataLayer);
        rootLayer.getChildLayerList().add(imageLayer);
        if (getRaster().getProduct().getProductType().startsWith("MIR_")) {
            // SMOS
            Layer createWorldLayer = createWorldLayer();
            if (createWorldLayer != null) {
                rootLayer.getChildLayerList().add(createWorldLayer);
            }
        }

        getProduct().addProductNodeListener(new BitmaskDefListener(bitmaskLayer));
        getProduct().addProductNodeListener(new BitmaskOverlayInfoListener(bitmaskLayer));
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
        blueMarbleLayer.getStyle().setOpacity(0.0);
        return blueMarbleLayer;
    }

    private ImageLayer createNoDataLayer() {
        final MultiLevelSource multiLevelSource;

        if (getRaster().getValidMaskExpression() != null) {
            // todo - get color from style, set color
            multiLevelSource = MaskImageMultiLevelSource.create(getRaster().getProduct(), Color.ORANGE,
                    getRaster().getValidMaskExpression(), true, new AffineTransform());
        } else {
            multiLevelSource = MultiLevelSource.NULL;
        }

        final ImageLayer noDataLayer = new ImageLayer(multiLevelSource);
        noDataLayer.setName("No-data mask of " + getRaster().getName());
        noDataLayer.setVisible(false);
        noDataLayer.getStyle().setOpacity(0.5);

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
        final Layer layer = new Layer();
        layer.setName("Bitmasks");
        final BitmaskDef[] bitmaskDefs = getProduct().getBitmaskDefs();
        for (final BitmaskDef bitmaskDef : bitmaskDefs) {
            layer.getChildLayerList().add(createBitmaskLayer(bitmaskDef));
        }

        return layer;
    }

    private Layer createBitmaskLayer(final BitmaskDef bitmaskDef) {
        final Color color = bitmaskDef.getColor();
        final String expr = bitmaskDef.getExpr();
        final MultiLevelSource multiLevelSource = MaskImageMultiLevelSource.create(getProduct(), color, expr, false,
                new AffineTransform());

        final Layer layer = new ImageLayer(multiLevelSource);
        layer.setName(bitmaskDef.getName());
        final BitmaskOverlayInfo overlayInfo = getRaster().getBitmaskOverlayInfo();
        layer.setVisible(overlayInfo != null && overlayInfo.containsBitmaskDef(bitmaskDef));
        layer.getStyle().setOpacity(bitmaskDef.getAlpha());
        // todo - add layer icon, bitmask color or even bitmaskDef to layer style?

        return layer;
    }

    public Layer getRootLayer() {
        return rootLayer;
    }

    private MultiLevelSource getMultiLevelSource() {
        return multiLevelSource;
    }

    private class BitmaskDefListener implements ProductNodeListener {
        private final Layer bitmaskLayer;
        private final Map<BitmaskDef, Layer> layerMap;

        public BitmaskDefListener(Layer bitmaskLayer) {
            this.bitmaskLayer = bitmaskLayer;
            this.layerMap = new HashMap<BitmaskDef, Layer>();

            final Product product = getProduct();
            for (final Layer layer : bitmaskLayer.getChildLayerList()) {
                layerMap.put(product.getBitmaskDef(layer.getName()), layer);
            }
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            final ProductNode sourceNode = event.getSourceNode();

            if (sourceNode instanceof BitmaskDef) {
                final BitmaskDef bitmaskDef = (BitmaskDef) sourceNode;
                final Layer layer = layerMap.remove(bitmaskDef);

                if (layer != null) {
                    final int index = bitmaskLayer.getChildLayerList().indexOf(layer);
                    if (index != -1) {
                        final Layer newLayer = createBitmaskLayer(bitmaskDef);
                        final Layer oldLayer = bitmaskLayer.getChildLayerList().remove(index);
                        bitmaskLayer.getChildLayerList().add(index, newLayer);
                        layerMap.put(bitmaskDef, newLayer);
                        oldLayer.dispose();
                    }
                }
            }
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            nodeChanged(event);
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            final ProductNode sourceNode = event.getSourceNode();

            if (sourceNode instanceof BitmaskDef) {
                final BitmaskDef[] bitmaskDefs = getProduct().getBitmaskDefs();

                for (int i = 0; i < bitmaskDefs.length; i++) {
                    if (sourceNode == bitmaskDefs[i]) {
                        final Layer layer = createBitmaskLayer(bitmaskDefs[i]);
                        bitmaskLayer.getChildLayerList().add(i, layer);
                        layerMap.put(bitmaskDefs[i], layer);
                        break;
                    }
                }
            }
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            final ProductNode sourceNode = event.getSourceNode();

            if (sourceNode instanceof BitmaskDef) {
                final BitmaskDef bitmaskDef = (BitmaskDef) sourceNode;
                final Layer layer = layerMap.remove(bitmaskDef);
                if (layer != null) {
                    if (bitmaskLayer.getChildLayerList().remove(layer)) {
                        layer.dispose();
                    }
                }
            }
        }
    }

    private class BitmaskOverlayInfoListener implements ProductNodeListener {
        private final Layer bitmaskLayer;

        private BitmaskOverlayInfoListener(Layer bitmaskLayer) {
            this.bitmaskLayer = bitmaskLayer;
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            final ProductNode sourceNode = event.getSourceNode();

            if (sourceNode == getRaster() &&
                    RasterDataNode.PROPERTY_NAME_BITMASK_OVERLAY_INFO.equals(event.getPropertyName())) {
                final BitmaskOverlayInfo overlayInfo = getRaster().getBitmaskOverlayInfo();
                final Product product = getProduct();

                for (final Layer layer : bitmaskLayer.getChildLayerList()) {
                    layer.setVisible(overlayInfo.containsBitmaskDef(product.getBitmaskDef(layer.getName())));
                }
            }
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
        }
    }
}
