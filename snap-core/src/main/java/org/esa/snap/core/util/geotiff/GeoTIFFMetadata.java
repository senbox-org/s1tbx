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

import com.sun.media.imageio.plugins.tiff.GeoTIFFTagSet;
import com.sun.media.imageio.plugins.tiff.TIFFTag;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;

public class GeoTIFFMetadata {

    public static final String IIO_METADATA_FORMAT_NAME = "com_sun_media_imageio_plugins_tiff_image_1.0";
    public static final String IIO_TIFF_ROOT_ELEMENT_NAME = IIO_METADATA_FORMAT_NAME;
    public static final String IIO_TIFF_IFD_ELEMENT_NAME = "TIFFIFD";
    public static final String IIO_TIFF_FIELD_ELEMENT_NAME = "TIFFField";
    public static final String IIO_TIFF_SHORT_ELEMENT_NAME = "TIFFShort";
    public static final String IIO_TIFF_SHORTS_ELEMENT_NAME = "TIFFShorts";
    public static final String IIO_TIFF_DOUBLE_ELEMENT_NAME = "TIFFDouble";
    public static final String IIO_TIFF_DOUBLES_ELEMENT_NAME = "TIFFDoubles";
    public static final String IIO_TIFF_ASCII_ELEMENT_NAME = "TIFFAscii";
    public static final String IIO_TIFF_ASCIIS_ELEMENT_NAME = "TIFFAsciis";
    public static final String IIO_TIFF_TAGSETS_ATT_NAME = "tagSets";
    public static final String IIO_TIFF_NUMBER_ATT_NAME = "number";
    public static final String IIO_TIFF_NAME_ATT_NAME = "name";
    public static final String IIO_TIFF_VALUE_ATT_NAME = "value";

    private static final int DEFAULT_GEOTIFF_VERSION = 1;
    private static final int DEFAULT_KEY_REVISION_MAJOR = 1;
    private static final int DEFAULT_KEY_REVISION_MINOR = 2;

    private static final int ARRAY_ELEM_INCREMENT = 32;
    private static final int TIFF_USHORT_MIN = 0;
    private static final int TIFF_USHORT_MAX = (1 << 16) - 1;


    private int _numModelTiePoints;
    private TiePoint[] _modelTiePoints;

    private double[] _modelPixelScale;

    private double[] _modelTransformation;

    private SortedMap<Integer, KeyEntry> _geoKeyEntries;

    private int _numGeoDoubleParams;
    private double[] _geoDoubleParams;

    private int _numGeoAsciiParams;
    private StringBuffer _geoAsciiParams;
    public static final String IIO_IMAGE_FORMAT_NAME = "TIFF";

    public GeoTIFFMetadata() {
        this(DEFAULT_GEOTIFF_VERSION,
                DEFAULT_KEY_REVISION_MAJOR,
                DEFAULT_KEY_REVISION_MINOR);
    }

    public GeoTIFFMetadata(final int geoTIFFVersion, final int keyRevisionMajor, final int keyRevisionMinor) {
        _geoKeyEntries = new TreeMap<Integer, KeyEntry>();
        _geoDoubleParams = new double[ARRAY_ELEM_INCREMENT];
        _geoAsciiParams = new StringBuffer();
        _modelTiePoints = new TiePoint[ARRAY_ELEM_INCREMENT];
        _modelPixelScale = new double[3];
        _modelTransformation = new double[16];
        setModelPixelScale(1.0, 1.0);
        addGeoKeyEntry(geoTIFFVersion, keyRevisionMajor, keyRevisionMinor, 0);
    }

    public static boolean isTiffUShort(final int value) {
        return value >= TIFF_USHORT_MIN && value <= TIFF_USHORT_MAX;
    }

    public int getGeoTIFFVersion() {
        return getGeoKeyEntryAt(0).data[0];
    }

    public void setGeoTIFFVersion(int version) {
        getGeoKeyEntryAt(0).data[0] = version;
    }

    public int getKeyRevisionMajor() {
        return getGeoKeyEntryAt(0).data[1];
    }

    public int getKeyRevisionMinor() {
        return getGeoKeyEntryAt(0).data[2];
    }

