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

package org.esa.beam.jai;

import org.esa.beam.util.ImageUtils;

import javax.media.jai.ImageLayout;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.FileLoadDescriptor;
import java.awt.image.ColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
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
        return load(imageProperties, imageDir);
    }

    public static ImageHeader load(Properties imageProperties, File imageDir) throws IOException {
        int dataType = Integer.parseInt(imageProperties.getProperty("dataType"));
        int minX = Integer.parseInt(imageProperties.getProperty("minX", "0"));
        int minY = Integer.parseInt(imageProperties.getProperty("minY", "0"));
        int width = Integer.parseInt(imageProperties.getProperty("width"));
        int height = Integer.parseInt(imageProperties.getProperty("height"));
        int tileGridXOffset = Integer.parseInt(imageProperties.getProperty("tileGridXOffset", "0"));
        int tileGridYOffset = Integer.parseInt(imageProperties.getProperty("tileGridYOffset", "0"));
        int tileWidth = Integer.parseInt(imageProperties.getProperty("tileWidth"));
        int tileHeight = Integer.parseInt(imageProperties.getProperty("tileHeight"));
        int numberOfBits = Integer.parseInt(imageProperties.getProperty("numberOfBits", "0"));
        String tileFormat = imageProperties.getProperty("tileFormat", "raw.zip");
        SampleModel sampleModel;
        ColorModel colorModel;
        if (tileFormat.startsWith("raw")) {
            if (numberOfBits == 1 || numberOfBits == 2 || numberOfBits == 4) {
                sampleModel = new MultiPixelPackedSampleModel(dataType, tileWidth, tileHeight, numberOfBits);
            } else {
                sampleModel = ImageUtils.createSingleBandedSampleModel(dataType, tileWidth, tileHeight);
            }
            colorModel = null;
        } else {
            RenderedOp tile00 = FileLoadDescriptor.create(new File(imageDir, "0-0." + tileFormat).getPath(), null, true,
                                                          null);
            sampleModel = tile00.getSampleModel().createCompatibleSampleModel(tileWidth, tileHeight);
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
