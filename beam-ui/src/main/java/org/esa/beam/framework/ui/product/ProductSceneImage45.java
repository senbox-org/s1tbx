package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.support.ShapeLayer;
import com.bc.ceres.glevel.LevelImage;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.datamodel.GcpDescriptor;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.glayer.PlacemarkLayer;
import org.esa.beam.glevel.BandMultiLevelImage;
import org.esa.beam.layer.FigureLayer;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.PropertyMap;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.*;
import java.util.List;

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

        final FigureLayer figureLayer = new FigureLayer(new Figure[0]);
        figureLayer.setVisible(true);

        rootLayer.getChildLayerList().add(noDataLayer);
        rootLayer.getChildLayerList().add(figureLayer);
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

    public static class FigureLayer extends Layer {
        // TODO: IMAGING 4.5: Layer.getStyle(), SVG property names!
        public static final boolean DEFAULT_SHAPE_OUTLINED = true;
        public static final double DEFAULT_SHAPE_OUTL_TRANSPARENCY = 0.1;
        public static final Color DEFAULT_SHAPE_OUTL_COLOR = Color.yellow;
        public static final double DEFAULT_SHAPE_OUTL_WIDTH = 1.0;
        public static final boolean DEFAULT_SHAPE_FILLED = true;
        public static final double DEFAULT_SHAPE_FILL_TRANSPARENCY = 0.5;
        public static final Color DEFAULT_SHAPE_FILL_COLOR = Color.blue;

        private final List<Figure> figureList;
        private final AffineTransform shapeToModelTransform;
        private final AffineTransform modelToShapeTransform;
        private Map<String, Object> figureAttributes;

        public FigureLayer(Figure[] figures) {
            this.figureList = new ArrayList<Figure>(Arrays.asList(figures));
            this.shapeToModelTransform =
                    this.modelToShapeTransform = new AffineTransform();
            figureAttributes = new HashMap<String, Object>();
        }

        public void addFigure(Figure currentShapeFigure) {
            currentShapeFigure.setAttributes(figureAttributes);
            figureList.add(currentShapeFigure);
        }

        public List<Figure> getFigureList() {
            return figureList;
        }

        public void setFigureList(List<Figure> list) {
            figureList.clear();
            figureList.addAll(list);
            for (Figure figure : figureList) {
                figure.setAttributes(figureAttributes);
            }
        }

//        public AffineTransform getShapeToModelTransform() {
//            return (AffineTransform) shapeToModelTransform.clone();
//        }
//
//        public AffineTransform getModelToShapeTransform() {
//            return (AffineTransform) modelToShapeTransform.clone();
//        }

        @Override
        public Rectangle2D getBounds() {
            Rectangle2D boundingBox = new Rectangle2D.Double();
            for (Figure figure : figureList) {
                boundingBox.add(figure.getShape().getBounds2D());
            }
            return shapeToModelTransform.createTransformedShape(boundingBox).getBounds2D();
        }

        @Override
        protected void renderLayer(Rendering rendering) {
            final Graphics2D g2d = rendering.getGraphics();
            final Viewport vp = rendering.getViewport();
            final AffineTransform transformSave = g2d.getTransform();
            try {
                final AffineTransform transform = new AffineTransform();
                transform.concatenate(transformSave);
                transform.concatenate(vp.getModelToViewTransform());
                transform.concatenate(shapeToModelTransform);
                g2d.setTransform(transform);

                for (Figure figure : figureList) {
                    figure.draw(g2d);
                }
            } finally {
                g2d.setTransform(transformSave);
            }
        }

        /**
         * @param propertyMap
         */
        public void setStyleProperties(PropertyMap propertyMap) {
            final boolean outlined = propertyMap.getPropertyBool("shape.outlined", FigureLayer.DEFAULT_SHAPE_OUTLINED);
            final float outlTransp = (float) propertyMap.getPropertyDouble("shape.outl.transparency",
                                                                           FigureLayer.DEFAULT_SHAPE_OUTL_TRANSPARENCY);
            final Color outlColor = propertyMap.getPropertyColor("shape.outl.color", FigureLayer.DEFAULT_SHAPE_OUTL_COLOR);
            final float outlWidth = (float) propertyMap.getPropertyDouble("shape.outl.width",
                                                                          FigureLayer.DEFAULT_SHAPE_OUTL_WIDTH);

            final boolean filled = propertyMap.getPropertyBool("shape.filled", FigureLayer.DEFAULT_SHAPE_FILLED);
            final float fillTransp = (float) propertyMap.getPropertyDouble("shape.fill.transparency",
                                                                           FigureLayer.DEFAULT_SHAPE_OUTL_TRANSPARENCY);
            final Color fillColor = propertyMap.getPropertyColor("shape.fill.color", FigureLayer.DEFAULT_SHAPE_OUTL_COLOR);

            final AlphaComposite outlComp;
            if (outlTransp > 0.0f) {
                outlComp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f - outlTransp);
            } else {
                outlComp = null;
            }

            final AlphaComposite fillComp;
            if (fillTransp > 0.0f) {
                fillComp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f - fillTransp);
            } else {
                fillComp = null;
            }

            figureAttributes.put(Figure.OUTLINED_KEY, outlined ? Boolean.TRUE : Boolean.FALSE);
            figureAttributes.put(Figure.OUTL_COMPOSITE_KEY, outlComp);
            figureAttributes.put(Figure.OUTL_COLOR_KEY, outlColor);
            figureAttributes.put(Figure.OUTL_STROKE_KEY, new BasicStroke(outlWidth));

            figureAttributes.put(Figure.FILLED_KEY, filled ? Boolean.TRUE : Boolean.FALSE);
            figureAttributes.put(Figure.FILL_COMPOSITE_KEY, fillComp);
            figureAttributes.put(Figure.FILL_PAINT_KEY, fillColor);

            for (Figure figure : figureList) {
                figure.setAttributes(figureAttributes);
            }
            
        }

    }
}
