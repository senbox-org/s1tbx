package org.esa.beam.framework.ui.product;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.IOException;

import org.esa.beam.framework.datamodel.GcpDescriptor;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.glayer.FigureLayer;
import org.esa.beam.glayer.GraticuleLayer;
import org.esa.beam.glayer.PlacemarkLayer;
import org.esa.beam.glevel.BandMultiLevelImage;
import org.esa.beam.glevel.MaskMultiLevelImage;
import org.esa.beam.util.ProductUtils;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.LevelImage;

/**
 * TODO - Apidoc
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
class ProductSceneImage45 extends ProductSceneImage {

    private Layer rootLayer;
    private BandMultiLevelImage levelImage;

    ProductSceneImage45(RasterDataNode raster, ProductSceneView45 view) throws IOException {
        super(raster.getDisplayName(), new RasterDataNode[]{raster}, view.getImageInfo());
        levelImage = view.getSceneImage45().getLevelImage();
        rootLayer = view.getRootLayer();
    }

    ProductSceneImage45(RasterDataNode raster) throws IOException {
        super(raster.getDisplayName(), new RasterDataNode[]{raster}, raster.getImageInfo());
        levelImage = new BandMultiLevelImage(raster, new AffineTransform());
        setImageInfo(raster.getImageInfo());
        initRootLayer();
    }

    ProductSceneImage45(RasterDataNode[] rasters) throws IOException {
        super("RGB", rasters, null);
        levelImage = new BandMultiLevelImage(rasters, new AffineTransform());
        setImageInfo(ProductUtils.createImageInfo(rasters, false, ProgressMonitor.NULL));
        initRootLayer();
    }

    private void initRootLayer() {
        rootLayer = new Layer();
        final ImageLayer imageLayer = new ImageLayer(levelImage);
        imageLayer.setName(getName());
        imageLayer.setVisible(true);
        rootLayer.getChildLayerList().add(imageLayer);

        final Color noDataOverlayColor = Color.ORANGE;
        final ImageLayer noDataLayer = new ImageLayer(LevelImage.NULL);
        noDataLayer.setName("No-data mask of " + getRaster().getName());
        noDataLayer.setVisible(false);
        noDataLayer.getStyle().setOpacity(0.5);

        final Color roiColor = Color.RED;
        final ImageLayer roiLayer = new ImageLayer(LevelImage.NULL);
        roiLayer.setName("ROI of " + getRaster().getName());
        roiLayer.setVisible(false);
        roiLayer.getStyle().setOpacity(0.5);
        
        final GraticuleLayer graticuleLayer = new GraticuleLayer(getProduct(), getRaster());
        graticuleLayer.setName("Graticule of " + getRaster().getName());
        graticuleLayer.setVisible(false);
        graticuleLayer.getStyle().setOpacity(0.5);

        final PlacemarkLayer pinLayer = new PlacemarkLayer(getRaster().getProduct(), PinDescriptor.INSTANCE,
                                                           new AffineTransform());
        pinLayer.setName("Pins");
        pinLayer.setVisible(false);
        pinLayer.setTextEnabled(true);

        final PlacemarkLayer gcpLayer = new PlacemarkLayer(getRaster().getProduct(), GcpDescriptor.INSTANCE,
                                                           new AffineTransform());
        gcpLayer.setName("GCPs");
        gcpLayer.setVisible(false);
        gcpLayer.setTextEnabled(false);

        final FigureLayer figureLayer = new FigureLayer(new Figure[0]);
        figureLayer.setVisible(true);

        rootLayer.getChildLayerList().add(noDataLayer);
        rootLayer.getChildLayerList().add(figureLayer);
        rootLayer.getChildLayerList().add(roiLayer);
        rootLayer.getChildLayerList().add(graticuleLayer);
        rootLayer.getChildLayerList().add(pinLayer);
        rootLayer.getChildLayerList().add(gcpLayer);

        final Layer bitmaskLayer = new Layer();
        bitmaskLayer.setName("Bitmasks");
        final BitmaskDef[] bitmaskDefs = getProduct().getBitmaskDefs();
        for (BitmaskDef bitmaskDef : bitmaskDefs) {
            final Color color = bitmaskDef.getColor();
            final String expression = bitmaskDef.getExpr();
            MaskMultiLevelImage maskImage = new MaskMultiLevelImage(getProduct(), color, expression, false, new AffineTransform());
            final ImageLayer maskImageLayer = new ImageLayer(maskImage);
            maskImageLayer.setName(bitmaskDef.getName());
            maskImageLayer.setVisible(false);
            maskImageLayer.getStyle().setOpacity(bitmaskDef.getAlpha());
            bitmaskLayer.getChildLayerList().add(maskImageLayer);
        }

        rootLayer.getChildLayerList().add(bitmaskLayer);
    }

    public Layer getRootLayer() {
        return rootLayer;
    }

    public BandMultiLevelImage getLevelImage() {
        return levelImage;
    }
}
