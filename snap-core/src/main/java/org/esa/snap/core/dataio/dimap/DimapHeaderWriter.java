/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.dataio.dimap;

import org.esa.snap.core.dataio.dimap.spi.DimapPersistable;
import org.esa.snap.core.dataio.dimap.spi.DimapPersistence;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.BasicPixelGeoCoding;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.FXYGeoCoding;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.GcpGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.MapGeoCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.SampleCoding;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.dataop.maptransf.Ellipsoid;
import org.esa.snap.core.dataop.maptransf.MapInfo;
import org.esa.snap.core.dataop.maptransf.MapProjection;
import org.esa.snap.core.dataop.maptransf.MapTransform;
import org.esa.snap.core.dataop.maptransf.MapTransformDescriptor;
import org.esa.snap.core.param.Parameter;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.XmlWriter;
import org.jdom.Element;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

/**
 * This class is used to print the DIMAP XML header of the given product to a given writer or file.
 * <p>
 * The BEAM-DIMAP version history is provided in the API doc of the {@link DimapProductWriterPlugIn}.
 */
public final class DimapHeaderWriter extends XmlWriter {

    private final Product product;
    private final String dataDirectory;

    public DimapHeaderWriter(Product product, File file, String dataDirectory) throws IOException {
        super(file);
        this.product = product;
        this.dataDirectory = dataDirectory;
    }

    public DimapHeaderWriter(Product product, Writer writer, String dataDirectory) {
        super(writer, true);
        this.product = product;
        this.dataDirectory = dataDirectory;
    }

    public void writeHeader() {
        int indent = 0;
        final String[][] attributes = new String[1][];
        final String documentName = product.getName() + DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION;
        attributes[0] = new String[]{DimapProductConstants.ATTRIB_NAME, documentName};
        final String[] tags = createTags(indent, DimapProductConstants.TAG_ROOT, attributes);
        println(tags[0]);
        indent++;
        writeMetadataId(indent);
        writeDatasetId(indent);
        writeDatasetUse(indent);
        writeProductionElements(indent);
        writeGeoCoding(indent);
        writeFlagCoding(indent);
        writeIndexCoding(indent);
        writeRasterDimensionElements(indent);
        writeDataAccessElements(indent);
        writeTiePointGridElements(indent);
        writeImageDisplayElements(indent);
        writeMasks(indent);
        writeImageInterpretationElements(indent);
        writeAnnotatonDataSet(indent);
        print(tags[1]);
        close();
    }

    protected void writeAnnotatonDataSet(int indent) {
        final MetadataElement metadataRoot = product.getMetadataRoot();
        if (metadataRoot != null && metadataRoot.getNumElements() > 0) {
            final String[] dsTags = createTags(indent, DimapProductConstants.TAG_DATASET_SOURCES);
            println(dsTags[0]);
            writeMetadataElements(indent + 1, new MetadataElement[]{metadataRoot});
            println(dsTags[1]);
        }
    }

    protected void writeMetadataElements(int indent, final MetadataElement[] elementes) {
        if (elementes == null) {
            return;
        }
        for (final MetadataElement element : elementes) {
            String[][] attributes;
            final String description = element.getDescription();
            if (description != null) {
                attributes = new String[2][];
                attributes[1] = new String[]{DimapProductConstants.ATTRIB_DESCRIPTION, description};
            } else {
                attributes = new String[1][];
            }
            attributes[0] = new String[]{DimapProductConstants.ATTRIB_NAME, element.getName()};
            final String[] meTags = createTags(indent, DimapProductConstants.TAG_METADATA_ELEMENT, attributes);
            if (element.getNumElements() == 0 && element.getNumAttributes() == 0) {
                printLine(meTags, null);
            } else {
                println(meTags[0]);
                writeMetadataAttributes(indent + 1, element.getAttributes());
                writeMetadataElements(indent + 1, element.getElements());
                println(meTags[1]);
            }
        }
    }

    protected void writeMetadataAttributes(int indent, final MetadataAttribute[] attributes) {
        if (attributes == null) {
            return;
        }
        for (final MetadataAttribute attribute : attributes) {
            final Vector<String[]> xmlAttribs = new Vector<String[]>();
            xmlAttribs.add(new String[]{DimapProductConstants.ATTRIB_NAME, attribute.getName()});
            final String description = attribute.getDescription();
            if (description != null) {
                xmlAttribs.add(new String[]{DimapProductConstants.ATTRIB_DESCRIPTION, description});
            }
            final String unit = attribute.getUnit();
            if (unit != null) {
                xmlAttribs.add(new String[]{DimapProductConstants.ATTRIB_UNIT, unit});
            }
            final String dataTypeString = attribute.getData().getTypeString();
            xmlAttribs.add(new String[]{DimapProductConstants.ATTRIB_TYPE, dataTypeString});
            if (!attribute.isReadOnly()) {
                xmlAttribs.add(new String[]{DimapProductConstants.ATTRIB_MODE, "rw"});
            }
            if (attribute.getNumDataElems() > 1 &&
                    !ProductData.TYPESTRING_ASCII.equals(dataTypeString) &&
                    !ProductData.TYPESTRING_UTC.equals(dataTypeString)) {
                xmlAttribs.add(
                        new String[]{DimapProductConstants.ATTRIB_ELEMS, String.valueOf(attribute.getNumDataElems())});
            }

            final String text = attribute.getData().getElemString();
            final String[][] attribs = xmlAttribs.toArray(new String[][]{});
            printLine(indent, DimapProductConstants.TAG_METADATA_ATTRIBUTE, attribs, text);
        }
    }

