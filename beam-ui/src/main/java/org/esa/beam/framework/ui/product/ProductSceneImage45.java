package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.LevelImage;
import org.esa.beam.framework.datamodel.GcpDescriptor;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glayer.PlacemarkLayer;
import org.esa.beam.glevel.BandMultiLevelImage;
import org.esa.beam.util.ProductUtils;

import java.awt.Color;
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

        rootLayer.getChildLayerList().add(noDataLayer);
        rootLayer.getChildLayerList().add(new ImageLayer(LevelImage.NULL)); // figure layer
        rootLayer.getChildLayerList().add(roiLayer);
        rootLayer.getChildLayerList().add(new ImageLayer(LevelImage.NULL)); // graticule layer
        rootLayer.getChildLayerList().add(pinLayer);
        rootLayer.getChildLayerList().add(gcpLayer);

//        figureLayer.setVisible(true);
//        roiLayer.setVisible(false);
//        graticuleLayer.setVisible(false);
//        pinLayer.setVisible(false);
//        pinLayer.setTextEnabled(true);
//        gcpLayer.setVisible(false);
//        gcpLayer.setTextEnabled(false);
//
//        rootLayer.getChildLayerList().add(new FigureLayer());
//        rootLayer.getChildLayerList().add(new ROILayer(getRaster()));
//        rootLayer.getChildLayerList().add(new GraticuleLayer(getProduct(), getRaster()));
//        rootLayer.getChildLayerList().add(new PlacemarkLayer(getProduct(), PinDescriptor.INSTANCE));
//        rootLayer.getChildLayerList().add(new PlacemarkLayer(getProduct(), GcpDescriptor.INSTANCE));

//        NoDataLayer noDataLayer = (NoDataLayer) layerModel.getLayer(1);
//        FigureLayer figureLayer = (FigureLayer) layerModel.getLayer(2);
//        ROILayer roiLayer = (ROILayer) layerModel.getLayer(3);
//        GraticuleLayer graticuleLayer = (GraticuleLayer) layerModel.getLayer(4);
//        PlacemarkLayer pinLayer = (PlacemarkLayer) layerModel.getLayer(5);
//        PlacemarkLayer gcpLayer = (PlacemarkLayer) layerModel.getLayer(6);
//
//        imageLayer.setVisible(true);
//        noDataLayer.setVisible(false);
//        figureLayer.setVisible(true);
//        roiLayer.setVisible(false);
//        graticuleLayer.setVisible(false);
//        pinLayer.setVisible(false);
//        pinLayer.setTextEnabled(true);
//        gcpLayer.setVisible(false);
//        gcpLayer.setTextEnabled(false);
    }

    public Layer getRootLayer() {
        return rootLayer;
    }


    public BandMultiLevelImage getLevelImage() {
        return levelImage;
    }
}