    public void setKeyRevision(int major, int minor) {
        getGeoKeyEntryAt(0).data[1] = major;
        getGeoKeyEntryAt(0).data[2] = minor;
    }

    public double getModelPixelScaleX() {
        return _modelPixelScale[0];
    }

    public double getModelPixelScaleY() {
        return _modelPixelScale[1];
    }

    public double getModelPixelScaleZ() {
        return _modelPixelScale[2];
    }

    public void setModelPixelScale(double x, double y) {
        setModelPixelScale(x, y, 0.0);
    }

    public void setModelPixelScale(double x, double y, double z) {
        _modelPixelScale[0] = x;
        _modelPixelScale[1] = y;
        _modelPixelScale[2] = z;
    }

    public double[] getModelPixelScale() {
        return _modelPixelScale.clone();
    }

    public void setModelTransformation(double[] matrix) {
        System.arraycopy(matrix, 0, _modelTransformation, 0, 16);
    }

    public double[] getModelTransformation() {
        return _modelTransformation.clone();
    }

    public int getNumModelTiePoints() {
        return _numModelTiePoints;
    }

    public TiePoint getModelTiePoint() {
        return getModelTiePointAt(0);
    }

    public TiePoint getModelTiePointAt(int index) {
        return _modelTiePoints[index];
    }

    public void setModelTiePoint(double i, double j, double x, double y) {
        setModelTiePoint(i, j, 0.0, x, y, 0.0);
    }

    public void setModelTiePoint(double i, double j, double k, double x, double y, double z) {
        if (getNumModelTiePoints() > 0) {
            getModelTiePointAt(0).set(i, j, k, x, y, z);
        } else {
            addModelTiePoint(i, j, k, x, y, z);
        }
    }

    public void addModelTiePoint(double i, double j, double x, double y) {
        addModelTiePoint(i, j, 0.0, x, y, 0.0);
    }

    public void addModelTiePoint(double i, double j, double k, double x, double y, double z) {
        final int numTiePoints = _numModelTiePoints;
        if (numTiePoints >= _modelTiePoints.length - 1) {
            final TiePoint[] tiePoints = new TiePoint[numTiePoints + ARRAY_ELEM_INCREMENT];
            System.arraycopy(_modelTiePoints, 0, tiePoints, 0, numTiePoints);
            _modelTiePoints = tiePoints;
        }
        _modelTiePoints[numTiePoints] = new TiePoint(i, j, k, x, y, z);
        _numModelTiePoints++;
    }

    public int getNumGeoKeyEntries() {
        return _geoKeyEntries.size();
    }

    public KeyEntry getGeoKeyEntryAt(int index) {
        return getGeoKeyEntries()[index];
    }

    public KeyEntry[] getGeoKeyEntries() {
        return _geoKeyEntries.values().toArray(new KeyEntry[_geoKeyEntries.size()]);
    }

    public KeyEntry getGeoKeyEntry(int keyID) {
        return _geoKeyEntries.get(keyID);
    }

    public boolean hasGeoKeyEntry(int keyID) {
        return getGeoKeyEntry(keyID) != null;
    }

    public int getGeoShortParam(int keyID) {
        final KeyEntry entry = getNonNullKeyEntry(keyID);
        final int[] data = entry.getData();
        final int tag = data[1];
        final int count = data[2]; // ignored here
        final int value = data[3];
        checkParamTag(tag, 0);
        return value;
    }

    public double[] getGeoDoubleParams() {
        return Arrays.copyOf(_geoDoubleParams, _numGeoDoubleParams);
    }

    public double getGeoDoubleParam(int keyID) {
        final KeyEntry entry = getNonNullKeyEntry(keyID);
        final int[] data = entry.getData();
        final int tag = data[1];
        final int count = data[2]; // ignored here
        final int offset = data[3];
        checkParamTag(tag, getGeoDoubleParamsTag().getNumber());
        return _geoDoubleParams[offset];
    }

    public double[] getGeoDoubleParams(int keyID) {
        return getGeoDoubleParams(keyID, null);
    }