    protected void writeImageInterpretationElements(int indent) {
        final Band[] bands = product.getBands();
        final boolean hasBands = bands != null && bands.length > 0;
        if (hasBands) {
            final String[] iiTags = createTags(indent, DimapProductConstants.TAG_IMAGE_INTERPRETATION);
            println(iiTags[0]);
            for (int i = 0; i < bands.length; i++) {
                final Band band = bands[i];
                if (band instanceof FilterBand) {
                    final DimapPersistable persistable = DimapPersistence.getPersistable(band);
                    if (persistable != null) {
                        final Element xmlFromObject = persistable.createXmlFromObject(band);
                        printElement(indent + 1, xmlFromObject);
                    }
                } else {
                    final String[] sbiTags = createTags(indent + 1, DimapProductConstants.TAG_SPECTRAL_BAND_INFO);
                    println(sbiTags[0]);
                    printLine(indent + 2, DimapProductConstants.TAG_BAND_INDEX, i);
                    printLine(indent + 2, DimapProductConstants.TAG_BAND_DESCRIPTION, band.getDescription());
                    printLine(indent + 2, DimapProductConstants.TAG_BAND_NAME, band.getName());
                    printLine(indent + 2, DimapProductConstants.TAG_BAND_RASTER_WIDTH, band.getRasterWidth());
                    printLine(indent + 2, DimapProductConstants.TAG_BAND_RASTER_HEIGHT, band.getRasterHeight());
                    printLine(indent + 2, DimapProductConstants.TAG_DATA_TYPE,
                              ProductData.getTypeString(band.getDataType()));
                    final String unit = band.getUnit();
                    if (unit != null && unit.length() > 0) {
                        printLine(indent + 2, DimapProductConstants.TAG_PHYSICAL_UNIT, unit);
                    }
                    printLine(indent + 2, DimapProductConstants.TAG_SOLAR_FLUX, band.getSolarFlux());
                    if (band.getSpectralBandIndex() > -1) {
                        printLine(indent + 2, DimapProductConstants.TAG_SPECTRAL_BAND_INDEX,
                                  band.getSpectralBandIndex());
                    }
                    printLine(indent + 2, DimapProductConstants.TAG_BAND_WAVELEN, band.getSpectralWavelength());
                    printLine(indent + 2, DimapProductConstants.TAG_BANDWIDTH, band.getSpectralBandwidth());
                    final FlagCoding flagCoding = band.getFlagCoding();
                    if (flagCoding != null) {
                        printLine(indent + 2, DimapProductConstants.TAG_FLAG_CODING_NAME, flagCoding.getName());
                    }
                    final IndexCoding indexCoding = band.getIndexCoding();
                    if (indexCoding != null) {
                        printLine(indent + 2, DimapProductConstants.TAG_INDEX_CODING_NAME, indexCoding.getName());
                    }
                    printLine(indent + 2, DimapProductConstants.TAG_SCALING_FACTOR, band.getScalingFactor());
                    printLine(indent + 2, DimapProductConstants.TAG_SCALING_OFFSET, band.getScalingOffset());
                    printLine(indent + 2, DimapProductConstants.TAG_SCALING_LOG_10, band.isLog10Scaled());
                    printLine(indent + 2, DimapProductConstants.TAG_NO_DATA_VALUE_USED, band.isNoDataValueUsed());
                    printLine(indent + 2, DimapProductConstants.TAG_NO_DATA_VALUE, band.getNoDataValue());
                    if (band instanceof VirtualBand) {
                        final VirtualBand vb = (VirtualBand) band;
                        printLine(indent + 2, DimapProductConstants.TAG_VIRTUAL_BAND, true);
                        printLine(indent + 2, DimapProductConstants.TAG_VIRTUAL_BAND_EXPRESSION, vb.getExpression());
                    }
                    final String validMaskExpression = band.getValidPixelExpression();
                    if (validMaskExpression != null) {
                        printLine(indent + 2, DimapProductConstants.TAG_VALID_MASK_TERM, validMaskExpression);
                    }
                    writeAncillaryInformation(band, indent);
                    writeImageToModelTransform(band, indent);
                    println(sbiTags[1]);
                }
            }
            println(iiTags[1]);
        }
    }

    private void writeAncillaryInformation(RasterDataNode rasterDataNode, int indent) {
        String[] ancillaryRelations = rasterDataNode.getAncillaryRelations();
        for (String ancillaryRelation : ancillaryRelations) {
            printLine(indent + 2, DimapProductConstants.TAG_ANCILLARY_RELATION, ancillaryRelation);
        }
        RasterDataNode[] ancillaryVariables = rasterDataNode.getAncillaryVariables();
        for (RasterDataNode ancillaryVariable : ancillaryVariables) {
            printLine(indent + 2, DimapProductConstants.TAG_ANCILLARY_VARIABLE, ancillaryVariable.getName());
        }
    }

    private void writeImageToModelTransform(RasterDataNode rasterDataNode, int indent) {
        final AffineTransform imageToModelTransform = rasterDataNode.getImageToModelTransform();
        if (!imageToModelTransform.isIdentity()) {
            final double[] matrix = new double[6];
            imageToModelTransform.getMatrix(matrix);
            printLine(indent + 2, DimapProductConstants.TAG_IMAGE_TO_MODEL_TRANSFORM, StringUtils.arrayToCsv(matrix));
        }
    }

    protected void writeMasks(int indent) {
        final Mask[] masks = product.getMaskGroup().toArray(new Mask[product.getMaskGroup().getNodeCount()]);
        int persistableMaskCount = 0;
        for (final Mask mask : masks) {
            final DimapPersistable persistable = DimapPersistence.getPersistable(mask);
            if (persistable != null) {
                persistableMaskCount++;
            }
        }
        if (persistableMaskCount > 0) {
            final String[] bdTags = createTags(indent, DimapProductConstants.TAG_MASKS);
            println(bdTags[0]);
            for (final Mask mask : masks) {
                final DimapPersistable persistable = DimapPersistence.getPersistable(mask);
                if (persistable != null) {
                    final Element element = persistable.createXmlFromObject(mask);
                    printElement(indent + 1, element);
                }
            }
            println(bdTags[1]);
        }
    }

    protected void writeImageDisplayElements(int indent) {
        final StringWriter stringWriter = new StringWriter();
        final XmlWriter sXmlW = new XmlWriter(stringWriter, false);
        final Band[] bands = product.getBands();
        final String[] idTags = createTags(indent, DimapProductConstants.TAG_IMAGE_DISPLAY);
        writeBandStatistics(sXmlW, indent, bands);
        writeMaskUsages(sXmlW, indent + 1, bands);
        writeMaskUsages(sXmlW, indent + 1, product.getTiePointGrids());

        sXmlW.close();
        final String childTags = stringWriter.toString();
        if (childTags != null && childTags.length() > 0) {
            println(idTags[0]);
            print(childTags);
            println(idTags[1]);
        }
    }

