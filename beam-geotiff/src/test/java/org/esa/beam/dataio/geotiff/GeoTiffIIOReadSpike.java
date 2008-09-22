/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.dataio.geotiff;

import com.sun.media.imageio.plugins.tiff.TIFFImageReadParam;
import com.sun.media.imageioimpl.plugins.tiff.TIFFIFD;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageMetadata;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageReader;
import com.sun.media.imageioimpl.plugins.tiff.TIFFRenderedImage;
import com.sun.media.imageioimpl.plugins.tiff.TIFFStreamMetadata;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author Marco
 */
public class GeoTiffIIOReadSpike {

    private static final String DEFAULT_DIR = "C:\\Dokumente und Einstellungen\\Marco Peters\\Eigene Dateien\\EOData\\geotiff";

    private GeoTiffIIOReadSpike() {
    }

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        File geoTiffFile;
        if (args.length == 0) {
            JFileChooser chooser = new JFileChooser(DEFAULT_DIR);
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setMultiSelectionEnabled(false);
            chooser.setFileFilter(new FileNameExtensionFilter("GeoTiff", "tif", "tiff"));
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                geoTiffFile = chooser.getSelectedFile();
            } else {
                System.out.println("No file selected");
                return;
            }
        } else {
            geoTiffFile = new File(args[0]);
        }
        final ImageInputStream inputStream = ImageIO.createImageInputStream(geoTiffFile);
        Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(inputStream);
        TIFFImageReader imageReader = (TIFFImageReader) imageReaders.next();
        imageReader.setInput(inputStream);
        System.out.println("imageReader.canReadRaster = " + imageReader.canReadRaster());
        System.out.println("imageReader.getWidth = " + imageReader.getWidth(0));
        System.out.println("imageReader.getHeight = " + imageReader.getHeight(0));
        System.out.println("imageReader.getTileWidth = " + imageReader.getTileWidth(0));
        System.out.println("imageReader.getTileHeight = " + imageReader.getTileHeight(0));
        TIFFImageReadParam readParam = (TIFFImageReadParam) imageReader.getDefaultReadParam();
        // has no effect
        // instead we can get the subsampled data from the image by an rectangle when cast to TIFFRenderedImage
//        readParam.setSourceRegion(new Rectangle(100, 100, 200, 200));
        readParam.setSourceSubsampling(10, 10, 0, 0);
        TIFFRenderedImage subsampledImage = (TIFFRenderedImage) imageReader.readAsRenderedImage(0, readParam);
        System.out.println("subsampledImage class = " + subsampledImage.getClass());
        System.out.println("subsampledImage.getNumBands = " + subsampledImage.getSampleModel().getNumBands());
        System.out.println("subsampledImage.getWidth = " + subsampledImage.getWidth());
        System.out.println("subsampledImage.getHeight = " + subsampledImage.getHeight());
        System.out.println("subsampledImage.getTileWidth = " + subsampledImage.getTileWidth());
        System.out.println("subsampledImage.getTileHeight = " + subsampledImage.getTileHeight());

        final TIFFStreamMetadata streamMetadata = (TIFFStreamMetadata) imageReader.getStreamMetadata();
        final Node streamTree = streamMetadata.getAsTree("com_sun_media_imageio_plugins_tiff_image_1.0");
        displayMetadata(streamTree); // contains only information about endianess of the tiff
        final TIFFImageMetadata imageMetadata = (TIFFImageMetadata) imageReader.getImageMetadata(0);
        final String[] formatNames = imageMetadata.getMetadataFormatNames();
        for (String formatName : formatNames) {
            System.out.println("formatName = " + formatName);
        }
        final Node imageTree = imageMetadata.getAsTree("com_sun_media_imageio_plugins_tiff_image_1.0");
//        displayMetadata(imageTree);
        final TIFFIFD ifd = imageMetadata.getRootIFD(); // replace TIFFDirectory with this

//        final TiffFileInfo fileInfo = new TiffFileInfo(ifd);

        // Get the SampleModel from the data and not directly from the image.
        // Otherwise it will lead to
        // java.lang.ArrayIndexOutOfBoundsException: Coordinate out of bounds!
        // Because SampleModel of image has width = height = 1 and the data
        // has a different size
        final Raster data = subsampledImage.getData(new Rectangle(100, 100, 200, 200));// for super sized image: java.lang.IllegalArgumentException: width*height > Integer.MAX_VALUE!
//        final WritableRaster data = subsampledImage.read(new Rectangle(100, 100, 200, 200));// for super sized image: java.lang.IllegalArgumentException: width*height > Integer.MAX_VALUE!

        final SampleModel sampleModel = data.getSampleModel();
        System.out.println("data.getSampleModel()");
        final int width = sampleModel.getWidth();
        final int height = sampleModel.getHeight();
        System.out.println("sampleModel.getWidth = " + width);
        System.out.println("sampleModel.getHeight = " + height);


        int[] intArray = new int[width * height];
        final int bandIndex = 0;
        final DataBuffer dataBuffer = data.getDataBuffer();
        sampleModel.getSamples(0, 0, width, height, bandIndex, intArray, dataBuffer);
        System.out.println(intArray.length);



        // leads to IllegalArgumentException: The specified dimensional parameter is non-positive.
//        RenderedImage secondBandimage = BandSelectDescriptor.create(subsampledImage, new int[]{0}, null);
//        System.out.println("subsampledImage.getNumBands = " + secondBandimage.getSampleModel().getNumBands());
//        Raster secondRaster = secondBandimage.getData();
//        System.out.println("raster.getWidth = " + secondRaster.getWidth());
//        System.out.println("subsampledImage.getWidth = " + secondBandimage.getWidth());

    }

    static void displayMetadata(Node root) {
        displayMetadata(root, 0);
    }

    static void indent(int level) {
        for (int i = 0; i < level; i++) {
            System.out.print("  ");
        }
    }

    static void displayMetadata(Node node, int level) {
        indent(level); // emit open tag
        System.out.print("<" + node.getNodeName());
        NamedNodeMap map = node.getAttributes();
        if (map != null) { // print attribute values
            int length = map.getLength();
            for (int i = 0; i < length; i++) {
                Node attr = map.item(i);
                System.out.print(" " + attr.getNodeName() +
                                 "=\"" + attr.getNodeValue() + "\"");
            }
        }

        Node child = node.getFirstChild();
        if (child != null) {
            System.out.println(">"); // close current tag
            while (child != null) { // emit child tags recursively
                displayMetadata(child, level + 1);
                child = child.getNextSibling();
            }
            indent(level); // emit close tag
            System.out.println("</" + node.getNodeName() + ">");
        } else {
            System.out.println("/>");
        }
    }


}