    public double[] getGeoDoubleParams(int keyID, double[] values) {
        final KeyEntry entry = getNonNullKeyEntry(keyID);
        final int[] data = entry.getData();
        final int tag = data[1];
        final int count = data[2];
        final int offset = data[3];
        checkParamTag(tag, getGeoDoubleParamsTag().getNumber());
        if (values == null) {
            values = new double[count];
        }
        System.arraycopy(_geoDoubleParams, offset, values, 0, count);
        return values;
    }

    public String getGeoAsciiParams() {
        return _geoAsciiParams.toString();
    }

    public String getGeoAsciiParam(int keyID) {
        final KeyEntry entry = getNonNullKeyEntry(keyID);
        final int[] data = entry.getData();
        final int tag = data[1];
        final int count = data[2];
        final int offset = data[3];
        checkParamTag(tag, getGeoAsciiParamsTag().getNumber());
        return _geoAsciiParams.substring(offset, offset + count - 1);
    }

    public void addGeoShortParam(int keyID, int value) {
        addGeoKeyEntry(keyID, 0, 1, value);
    }

    public void addGeoDoubleParam(int keyID, double value) {
        addGeoDoubleParamsRef(keyID, 1);
        addDoubleParam(value);
    }

    public void addGeoDoubleParams(int keyID, double[] values) {
        addGeoDoubleParamsRef(keyID, values.length);
        for (double value : values) {
            addDoubleParam(value);
        }
    }

    public void addGeoAscii(int keyID, String value) {
        addGeoAsciiParamsRef(keyID, value.length() + 1); // +1 for the '|' character to be appended
        addAsciiParam(value);
    }

    private void addGeoKeyEntry(int keyID, int tag, int count, int offset) {
        if (!isTiffUShort(keyID)) {
            throw new IllegalArgumentException("keyID is not a TIFF USHORT");
        }
        if (!isTiffUShort(tag)) {
            throw new IllegalArgumentException("tag is not a TIFF USHORT");
        }
        if (!isTiffUShort(count)) {
            throw new IllegalArgumentException("count is not a TIFF USHORT");
        }
        if (!isTiffUShort(offset)) {
            throw new IllegalArgumentException("offset is not a TIFF USHORT");
        }
        _geoKeyEntries.put(keyID, new KeyEntry(keyID, tag, count, offset));
        getGeoKeyEntryAt(0).data[3] = _geoKeyEntries.size() - 1; // exclusive the basic GeoKeyDirectoryTag
    }

    public void assignTo(Element element, String metadataFormatName, String classNameList) {
        if (!element.getName().equals(metadataFormatName)) {
            throw new IllegalArgumentException("root not found: " + metadataFormatName);
        }
        final Element ifd1 = element.getChild(IIO_TIFF_IFD_ELEMENT_NAME);
        if (ifd1 == null) {
            throw new IllegalArgumentException("child not found: " + IIO_TIFF_IFD_ELEMENT_NAME);
        }
        final Element ifd2 = createIFD(classNameList);
        ifd1.setAttribute(IIO_TIFF_TAGSETS_ATT_NAME, ifd2.getAttributeValue(IIO_TIFF_TAGSETS_ATT_NAME));
        final Element[] childElems = (Element[]) ifd2.getChildren().toArray(new Element[0]);
        for (Element child : childElems) {
            ifd2.removeContent(child);
            ifd1.addContent(child);
        }
    }

    public Element createRootTree(String classNameList) {
        final Element rootElement = new Element(IIO_TIFF_ROOT_ELEMENT_NAME);
        rootElement.addContent(createIFD(classNameList));
        return rootElement;
    }

    public void dump() {
        final PrintWriter writer = new PrintWriter(System.out);
        try {
            dump(writer);
            writer.flush();
        } finally {
            writer.close();
        }
    }