    protected void writeBandStatistics(final XmlWriter sXmlW, int indent, final Band[] bands) {
        Debug.assertNotNull(sXmlW);
        Debug.assertNotNull(bands);
        for (int i = 0; i < bands.length; i++) {
            final Band band = bands[i];
            if (band.getImageInfo() != null) {
                final String[] bsTags = createTags(indent + 1, DimapProductConstants.TAG_BAND_STATISTICS);
                sXmlW.println(bsTags[0]);
                sXmlW.printLine(indent + 2, DimapProductConstants.TAG_BAND_INDEX, i);

                if (band.isStxSet()) {
                    sXmlW.printLine(indent + 2, DimapProductConstants.TAG_STX_MIN, band.getStx().getMinimum());
                    sXmlW.printLine(indent + 2, DimapProductConstants.TAG_STX_MAX, band.getStx().getMaximum());
                    sXmlW.printLine(indent + 2, DimapProductConstants.TAG_STX_MEAN, band.getStx().getMean());
                    sXmlW.printLine(indent + 2, DimapProductConstants.TAG_STX_STDDEV, band.getStx().getStandardDeviation());
                    sXmlW.printLine(indent + 2, DimapProductConstants.TAG_STX_LEVEL, band.getStx().getResolutionLevel());
                    final int[] bins = band.getStx().getHistogramBins();
                    if (bins != null && bins.length > 0) {
                        sXmlW.printLine(indent + 2, DimapProductConstants.TAG_HISTOGRAM, StringUtils.arrayToCsv(bins));
                    }
                }

                if (band.getImageInfo() != null) {
                    final ColorPaletteDef paletteDefinition = band.getImageInfo().getColorPaletteDef();
                    sXmlW.printLine(indent + 2, DimapProductConstants.TAG_NUM_COLORS, paletteDefinition.getNumColors());
                    final Iterator iterator = paletteDefinition.getIterator();
                    while (iterator.hasNext()) {
                        final ColorPaletteDef.Point point = (ColorPaletteDef.Point) iterator.next();
                        final String[] cppTags = createTags(indent + 2, DimapProductConstants.TAG_COLOR_PALETTE_POINT);
                        sXmlW.println(cppTags[0]);
                        sXmlW.printLine(indent + 3, DimapProductConstants.TAG_SAMPLE, point.getSample());
                        if (StringUtils.isNotNullAndNotEmpty(point.getLabel())) {
                            sXmlW.printLine(indent + 3, DimapProductConstants.TAG_LABEL, point.getLabel());
                        }
                        DimapProductHelpers.printColorTag(indent + 3, DimapProductConstants.TAG_COLOR, point.getColor(),
                                                          sXmlW);
                        sXmlW.println(cppTags[1]);
                    }
                    DimapProductHelpers.printColorTag(indent + 2, DimapProductConstants.TAG_NO_DATA_COLOR,
                                                      band.getImageInfo().getNoDataColor(), sXmlW);
                    sXmlW.printLine(indent + 2, DimapProductConstants.TAG_HISTOGRAM_MATCHING,
                                    band.getImageInfo().getHistogramMatching().toString());
                }

                sXmlW.println(bsTags[1]);
            }
        }
    }

    protected void writeMaskUsages(XmlWriter pw, int indent, RasterDataNode[] rasterDataNodes) {
        Guardian.assertNotNull("pw", pw);
        Guardian.assertNotNull("rasterDataNodes", rasterDataNodes);
        for (int i = 0; i < rasterDataNodes.length; i++) {
            final RasterDataNode rasterDataNode = rasterDataNodes[i];
            ProductNodeGroup<Mask> overlayMaskGroup = rasterDataNode.getOverlayMaskGroup();
            if (overlayMaskGroup.getNodeCount() > 0) {
                final String[] boTags = createTags(indent, DimapProductConstants.TAG_MASK_USAGE);
                pw.println(boTags[0]);
                if (rasterDataNode instanceof Band) {
                    pw.printLine(indent + 1, DimapProductConstants.TAG_BAND_INDEX, i);
                } else {
                    pw.printLine(indent + 1, DimapProductConstants.TAG_TIE_POINT_GRID_INDEX, i);
                }

                final String[][] attributes = new String[1][];
                if (overlayMaskGroup.getNodeCount() > 0) {
                    attributes[0] = new String[]{
                            DimapProductConstants.ATTRIB_NAMES,
                            StringUtils.arrayToCsv(overlayMaskGroup.getNodeNames())
                    };
                    pw.printLine(indent + 1, DimapProductConstants.TAG_OVERLAY, attributes, null);
                }
                pw.println(boTags[1]);
            }
        }
    }

    protected void writeTiePointGridElements(int indent) {
        final TiePointGrid[] tiePointGrids = product.getTiePointGrids();
        if (tiePointGrids != null && tiePointGrids.length > 0) {
            final String[] tpgTags = createTags(indent, DimapProductConstants.TAG_TIE_POINT_GRIDS);
            println(tpgTags[0]);
            printLine(indent + 1, DimapProductConstants.TAG_TIE_POINT_NUM_TIE_POINT_GRIDS, tiePointGrids.length);
            for (int i = 0; i < tiePointGrids.length; i++) {
                final TiePointGrid tiePointGrid = tiePointGrids[i];
                final String[] tpgInfoTags = createTags(indent + 1, DimapProductConstants.TAG_TIE_POINT_GRID_INFO);
                println(tpgInfoTags[0]);
                printLine(indent + 2, DimapProductConstants.TAG_TIE_POINT_GRID_INDEX, i);
                printLine(indent + 2, DimapProductConstants.TAG_TIE_POINT_DESCRIPTION, tiePointGrid.getDescription());
                printLine(indent + 2, DimapProductConstants.TAG_TIE_POINT_PHYSICAL_UNIT, tiePointGrid.getUnit());
                printLine(indent + 2, DimapProductConstants.TAG_TIE_POINT_GRID_NAME, tiePointGrid.getName());
                printLine(indent + 2, DimapProductConstants.TAG_TIE_POINT_DATA_TYPE,
                          ProductData.getTypeString(tiePointGrid.getDataType()));
                printLine(indent + 2, DimapProductConstants.TAG_TIE_POINT_NCOLS, tiePointGrid.getGridWidth());
                printLine(indent + 2, DimapProductConstants.TAG_TIE_POINT_NROWS, tiePointGrid.getGridHeight());
                printLine(indent + 2, DimapProductConstants.TAG_TIE_POINT_OFFSET_X, tiePointGrid.getOffsetX());
                printLine(indent + 2, DimapProductConstants.TAG_TIE_POINT_OFFSET_Y, tiePointGrid.getOffsetY());
                printLine(indent + 2, DimapProductConstants.TAG_TIE_POINT_STEP_X, tiePointGrid.getSubSamplingX());
                printLine(indent + 2, DimapProductConstants.TAG_TIE_POINT_STEP_Y, tiePointGrid.getSubSamplingY());
                final boolean cyclic = tiePointGrid.getDiscontinuity() != TiePointGrid.DISCONT_NONE;
                printLine(indent + 2, DimapProductConstants.TAG_TIE_POINT_CYCLIC, cyclic);
                writeAncillaryInformation(tiePointGrid, indent);
                writeImageToModelTransform(tiePointGrid, indent);
                println(tpgInfoTags[1]);
            }
            println(tpgTags[1]);
        }
    }

