package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.LayerImage;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.GcpDescriptor;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.glayer.FigureLayer;
import org.esa.beam.glayer.GraticuleLayer;
import org.esa.beam.glayer.PlacemarkLayer;
import org.esa.beam.glevel.BandMultiLevelImage;
import org.esa.beam.glevel.MaskMultiLevelImage;
import org.esa.beam.glevel.RoiMultiLevelImage;
import org.esa.beam.util.ProductUtils;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.IOException;

/**
 * TODO - Apidoc
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
class ProductSceneImage45 extends ProductSceneImage {

    private Layer rootLayer;
    private LayerImage layerImage;

    ProductSceneImage45(RasterDataNode raster, ProductSceneView45 view) throws IOException {
        super(raster.getDisplayName(), new RasterDataNode[]{raster}, view.getImageInfo());
        layerImage = view.getSceneImage45().getLayerImage();
        rootLayer = view.getRootLayer();
    }

    ProductSceneImage45(RasterDataNode raster) throws IOException {
        super(raster.getDisplayName(), new RasterDataNode[]{raster}, raster.getImageInfo());
        layerImage = BandMultiLevelImage.create(raster, new AffineTransform());
        setImageInfo(raster.getImageInfo());
        initRootLayer();
    }

    ProductSceneImage45(RasterDataNode[] rasters) throws IOException {
        super("RGB", rasters, null);
        layerImage = BandMultiLevelImage.create(rasters, new AffineTransform());
        setImageInfo(ProductUtils.createImageInfo(rasters, false, ProgressMonitor.NULL));
        initRootLayer();
    }

    private void initRootLayer() {
        rootLayer = new Layer();
        final ImageLayer imageLayer = new ImageLayer(layerImage);
        imageLayer.setName(getName());
        imageLayer.setVisible(true);
        rootLayer.getChildLayerList().add(imageLayer);

        final ImageLayer noDataLayer = createNoDataLayer();
        final FigureLayer figureLayer = createFigureLayer();
        final ImageLayer roiLayer = createRoiLayer();
        final GraticuleLayer graticuleLayer = createGraticuleLayer();
        final PlacemarkLayer pinLayer = createPinLayer();
        final PlacemarkLayer gcpLayer = createGcpLayer();
        final Layer bitmaskLayer = createBitmaskLayer();

        rootLayer.getChildLayerList().add(noDataLayer);
        rootLayer.getChildLayerList().add(figureLayer);
        rootLayer.getChildLayerList().add(roiLayer);
        rootLayer.getChildLayerList().add(graticuleLayer);
        rootLayer.getChildLayerList().add(pinLayer);
        rootLayer.getChildLayerList().add(gcpLayer);
        rootLayer.getChildLayerList().add(bitmaskLayer);
    }

    private ImageLayer createNoDataLayer() {
        final LayerImage layerImage;

        if (getRaster().getValidMaskExpression() != null) {
            // todo - get color from style, set color
            layerImage = MaskMultiLevelImage.create(getRaster().getProduct(), Color.ORANGE,
                    getRaster().getValidMaskExpression(), true, new AffineTransform());
        } else {
            layerImage = LayerImage.NULL;
        }

        final ImageLayer noDataLayer = new ImageLayer(layerImage);
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
        final LayerImage layerImage;

        if (getRaster().getROIDefinition() != null && getRaster().getROIDefinition().isUsable()) {
            // todo - get color from style, set color
            layerImage = RoiMultiLevelImage.create(getRaster(), Color.RED, new AffineTransform());
        } else {
            layerImage = LayerImage.NULL;
        }

        final ImageLayer roiLayer = new ImageLayer(layerImage);
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

    private Layer createBitmaskLayer() {
        final Layer bitmaskLayer = new Layer();
        bitmaskLayer.setName("Bitmasks");
        final BitmaskDef[] bitmaskDefs = getProduct().getBitmaskDefs();
        for (BitmaskDef bitmaskDef : bitmaskDefs) {
            final Color color = bitmaskDef.getColor();
            final String expr = bitmaskDef.getExpr();
            LayerImage maskImage = MaskMultiLevelImage.create(getProduct(), color, expr, false, new AffineTransform());
            final ImageLayer maskImageLayer = new ImageLayer(maskImage);
            maskImageLayer.setName(bitmaskDef.getName());
            maskImageLayer.setVisible(false);
            maskImageLayer.getStyle().setOpacity(bitmaskDef.getAlpha());
            bitmaskLayer.getChildLayerList().add(maskImageLayer);
        }
        return bitmaskLayer;
    }

    public Layer getRootLayer() {
        return rootLayer;
    }

    private LayerImage getLayerImage() {
        return layerImage;
    }
}