    public void dump(PrintWriter out) {
        final String indent = "    ";

        out.println();
        out.println(getGeoKeyDirectoryTag().getName() + " = { // " + getGeoKeyDirectoryTag().getNumber());
        for (int i = 0; i < getNumGeoKeyEntries(); i++) {
            out.print(indent);
            out.print("{");
            out.print(toAlignedString(getGeoKeyEntryAt(i).data[0], 6));
            out.print(",");
            out.print(toAlignedString(getGeoKeyEntryAt(i).data[1], 6));
            out.print(",");
            out.print(toAlignedString(getGeoKeyEntryAt(i).data[2], 3));
            out.print(",");
            out.print(toAlignedString(getGeoKeyEntryAt(i).data[3], 6));
            out.print("}");
            out.print(i < getNumGeoKeyEntries() - 1 ? "," : " ");
            out.print(" // ");
            out.print(toAlignedString(i, 2));
            out.println();
        }
        out.println("};");

        out.println();
        out.println(getGeoDoubleParamsTag().getName() + " = { // " + getGeoDoubleParamsTag().getNumber());
        for (int i = 0; i < _numGeoDoubleParams; i++) {
            out.print(indent);
            out.print(toAlignedString(_geoDoubleParams[i], 12));
            out.print(i < _numGeoDoubleParams - 1 ? "," : " ");
            out.print(" // ");
            out.print(toAlignedString(i, 2));
            out.println();
        }
        out.println("};");
        out.println();

        out.println(getGeoAsciiParamsTag().getName() + " = // " + getGeoAsciiParamsTag().getNumber());
        out.print(indent);
        out.print("\"");
        out.print(_geoAsciiParams);
        out.print("\"");
        out.println("};");
        out.println();
    }

    public String getAsXML() {
        // following lines uses the old JDOM jar
//        xmlOutputter.setIndent(true);
//        xmlOutputter.setIndent("  ");
//        xmlOutputter.setNewlines(true);
//        xmlOutputter.setExpandEmptyElements(false);
//        xmlOutputter.setOmitEncoding(true);
//        xmlOutputter.setOmitDeclaration(true);
//        xmlOutputter.setTextNormalize(true);
        final Format prettyFormat = Format.getPrettyFormat();
        prettyFormat.setExpandEmptyElements(false);
        prettyFormat.setOmitEncoding(true);
        prettyFormat.setOmitDeclaration(true);
        prettyFormat.setTextMode(Format.TextMode.NORMALIZE);
        final XMLOutputter xmlOutputter = new XMLOutputter(prettyFormat);
        return xmlOutputter.outputString(createRootTree("class name list template"));
    }

    protected static TIFFTag getGeoKeyDirectoryTag() {
        return GeoTIFFTagSet.getInstance().getTag(GeoTIFFTagSet.TAG_GEO_KEY_DIRECTORY);
    }

    protected static TIFFTag getGeoDoubleParamsTag() {
        return GeoTIFFTagSet.getInstance().getTag(GeoTIFFTagSet.TAG_GEO_DOUBLE_PARAMS);
    }

    protected static TIFFTag getGeoAsciiParamsTag() {
        return GeoTIFFTagSet.getInstance().getTag(GeoTIFFTagSet.TAG_GEO_ASCII_PARAMS);
    }

    protected static TIFFTag getModelPixelScaleTag() {
        return GeoTIFFTagSet.getInstance().getTag(GeoTIFFTagSet.TAG_MODEL_PIXEL_SCALE);
    }

    protected static TIFFTag getModelTiePointTag() {
        return GeoTIFFTagSet.getInstance().getTag(GeoTIFFTagSet.TAG_MODEL_TIE_POINT);
    }

    protected static TIFFTag getModelTransformationTag() {
        return GeoTIFFTagSet.getInstance().getTag(GeoTIFFTagSet.TAG_MODEL_TRANSFORMATION);
    }

    /////////////////////////////////////////////////////////////////////////
    // Private Implementation Helpers

    private KeyEntry getNonNullKeyEntry(int keyID) {
        final KeyEntry entry = getGeoKeyEntry(keyID);
        if (entry == null) {
            throw new IllegalArgumentException("entry not found for geo key " + keyID);
        }
        return entry;
    }