    protected void writeDataAccessElements(int indent) {
        if (product.getNumBands() > 0 || product.getNumTiePointGrids() > 0) {
            final String[] daTags = createTags(indent, DimapProductConstants.TAG_DATA_ACCESS);
            println(daTags[0]);
            printLine(indent + 1, DimapProductConstants.TAG_DATA_FILE_FORMAT, DimapProductConstants.DATA_FILE_FORMAT);
            printLine(indent + 1, DimapProductConstants.TAG_DATA_FILE_FORMAT_DESC,
                      DimapProductConstants.DATA_FILE_FORMAT_DESCRIPTION);
            printLine(indent + 1, DimapProductConstants.TAG_DATA_FILE_ORGANISATION,
                      DimapProductConstants.DATA_FILE_ORGANISATION);

            final Band[] bands = product.getBands();
            for (int i = 0; i < bands.length; i++) {
                Band band = bands[i];
                if (!(band instanceof VirtualBand || band instanceof FilterBand)) {
                    final String[] dfTags = createTags(indent + 1, DimapProductConstants.TAG_DATA_FILE);
                    println(dfTags[0]);
                    final String href = dataDirectory + "/" + band.getName() + EnviHeader.FILE_EXTENSION;
                    final String[][] attributes = new String[][]{
                            new String[]{DimapProductConstants.ATTRIB_HREF, href}
                    };
                    printLine(indent + 2, DimapProductConstants.TAG_DATA_FILE_PATH, attributes, null);
                    printLine(indent + 2, DimapProductConstants.TAG_BAND_INDEX, i);
                    println(dfTags[1]);
                }
            }

            final String[] tpgNames = product.getTiePointGridNames();
            for (int i = 0; i < tpgNames.length; i++) {
                final String[] tpgfTags = createTags(indent + 1, DimapProductConstants.TAG_TIE_POINT_GRID_FILE);
                println(tpgfTags[0]);
                final String href = dataDirectory + "/" + DimapProductConstants.TIE_POINT_GRID_DIR_NAME + "/" + tpgNames[i] + EnviHeader.FILE_EXTENSION;
                final String[][] attributes = new String[][]{new String[]{DimapProductConstants.ATTRIB_HREF, href}};
                printLine(indent + 2, DimapProductConstants.TAG_TIE_POINT_GRID_FILE_PATH, attributes, null);
                printLine(indent + 2, DimapProductConstants.TAG_TIE_POINT_GRID_INDEX, i);
                println(tpgfTags[1]);
            }

            println(daTags[1]);
        }
    }

    protected void writeRasterDimensionElements(int indent) {
        final String[] rdTags = createTags(indent, DimapProductConstants.TAG_RASTER_DIMENSIONS);
        println(rdTags[0]);
        printLine(indent + 1, DimapProductConstants.TAG_NCOLS, product.getSceneRasterWidth());
        printLine(indent + 1, DimapProductConstants.TAG_NROWS, product.getSceneRasterHeight());
        printLine(indent + 1, DimapProductConstants.TAG_NBANDS, product.getNumBands());
        println(rdTags[1]);
    }

    protected void writeFlagCoding(int indent) {
        SampleCoding[] a = product.getFlagCodingGroup().toArray(new FlagCoding[0]);
        writeSampleCodings(indent, a, DimapProductConstants.TAG_FLAG_CODING, DimapProductConstants.TAG_FLAG,
                           DimapProductConstants.TAG_FLAG_NAME, DimapProductConstants.TAG_FLAG_INDEX,
                           DimapProductConstants.TAG_FLAG_DESCRIPTION);
    }

    protected void writeIndexCoding(int indent) {
        SampleCoding[] a = product.getIndexCodingGroup().toArray(new IndexCoding[0]);
        writeSampleCodings(indent, a, DimapProductConstants.TAG_INDEX_CODING, DimapProductConstants.TAG_INDEX,
                           DimapProductConstants.TAG_INDEX_NAME, DimapProductConstants.TAG_INDEX_VALUE,
                           DimapProductConstants.TAG_INDEX_DESCRIPTION);
    }

    private void writeSampleCodings(int indent, SampleCoding[] a, String tagCoding, String tagFlag, String tagName,
                                    String tagIndex,
                                    String tagDescription) {
        for (SampleCoding sampleCoding : a) {
            final String[][] attributes = new String[1][];
            attributes[0] = new String[]{DimapProductConstants.ATTRIB_NAME, sampleCoding.getName()};
            final String[] fcTags = createTags(indent, tagCoding, attributes);
            println(fcTags[0]);
            writeSampleCoding(indent, sampleCoding, fcTags, tagFlag, tagName, tagIndex, tagDescription);
        }
    }

    private void writeSampleCoding(int indent, SampleCoding sampleCoding, String[] fcTags, String tagFlag,
                                   String tagName,
                                   String tagIndex, String tagDescription) {
        final String[] names = sampleCoding.getAttributeNames();
        for (String name : names) {
            final MetadataAttribute attribute = sampleCoding.getAttribute(name);
            final String[] fTags = createTags(indent + 1, tagFlag);
            println(fTags[0]);
            printLine(indent + 2, tagName, attribute.getName());
            printLine(indent + 2, tagIndex, attribute.getData().getElemInt());
            printLine(indent + 2, tagDescription, attribute.getDescription());
            println(fTags[1]);
        }
        println(fcTags[1]);
    }

    protected void writeGeoCoding(final int indent) {
        if (product.isUsingSingleGeoCoding()) {
            writeGeoCoding(product.getSceneGeoCoding(), indent, -1);
        } else {
            final Band[] bands = product.getBands();
            for (int i = 0; i < bands.length; i++) {
                final Band band = bands[i];
                writeGeoCoding(band.getGeoCoding(), indent, i);
            }
        }
    }

    private void writeGeoCoding(final GeoCoding geoCoding, final int indent, final int index) {
        if (geoCoding != null) {
            final DimapPersistable persistable = DimapPersistence.getPersistable(geoCoding);
            if (persistable != null) {
                final String[] geopositionTags = createTags(indent, DimapProductConstants.TAG_GEOPOSITION);
                println(geopositionTags[0]);
                writeBandIndexIf(index >= 0, index, indent + 1);
                printElement(indent + 1, persistable.createXmlFromObject(geoCoding));
                println(geopositionTags[1]);
            } else if (geoCoding instanceof TiePointGeoCoding) {
                writeGeoCoding((TiePointGeoCoding) geoCoding, indent, index);
            } else if (geoCoding instanceof MapGeoCoding) {
                writeGeoCoding((MapGeoCoding) geoCoding, indent);
            } else if (geoCoding instanceof BasicPixelGeoCoding) {
                writeGeoCoding((BasicPixelGeoCoding) geoCoding, indent, index);
            } else if (geoCoding instanceof FXYGeoCoding) {
                writeGeoCoding((FXYGeoCoding) geoCoding, indent, index);
            } else if (geoCoding instanceof GcpGeoCoding) {
                writeGeoCoding((GcpGeoCoding) geoCoding, indent, index);
            } else if (geoCoding instanceof CrsGeoCoding) {
                writeGeoCoding((CrsGeoCoding) geoCoding, indent, index);
            }
        }
    }

