/*
 * Copyright (C) 2019 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.io.uavsar;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.commons.io.FileImageInputStreamExtImpl;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;

import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * The product reader for UAVSAR products.
 */
public class UAVSARReader extends SARReader {

    private int rasterWidth = 0;
    private int rasterHeight = 0;
    private final static ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

    private final int _startPosImageRecords = 0;
    private int _imageHeaderLength = 0;

    private final static String mission = "UAVSAR";
    private String productTypeStr;

    private enum ProductTypes {SLC, MLC, GRD, DAT}

    private ProductTypes productType;

    private enum BandType {OneOfOne, OneOfTwo, TwoOfTwo}

    private final Map<Band, ImageInputStream> imgInStreamMap = new HashMap<>();
    private final Map<Band, BandType> bandTypeMap = new HashMap<>();

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public UAVSARReader(final ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    static File findAnnotationFile(final File uavDatafile) {

        final File parentDir = uavDatafile.getParentFile();
        final File[] listFiles = parentDir.listFiles();
        if (listFiles == null)
            return null;
        for (File f : listFiles) {
            final String ext = FileUtils.getExtension(f.toPath().getFileName().toString());
            if (ext != null && ext.equalsIgnoreCase(".ann")) {
                return f;
            }
        }
        return null;
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code> interface method. Clients implementing this
     * method can be sure that the input object and eventually the subset information has already been set.
     * <p>
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {

        final Path inputPath = getPathFromInput(getInput());
        final File inputFile = inputPath.toFile();

        final File annFile = findAnnotationFile(inputFile);
        final MetadataElement annElem = readAnnotation(annFile);

        getProductType(inputPath.getFileName().toString());
        getDimensions(annElem);

        final Product product = new Product(FileUtils.getFilenameWithoutExtension(annFile),
                productTypeStr,
                rasterWidth, rasterHeight);

        addBands(inputFile.getParentFile(), product);
        addMetaData(product, annElem);

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        ReaderUtils.addGeoCoding(product, getLatCorners(absRoot), getLonCorners(absRoot));

        product.setProductReader(this);
        product.setFileLocation(inputFile);
        setQuicklookBandName(product);
        product.setModified(false);

        return product;
    }

    private void addBands(final File folder, final Product product) throws IOException {
        final File[] listFiles = folder.listFiles();
        if (listFiles == null)
            return;

        for (File f : listFiles) {
            final String name = f.getName().toUpperCase();
            if (name.endsWith(productTypeStr)) {
                final ImageInputStream imageInputStream = FileImageInputStreamExtImpl.createInputStream(f);
                imageInputStream.setByteOrder(byteOrder);

                String bandName = "Amplitude";
                String unit = Unit.AMPLITUDE;
                BandType bandType = BandType.OneOfOne;
                String qBandName = null;
                if (productTypeStr.equals("SLC")) {
                    unit = Unit.REAL;
                    bandType = BandType.OneOfTwo;
                    if (name.contains("HH")) {
                        bandName = "i_HH";
                        qBandName = "q_HH";
                    } else if (name.contains("HV")) {
                        bandName = "i_HV";
                        qBandName = "q_HV";
                    } else if (name.contains("VV")) {
                        bandName = "i_VV";
                        qBandName = "q_VV";
                    } else if (name.contains("VH")) {
                        bandName = "i_VH";
                        qBandName = "q_VH";
                    }
                } else {
                    if (name.contains("HHHH")) {
                        bandName = "C11";
                        unit = Unit.INTENSITY;
                    } else if (name.contains("VVVV")) {
                        bandName = "C33";
                        unit = Unit.INTENSITY;
                    } else if (name.contains("HHVV")) {
                        bandName = "C13_real";
                        unit = Unit.REAL;
                        bandType = BandType.OneOfTwo;
                        qBandName = "C13_imag";
                    } else if (name.contains("HVVV")) {
                        bandName = "C23_real";
                        unit = Unit.REAL;
                        bandType = BandType.OneOfTwo;
                        qBandName = "C23_imag";
                    } else if (name.contains("HHHV")) {
                        bandName = "C12_real";
                        unit = Unit.REAL;
                        bandType = BandType.OneOfTwo;
                        qBandName = "C12_imag";
                    } else if (name.contains("HVHV")) {
                        bandName = "C22";
                        unit = Unit.INTENSITY;
                    }
                }
                final Band band = new Band(bandName, ProductData.TYPE_FLOAT32, rasterWidth, rasterHeight);
                band.setUnit(unit);
                bandTypeMap.put(band, bandType);
                imgInStreamMap.put(band, imageInputStream);
                product.addBand(band);
                if (qBandName != null) {
                    final Band qBand = new Band(qBandName, ProductData.TYPE_FLOAT32, rasterWidth, rasterHeight);
                    qBand.setUnit(Unit.IMAGINARY);
                    product.addBand(qBand);
                    imgInStreamMap.put(qBand, imageInputStream);
                    bandTypeMap.put(qBand, BandType.TwoOfTwo);
                }
            }
        }
    }

    private void getProductType(final String fileName) {
        final String ext = FileUtils.getExtension(fileName);
        if (ext != null) {
            productTypeStr = ext.substring(1).toUpperCase();
            switch (productTypeStr) {
                case "SLC":
                    productType = ProductTypes.SLC;
                    break;
                case "GRD":
                    productType = ProductTypes.GRD;
                    break;
                case "DAT":
                    productType = ProductTypes.DAT;
                    break;
                default:
                    productType = ProductTypes.MLC;
                    break;
            }
        }
    }

    private void getDimensions(final MetadataElement annElem) {
        if (productType.equals(ProductTypes.MLC)) {
            rasterHeight = Integer.parseInt(annElem.getAttributeString("mlc_pwr.set_rows"));
            rasterWidth = Integer.parseInt(annElem.getAttributeString("mlc_pwr.set_cols"));
        } else if (productType.equals(ProductTypes.SLC)) {
            String rowsStr = annElem.getAttributeString("slc_mag.set_rows", null);
            if (rowsStr == null)
                rowsStr = annElem.getAttributeString("slt_mag.set_rows");
            rasterHeight = Integer.parseInt(rowsStr);
            String colsStr = annElem.getAttributeString("slc_mag.set_cols", null);
            if (colsStr == null)
                colsStr = annElem.getAttributeString("slt_mag.set_cols");
            rasterWidth = Integer.parseInt(colsStr);
        } else if (productType.equals(ProductTypes.GRD)) {
            rasterHeight = Integer.parseInt(annElem.getAttributeString("grd_pwr.set_rows"));
            rasterWidth = Integer.parseInt(annElem.getAttributeString("grd_pwr.set_cols"));
        } else if (productType.equals(ProductTypes.DAT)) {
            rasterHeight = Integer.parseInt(annElem.getAttributeString("dat.set_rows"));
            rasterWidth = Integer.parseInt(annElem.getAttributeString("dat.set_cols"));
        }
    }

    protected static MetadataElement readAnnotation(final File annFile) throws IOException {

        final MetadataElement annElem = new MetadataElement("Annotation");

        try {
            final FileInputStream fstream = new FileInputStream(annFile);
            final DataInputStream in = new DataInputStream(fstream);
            final BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            //Read File Line By Line
            while ((strLine = br.readLine()) != null) {
                int commentIndex = strLine.indexOf(';');
                final int exclaimIndex = strLine.indexOf('!');
                if (exclaimIndex > 0 && (commentIndex == -1 || exclaimIndex < commentIndex))
                    commentIndex = exclaimIndex;
                if (strLine.isEmpty() || (commentIndex != -1 && commentIndex < 2))
                    continue;

                String line;
                String comment = "";
                String unit = "";
                String name = "";
                String value = "";
                if (commentIndex > 0) {
                    line = strLine.substring(0, commentIndex);
                    comment = strLine.substring(commentIndex + 1, strLine.length()).trim();
                } else {
                    line = strLine;
                }

                final StringTokenizer st = new StringTokenizer(line);
                boolean isValue = false;
                while (st.hasMoreTokens()) {
                    final String token = st.nextToken();
                    if (token.startsWith("(") && token.endsWith(")")) {
                        unit = token.substring(1, token.length() - 1);
                    } else if (token.equals("=")) {
                        isValue = true;
                    } else {
                        if (isValue) {
                            value += token;
                        } else {
                            name += token;
                        }
                    }
                }
                final MetadataAttribute attrib = AbstractMetadata.addAbstractedAttribute(annElem, name,
                        ProductData.TYPE_ASCII, unit, comment);
                if (!value.isEmpty())
                    attrib.getData().setElems(value);
            }
            in.close();
            return annElem;
        } catch (Exception e) {//Catch exception if any
            throw new IOException("Unable to read annotation: " + e.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    private void addMetaData(final Product product, final MetadataElement annElem) throws IOException {
        final MetadataElement root = product.getMetadataRoot();
        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, product.getName());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, product.getProductType());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line, product.getSceneRasterWidth());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines, product.getSceneRasterHeight());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION, mission);

        setAttrib(absRoot, annElem, AbstractMetadata.SPH_DESCRIPTOR, "SiteDescription");
        setAttrib(absRoot, annElem, AbstractMetadata.ACQUISITION_MODE, "AcquisitionMode");
        setAttribUTC(absRoot, annElem, AbstractMetadata.first_line_time, "DateofAcquisition", "dd-MMM-yyyyhh:mm:ss");
        setAttribUTC(absRoot, annElem, AbstractMetadata.PROC_TIME, "DateofProcessing", "dd-MMM-yyyy");
        setAttrib(absRoot, annElem, AbstractMetadata.ProcessingSystemIdentifier, "ProcessorVersionNumber");

        setAttribDbl(absRoot, annElem, AbstractMetadata.first_near_lat, "ApproximateUpperLeftLatitude");
        setAttribDbl(absRoot, annElem, AbstractMetadata.first_near_long, "ApproximateUpperLeftLongitude");
        setAttribDbl(absRoot, annElem, AbstractMetadata.first_far_lat, "ApproximateUpperRightLatitude");
        setAttribDbl(absRoot, annElem, AbstractMetadata.first_far_long, "ApproximateUpperRightLongitude");
        setAttribDbl(absRoot, annElem, AbstractMetadata.last_near_lat, "ApproximateLowerLeftLatitude");
        setAttribDbl(absRoot, annElem, AbstractMetadata.last_near_long, "ApproximateLowerLeftLongitude");
        setAttribDbl(absRoot, annElem, AbstractMetadata.last_far_lat, "ApproximateLowerRightLatitude");
        setAttribDbl(absRoot, annElem, AbstractMetadata.last_far_long, "ApproximateLowerRightLongitude");

        if (productType.equals(ProductTypes.MLC)) {
            setAttribDbl(absRoot, annElem, AbstractMetadata.range_spacing, "mlc_mag.col_mult");
            setAttribDbl(absRoot, annElem, AbstractMetadata.azimuth_spacing, "mlc_mag.row_mult");

            setAttribDbl(absRoot, annElem, AbstractMetadata.range_looks, "NumberofRangeLooksinMLC");
            setAttribDbl(absRoot, annElem, AbstractMetadata.azimuth_looks, "NumberofAzimuthLooksinMLC");
        } else if (productType.equals(ProductTypes.GRD)) {
            setAttribDbl(absRoot, annElem, AbstractMetadata.lat_pixel_res, "grd_mag.row_mult");
            setAttribDbl(absRoot, annElem, AbstractMetadata.lon_pixel_res, "grd_mag.col_mult");

            setAttribDbl(absRoot, annElem, AbstractMetadata.range_looks, "NumberofRangeLooksinGRD");
            setAttribDbl(absRoot, annElem, AbstractMetadata.azimuth_looks, "NumberofAzimuthLooksinMLC");
        } else {
            Double col = setAttribDbl(absRoot, annElem, AbstractMetadata.range_spacing, "slc_mag.col_mult");
            if (col == null)
                setAttribDbl(absRoot, annElem, AbstractMetadata.range_spacing, "slt_mag.col_mult");
            Double row = setAttribDbl(absRoot, annElem, AbstractMetadata.azimuth_spacing, "slc_mag.row_mult");
            if (row == null)
                setAttribDbl(absRoot, annElem, AbstractMetadata.azimuth_spacing, "slt_mag.row_mult");

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_looks, 1);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks, 1);
        }

        setAttribDbl(absRoot, annElem, AbstractMetadata.avg_scene_height, "AverageTerrainHeight");

        root.addElement(annElem);
    }

    private static void setAttrib(MetadataElement dstElem, MetadataElement srcElem,
                                  final String dstTag, final String srcTag) {
        final String val = srcElem.getAttributeString(srcTag, "");
        if (!val.isEmpty())
            AbstractMetadata.setAttribute(dstElem, dstTag, val);
    }

    private static Double setAttribDbl(MetadataElement dstElem, MetadataElement srcElem,
                                       final String dstTag, final String srcTag) {
        String valStr = srcElem.getAttributeString(srcTag, null);
        if (valStr == null)
            return null;
        final Double val = Double.parseDouble(valStr);
        if (val != null)
            AbstractMetadata.setAttribute(dstElem, dstTag, val);
        return val;
    }

    private static void setAttribUTC(MetadataElement dstElem, MetadataElement srcElem,
                                     final String dstTag, final String srcTag, final String format) {
        String val = srcElem.getAttributeString(srcTag, "");
        if (val.contains("UTC")) {
            val = val.substring(0, val.indexOf("UTC")).trim();
        }
        if (!val.isEmpty()) {
            final DateFormat dateFormat = ProductData.UTC.createDateFormat(format);
            final ProductData.UTC utc = AbstractMetadata.parseUTC(val, dateFormat);
            AbstractMetadata.setAttribute(dstElem, dstTag, utc);
        }
    }

    private static double[] getLatCorners(final MetadataElement absRoot) {

        final double latUL = absRoot.getAttributeDouble(AbstractMetadata.first_near_lat);
        final double latUR = absRoot.getAttributeDouble(AbstractMetadata.first_far_lat);
        final double latLL = absRoot.getAttributeDouble(AbstractMetadata.last_near_lat);
        final double latLR = absRoot.getAttributeDouble(AbstractMetadata.last_far_lat);
        return new double[]{latUL, latUR, latLL, latLR};
    }

    private static double[] getLonCorners(final MetadataElement absRoot) {

        final double lonUL = absRoot.getAttributeDouble(AbstractMetadata.first_near_long);
        final double lonUR = absRoot.getAttributeDouble(AbstractMetadata.first_far_long);
        final double lonLL = absRoot.getAttributeDouble(AbstractMetadata.last_near_long);
        final double lonLR = absRoot.getAttributeDouble(AbstractMetadata.last_far_long);
        return new double[]{lonUL, lonUR, lonLL, lonLR};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        final ImageInputStream imgInStream = imgInStreamMap.get(destBand);
        final BandType bandType = bandTypeMap.get(destBand);

        if (bandType == BandType.OneOfOne) {
            readBandRasterData(sourceOffsetX, sourceOffsetY,
                    sourceWidth, sourceHeight,
                    sourceStepX, sourceStepY,
                    _startPosImageRecords + _imageHeaderLength, imgInStream,
                    destBand, destWidth, destBuffer);
        } else {
            readBandRasterDataComplex(sourceOffsetX, sourceOffsetY,
                    sourceWidth, sourceHeight,
                    sourceStepX, sourceStepY,
                    _startPosImageRecords + _imageHeaderLength, imgInStream,
                    destBand, destWidth, destBuffer, bandType == BandType.OneOfTwo);
        }
    }

    private static synchronized void readBandRasterData(final int sourceMinX, final int sourceMinY,
                                                        final int sourceWidth, final int sourceHeight,
                                                        final int sourceStepX, final int sourceStepY,
                                                        final long bandOffset, final ImageInputStream imageInputStream,
                                                        final Band destBand, final int destWidth,
                                                        final ProductData destBuffer) throws IOException {

        final int sourceMaxX = sourceMinX + sourceWidth - 1;
        final int sourceMaxY = sourceMinY + sourceHeight - 1;
        final int sourceRasterWidth = destBand.getRasterWidth();

        final int elemSize = destBuffer.getElemSize();
        int destPos = 0;

        for (int sourceY = sourceMinY; sourceY <= sourceMaxY; sourceY += sourceStepY) {
            final long sourcePosY = sourceY * sourceRasterWidth;
            if (sourceStepX == 1) {
                imageInputStream.seek(bandOffset + elemSize * (sourcePosY + sourceMinX));
                destBuffer.readFrom(destPos, destWidth, imageInputStream);
                destPos += destWidth;
            } else {
                for (int sourceX = sourceMinX; sourceX <= sourceMaxX; sourceX += sourceStepX) {
                    imageInputStream.seek(bandOffset + elemSize * (sourcePosY + sourceX));
                    destBuffer.readFrom(destPos, 1, imageInputStream);
                    destPos++;
                }
            }
        }
    }

    private static synchronized void readBandRasterDataComplex(final int sourceMinX, final int sourceMinY,
                                                               final int sourceWidth, final int sourceHeight,
                                                               final int sourceStepX, final int sourceStepY,
                                                               final long bandOffset, final ImageInputStream imageInputStream,
                                                               final Band destBand, final int destWidth, final ProductData destBuffer,
                                                               final boolean oneOfTwo) throws IOException {

        final int sourceMaxX = sourceMinX + sourceWidth - 1;
        final int sourceMaxY = sourceMinY + sourceHeight - 1;
        final int sourceRasterWidth = destBand.getRasterWidth();

        final int elemSize = destBuffer.getElemSize() * 2;
        int destPos = 0;

        final ProductData tmpBuffer = ProductData.createInstance(new float[destBuffer.getNumElems() * 2]);
        final int tmpWidth = destWidth * 2;

        for (int sourceY = sourceMinY; sourceY <= sourceMaxY; sourceY += sourceStepY) {
            final long sourcePosY = sourceY * sourceRasterWidth;
            if (sourceStepX == 1) {
                imageInputStream.seek(bandOffset + elemSize * (sourcePosY + sourceMinX));
                tmpBuffer.readFrom(destPos, tmpWidth, imageInputStream);
                destPos += tmpWidth;
            } else {
                for (int sourceX = sourceMinX; sourceX <= sourceMaxX; sourceX += sourceStepX) {
                    imageInputStream.seek(bandOffset + elemSize * (sourcePosY + sourceX));
                    tmpBuffer.readFrom(destPos, 2, imageInputStream);
                    destPos++;
                }
            }
            if (oneOfTwo)
                copyOneOfTwo(tmpBuffer, destBuffer);
            else
                copyTwoOfTwo(tmpBuffer, destBuffer);
        }
    }

    private static void copyOneOfTwo(final ProductData tmpBuffer, final ProductData destBuffer) {
        final int numElems = destBuffer.getNumElems();
        for (int x = 0, i = 0; x < numElems; ++x, i += 2) {
            destBuffer.setElemFloatAt(x, tmpBuffer.getElemFloatAt(i));
        }
    }

    private static void copyTwoOfTwo(final ProductData tmpBuffer, final ProductData destBuffer) {
        final int numElems = destBuffer.getNumElems();
        for (int x = 0, i = 1; x < numElems; ++x, i += 2) {
            destBuffer.setElemFloatAt(x, tmpBuffer.getElemFloatAt(i));
        }
    }
}