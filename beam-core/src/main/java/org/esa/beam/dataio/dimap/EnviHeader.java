/*
 * $Id: EnviHeader.java,v 1.1.1.1 2006/09/11 08:16:44 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.dimap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.dataop.maptransf.LambertConformalConicDescriptor;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.MapProjection;
import org.esa.beam.framework.dataop.maptransf.MapTransform;
import org.esa.beam.framework.dataop.maptransf.TransverseMercatorDescriptor;
import org.esa.beam.framework.dataop.maptransf.UTMProjection;

/**
 * This utility class is used to write ENVI header files.
 *
 * @author Tom Block
 * @version $Revision$ $Date$
 */
class EnviHeader {

    /**
     * The extension used for ENVI header files.
     */
    public static final String FILE_EXTENSION = ".hdr";

    private static final String _enviHeaderTag = "ENVI";
    private static final String _enviSamplesTag = "samples";
    private static final String _enviLinesTag = "lines";
    private static final String _enviBandsTag = "bands";
    private static final String _enviOffsetTag = "header offset";
    private static final String _enviFileTypeTag = "file type";
    private static final String _enviDataTypeTag = "data type";
    private static final String _enviInterleaveTag = "interleave";
    private static final String _enviByteOrderTag = "byte order";
    private static final String _enviMapInfo = "map info";
    private static final String _enviProjectionInfo = "projection info";

    private static final String _enviStandardType = "ENVI Standard";
    private static final String _enviBSQType = "bsq";

    private static final int _enviTypeByte = 1;
    private static final int _enviTypeShort = 2;
    private static final int _enviTypeUShort = 12;
    private static final int _enviTypeInt = 3;
    private static final int _enviTypeFloat = 4;
    private static final int _enviTypeDouble = 5;

    // byte ordering - in Java always high byte first
    private static final int _enviMSFOrder = 1;

    /**
     * Writes the header for the <code>RasterDataNode</code> to the <code>File</code>.
     *
     * @param headerFile     - the <code>File</code> destination
     * @param rasterDataNode the <code>RasterDataNode</code>
     * @param width          the nodes's width in pixels
     * @param height         the nodes's height in pixels
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    public static void createPhysicalFile(File headerFile,
                                          RasterDataNode rasterDataNode,
                                          int width,
                                          int height) throws IOException {
        FileWriter fileWriter = new FileWriter(headerFile);
        PrintWriter out = new PrintWriter(fileWriter);
        writeFileMagic(out);
        writedescription(out, rasterDataNode);
        writeBandSize(out, width, height);
        writeNumBands(out);
        writeHeaderOffset(out);
        writeFileType(out);
        writeDataType(out, rasterDataNode.getDataType());
        writeInterleave(out);
        writeByteOrder(out);
        writeMapProjectionInfo(out, rasterDataNode);
        writeWavelength(out, rasterDataNode);
        writeScalingFactor(out, rasterDataNode);
        writeScalingOffset(out, rasterDataNode);
        out.close();
    }

    private static void writedescription(PrintWriter out, RasterDataNode rasterDataNode) {
        assert rasterDataNode != null;
        String description = rasterDataNode.getDescription();
        String unit = rasterDataNode.getUnit();
        if (unit == null || unit.trim().length() == 0) {
            unit = "1";
        }
        if (rasterDataNode.isLog10Scaled()) {
            unit = "log(" + unit + ")";
        }
        unit = " - Unit: " + unit;
        String wavelength = "";
//        String bandwidth = "";
        if (rasterDataNode instanceof Band) {
            Band band = (Band) rasterDataNode;
            if (band.getSpectralWavelength() != 0.0) {
                wavelength = " - Wavelength: " + band.getSpectralWavelength() + "nm";
//                bandwidth = " - Bandwidth: " + band.getSpectralBandwidth() + "nm";
            }
        }
        if (description == null || description.trim().length() == 0) {
            description = "none description";
        }
        out.println("description = {"
                    + description
                    + unit
                    + wavelength
//                    + bandwidth
                    + "}");
    }

    /**
     * Writes the Envi header "magic" to the out stream
     *
     * @param out - the tream to write to
     */
    private static void writeFileMagic(PrintWriter out) {
        out.println(_enviHeaderTag);
    }