    private void writeGeoCoding(final CrsGeoCoding crsGeoCoding, int indent, int index) {
        final CoordinateReferenceSystem crs = crsGeoCoding.getMapCRS();
        final double[] matrix = new double[6];
        final MathTransform transform = crsGeoCoding.getImageToMapTransform();
        if (transform instanceof AffineTransform) {
            ((AffineTransform) transform).getMatrix(matrix);
        }

        final String[] crsTags = createTags(indent, DimapProductConstants.TAG_COORDINATE_REFERENCE_SYSTEM);
        println(crsTags[0]);
        final String[] wktTags = createTags(indent + 1, DimapProductConstants.TAG_WKT);
        println(wktTags[0]);
        final char[] wsChars = new char[wktTags[0].length()];
        Arrays.fill(wsChars, ' ');
        final String ws = new String(wsChars);
        for (String wktLine : crs.toString().split(SystemUtils.LS)) {
            print(ws);
            println(wktLine);
        }
        println(wktTags[1]);
        println(crsTags[1]);
        final String[] geopositionTags = createTags(indent, DimapProductConstants.TAG_GEOPOSITION);
        println(geopositionTags[0]);
        writeBandIndexIf(index >= 0, index, indent + 1);
        printLine(indent + 1, DimapProductConstants.TAG_IMAGE_TO_MODEL_TRANSFORM, StringUtils.arrayToCsv(matrix));
        println(geopositionTags[1]);

    }

    private void writeGeoCoding(final GcpGeoCoding gcpPointGeoCoding, int indent, int index) {
        final String[] crsTags = createTags(indent, DimapProductConstants.TAG_COORDINATE_REFERENCE_SYSTEM);
        println(crsTags[0]);
        writeDatum(gcpPointGeoCoding.getDatum(), indent + 1);
        println(crsTags[1]);
        final String[] posTags = createTags(indent, DimapProductConstants.TAG_GEOPOSITION);
        println(posTags[0]);
        final String[] gcpTags = createTags(indent + 1, DimapProductConstants.TAG_GEOPOSITION_POINTS);
        println(gcpTags[0]);
        printLine(indent + 2, DimapProductConstants.TAG_INTERPOLATION_METHOD, gcpPointGeoCoding.getMethod().name());
        final GeoCoding originalGeoCoding = gcpPointGeoCoding.getOriginalGeoCoding();
        if (!(originalGeoCoding == null || originalGeoCoding instanceof GcpGeoCoding)) {
            final String[] ogcTags = createTags(indent + 2, DimapProductConstants.TAG_ORIGINAL_GEOCODING);
            println(ogcTags[0]);
            writeGeoCoding(originalGeoCoding, indent + 3, index);
            println(ogcTags[1]);
        }
        println(gcpTags[1]);
        println(posTags[1]);
    }

    private void writeGeoCoding(final TiePointGeoCoding tiePointGeoCoding, final int indent, final int index) {
        final String[] crsTags = createTags(indent, DimapProductConstants.TAG_COORDINATE_REFERENCE_SYSTEM);
        println(crsTags[0]);
        writeDatum(tiePointGeoCoding.getDatum(), indent + 1);
        println(crsTags[1]);
        final String latGridName = tiePointGeoCoding.getLatGrid().getName();
        final String lonGridName = tiePointGeoCoding.getLonGrid().getName();
        if (latGridName == null || lonGridName == null) {
            return;
        }
        final String[] geopositionTags = createTags(indent, DimapProductConstants.TAG_GEOPOSITION);
        println(geopositionTags[0]);
        writeBandIndexIf(index >= 0, index, indent + 1);
        final String[] pointsTags = createTags(indent + 1, DimapProductConstants.TAG_GEOPOSITION_POINTS);
        println(pointsTags[0]);
        printLine(indent + 2, DimapProductConstants.TAG_TIE_POINT_GRID_NAME_LAT, latGridName);
        printLine(indent + 2, DimapProductConstants.TAG_TIE_POINT_GRID_NAME_LON, lonGridName);
        println(pointsTags[1]);
        println(geopositionTags[1]);
    }

    private void writeBandIndexIf(final boolean condition, final int index, final int indent) {
        if (condition) {
            printLine(indent, DimapProductConstants.TAG_BAND_INDEX, String.valueOf(index));
        }
    }

