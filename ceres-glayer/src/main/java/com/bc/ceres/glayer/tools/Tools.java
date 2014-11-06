/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.glayer.tools;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.FileMultiLevelSource;
import com.sun.media.jai.codec.TIFFEncodeParam;

import javax.media.jai.ImageLayout;
import javax.media.jai.ImageMIPMap;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.AffineDescriptor;
import javax.media.jai.operator.FileLoadDescriptor;
import javax.media.jai.operator.FileStoreDescriptor;
import javax.media.jai.operator.FormatDescriptor;
import javax.media.jai.operator.MosaicDescriptor;
import javax.media.jai.operator.ScaleDescriptor;
import javax.media.jai.util.ImagingListener;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Tools {

    public static void configureJAI() {
        JAI.getDefaultInstance().setImagingListener(new ImagingListener() {
            @Override
            public boolean errorOccurred(String message, Throwable thrown, Object where, boolean isRetryable) throws
                    RuntimeException {
                System.out.println("JAI error occurred: " + message);
                return false;
            }
        });
        final long memoryCapacity = 256 * (1024 * 1024);
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(memoryCapacity);
        JAI.getDefaultInstance().getTileCache().setMemoryThreshold(0.75f);
    }


    public static AffineTransform loadWorldFile(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        try {
            double[] flatMatrix = new double[6];
            new AffineTransform().getMatrix(flatMatrix); // init with identity
            for (int i = 0; i < flatMatrix.length; i++) {
                final String parameter = reader.readLine();
                if (parameter == null) {
                    throw new IOException("Could not read world file: Missing a parameter.");
                }
                try {
                    flatMatrix[i] = Double.valueOf(parameter);
                } catch (NumberFormatException e) {
                    IOException ioException = new IOException("Could not read world file. " + e.getMessage());
                    ioException.initCause(e);
                    throw ioException;
                }
            }
            return new AffineTransform(flatMatrix);
        } finally {
            reader.close();
        }
    }

    public static RenderedOp createMosaic(RenderedImage[] images) {
        return MosaicDescriptor.create(images, MosaicDescriptor.MOSAIC_TYPE_OVERLAY,
                                       null, null, null, new double[]{0.0}, null);

    }

    public static void storeTiledTiff(RenderedImage image, String filePath) {
        final TIFFEncodeParam encodeParam = new TIFFEncodeParam();
        encodeParam.setTileSize(image.getTileWidth(), image.getTileHeight());
        encodeParam.setWriteTiled(true);
        encodeParam.setCompression(TIFFEncodeParam.COMPRESSION_DEFLATE);
        System.out.println("Storing tiled TIFF image to " + filePath + "...");
        FileStoreDescriptor.create(image, filePath, "TIFF", encodeParam, false, null);
    }

    public static RenderedOp scaleImage(RenderedImage image, float scale) {
        final Interpolation interpol = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        return ScaleDescriptor.create(image, scale, scale, 0.0f, 0.0f, interpol, null);
    }

    public static RenderedOp transformImage(RenderedImage image, double x0, double y0, double theta, double scale) {
        final AffineTransform transform = new AffineTransform();
        transform.rotate(theta, -0.5f * image.getWidth(), -0.5f * image.getHeight());
        transform.scale(scale, scale);
        transform.translate(x0, y0);
        return transformImage(image, transform);
    }

    public static RenderedOp transformImage(RenderedImage image, AffineTransform transform) {
        return AffineDescriptor.create(image, transform,
                                       Interpolation.getInstance(Interpolation.INTERP_NEAREST),
                                       new double[]{0.0}, null);
    }

    public static RenderedOp createTiledImage(RenderedImage image, int tileWidth, int tileHeight) {
        final int dataType = image.getSampleModel().getDataType();
        final ImageLayout imageLayout = new ImageLayout();
        imageLayout.setTileWidth(tileWidth);
        imageLayout.setTileHeight(tileHeight);
        final RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
        return FormatDescriptor.create(image, dataType, hints);
    }

    public static RenderedOp loadImage(String filePath) {
        System.out.println("Loading image from " + filePath);
        return FileLoadDescriptor.create(filePath, null, true, null);
    }

    public static void displayImage(File location,
                                    String extension,
                                    AffineTransform imageToModelTransform,
                                    int levelCount) {
        final LayerCanvas layerCanvas = new LayerCanvas();
        installLayerCanvasNavigation(layerCanvas);
        final Layer collectionLayer = layerCanvas.getLayer();
        final MultiLevelSource source = FileMultiLevelSource.create(location, extension, imageToModelTransform,
                                                                    levelCount);
        final ImageLayer layer = new ImageLayer(source);
        collectionLayer.getChildren().add(layer);
        final Rectangle viewportBounds = new Rectangle(0, 0, 640, 480);
        layerCanvas.setPreferredSize(new Dimension(640, 480));
        layerCanvas.getViewport().setViewBounds(viewportBounds);
        layerCanvas.getViewport().zoom(layer.getModelBounds());
        openFrame(layerCanvas, location.getPath(), viewportBounds);
    }

    public static void displayImage(String title,
                                    RenderedImage image,
                                    AffineTransform imageToModelTransform,
                                    int levelCount) {
        displayImages(title,
                      new RenderedImage[]{image},
                      new AffineTransform[]{imageToModelTransform},
                      levelCount);
    }

    public static void displayImages(String title, RenderedImage[] images,
                                     AffineTransform[] imageToModelTransforms,
                                     int levelCount) {
        final LayerCanvas layerCanvas = new LayerCanvas();
        installLayerCanvasNavigation(layerCanvas);
        final Layer collectionLayer = layerCanvas.getLayer();
        for (int i = 0; i < images.length; i++) {
            final ImageLayer layer = new ImageLayer(images[i], imageToModelTransforms[i], levelCount);
            collectionLayer.getChildren().add(layer);
        }

        openFrame(layerCanvas, title, new Rectangle(0, 0, 512, 512));
    }

    public static void dumpImageInfo(RenderedImage image) {
        final SampleModel sampleModel = image.getSampleModel();
        final ColorModel colorModel = image.getColorModel();
        System.out.println("image: " + image);
        System.out.println("  minX            = " + image.getMinX());
        System.out.println("  minY            = " + image.getMinY());
        System.out.println("  width           = " + image.getWidth());
        System.out.println("  height          = " + image.getHeight());
        System.out.println("  colorModel      = " + (colorModel != null ? colorModel.getClass() : "null"));
        System.out.println("  colorSpace      = " + (colorModel != null ? colorModel.getColorSpace() : "null"));
        System.out.println("  sampleModel     = " + sampleModel.getClass());
        System.out.println("  numBands        = " + sampleModel.getNumBands());
        System.out.println("  dataType        = " + sampleModel.getDataType());
        System.out.println("  transferType    = " + sampleModel.getTransferType());
        System.out.println("  tileGridXOffset = " + image.getTileGridXOffset());
        System.out.println("  tileGridYOffset = " + image.getTileGridYOffset());
        System.out.println("  minTileX        = " + image.getMinTileX());
        System.out.println("  minTileY        = " + image.getMinTileY());
        System.out.println("  tileWidth       = " + image.getTileWidth());
        System.out.println("  tileHeight      = " + image.getTileHeight());
    }

    public static void storeTiffPyramid(RenderedImage sourceImage, String targetBaseName, int maxLevel) {
        ImageMIPMap imageMIPMap = new ImageMIPMap(sourceImage, AffineTransform.getScaleInstance(0.5, 0.5),
                                                  Interpolation.getInstance(Interpolation.INTERP_NEAREST));
        for (int level = 0; level <= maxLevel; level++) {
            final RenderedImage scaledImage = imageMIPMap.getImage(level);
            storeTiledTiff(scaledImage, targetBaseName + "." + level + ".tif");
        }
    }

    private static void openFrame(LayerCanvas layerCanvas, String title, Rectangle bounds) {
        final JFrame frame = new JFrame(title);
        frame.getContentPane().add(layerCanvas, BorderLayout.CENTER);
        frame.setBounds(bounds);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public static void installLayerCanvasNavigation(LayerCanvas layerCanvas) {
        layerCanvas.setNavControlShown(true);
        final MouseHandler mouseHandler = new MouseHandler(layerCanvas);
        layerCanvas.addMouseListener(mouseHandler);
        layerCanvas.addMouseMotionListener(mouseHandler);
        layerCanvas.addMouseWheelListener(mouseHandler);
    }

    public static class MouseHandler extends MouseInputAdapter {

        private LayerCanvas layerCanvas;
        private SliderPopUp sliderPopUp;
        private Point p0;

        private MouseHandler(LayerCanvas layerCanvas) {
            this.layerCanvas = layerCanvas;
            this.sliderPopUp = new SliderPopUp();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            p0 = e.getPoint();
        }

        @Override
        public void mouseReleased(final MouseEvent mouseEvent) {
            if (mouseEvent.isPopupTrigger()) {
                final Point point = mouseEvent.getPoint();
                SwingUtilities.convertPointToScreen(point, layerCanvas);
                sliderPopUp.show(point);
            } else {
                sliderPopUp.hide();
            }
        }


        @Override
        public void mouseDragged(MouseEvent e) {
            final Point p = e.getPoint();
            final double dx = p.x - p0.x;
            final double dy = p.y - p0.y;
            layerCanvas.getViewport().moveViewDelta(dx, dy);
            p0 = p;
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            final int wheelRotation = e.getWheelRotation();
            final double newZoomFactor = layerCanvas.getViewport().getZoomFactor() * Math.pow(1.1, wheelRotation);
            layerCanvas.getViewport().setZoomFactor(newZoomFactor);
        }

        private class SliderPopUp {

            private JWindow window;
            private JSlider slider;

            public void show(Point location) {
                if (window == null) {
                    initUI();
                }
                final double oldZoomFactor = layerCanvas.getViewport().getZoomFactor();
                slider.setValue((int) Math.round(10.0 * Math.log(oldZoomFactor) / Math.log(2.0)));
                window.setLocation(location);
                window.setVisible(true);
            }

            public void hide() {
                if (window != null) {
                    window.setVisible(false);
                }
            }

            private void initUI() {
                window = new JWindow();
                final int min = -100;
                final int max = 100;
                slider = new JSlider(min, max);
                slider.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        final double newZoomFactor = Math.pow(2.0, slider.getValue() / 10.0);
                        layerCanvas.getViewport().setZoomFactor(newZoomFactor);
                        if (!slider.getValueIsAdjusting()) {
                            hide();
                        }
                    }
                });

                window.requestFocus();
                window.setAlwaysOnTop(true);
                window.add(slider);
                window.pack();
                window.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusLost(FocusEvent e) {
                        hide();
                    }
                });
            }
        }
    }
}