    /**
     * Writes the <code>Band</code> sizes to the out stream
     *
     * @param width  - the band's width to be written
     * @param height - the band's height to be written
     * @param out    - the stream to write to
     */
    private static void writeBandSize(PrintWriter out, int width, int height) {
        out.print(_enviSamplesTag);
        out.print(" = ");
        out.println(width);

        out.print(_enviLinesTag);
        out.print(" = ");
        out.println(height);
    }

    /**
     * Writes the number of bands to the out stream. Currently we only support ONE band.
     *
     * @param out - the stream to write to
     */
    private static void writeNumBands(PrintWriter out) {
        out.print(_enviBandsTag);
        out.print(" = ");
        out.println(1);
    }

    /**
     * Writes the header offset to the out stream. Currently we have NO offset.
     *
     * @param out - the stream to write to
     */
    private static void writeHeaderOffset(PrintWriter out) {
        out.print(_enviOffsetTag);
        out.print(" = ");
        out.println(0);
    }

    /**
     * Writes the file type to the out stream. Currently only writes "ENVI Standard".
     *
     * @param out - the stream to write to
     */
    private static void writeFileType(PrintWriter out) {
        out.print(_enviFileTypeTag);
        out.print(" = ");
        out.println(_enviStandardType);
    }

    /**
     * Writes the datatype of the <code>Band</code> to the out stream
     *
     * @param dataType one of <code>ProductData.TYPE_<i>X</i></code>
     * @param out      the stream to write to
     *
     * @throws java.lang.IllegalArgumentException
     *
     */
    private static void writeDataType(PrintWriter out, int dataType) {
        int enviType;
        switch (dataType) {
        case ProductData.TYPE_INT8:
            enviType = _enviTypeByte;
            break;

        case ProductData.TYPE_UINT8:
            enviType = _enviTypeByte;
            break;

        case ProductData.TYPE_INT16:
            enviType = _enviTypeShort;
            break;

        case ProductData.TYPE_UINT16:
            enviType = _enviTypeUShort;
            break;

        case ProductData.TYPE_INT32:
            enviType = _enviTypeInt;
            break;

        case ProductData.TYPE_UINT32:
            enviType = _enviTypeInt;
            break;

        case ProductData.TYPE_FLOAT32:
            enviType = _enviTypeFloat;
            break;

        case ProductData.TYPE_FLOAT64:
            enviType = _enviTypeDouble;
            break;

        default:
            throw new IllegalArgumentException("invalid data type ID: " + dataType);
        }

        out.print(_enviDataTypeTag);
        out.print(" = ");
        out.println(enviType);
    }

    /**
     * Writes the interleave type to the out stream. Currently only band sequential.
     *
     * @param out the stream to write to
     */
    private static void writeInterleave(PrintWriter out) {
        out.print(_enviInterleaveTag);
        out.print(" = ");
        out.println(_enviBSQType);
    }

    /**
     * Writes the systems byte order to the out stream. In java only high byte first.
     *
     * @param out the stream to write to
     */
    private static void writeByteOrder(PrintWriter out) {
        out.print(_enviByteOrderTag);
        out.print(" = ");
        out.println(_enviMSFOrder);
    }