    private void checkParamTag(final int tag, final int expectedTag) {
        if (tag != expectedTag) {
            if (expectedTag == 0) {
                throw new IllegalArgumentException("invalid key access, not a GeoTIFF SHORT parameter");
            } else if (expectedTag == getGeoDoubleParamsTag().getNumber()) {
                throw new IllegalArgumentException("invalid key access, not a GeoTIFF DOUBLE parameter");
            } else if (expectedTag == getGeoAsciiParamsTag().getNumber()) {
                throw new IllegalArgumentException("invalid key access, not a GeoTIFF ASCII parameter");
            } else {
                throw new IllegalStateException();
            }
        }
    }

    private void addDoubleParam(double param) {
        final int numDoubleParams = _numGeoDoubleParams;
        if (numDoubleParams >= _geoDoubleParams.length - 1) {
            final double[] doubleParams = new double[numDoubleParams + ARRAY_ELEM_INCREMENT];
            System.arraycopy(_geoDoubleParams, 0, doubleParams, 0, numDoubleParams);
            _geoDoubleParams = doubleParams;
        }
        _geoDoubleParams[numDoubleParams] = param;
        _numGeoDoubleParams++;
    }

    private void addAsciiParam(String param) {
        _geoAsciiParams.append(param);
        _geoAsciiParams.append('|');
        _numGeoAsciiParams++;
    }

    private void addGeoDoubleParamsRef(int keyID, int count) {
        addGeoKeyEntry(keyID, getGeoDoubleParamsTag().getNumber(), count, getCurrentGeoDoublesOffset());
    }

    private void addGeoAsciiParamsRef(int keyID, int length) {
        addGeoKeyEntry(keyID, getGeoAsciiParamsTag().getNumber(), length, getCurrentGeoAsciisOffset());
    }

    private int getCurrentGeoDoublesOffset() {
        return _numGeoDoubleParams;
    }

    private int getCurrentGeoAsciisOffset() {
        return _geoAsciiParams.length();
    }

    private Element createIFD(String classNameList) {
        Element ifd = new Element(IIO_TIFF_IFD_ELEMENT_NAME);
        ifd.setAttribute(
                IIO_TIFF_TAGSETS_ATT_NAME,
                classNameList);
        if (isModelPixelScaleSet()) {
            ifd.addContent(createModelPixelScaleElement());
        }
        if (hasModelTiePoints()) {
            ifd.addContent(createModelTiePointsElement());
        }
        if (isModelTransformationSet()) {
            ifd.addContent(createModelTransformationElement());
        }
        if (getNumGeoKeyEntries() > 1) {
            ifd.addContent(createGeoKeyDirectoryElement());
        }
        if (_numGeoDoubleParams > 0) {
            ifd.addContent(createGeoDoubleParamsElement());
        }
        if (_numGeoAsciiParams > 0) {
            ifd.addContent(createGeoAsciiParamsElement());
        }
        return ifd;
    }

    private boolean isModelPixelScaleSet() {
        for (double scale : _modelPixelScale) {
            if (scale != 0.0 && scale != 1.0) {
                return true;
            }
        }
        return false;
    }

    private boolean hasModelTiePoints() {
        return _numModelTiePoints > 0;
    }

    private boolean isModelTransformationSet() {
        for (double trans : _modelTransformation) {
            if (trans != 0.0) {
                return true;
            }
        }
        return false;
    }

    private Element createGeoKeyDirectoryElement() {
        Element field = createFieldElement(getGeoKeyDirectoryTag());
        Element data = new Element(IIO_TIFF_SHORTS_ELEMENT_NAME);
        field.addContent(data);
        for (int i = 0; i < getNumGeoKeyEntries(); i++) {
            final int[] values = getGeoKeyEntryAt(i).data;
            for (int value : values) {
                Element keyEntry = createShortElement(value);
                data.addContent(keyEntry);
            }
        }
        return field;
    }

    private Element createGeoDoubleParamsElement() {
        Element field = createFieldElement(getGeoDoubleParamsTag());
        Element data = new Element(IIO_TIFF_DOUBLES_ELEMENT_NAME);
        field.addContent(data);
        for (int i = 0; i < _numGeoDoubleParams; i++) {
            Element param = createDoubleElement(_geoDoubleParams[i]);
            data.addContent(param);
        }
        return field;
    }

