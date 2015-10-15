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
package org.esa.snap.core.util.geotiff;

import org.jdom.Element;
import org.jdom.input.DOMBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class IIOUtils {

    /**
     * Returns a <code>BufferedImage</code> as the result of decoding a supplied <code>File</code> with an
     * <code>ImageReader</code> chosen automatically from among those currently registered. The <code>File</code> is
     * wrapped in an <code>ImageInputStream</code>.  If no registered <code>ImageReader</code> claims to be able to
     * readImage the resulting stream, <code>null</code> is returned.
     * <p> The current cache settings from <code>getUseCache</code>and <code>getCacheDirectory</code> will be used to
     * control caching in the <code>ImageInputStream</code> that is created.
     * <p> Note that there is no <code>readImage</code> method that takes a filename as a <code>String</code>; use this
     * method instead after creating a <code>File</code> from the filename.
     * <p> This methods does not attempt to locate <code>ImageReader</code>s that can readImage directly from a
     * <code>File</code>; that may be accomplished using <code>IIORegistry</code> and <code>ImageReaderSpi</code>.
     *
     * @param input a <code>File</code> to readImage from.
     *
     * @return a <code>BufferedImage</code> containing the decoded contents of the input, or <code>null</code>.
     *
     * @throws IllegalArgumentException if <code>input</code> is <code>null</code>.
     * @throws java.io.IOException      if an error occurs during reading.
     */
    public static IIOImage readImage(File input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("input == null!");
        }
        if (!input.canRead()) {
            throw new IIOException("Can't readImage input file!");
        }

        ImageInputStream stream = ImageIO.createImageInputStream(input);
        if (stream == null) {
            throw new IIOException("Can't create an ImageInputStream!");
        }
        return readImage(stream);
    }

    /**
     * Returns a <code>BufferedImage</code> as the result of decoding a supplied <code>ImageInputStream</code> with an
     * <code>ImageReader</code> chosen automatically from among those currently registered.  If no registered
     * <code>ImageReader</code> claims to be able to readImage the stream, <code>null</code> is returned.
     *
     * @param stream an <code>ImageInputStream</code> to readImage from.
     *
     * @return a <code>BufferedImage</code> containing the decoded contents of the input, or <code>null</code>.
     *
     * @throws IllegalArgumentException if <code>stream</code> is <code>null</code>.
     * @throws IOException              if an error occurs during reading.
     */
    public static IIOImage readImage(ImageInputStream stream)
            throws IOException {
        if (stream == null) {
            throw new IllegalArgumentException("stream == null!");
        }

        Iterator iter = ImageIO.getImageReaders(stream);
        if (!iter.hasNext()) {
            return null;
        }

        ImageReader reader = (ImageReader) iter.next();
        ImageReadParam param = reader.getDefaultReadParam();
        reader.setInput(stream, true, true);
        IIOImage iioImage = reader.readAll(0, param);
        stream.close();
        reader.dispose();
        return iioImage;
    }


    /**
     * Writes an image using an arbitrary <code>ImageWriter</code> that supports the given format to a
     * <code>File</code>.  If there is already a <code>File</code> present, its contents are discarded.
     *
     * @param iioImage   the image data to be written.
     * @param formatName a <code>String</code> containg the informal name of the format.
     * @param output     a <code>File</code> to be written to.
     *
     * @return <code>false</code> if no appropriate writer is found.
     *
     * @throws IllegalArgumentException if any parameter is <code>null</code>.
     * @throws IOException              if an error occurs during writing.
     */
    public static boolean writeImage(IIOImage iioImage,
                                     String formatName,
                                     File output) throws IOException {
        if (output == null) {
            throw new IllegalArgumentException("output == null!");
        }
        ImageOutputStream stream = null;
        try {
            output.delete();
            stream = ImageIO.createImageOutputStream(output);
        } catch (IOException e) {
            throw new IIOException("Can't create output stream!", e);
        }

        boolean val = writeImage(iioImage, formatName, stream);
        stream.close();
        return val;
    }


    /**
     * Writes an image using the an arbitrary <code>ImageWriter</code> that supports the given format to an
     * <code>ImageOutputStream</code>.  The image is written to the <code>ImageOutputStream</code> starting at the
     * current stream pointer, overwriting existing stream data from that point forward, if present.
     *
     * @param iioImage   the image data to be written.
     * @param formatName a <code>String</code> containg the informal name of the format.
     * @param output     an <code>ImageOutputStream</code> to be written to.
     *
     * @return <code>false</code> if no appropriate writer is found.
     *
     * @throws IllegalArgumentException if any parameter is <code>null</code>.
     * @throws IOException              if an error occurs during writing.
     */
    public static boolean writeImage(IIOImage iioImage,
                                     String formatName,
                                     ImageOutputStream output) throws IOException {
        if (iioImage == null) {
            throw new IllegalArgumentException("iioImage == null!");
        }
        if (formatName == null) {
            throw new IllegalArgumentException("formatName == null!");
        }
        if (output == null) {
            throw new IllegalArgumentException("output == null!");
        }

        ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(iioImage.getRenderedImage());
        ImageWriter writer = getImageWriter(type, formatName);
        if (writer == null) {
            return false;
        }

        writer.setOutput(output);
        writer.write(iioImage);
        output.flush();
        writer.dispose();

        return true;
    }

    /**
     * Gets an image writer suitable to be used for the given image type and image format.
     *
     * @param imageType       the type of the image to be written later
     * @param imageFormatName the image format name, e.g. "TIFF"
     *
     * @return a suitable image writer, or <code>null</code> if no writer is found
     */
    public static ImageWriter getImageWriter(ImageTypeSpecifier imageType, String imageFormatName) {
        return getImageWriter(imageType, imageFormatName, null);

    }

    /**
     * Gets an image writer suitable to be used for the given image type, image format and metadata format.
     *
     * @param imageType          the type of the image to be written later
     * @param imageFormatName    the image format name, e.g. "TIFF"
     * @param metadataFormatName the metadata format name, e.g. "com_sun_media_imageio_plugins_tiff_image_1.0", or
     *                           <code>null</code>
     *
     * @return a suitable image writer, or <code>null</code> if no writer is found
     */
    public static ImageWriter getImageWriter(ImageTypeSpecifier imageType,
                                             String imageFormatName,
                                             String metadataFormatName) {

        Iterator writers = ImageIO.getImageWriters(imageType, imageFormatName);
        while (writers.hasNext()) {
            final ImageWriter writer = (ImageWriter) writers.next();
            if (metadataFormatName == null) {
                return writer;
            }
            final String nativeImageMetadataFormatName = writer.getOriginatingProvider().getNativeImageMetadataFormatName();
            if (metadataFormatName.equals(nativeImageMetadataFormatName)) {
                return writer;
            }
        }

        writers = ImageIO.getImageWriters(imageType, imageFormatName);
        while (writers.hasNext()) {
            final ImageWriter writer = (ImageWriter) writers.next();
            final String[] extraImageMetadataFormatNames = writer.getOriginatingProvider().getExtraImageMetadataFormatNames();
            for (int i = 0; i < extraImageMetadataFormatNames.length; i++) {
                final String extraImageMetadataFormatName = extraImageMetadataFormatNames[i];
                if (metadataFormatName.equals(extraImageMetadataFormatName)) {
                    return writer;
                }
            }
        }

        return null;
    }


    public static String getXML(final IIOMetadata metadata) {
        final String metadataFormatName = metadata.getNativeMetadataFormatName();
        return getXML((org.w3c.dom.Element) metadata.getAsTree(metadataFormatName));
    }

    private static String getXML(final org.w3c.dom.Element metadataElement) {
        return getXML(convertToJDOM(metadataElement));
    }

    private static String getXML(Element metadataElement) {
        // following lines uses the old JDOM jar
//        xmlOutputter.setIndent(true);
//        xmlOutputter.setIndent("  ");
//        xmlOutputter.setNewlines(true);
//        xmlOutputter.setExpandEmptyElements(false);
//        xmlOutputter.setOmitEncoding(true);
//        xmlOutputter.setOmitDeclaration(true);
//        xmlOutputter.setTextNormalize(false);
        final Format prettyFormat = Format.getPrettyFormat();
        prettyFormat.setExpandEmptyElements(false);
        prettyFormat.setOmitEncoding(true);
        prettyFormat.setOmitDeclaration(true);
        prettyFormat.setTextMode(Format.TextMode.PRESERVE);
        final XMLOutputter xmlOutputter = new XMLOutputter(prettyFormat);
        final String xml = xmlOutputter.outputString(metadataElement);
        return xml;
    }

    private static Element convertToJDOM(final org.w3c.dom.Element metadataElement) {
        return new DOMBuilder().build(metadataElement);
    }


    private IIOUtils() {
    }
}
