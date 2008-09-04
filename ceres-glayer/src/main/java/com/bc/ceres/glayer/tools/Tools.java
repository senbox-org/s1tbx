package com.bc.ceres.glayer.tools;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.FileMultiLevelSourceFactory;
import com.sun.media.jai.codec.TIFFEncodeParam;

import javax.media.jai.*;
import javax.media.jai.operator.*;
import javax.media.jai.util.ImagingListener;
import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.RenderingHints;
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
            public boolean errorOccurred(String message, Throwable thrown, Object where, boolean isRetryable) throws RuntimeException {
                System.out.println("JAI error occured: " + message);
                return false;
            }
        });
        final long memoryCapacity = 256 * (1024 * 1024);
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(memoryCapacity);
        JAI.getDefaultInstance().getTileCache().setMemoryThreshold(0.75f);
    }


    public static AffineTransform loadWorldFile(String filename) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            try {
                double[] flatMatrix = new double[]{
                        Double.parseDouble(reader.readLine().trim()), // X scale in resulting X direction
                        Double.parseDouble(reader.readLine().trim()), // Y scale in resulting X direction
                        Double.parseDouble(reader.readLine().trim()), // X scale in resulting Y direction
                        Double.parseDouble(reader.readLine().trim()), // Y scale in resulting Y direction
                        Double.parseDouble(reader.readLine().trim()), // X coordinate of the center of rotation (the center of the Upper Left Pixel of the unrotated image)
                        Double.parseDouble(reader.readLine().trim()), // Y coordinate of the center of rotation (the center of the Upper Left Pixel of the unrotated image)
                };
                return new AffineTransform(flatMatrix);
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
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
                                    int levelCount,
                                    boolean concurrent) {
        final LayerCanvas layerCanvas = new LayerCanvas();
        layerCanvas.installMouseHandler();
        final Layer collectionLayer = layerCanvas.getLayer();
        final MultiLevelSource source = FileMultiLevelSourceFactory.create(location, extension, imageToModelTransform, levelCount);
        final ImageLayer layer = new ImageLayer(source);
        collectionLayer.getChildLayerList().add(layer);
        final Rectangle viewportBounds = new Rectangle(0, 0, 640, 480);
        layerCanvas.setPreferredSize(new Dimension(640, 480));
        layerCanvas.getViewport().setBounds(viewportBounds);
        layerCanvas.getViewport().zoom(layer.getBounds());
        openFrame(layerCanvas, location.getPath(), viewportBounds);
    }

    public static void displayImage(String title,
                                    RenderedImage image,
                                    AffineTransform imageToModelTransform,
                                    int levelCount,
                                    boolean concurrent) {
        displayImages(title,
                      new RenderedImage[]{image},
                      new AffineTransform[]{imageToModelTransform},
                      levelCount,
                      concurrent);
    }

    public static void displayImages(String title, RenderedImage[] images,
                                     AffineTransform[] imageToModelTransforms,
                                     int levelCount,
                                     boolean concurrent) {
        final LayerCanvas layerCanvas = new LayerCanvas();
        layerCanvas.installMouseHandler();
        final Layer collectionLayer = layerCanvas.getLayer();
        for (int i = 0; i < images.length; i++) {
            final ImageLayer layer = new ImageLayer(images[i], imageToModelTransforms[i], levelCount);
            layer.setDebug(true);
            collectionLayer.getChildLayerList().add(layer);
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
        ImageMIPMap imageMIPMap = new ImageMIPMap(sourceImage, AffineTransform.getScaleInstance(0.5, 0.5), Interpolation.getInstance(Interpolation.INTERP_NEAREST));
        for (int level = 0; level <= maxLevel; level++) {
            final RenderedImage scaledImage = imageMIPMap.getImage(level);
            storeTiledTiff(scaledImage, targetBaseName + "." + level + ".tif");
        }
    }

    public static void storeImagePyramid(RenderedImage sourceImage, String targetBaseName, int maxLevel, String format) {
        ImageMIPMap imageMIPMap = new ImageMIPMap(sourceImage, AffineTransform.getScaleInstance(0.5, 0.5), Interpolation.getInstance(Interpolation.INTERP_NEAREST));
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

}