    private Element createGeoAsciiParamsElement() {
        Element field = createFieldElement(getGeoAsciiParamsTag());
        Element data = new Element(IIO_TIFF_ASCIIS_ELEMENT_NAME);
        field.addContent(data);
        data.addContent(createAsciiElement(_geoAsciiParams.toString()));
        return field;
    }

    private Element createModelPixelScaleElement() {
        Element field = createFieldElement(getModelPixelScaleTag());
        Element data = new Element(IIO_TIFF_DOUBLES_ELEMENT_NAME);
        field.addContent(data);
        addDoubleElements(data, _modelPixelScale);
        return field;
    }

    private Element createModelTransformationElement() {
        Element field = createFieldElement(getModelTransformationTag());
        Element data = new Element(IIO_TIFF_DOUBLES_ELEMENT_NAME);
        field.addContent(data);
        addDoubleElements(data, _modelTransformation);
        return field;
    }

    private Element createModelTiePointsElement() {
        Element field = createFieldElement(getModelTiePointTag());
        Element data = new Element(IIO_TIFF_DOUBLES_ELEMENT_NAME);
        field.addContent(data);
        for (int i = 0; i < _numModelTiePoints; i++) {
            addDoubleElements(data, _modelTiePoints[i].data);
        }
        return field;
    }

    private Element createFieldElement(final TIFFTag tag) {
        Element field = new Element(IIO_TIFF_FIELD_ELEMENT_NAME);
        field.setAttribute(IIO_TIFF_NUMBER_ATT_NAME, String.valueOf(tag.getNumber()));
        field.setAttribute(IIO_TIFF_NAME_ATT_NAME, tag.getName());
        return field;
    }

    private Element createShortElement(final int value) {
        Element keyEntry = new Element(IIO_TIFF_SHORT_ELEMENT_NAME);
        keyEntry.setAttribute(IIO_TIFF_VALUE_ATT_NAME, String.valueOf(value));
        return keyEntry;
    }

    private Element createDoubleElement(final double value) {
        Element param = new Element(IIO_TIFF_DOUBLE_ELEMENT_NAME);
        param.setAttribute(IIO_TIFF_VALUE_ATT_NAME, String.valueOf(value));
        return param;
    }

    private Element createAsciiElement(final String value) {
        Element param = new Element(IIO_TIFF_ASCII_ELEMENT_NAME);
        param.setAttribute(IIO_TIFF_VALUE_ATT_NAME, String.valueOf(value));
        return param;
    }

    private void addDoubleElements(Element data, final double[] values) {
        for (double value : values) {
            Element keyEntry = createDoubleElement(value);
            data.addContent(keyEntry);
        }
    }

    private static String toAlignedString(int value, int length) {
        return toAlignedString(String.valueOf(value), length, true);
    }

    private static String toAlignedString(double value, int length) {
        return toAlignedString(String.valueOf(value), length, true);
    }

    private static String toAlignedString(String value, int length, boolean right) {
        int n = length - value.length();
        if (n > 0) {
            final char[] chars = new char[n];
            for (int i = 0; i < chars.length; i++) {
                chars[i] = ' ';
            }
            final String spaces = new String(chars);
            return right ? spaces + value : value + spaces;
        }
        return value;
    }

    public static class KeyEntry {

        private int[] data;

        private KeyEntry(int keyID, int tag, int count, int offset) {
            data = new int[4];
            set(keyID, tag, count, offset);
        }

        private void set(int keyID, int tag, int count, int offset) {
            data[0] = keyID;
            data[1] = tag;
            data[2] = count;
            data[3] = offset;
        }

        public int[] getData() {
            return data;
        }
    }

    public static class TiePoint {

        private double[] data;

        private TiePoint(double i, double j, double k, double x, double y, double z) {
            data = new double[6];
            set(i, j, k, x, y, z);
        }

        private void set(double i, double j, double k, double x, double y, double z) {
            data[0] = i;
            data[1] = j;
            data[2] = k;
            data[3] = x;
            data[4] = y;
            data[5] = z;
        }

        public double[] getData() {
            return data;
        }
    }
}
