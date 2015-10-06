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

import com.sun.media.imageio.plugins.tiff.BaselineTIFFTagSet;
import com.sun.media.imageio.plugins.tiff.GeoTIFFTagSet;
import org.esa.snap.core.util.Debug;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Parent;
import org.jdom.input.DOMBuilder;
import org.jdom.output.DOMOutputter;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

public class GeoTIFF {

    /**
     * Writes an image using an arbitrary <code>ImageWriter</code> that supports the GeoTIFF format to a
     * <code>File</code>.  If there is already a <code>File</code> present, its contents are discarded.
     *
     * @param image           a <code>RenderedImage</code> to be written. name of the format.
     * @param outputFile      a <code>File</code> to be written to.
     * @param geoTIFFMetadata the GeoTIFF specific metadata
     *
     * @return <code>false</code> if no appropriate image I/O writer was found.
     *
     * @throws IllegalArgumentException if any parameter is <code>null</code>.
     * @throws java.io.IOException      if an error occurs during writing.
     */
    public static boolean writeImage(final RenderedImage image,
                                     final File outputFile,
                                     final GeoTIFFMetadata geoTIFFMetadata) throws IOException {
        if (outputFile == null) {
            throw new IllegalArgumentException("outputFile == null!");
        }

        outputFile.delete();

        final ImageOutputStream stream;
        try {
            stream = ImageIO.createImageOutputStream(outputFile);
        } catch (IIOException e) {
            throw e;
        } catch (IOException e) {
            throw new IIOException("Failed to create TIFF output stream.", e);
        }

        boolean writerFound = false;
        try {
            writerFound = writeImage(image, stream, geoTIFFMetadata);
        } finally {
            stream.close();
        }

        return writerFound;
    }


    /**
     * Writes an image using an arbitrary <code>ImageWriter</code> that supports the GeoTIFF format to an
     * <code>ImageOutputStream</code>.  The image is written to the <code>ImageOutputStream</code> starting at the
     * current stream pointer, overwriting existing stream data from that point forward, if present.
     *
     * @param image           a <code>RenderedImage</code> to be written.
     * @param outputStream    an <code>ImageOutputStream</code> to be written to.
     * @param geoTIFFMetadata the GeoTIFF specific metadata
     *
     * @return <code>false</code> if no appropriate writer is found.
     *
     * @throws IllegalArgumentException if any parameter is <code>null</code>.
     * @throws java.io.IOException      if an error occurs during writing.
     */
    public static boolean writeImage(final RenderedImage image,
                                     final ImageOutputStream outputStream,
                                     final GeoTIFFMetadata geoTIFFMetadata) throws IOException {
        if (image == null) {
            throw new IllegalArgumentException("image == null!");
        }
        if (outputStream == null) {
            throw new IllegalArgumentException("outputStream == null!");
        }

        ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(image);
        ImageWriter writer = getImageWriter(type);
        if (writer == null) {
            return false;
        }

        writer.setOutput(outputStream);
        writer.write(createIIOImage(writer, image, geoTIFFMetadata));
        outputStream.flush();
        writer.dispose();

        return true;
    }

    /**
     * Gets an image writer suitable to be used for GeoTIFF.
     *
     * @param image the image to be written later
     *
     * @return a suitable image writer, or <code>null</code> if no writer is found
     */
    public static ImageWriter getImageWriter(RenderedImage image) {
        return getImageWriter(ImageTypeSpecifier.createFromRenderedImage(image));
    }

    /**
     * Gets an image writer suitable to be used for GeoTIFF.
     *
     * @param imageType the type of the image to be written later
     *
     * @return a suitable image writer, or <code>null</code> if no writer is found
     */
    public static ImageWriter getImageWriter(ImageTypeSpecifier imageType) {
        return IIOUtils.getImageWriter(
                imageType, GeoTIFFMetadata.IIO_IMAGE_FORMAT_NAME,
                GeoTIFFMetadata.IIO_METADATA_FORMAT_NAME);
    }