    private void writeGeoCoding(final MapGeoCoding mapGeoCoding, int indent) {
        final MapInfo info = mapGeoCoding.getMapInfo();
        if (info == null) {
            return;
        }

        final String[] crsTags = createTags(indent, DimapProductConstants.TAG_COORDINATE_REFERENCE_SYSTEM);
        println(crsTags[0]);
        ++indent;
        final Datum datum = info.getDatum();
        final MapProjection projection = info.getMapProjection();
        final Ellipsoid ellipsoid = datum.getEllipsoid();
        final MapTransform mapTransform = projection.getMapTransform();
        final double[] parameterValues = mapTransform.getParameterValues();
        final MapTransformDescriptor descriptor = mapTransform.getDescriptor();
        final String datumName = datum.getName();
        final String projectionName = projection.getName();
        final String ellipsoidName = ellipsoid.getName();
        final double semiMajor = ellipsoid.getSemiMajor();
        final double semiMinor = ellipsoid.getSemiMinor();
        final String typeID = descriptor.getTypeID();
        final Parameter[] parameters = descriptor.getParameters();

        printLine(indent, DimapProductConstants.TAG_GEO_TABLES, new String[][]{new String[]{"version", "1.0"}},
                  "CUSTOM");
        final String[] horizontalCsTags = createTags(indent, DimapProductConstants.TAG_HORIZONTAL_CS);
        println(horizontalCsTags[0]);
        ++indent;
        printLine(indent, "HORIZONTAL_CS_TYPE", "PROJECTED");
        printLine(indent, DimapProductConstants.TAG_HORIZONTAL_CS_NAME, projectionName);
        final String[] geographicCsTags = createTags(indent, DimapProductConstants.TAG_GEOGRAPHIC_CS);
        println(geographicCsTags[0]);
        ++indent;
        printLine(indent, DimapProductConstants.TAG_GEOGRAPHIC_CS_NAME, projectionName);
        final String[] horizontalDatumTags = createTags(indent, DimapProductConstants.TAG_HORIZONTAL_DATUM);
        println(horizontalDatumTags[0]);
        ++indent;
        printLine(indent, DimapProductConstants.TAG_HORIZONTAL_DATUM_NAME,
                  datumName);  // @todo mp - write also DX,DY,DZ
        final String[] ellipsoidTags = createTags(indent, DimapProductConstants.TAG_ELLIPSOID);
        println(ellipsoidTags[0]);
        ++indent;
        printLine(indent, DimapProductConstants.TAG_ELLIPSOID_NAME, ellipsoidName);
        final String[] ellipsoidParametersTags = createTags(indent, DimapProductConstants.TAG_ELLIPSOID_PARAMETERS);
        println(ellipsoidParametersTags[0]);
        ++indent;
        final String[][] attributes = new String[][]{new String[]{"unit", "meter"}};
        printLine(indent, DimapProductConstants.TAG_ELLIPSOID_MAJ_AXIS, attributes, String.valueOf(semiMajor));
        printLine(indent, DimapProductConstants.TAG_ELLIPSOID_MIN_AXIS, attributes, String.valueOf(semiMinor));
        println(ellipsoidParametersTags[1]);
        --indent;
        println(ellipsoidTags[1]);
        --indent;
        println(horizontalDatumTags[1]);
        --indent;
        println(geographicCsTags[1]);
        --indent;
        final String[] projectionTags = createTags(indent, DimapProductConstants.TAG_PROJECTION);
        println(projectionTags[0]);
        ++indent;
        printLine(indent, DimapProductConstants.TAG_PROJECTION_NAME, projectionName);
        final String[] projectionCtMethodTags = createTags(indent, DimapProductConstants.TAG_PROJECTION_CT_METHOD);
        println(projectionCtMethodTags[0]);
        ++indent;
        printLine(indent, DimapProductConstants.TAG_PROJECTION_CT_NAME, typeID);
        final String[] projectionParametersTags = createTags(indent, DimapProductConstants.TAG_PROJECTION_PARAMETERS);
        println(projectionParametersTags[0]);
        ++indent;
        final String[] projectionParameterTags = createTags(indent, DimapProductConstants.TAG_PROJECTION_PARAMETER);
        ++indent;
        final String[][] paramUnitAttributes = new String[1][2];
        paramUnitAttributes[0][0] = DimapProductConstants.ATTRIB_UNIT;
        for (int i = 0; i < parameters.length; i++) {
            println(projectionParameterTags[0]);
            printLine(indent, DimapProductConstants.TAG_PROJECTION_PARAMETER_NAME, parameters[i].getName());
            paramUnitAttributes[0][1] = parameters[i].getProperties().getPhysicalUnit();
            printLine(indent, DimapProductConstants.TAG_PROJECTION_PARAMETER_VALUE, paramUnitAttributes,
                      String.valueOf(parameterValues[i]));
            println(projectionParameterTags[1]);
        }
        --indent;
        println(projectionParametersTags[1]);
        --indent;
        println(projectionCtMethodTags[1]);
        --indent;
        println(projectionTags[1]);
        --indent;
        final String[] mi2Tags = createTags(indent, DimapProductConstants.TAG_GEOCODING_MAP_INFO);
        println(mi2Tags[0]);
        ++indent;
        final String[][] mapAttrib = new String[][]{{DimapProductConstants.ATTRIB_VALUE, ""}};
        mapAttrib[0][1] = String.valueOf(info.getPixelX());
        printLine(indent, DimapProductConstants.TAG_MAP_INFO_PIXEL_X, mapAttrib, null);
        mapAttrib[0][1] = String.valueOf(info.getPixelY());
        printLine(indent, DimapProductConstants.TAG_MAP_INFO_PIXEL_Y, mapAttrib, null);
        mapAttrib[0][1] = String.valueOf(info.getEasting());
        printLine(indent, DimapProductConstants.TAG_MAP_INFO_EASTING, mapAttrib, null);
        mapAttrib[0][1] = String.valueOf(info.getNorthing());
        printLine(indent, DimapProductConstants.TAG_MAP_INFO_NORTHING, mapAttrib, null);
        mapAttrib[0][1] = String.valueOf(info.getOrientation());
        printLine(indent, DimapProductConstants.TAG_MAP_INFO_ORIENTATION, mapAttrib, null);
        mapAttrib[0][1] = String.valueOf(info.getPixelSizeX());
        printLine(indent, DimapProductConstants.TAG_MAP_INFO_PIXELSIZE_X, mapAttrib, null);
        mapAttrib[0][1] = String.valueOf(info.getPixelSizeY());
        printLine(indent, DimapProductConstants.TAG_MAP_INFO_PIXELSIZE_Y, mapAttrib, null);
        mapAttrib[0][1] = String.valueOf(info.getNoDataValue());
        printLine(indent, DimapProductConstants.TAG_MAP_INFO_NODATA_VALUE, mapAttrib, null);
        mapAttrib[0][1] = info.getMapProjection().getMapUnit();
        printLine(indent, DimapProductConstants.TAG_MAP_INFO_MAPUNIT, mapAttrib, null);
        mapAttrib[0][1] = String.valueOf(info.isOrthorectified());
        printLine(indent, DimapProductConstants.TAG_MAP_INFO_ORTHORECTIFIED, mapAttrib, null);
        mapAttrib[0][1] = info.getElevationModelName();
        printLine(indent, DimapProductConstants.TAG_MAP_INFO_ELEVATION_MODEL, mapAttrib, null);
        mapAttrib[0][1] = String.valueOf(info.isSceneSizeFitted());
        printLine(indent, DimapProductConstants.TAG_MAP_INFO_SCENE_FITTED, mapAttrib, null);
        mapAttrib[0][1] = String.valueOf(info.getSceneWidth());
        printLine(indent, DimapProductConstants.TAG_MAP_INFO_SCENE_WIDTH, mapAttrib, null);
        mapAttrib[0][1] = String.valueOf(info.getSceneHeight());
        printLine(indent, DimapProductConstants.TAG_MAP_INFO_SCENE_HEIGHT, mapAttrib, null);
        mapAttrib[0][1] = String.valueOf(info.getResampling().getName());
        printLine(indent, DimapProductConstants.TAG_MAP_INFO_RESAMPLING, mapAttrib, null);
        --indent;
        println(mi2Tags[1]);
        --indent;
        println(horizontalCsTags[1]);
        --indent;
        println(crsTags[1]);
    }

