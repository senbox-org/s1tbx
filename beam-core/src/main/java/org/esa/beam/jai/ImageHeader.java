package org.esa.beam.jai;

import org.esa.beam.util.ImageUtils;

import javax.media.jai.ImageLayout;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.FileLoadDescriptor;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

public class ImageHeader {
    private final ImageLayout imageLayout;
    private final String tileFormat;

    public ImageHeader(RenderedImage image, String tileFormat) {
        this(new ImageLayout(image.getMinX(), image.getMinY(),
                             image.getWidth(), image.getHeight(),
                             image.getTileGridXOffset(),
                             image.getTileGridYOffset(),
                             image.getTileWidth(), image.getTileHeight(),
                             image.getSampleModel(), image.getColorModel()), tileFormat);
    }

    public ImageHeader(ImageLayout imageLayout, String tileFormat) {
        this.imageLayout = imageLayout;
        this.tileFormat = tileFormat;
    }

    public static ImageHeader load(File imageDir, Properties defaultImageProperties) throws IOException {
        final FileReader reader = new FileReader(new File(imageDir, "image.properties"));
        try {
            return load(reader, defaultImageProperties, imageDir);
        } finally {
            reader.close();
        }
    }

    public static ImageHeader load(Reader reader, Properties defaultImageProperties, File imageDir) throws IOException {
        Properties imageProperties = new Properties(defaultImageProperties);
        imageProperties.load(reader);

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
            RenderedOp tile00 = FileLoadDescriptor.create(new File(imageDir, "0-0." + tileFormat).getPath(), null, true, null);
            sampleModel = tile00.getSampleModel().createCompatibleSampleModel(width, height);
            colorModel = tile00.getColorModel();
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

    public void store(Writer writer, Properties defaultProperties) throws IOException {
        final Properties properties = getAsProperties(defaultProperties);
        properties.store(writer, "BEAM tiled image header");
    }

    public Properties getAsProperties(Properties defaultProperties) {
        final Properties properties = new Properties(defaultProperties);
        properties.setProperty("dataType", imageLayout.getSampleModel(null).getDataType() + "");
        properties.setProperty("minX", imageLayout.getMinX(null) + "");
        properties.setProperty("minY", imageLayout.getMinY(null) + "");
        properties.setProperty("width", imageLayout.getWidth(null) + "");
        properties.setProperty("height", imageLayout.getHeight(null) + "");
        properties.setProperty("tileGridXOffset", imageLayout.getTileGridXOffset(null) + "");
        properties.setProperty("tileGridYOffset", imageLayout.getTileGridYOffset(null) + "");
        properties.setProperty("tileWidth", imageLayout.getTileWidth(null) + "");
        properties.setProperty("tileHeight", imageLayout.getTileHeight(null) + "");
        properties.setProperty("tileFormat", tileFormat);
        return properties;
    }
}
