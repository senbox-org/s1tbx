package com.bc.layer.swing;

import com.bc.ImagingTestCase;
import com.bc.jai.tools.Utils;
import com.bc.layer.CollectionLayer;
import com.bc.layer.ImageLayer;
import com.bc.layer.ShapeLayer;
import com.bc.layer.Viewport;
import com.sun.media.jai.codec.FileCacheSeekableStream;

import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;
import javax.media.jai.operator.StreamDescriptor;
import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;


public class LayerCanvasTest extends ImagingTestCase {
    public void testDefaults() {
        final LayerCanvas layerCanvas = new LayerCanvas();
        assertNotNull(layerCanvas.getCollectionLayer());
        assertNotNull(layerCanvas.getViewport());
        assertNotNull(layerCanvas.getViewport().getView());
    }

    public static void main(String[] args) throws IOException {
        Utils.configureJAI();
        final LayerCanvas layerCanvas = new LayerCanvas();
        final PlanarImage image = loadTestImage();
        Utils.dumpImageInfo(image);
        final CollectionLayer collectionLayer = layerCanvas.getCollectionLayer();
        int levels = 5;
//        collectionLayer.add(new ImageLayer(image, new AffineTransform(0.25, 0, 0, 0.25, -image.getWidth() * 0.5 - image.getWidth() * 0.25, 0),levels));
//        collectionLayer.add(new ImageLayer(image, new AffineTransform(0.5, 0, 0, 0.5, -image.getWidth() * 0.5, 0),levels));
//        collectionLayer.add(new ImageLayer(image, new AffineTransform(),levels));
//        collectionLayer.add(new ImageLayer(image, new AffineTransform(2.0, 0, 0, 2.0, image.getWidth(), 0),levels));
//        collectionLayer.add(new ImageLayer(image, new AffineTransform(4.0, 0, 0, 4.0, image.getWidth() + 2 * image.getWidth(), 0),levels));
//        for (GraphicalLayer layer : collectionLayer) {
//            ((ImageLayer) layer).setSchedulingTiles(true);
//        }

//        final ImageLayer layer = new ImageLayer(image, new AffineTransform(), 5);
//        layer.setSchedulingTiles(true);
//        collectionLayer.add(layer);

        for (int i = 5; i >= -5; i--) {
            final double s = Math.pow(1.5, i);
            AffineTransform t = new AffineTransform();
            t.translate(s, s);
            t.rotate(Math.toRadians(i * 36.0));
            t.scale(s, s);
            final ImageLayer layer = new ImageLayer(image, t, levels);
            layer.setConcurrent(true);
            layer.setDebug(true);
            layerCanvas.getCollectionLayer().add(layer);
        }

        for (int i = 5; i >= -5; i--) {
            final double s = Math.pow(1.5, i);
            AffineTransform t = new AffineTransform();
            t.translate(s, s);
            t.rotate(Math.toRadians(i * 36.0));
            t.scale(s, s);
            layerCanvas.getCollectionLayer().add(new ShapeLayer(new Shape[]{
                    new Rectangle(0, 0, 50, 50),
                    new Ellipse2D.Double(100, 100, 100, 100),
            }, t));
        }


        final Viewport viewport = layerCanvas.getViewport();
        final Rectangle2D boundingBoxModel = collectionLayer.getBoundingBox();
        viewport.setModelScale(Math.max(boundingBoxModel.getWidth() / 512.0,
                               boundingBoxModel.getHeight() / 512.0),
                      new Point2D.Double(256, 256));
        viewport.pan(new Point2D.Double(-256, -256));

        final JFrame frame = new JFrame("LayerCanvasTest");
        frame.getContentPane().add(layerCanvas, BorderLayout.CENTER);
        frame.setSize(512, 512);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        LayerManagerForm.showLayerManager(frame, collectionLayer);
    }

    private static PlanarImage loadTestImage() throws IOException {
        final InputStream stream = LayerCanvasTest.class.getResourceAsStream("/images/mapimage.png");
        final RenderedOp image = StreamDescriptor.create(new FileCacheSeekableStream(stream), null, null);
        return new TiledImage(image, 256, 256);
//        return image;
    }
}