    private void writeGeoCoding(final BasicPixelGeoCoding pixelGeoCoding, final int indent, final int index) {
        final String latBandName = pixelGeoCoding.getLatBand().getName();
        final String lonBandName = pixelGeoCoding.getLonBand().getName();
        final String validMask = pixelGeoCoding.getValidMask();
        final int searchRadius = pixelGeoCoding.getSearchRadius();
        final GeoCoding posEstimator = pixelGeoCoding.getPixelPosEstimator();

        final String[] geopositionTags = createTags(indent, DimapProductConstants.TAG_GEOPOSITION);
        println(geopositionTags[0]);
        printLine(indent + 1, DimapProductConstants.TAG_LATITUDE_BAND, latBandName);
        printLine(indent + 1, DimapProductConstants.TAG_LONGITUDE_BAND, lonBandName);
        if (validMask != null && !validMask.trim().isEmpty()) {
            printLine(indent + 1, DimapProductConstants.TAG_VALID_MASK_EXPRESSION, validMask);
        }
        printLine(indent + 1, DimapProductConstants.TAG_SEARCH_RADIUS, searchRadius);
        if (posEstimator != null) {
            final String[] pixelPosEstimatorTags = createTags(indent + 1,
                                                              DimapProductConstants.TAG_PIXEL_POSITION_ESTIMATOR);
            println(pixelPosEstimatorTags[0]);
            writeGeoCoding(posEstimator, indent + 2, index);
            println(pixelPosEstimatorTags[1]);
        }
        println(geopositionTags[1]);
    }

    private void writeGeoCoding(final FXYGeoCoding fxyGeoCoding, int indent, final int index) {

        if (index <= 0) {
            indent = writeFXYCoordRefSystem(fxyGeoCoding, indent);
        }

        // <Geoposition>
        //   <Geoposition_Insert>
        //      <ULXMAP unit="M">593240.0</ULXMAP>
        //      <ULYMAP unit="M">4697200.0</ULYMAP>
        //      <XDIM unit="M">10.0</XDIM>
        //      <YDIM unit="M">10.0</YDIM>
        //   </Geoposition_Insert>
        //   <Simplified_Location_Model>
        //      <Direct_Location_Model order="3">
        //         <lc_List>
        //           <lc index="0">2.438745e-02</lc>
        //           <lc index="1">3.564434e-03</lc>
        //           ...
        //         </lc_List>
        //         <pc_List>
        //           <pc index="0">7.326785e-02</pc>
        //           <pc index="1">1.436828e-03</pc>
        //           ...
        //         </pc_List>
        //      </Direct_Location_Model>
        //      <Reverse_Location_Model order="3">
        //         <ic_List>
        //           <ic index="0">2.438745e-02</ic>
        //           <ic index="1">3.564434e-03</ic>
        //           ...
        //         </ic_List>
        //         <jc_List>
        //           <jc index="0">7.326785e-02</jc>
        //           <jc index="1">1.436828e-03</jc>
        //           ...
        //         </jc_List>
        //      </Reverse_Location_Model>
        //   </Simplified_Location_Model>
        // </Geoposition>

        final String[] gpTags = createTags(indent, DimapProductConstants.TAG_GEOPOSITION);
        println(gpTags[0]);

        indent++;
        writeBandIndexIf(index >= 0, index, indent);
        final String[] gpiTags = createTags(indent, DimapProductConstants.TAG_GEOPOSITION_INSERT);
        println(gpiTags[0]);


        indent++;
        printLine(indent, DimapProductConstants.TAG_ULX_MAP, fxyGeoCoding.getPixelOffsetX());
        printLine(indent, DimapProductConstants.TAG_ULY_MAP, fxyGeoCoding.getPixelOffsetY());
        printLine(indent, DimapProductConstants.TAG_X_DIM, fxyGeoCoding.getPixelSizeX());
        printLine(indent, DimapProductConstants.TAG_Y_DIM, fxyGeoCoding.getPixelSizeY());
        --indent;
        println(gpiTags[1]);

        final String[] slmTags = createTags(indent, DimapProductConstants.TAG_SIMPLIFIED_LOCATION_MODEL);
        println(slmTags[0]);

        final int directOrder = fxyGeoCoding.getLatFunction().getOrder();
        final int reverseOrder = fxyGeoCoding.getPixelXFunction().getOrder();
        final double[] lambdaCoeffs = fxyGeoCoding.getLonFunction().getCoefficients();
        final double[] phiCoeffs = fxyGeoCoding.getLatFunction().getCoefficients();
        final double[] xCoeffs = fxyGeoCoding.getPixelXFunction().getCoefficients();
        final double[] yCoeffs = fxyGeoCoding.getPixelYFunction().getCoefficients();
        indent++;
        writeDirectLocationModel(indent, directOrder, lambdaCoeffs, phiCoeffs);
        writeReverseLocationModel(indent, reverseOrder, xCoeffs, yCoeffs);


        --indent;
        println(slmTags[1]);
        --indent;
        println(gpTags[1]);
    }

    private int writeFXYCoordRefSystem(final FXYGeoCoding fxyGeoCoding, int indent) {
        final String[] crsTags = createTags(indent, DimapProductConstants.TAG_COORDINATE_REFERENCE_SYSTEM);
        println(crsTags[0]);
        indent++;
        indent = writeDatum(fxyGeoCoding.getDatum(), indent);
        --indent;
        println(crsTags[1]);
        --indent;
        return indent;
    }

    private int writeDatum(Datum datum, int indent) {
        final String[] horizontalCsTags = createTags(indent, DimapProductConstants.TAG_HORIZONTAL_CS);
        println(horizontalCsTags[0]);
        indent++;
        printLine(indent, DimapProductConstants.TAG_HORIZONTAL_CS_TYPE, "GEOGRAPHIC");
        final String[] geographicCsTags = createTags(indent, DimapProductConstants.TAG_GEOGRAPHIC_CS);
        println(geographicCsTags[0]);
        indent++;
        final String[] horizontalDatumTags = createTags(indent, DimapProductConstants.TAG_HORIZONTAL_DATUM);
        println(horizontalDatumTags[0]);
        indent++;
        printLine(indent, DimapProductConstants.TAG_HORIZONTAL_DATUM_NAME, datum.getName());
        final String[] ellipsoidTags = createTags(indent, DimapProductConstants.TAG_ELLIPSOID);
        println(ellipsoidTags[0]);
        indent++;
        final Ellipsoid ellipsoid = datum.getEllipsoid();
        printLine(indent, DimapProductConstants.TAG_ELLIPSOID_NAME, ellipsoid.getName());
        final String[] ellipsoidParametersTags = createTags(indent, DimapProductConstants.TAG_ELLIPSOID_PARAMETERS);
        println(ellipsoidParametersTags[0]);
        indent++;
        final String[][] ellipsoidAttrib = new String[][]{new String[]{DimapProductConstants.ATTRIB_UNIT, "M"}};
        printLine(indent, DimapProductConstants.TAG_ELLIPSOID_MAJ_AXIS,
                  ellipsoidAttrib, String.valueOf(ellipsoid.getSemiMajor()));
        printLine(indent, DimapProductConstants.TAG_ELLIPSOID_MIN_AXIS,
                  ellipsoidAttrib, String.valueOf(ellipsoid.getSemiMinor()));
        println(ellipsoidParametersTags[1]);
        --indent;
        println(ellipsoidTags[1]);
        --indent;
        println(horizontalDatumTags[1]);
        --indent;
        println(geographicCsTags[1]);
        --indent;
        println(horizontalCsTags[1]);
        return indent;
    }

