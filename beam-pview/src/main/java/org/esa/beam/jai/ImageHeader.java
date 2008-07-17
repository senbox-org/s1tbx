package org.esa.beam.jai;

import org.esa.beam.util.ImageUtils;

import javax.media.jai.ImageLayout;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.FileLoadDescriptor;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class ImageHeader {
    private ImageLayout imageLayout;
    private String tileFormat;

    public ImageHeader(ImageLayout imageLayout, String tileFormat) {
        this.imageLayout = imageLayout;
        this.tileFormat = tileFormat;
    }

    public static ImageHeader load(File imageDir, Properties defaultImageProperties) throws IOException {
        Properties imageProperties = new Properties(defaultImageProperties);
        imageProperties.load(new FileReader(new File(imageDir, "image.properties")));

        int dataType = Integer.parseInt(imageProperties.getProperty("dataType"));
        int minX = Integer.parseInt(imageProperties.getProperty("minX", "0"));
        int minY = Integer.parseInt(imageProperties.getProperty("minY", "0"));
        int width = Integer.parseInt(imageProperties.getProperty("width"));
        int height = Integer.parseInt(imageProperties.getProperty("height"));
        int tileGridXOffset = Integer.parseInt(imageProperties.getProperty("tileGridXOffset", "0"));
        int tileGridYOffset = Integer.parseInt(imageProperties.getProperty("tileGridYOffset", "0"));
        int tileWidth = Integer.parseInt(imageProperties.getProperty("tileWidth"));
        int tileHeight = Integer.parseInt(imageProperties.getProperty("tileHeight"));
        String tileFormat = imageProperties.getProperty("tileFormat", "raw.zip");
        SampleModel sampleModel;
        ColorModel colorModel;
        if (tileFormat.startsWith("raw")) {
            sampleModel = ImageUtils.createSingleBandedSampleModel(dataType, width, height);
            colorModel = null;
        } else {
            final long t0 = System.currentTimeMillis();
            RenderedOp tile00 = FileLoadDescriptor.create(new File(imageDir, "0-0." + tileFormat).getPath(), null, true, null);
            sampleModel = tile00.getSampleModel().createCompatibleSampleModel(width, height);
            colorModel = tile00.getColorModel();
            final long t1 = System.currentTimeMillis();
            System.out.println("Aquired sample model which took " + (t1 - t0) + " ms");

        }
        ImageLayout imageLayout = new ImageLayout(minX, minY,
                                                  width, height,
                                                  tileGridXOffset,
                                                  tileGridYOffset,
                                                  tileWidth, tileHeight,
                                                  sampleModel, colorModel);
        return new ImageHeader(imageLayout, tileFormat);
    }

    public ImageLayout getImageLayout() {
        return imageLayout;
    }

    public String getTileFormat() {
        return tileFormat;
    }
}