    /**
     * Creates IIO image instance given image and GeoTIFF metadata.
     *
     * @param writer          the image writer, must not be null
     * @param im              the image, must not be null
     * @param geoTIFFMetadata the GeoTIFF metadata, must not be null
     *
     * @return the IIO image, never null
     *
     * @throws IIOException if the metadata cannot be created
     */
    public static IIOImage createIIOImage(ImageWriter writer, RenderedImage im, GeoTIFFMetadata geoTIFFMetadata) throws IIOException {
        final ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(im);
        return new IIOImage(im, null, createIIOMetadata(writer, type, geoTIFFMetadata));
    }

    /**
     * Creates image metadata which complies to the GeoTIFF specification for the given image writer, image type and
     * GeoTIFF metadata.
     *
     * @param writer          the image writer, must not be null
     * @param type            the image type, must not be null
     * @param geoTIFFMetadata the GeoTIFF metadata, must not be null
     * @return the image metadata, never null
     * @throws IIOException if the metadata cannot be created
     */
    public static IIOMetadata createIIOMetadata(ImageWriter writer, ImageTypeSpecifier type,
                                                GeoTIFFMetadata geoTIFFMetadata) throws IIOException {
        final String classnameList = BaselineTIFFTagSet.class.getName() + "," + GeoTIFFTagSet.class.getName();
        return createIIOMetadata(writer, type, geoTIFFMetadata, GeoTIFFMetadata.IIO_METADATA_FORMAT_NAME, classnameList);
    }

    /**
     * Creates image metadata which complies to the GeoTIFF specification for the given image writer, image type and
     * GeoTIFF metadata.
     *
     * @param writer             the image writer, must not be null
     * @param type               the image type, must not be null
     * @param geoTIFFMetadata    the GeoTIFF metadata, must not be null
     * @param metadataFormatName the name of the Metadata Format specification
     * @param classNameList      comma separated list of Metadata classes
     * @return the image metadata, never null
     * @throws IIOException if the metadata cannot be created
     */
    public static IIOMetadata createIIOMetadata(ImageWriter writer, ImageTypeSpecifier type,
                                                GeoTIFFMetadata geoTIFFMetadata, String metadataFormatName, String classNameList) throws IIOException {
        final IIOMetadata imageMetadata = writer.getDefaultImageMetadata(type, null);
        org.w3c.dom.Element w3cElement = (org.w3c.dom.Element) imageMetadata.getAsTree(metadataFormatName);
        final Element element = new DOMBuilder().build(w3cElement);

        if (Debug.isEnabled()) {
            Debug.trace("Dumping original TIFF metadata tree:\n" + toXMLString(element));
        }

        geoTIFFMetadata.assignTo(element, metadataFormatName, classNameList);

        if (Debug.isEnabled()) {
            Debug.trace("Dumping modified GeoTIFF metadata tree:\n" + toXMLString(element));
        }

        final Parent parent = element.getParent();
        parent.removeContent(element);
        final Document document = new Document(element);
        try {
            final org.w3c.dom.Document w3cDoc = new DOMOutputter().output(document);
            imageMetadata.setFromTree(metadataFormatName, w3cDoc.getDocumentElement());
        } catch (JDOMException e) {
            throw new IIOException("Failed to set GeoTIFF specific tags.", e);
        } catch (IIOInvalidTreeException e) {
            throw new IIOException("Failed to set GeoTIFF specific tags.", e);
        }

        return imageMetadata;
    }

    private static String toXMLString(Element metadataElement) {
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
        prettyFormat.setTextMode(Format.TextMode.NORMALIZE);

        final XMLOutputter xmlOutputter = new XMLOutputter(prettyFormat);
        final String xml = xmlOutputter.outputString(metadataElement);
        return xml;
    }
}