    private void writeDirectLocationModel(int indent, final int order, final double[] lambdaCoeffs,
                                          final double[] phiCoeffs) {
        final String[][] attributes = new String[][]{
                new String[]{
                        DimapProductConstants.ATTRIB_ORDER, String.valueOf(order)
                }
        };
        final String[] dlmTags = createTags(indent, DimapProductConstants.TAG_DIRECT_LOCATION_MODEL, attributes);
        println(dlmTags[0]);
        indent++;
        final String[] lclTags = createTags(indent, DimapProductConstants.TAG_LC_LIST);
        println(lclTags[0]);
        indent++;
        writeCoeffsToList(indent, lambdaCoeffs, DimapProductConstants.TAG_LC);
        --indent;
        println(lclTags[1]);

        final String[] pclTags = createTags(indent, DimapProductConstants.TAG_PC_LIST);
        println(pclTags[0]);
        indent++;
        writeCoeffsToList(indent, phiCoeffs, DimapProductConstants.TAG_PC);
        --indent;
        println(pclTags[1]);
        --indent;
        println(dlmTags[1]);
    }

    private void writeReverseLocationModel(int indent, final int order, final double[] xCoeffs,
                                           final double[] yCoeffs) {
        final String[][] attributes = new String[][]{
                new String[]{
                        DimapProductConstants.ATTRIB_ORDER, String.valueOf(order)
                }
        };
        final String[] rlmTags = createTags(indent, DimapProductConstants.TAG_REVERSE_LOCATION_MODEL, attributes);
        println(rlmTags[0]);
        indent++;
        final String[] iclTags = createTags(indent, DimapProductConstants.TAG_IC_LIST);
        println(iclTags[0]);
        indent++;
        writeCoeffsToList(indent, xCoeffs, DimapProductConstants.TAG_IC);
        --indent;
        println(iclTags[1]);

        final String[] jclTags = createTags(indent, DimapProductConstants.TAG_JC_LIST);
        println(jclTags[0]);
        indent++;
        writeCoeffsToList(indent, yCoeffs, DimapProductConstants.TAG_JC);
        --indent;
        println(jclTags[1]);
        --indent;
        println(rlmTags[1]);
    }

    private void writeCoeffsToList(int indent, final double[] phiCoeffs, final String tagListElement) {
        final String[][] attributes = new String[][]{new String[]{DimapProductConstants.ATTRIB_INDEX, ""}};
        for (int i = 0; i < phiCoeffs.length; i++) {
            attributes[0][1] = String.valueOf(i);
            printLine(indent, tagListElement, attributes, String.valueOf(phiCoeffs[i]));
        }
    }

    protected void writeProductionElements(int indent) {
        final String[] productionTags = createTags(indent, DimapProductConstants.TAG_PRODUCTION);
        println(productionTags[0]);
        printLine(indent + 1, DimapProductConstants.TAG_DATASET_PRODUCER_NAME,
                  DimapProductConstants.DATASET_PRODUCER_NAME);
        printLine(indent + 1, DimapProductConstants.TAG_PRODUCT_TYPE, product.getProductType());
        final ProductData.UTC sceneRasterStartTime = product.getStartTime();
        if (sceneRasterStartTime != null) {
            printLine(indent + 1, DimapProductConstants.TAG_PRODUCT_SCENE_RASTER_START_TIME,
                      sceneRasterStartTime.format());
        }
        final ProductData.UTC sceneRasterStopTime = product.getEndTime();
        if (sceneRasterStopTime != null) {
            printLine(indent + 1, DimapProductConstants.TAG_PRODUCT_SCENE_RASTER_STOP_TIME,
                      sceneRasterStopTime.format());
        }
        if(product.getQuicklookBandName() != null) {
            printLine(indent + 1, DimapProductConstants.TAG_QUICKLOOK_BAND_NAME,
                      product.getQuicklookBandName());
        }
        println(productionTags[1]);
    }

    protected void writeMetadataId(int indent) {
        final String[] idTags = createTags(indent, DimapProductConstants.TAG_METADATA_ID);
        println(idTags[0]);
        final String[][] attributes = new String[1][];
        attributes[0] = new String[]{
                DimapProductConstants.ATTRIB_VERSION,
                DimapProductConstants.DIMAP_CURRENT_VERSION
        };
        printLine(indent + 1, DimapProductConstants.TAG_METADATA_FORMAT, attributes, "DIMAP");
        printLine(indent + 1, DimapProductConstants.TAG_METADATA_PROFILE, DimapProductConstants.DIMAP_METADATA_PROFILE);
        println(idTags[1]);
    }

    protected void writeDatasetId(int indent) {
        final String[] idTags = createTags(indent, DimapProductConstants.TAG_DATASET_ID);
        println(idTags[0]);
        printLine(indent + 1, DimapProductConstants.TAG_DATASET_SERIES, DimapProductConstants.DIMAP_DATASET_SERIES);
        printLine(indent + 1, DimapProductConstants.TAG_DATASET_NAME, product.getName());
        println(idTags[1]);
    }

    protected void writeDatasetUse(int indent) {
        final String description = product.getDescription();
        final Product.AutoGrouping autoGrouping = product.getAutoGrouping();
        if ((description != null && description.length() > 0) || autoGrouping != null) {
            final String[] idTags = createTags(indent, DimapProductConstants.TAG_DATASET_USE);
            println(idTags[0]);
            if (description != null && description.length() > 0) {
                printLine(indent + 1, DimapProductConstants.TAG_DATASET_COMMENTS, description);
            }
            if (autoGrouping != null) {
                printLine(indent + 1, DimapProductConstants.TAG_DATASET_AUTO_GROUPING, autoGrouping.toString());
            }
            println(idTags[1]);
        }
    }
}