    /**
     * Writes the systems byte order to the out stream. In java only high byte first.
     *
     * @param out            the stream to write to
     * @param rasterDataNode
     */
    private static void writeMapProjectionInfo(PrintWriter out, RasterDataNode rasterDataNode) {
        Product product = rasterDataNode.getProduct();
        if (product == null) {
            return;
        }
        MapGeoCoding mapGeoCoding = null;
        if (product.getGeoCoding() instanceof MapGeoCoding) {
            mapGeoCoding = (MapGeoCoding) product.getGeoCoding();
        }
        if (mapGeoCoding == null) {
            return;
        }
        MapInfo info = mapGeoCoding.getMapInfo();
        if (info == null) {
            return;
        }
        final MapProjection mapProjection = info.getMapProjection();

        final String mapProjectionName;
        if (mapProjection instanceof UTMProjection) {
            mapProjectionName = "UTM";
        } else if (mapProjection.isPreDefined()) {
            mapProjectionName = mapProjection.getName();
        } else {
            mapProjectionName = "User-Defined " + mapProjection.getName();
        }

        final String mapUnits;
        if ("meter".equals(mapProjection.getMapUnit())) {
            mapUnits = "Meters";
        } else if ("degree".equals(mapProjection.getMapUnit())) {
            mapUnits = "Degrees";
        } else {
            mapUnits = mapProjection.getMapUnit();
        }

        out.print(_enviMapInfo);
        out.print(" = {");
        out.print(mapProjectionName);
        out.print(",");
        out.print(info.getPixelX() + 1.0f);
        out.print(",");
        out.print(info.getPixelY() + 1.0f);
        out.print(",");
        out.print(info.getEasting());
        out.print(",");
        out.print(info.getNorthing());
        out.print(",");
        out.print(info.getPixelSizeX());
        out.print(",");
        out.print(info.getPixelSizeY());
        out.print(",");
        if (mapProjection instanceof UTMProjection) {
            UTMProjection utmProjection = (UTMProjection) mapProjection;
            out.print(utmProjection.getZone());
            out.print(",");
            out.print(utmProjection.isNorth() ? "North" : "South");
            out.print(",");
        }
        out.print(info.getDatum().getName());
        out.print(",");
        out.print("units=" + mapUnits);
        out.print("}");
        out.println();

        if (!mapProjection.isPreDefined()) {
            final MapTransform mapTransform = mapProjection.getMapTransform();
            final double[] parameterValues = mapTransform.getParameterValues();
            final String transformName = mapTransform.getDescriptor().getName();
            out.print(_enviProjectionInfo);
            out.print(" = {");
            if (transformName.equals(TransverseMercatorDescriptor.NAME)) {
                out.print(3);
                out.print(",");
                out.print(parameterValues[0]); // semi_major (meters)
                out.print(",");
                out.print(parameterValues[1]); // semi_minor (meters)
                out.print(",");
                out.print(parameterValues[2]); // latitude_of_origin (degree)
                out.print(",");
                out.print(parameterValues[3]); // central_meridian (degree)
                out.print(",");
                out.print(parameterValues[5]); // false_easting (meters)
                out.print(",");
                out.print(parameterValues[6]); // false_northing (meters)
                out.print(",");
                out.print(parameterValues[4]); //  scaling_factor (no unit)
                out.print(",");
            } else if (transformName.equals(LambertConformalConicDescriptor.NAME)) {
                out.print(4);
                out.print(",");
                out.print(parameterValues[0]); // semi_major (meters)
                out.print(",");
                out.print(parameterValues[1]); // semi_minor (meters)
                out.print(",");
                out.print(parameterValues[2]); // latitude_of_origin (degree)
                out.print(",");
                out.print(parameterValues[3]); // central_meridian (degree)
                out.print(",");
                out.print(0.0);  // false_easting (meters)
                out.print(",");
                out.print(0.0);  // false_northing (meters)
                out.print(",");
                out.print(parameterValues[4]); // latitude_of_intersection_1 (meters)
                out.print(",");
                out.print(parameterValues[5]); // latitude_of_intersection_2 (meters)
                out.print(",");
            }
            out.print(mapProjectionName);
            out.print(",");
            out.print("units=" + mapUnits);
            out.print("}");
            out.println();
        }
    }

    /**
     * Writes the wavelength value to the out stream if the given rasterDataNode is an instance of <code>BAND</code>
     *
     * @param out            - the tream to write to
     * @param rasterDataNode the <code>RasterDataNode</code>
     */
    private static void writeWavelength(PrintWriter out, RasterDataNode rasterDataNode) {
        if (rasterDataNode instanceof Band) {
            final Band band = (Band) rasterDataNode;
            final float spectralWavelength = band.getSpectralWavelength();
            if (spectralWavelength != 0) {
                out.println("wavelength = {" + spectralWavelength + "}");
            }
        }
    }

    /**
     * Writes the scaling factor value to the out stream.
     *
     * @param out            - the tream to write to
     * @param rasterDataNode the <code>RasterDataNode</code>
     */
    private static void writeScalingFactor(PrintWriter out, RasterDataNode rasterDataNode) {
        out.println("data gain values = {" + rasterDataNode.getScalingFactor() + "}");
    }

    /**
     * Writes the scaling offset value to the out stream.
     *
     * @param out            - the tream to write to
     * @param rasterDataNode the <code>RasterDataNode</code>
     */
    private static void writeScalingOffset(PrintWriter out, RasterDataNode rasterDataNode) {
        out.println("data offset values = {" + rasterDataNode.getScalingOffset() + "}");
    }

    /**
     * Private constructor.
     */
    private EnviHeader() {
    }
}
