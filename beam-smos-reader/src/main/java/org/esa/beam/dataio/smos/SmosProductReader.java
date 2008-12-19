/*
 * $Id: $
 *
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.dataio.smos;

import com.bc.ceres.binio.*;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.maptransf.IdentityTransformDescriptor;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.MapProjectionRegistry;
import org.esa.beam.glevel.TiledFileMultiLevelSource;
import org.esa.beam.util.io.FileUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import javax.media.jai.JAI;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Random;

public class SmosProductReader extends AbstractProductReader {
    private static final String SMOS_DGG_DIR_PROPERTY_NAME = "org.esa.beam.pview.smosDggDir";

    private static MultiLevelImage dggridMultiLevelImage;

    private SmosFile smosFile;
    private Rectangle2D region;

    SmosProductReader(final SmosProductReaderPlugIn productReaderPlugIn) {
        super(productReaderPlugIn);
    }

    public SmosFile getSmosFile() {
        return smosFile;
    }

    @Override
    protected synchronized Product readProductNodesImpl() throws IOException {
        if (dggridMultiLevelImage == null) {
            String dirPath = System.getProperty(SMOS_DGG_DIR_PROPERTY_NAME);
            if (dirPath == null || !new File(dirPath).exists()) {
                throw new IOException(
                        MessageFormat.format(
                                "SMOS products require a DGG image.\nPlease set system property ''{0}''to a valid DGG image directory.",
                                SMOS_DGG_DIR_PROPERTY_NAME));
            }

            try {
                MultiLevelSource dggridMultiLevelSource = TiledFileMultiLevelSource.create(new File(dirPath), false);
                dggridMultiLevelImage = new DefaultMultiLevelImage(dggridMultiLevelSource);
            } catch (IOException e) {
                throw new IOException(MessageFormat.format("Failed to load SMOS DDG ''{0}''", dirPath), e);
            }
        }

        final File inputFile = getInputFile();
        final File hdrFile = FileUtils.exchangeExtension(inputFile, ".HDR");
        final File dblFile = FileUtils.exchangeExtension(inputFile, ".DBL");

        final DataFormat format = SmosFormats.getFormat(hdrFile);
        if (format == null) {
            throw new IOException(MessageFormat.format("File ''{0}'': Unknown SMOS data format", inputFile));
        }

        return createProduct(hdrFile, dblFile, format);
    }

    @Override
    protected synchronized void readBandRasterDataImpl(int sourceOffsetX,
                                                       int sourceOffsetY,
                                                       int sourceWidth,
                                                       int sourceHeight,
                                                       int sourceStepX,
                                                       int sourceStepY,
                                                       Band destBand,
                                                       int destOffsetX,
                                                       int destOffsetY,
                                                       int destWidth,
                                                       int destHeight,
                                                       ProductData destBuffer,
                                                       ProgressMonitor pm) throws IOException {
        final RenderedImage image = destBand.getSourceImage();
        final Raster data = image.getData(new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight));
        data.getDataElements(destOffsetX, destOffsetY, destWidth, destHeight, destBuffer.getElems());
    }

    @Override
    public void close() throws IOException {
        smosFile.close();
        super.close();
    }

    private Product createProduct(File hdrFile, File dblFile, DataFormat format) throws IOException {
        final int sceneWidth = dggridMultiLevelImage.getWidth();
        final int sceneHeight = dggridMultiLevelImage.getHeight();

        final String productName = FileUtils.getFilenameWithoutExtension(hdrFile);
        final String productType = format.getName().substring(12, 22);
        final Product product = new Product(productName, productType, sceneWidth, sceneHeight);

        addMetadata(product.getMetadataRoot(), hdrFile);
        product.setPreferredTileSize(512, 512);
        product.setFileLocation(dblFile);
        product.setGeoCoding(createGeoCoding(product));
        addGridCellIdBand(product);

        final String formatName = format.getName();

        if (formatName.contains("MIR_BWLD1C")
                || formatName.contains("MIR_BWND1C")
                || formatName.contains("MIR_BWSD1C")) {
            addL1cFlagCoding(product);
            smosFile = new L1cBrowseSmosFile(dblFile, format);
            addDualPolBands(product, ((L1cSmosFile) smosFile).getBtDataType());
        } else if (formatName.contains("MIR_BWLF1C")
                || formatName.contains("MIR_BWNF1C")
                || formatName.contains("MIR_BWSF1C")) {
            addL1cFlagCoding(product);
            smosFile = new L1cBrowseSmosFile(dblFile, format);
            addFullPolBrowseBands(product, ((L1cSmosFile) smosFile).getBtDataType());
        } else if (formatName.contains("MIR_SCLD1C")
                || formatName.contains("MIR_SCSD1C")) {
            addL1cFlagCoding(product);
            smosFile = new L1cScienceSmosFile(dblFile, format);
            addDualPolBands(product, ((L1cSmosFile) smosFile).getBtDataType());
        } else if (formatName.contains("MIR_SCLF1C")
                || formatName.contains("MIR_SCSF1C")) {
            addL1cFlagCoding(product);
            smosFile = new L1cScienceSmosFile(dblFile, format);
            addFullPolScienceBands(product, ((L1cSmosFile) smosFile).getBtDataType());
        } else if (formatName.contains("MIR_OSUDP2")
                || formatName.contains("MIR_SMUDP2")) {
            smosFile = new SmosFile(dblFile, format);
            addSmosL2BandsFromCompound(product, smosFile.getGridPointType());
        } else {
            throw new IllegalStateException("Illegal SMOS format: " + formatName);
        }

        return product;
    }

    private void addDualPolBands(Product product, CompoundType compoundDataType) {
        final CompoundMember[] members = compoundDataType.getMembers();

        for (int fieldIndex = 0; fieldIndex < members.length; fieldIndex++) {
            final CompoundMember member = members[fieldIndex];
            final String memberName = member.getName();
            final BandInfo bandInfo = BandInfoRegistry.getInstance().getBandInfo(memberName);

            if (bandInfo != null) {
                if ("Flags".equals(memberName)) {
                    // flags do not depend on polarisation mode, so there is a single flag band only
                    addL1cBand(product, memberName,
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex,
                               SmosFormats.L1C_POL_MODE_X);
                } else {
                    addL1cBand(product, memberName + "_X",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex,
                               SmosFormats.L1C_POL_MODE_X);
                    addL1cBand(product, memberName + "_Y",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex,
                               SmosFormats.L1C_POL_MODE_Y);
                }
            }
        }
    }

    private void addFullPolBrowseBands(Product product, CompoundType compoundDataType) {
        final CompoundMember[] members = compoundDataType.getMembers();

        for (int fieldIndex = 0; fieldIndex < members.length; fieldIndex++) {
            final CompoundMember member = members[fieldIndex];
            final String memberName = member.getName();
            final BandInfo bandInfo = BandInfoRegistry.getInstance().getBandInfo(memberName);

            if (bandInfo != null) {
                if ("Flags".equals(memberName)) {
                    // flags do not depend on polarisation mode, so there is a single flag band only
                    addL1cBand(product, memberName,
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex,
                               SmosFormats.L1C_POL_MODE_X);
                    continue;
                }
                if ("BT_Value".equals(memberName)) {
                    addL1cBand(product, memberName + "_X",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex,
                               SmosFormats.L1C_POL_MODE_X);
                    addL1cBand(product, memberName + "_Y",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex,
                               SmosFormats.L1C_POL_MODE_Y);
                    addL1cBand(product, memberName + "_XY_Real",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex,
                               SmosFormats.L1C_POL_MODE_XY1);
                    addL1cBand(product, memberName + "_XY_Imag",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex,
                               SmosFormats.L1C_POL_MODE_XY2);
                } else {
                    addL1cBand(product, memberName + "_X",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex,
                               SmosFormats.L1C_POL_MODE_X);
                    addL1cBand(product, memberName + "_Y",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex,
                               SmosFormats.L1C_POL_MODE_Y);
                    addL1cBand(product, memberName + "_XY",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex,
                               SmosFormats.L1C_POL_MODE_XY1);
                }
            }
        }
    }

    private void addFullPolScienceBands(Product product, CompoundType compoundDataType) {
        final CompoundMember[] members = compoundDataType.getMembers();

        for (int fieldIndex = 0; fieldIndex < members.length; fieldIndex++) {
            final CompoundMember member = members[fieldIndex];
            final String memberName = member.getName();
            final BandInfo bandInfo = BandInfoRegistry.getInstance().getBandInfo(memberName);

            if (bandInfo != null) {
                if ("Flags".equals(memberName)) {
                    // flags do not depend on polarisation mode, so there is a single flag band only
                    addL1cBand(product, memberName,
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex,
                               SmosFormats.L1C_POL_MODE_X);
                    continue;
                }
                if ("BT_Value_Real".equals(memberName)) {
                    addL1cBand(product, "BT_Value_X",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex,
                               SmosFormats.L1C_POL_MODE_X);
                    addL1cBand(product, "BT_Value_Y",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex,
                               SmosFormats.L1C_POL_MODE_Y);
                    addL1cBand(product, "BT_Value_XY_Real",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex,
                               SmosFormats.L1C_POL_MODE_XY1);
                    continue;
                }
                if ("BT_Value_Imag".equals(memberName)) {
                    addL1cBand(product, "BT_Value_XY_Imag",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex,
                               SmosFormats.L1C_POL_MODE_XY1);
                } else {
                    addL1cBand(product, memberName + "_X",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex,
                               SmosFormats.L1C_POL_MODE_X);
                    addL1cBand(product, memberName + "_Y",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex,
                               SmosFormats.L1C_POL_MODE_Y);
                    addL1cBand(product, memberName + "_XY",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex,
                               SmosFormats.L1C_POL_MODE_XY1);
                }
            }
        }
    }

    private void addSmosL2BandsFromCompound(Product product, CompoundType compoundDataType) {
        final CompoundMember[] members = compoundDataType.getMembers();

        for (int fieldIndex = 0; fieldIndex < members.length; fieldIndex++) {
            final CompoundMember member = members[fieldIndex];
            final String memberName = member.getName();
            // todo - band info
            BandInfo bandInfo = new BandInfo(memberName);
            addL2Band(product, memberName, memberTypeToBandType(member.getType()), bandInfo, fieldIndex);
        }
    }

    private void addL1cBand(Product product, String bandName, int bandType, BandInfo bandInfo, int fieldIndex, int polMode) {
        final GridPointValueProvider valueProvider =
                new L1cFieldValueProvider((L1cSmosFile) smosFile, fieldIndex, polMode);
        final Band band = addBand(product, bandName, bandType, bandInfo, valueProvider);

        if (bandName.equals("Flags")) {
            addFlagCodingAndBitmaskDefs(band, product);
        }
    }

    private void addL2Band(Product product, String bandName, int bandType, BandInfo bandInfo, int fieldIndex) {
        addBand(product, bandName, bandType, bandInfo, new L2FieldValueProvider(smosFile, fieldIndex));
    }

    private File getInputFile() {
        final Object input = getInput();

        if (input instanceof String) {
            return new File((String) input);
        }
        if (input instanceof File) {
            return (File) input;
        }

        throw new IllegalArgumentException(MessageFormat.format("Unsupported input: {0}", input));
    }

    private static GeoCoding createGeoCoding(Product product) {
        final MapInfo mapInfo = new MapInfo(MapProjectionRegistry.getProjection(IdentityTransformDescriptor.NAME),
                                            0.0f, 0.0f,
                                            -180.0f, +90.0f,
                                            360.0f / product.getSceneRasterWidth(),
                                            180.0f / product.getSceneRasterHeight(),
                                            Datum.WGS_84);
        mapInfo.setSceneWidth(product.getSceneRasterWidth());
        mapInfo.setSceneHeight(product.getSceneRasterHeight());

        return new MapGeoCoding(mapInfo);
    }

    private static void addL1cFlagCoding(Product product) {
        final FlagCoding flagCoding = new FlagCoding("SMOS_L1C");

        for (final FlagDescriptor descriptor : SmosFormats.L1C_FLAGS) {
            // skip polarisation flags since they are not meaningful
            if ((descriptor.getMask() & SmosFormats.L1C_POL_FLAGS_MASK) == 0) {
                flagCoding.addFlag(descriptor.getName(), descriptor.getMask(), descriptor.getDescription());
            }
        }

        product.getFlagCodingGroup().add(flagCoding);
    }

    private static void addGridCellIdBand(Product product) {
        final BandInfo bandInfo = new BandInfo("Grid_Cell_ID", "", 0.0, 1.0, -999, 0, 1 << 31,
                                               "Unique identifier for Earth fixed grid point (ISEA4H9 DGG).");
        final Band band = product.addBand(bandInfo.name, ProductData.TYPE_UINT32);

        band.setDescription(bandInfo.description);
        band.setSourceImage(dggridMultiLevelImage);
        band.setImageInfo(createDefaultImageInfo(bandInfo));
    }

    private Band addBand(Product product, String bandName, int bandType, BandInfo bandInfo,
                                GridPointValueProvider valueProvider) {
        final Band band = product.addBand(bandName, bandType);
        band.setScalingFactor(bandInfo.scaleFactor);
        band.setScalingOffset(bandInfo.scaleOffset);
        band.setUnit(bandInfo.unit);
        band.setDescription(bandInfo.description);

        if (bandInfo.noDataValue != null) {
            band.setNoDataValueUsed(true);
            band.setNoDataValue(bandInfo.noDataValue.doubleValue());
        }

        band.setSourceImage(createSourceImage(valueProvider, band));
        band.setImageInfo(createDefaultImageInfo(bandInfo));

        return band;
    }

    private static void addFlagCodingAndBitmaskDefs(Band band, Product product) {
        final FlagCoding flagCoding = product.getFlagCodingGroup().get(0);
        band.setSampleCoding(flagCoding);

        final String bandName = band.getName();
        final Random random = new Random(5489);

        for (final MetadataAttribute flag : flagCoding.getAttributes()) {
            final String name = bandName + "_" + flag.getName();
            final String expr = bandName + " != " + band.getNoDataValue() + " && " + bandName + "." + flag;
            final Color color = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
            final BitmaskDef bitmaskDef = new BitmaskDef(name, flag.getDescription(), expr, color, 0.5f);

            product.addBitmaskDef(bitmaskDef);
        }
    }

    private static int memberTypeToBandType(Type type) {
        int bandType;

        if (type.equals(SimpleType.BYTE)) {
            bandType = ProductData.TYPE_INT8;
        } else if (type.equals(SimpleType.UBYTE)) {
            bandType = ProductData.TYPE_UINT8;
        } else if (type.equals(SimpleType.SHORT)) {
            bandType = ProductData.TYPE_INT16;
        } else if (type.equals(SimpleType.USHORT)) {
            bandType = ProductData.TYPE_UINT16;
        } else if (type.equals(SimpleType.INT)) {
            bandType = ProductData.TYPE_INT32;
        } else if (type.equals(SimpleType.UINT)) {
            bandType = ProductData.TYPE_UINT32;
        } else if (type.equals(SimpleType.FLOAT)) {
            bandType = ProductData.TYPE_FLOAT32;
        } else if (type.equals(SimpleType.DOUBLE)) {
            bandType = ProductData.TYPE_FLOAT64;
        } else {
            throw new IllegalStateException("type = " + type);
        }

        return bandType;
    }

    private MultiLevelImage createSourceImage(GridPointValueProvider valueProvider, Band band) {
        return new DefaultMultiLevelImage(new SmosMultiLevelSource(valueProvider, dggridMultiLevelImage, band));
    }

    private static ImageInfo createDefaultImageInfo(BandInfo bandInfo) {
        final Color[] colors = new Color[]{
                new Color(0, 0, 0),
                new Color(85, 0, 136),
                new Color(0, 0, 255),
                new Color(0, 255, 255),
                new Color(0, 255, 0),
                new Color(255, 255, 0),
                new Color(255, 140, 0),
                new Color(255, 0, 0)
        };
        final ColorPaletteDef.Point[] points = new ColorPaletteDef.Point[colors.length];
        for (int i = 0; i < colors.length; i++) {
            final double sample = bandInfo.min + ((bandInfo.max - bandInfo.min) * i / (colors.length - 1));
            points[i] = new ColorPaletteDef.Point(sample, colors[i]);
        }

        return new ImageInfo(new ColorPaletteDef(points));
    }

    private static void addMetadata(MetadataElement metadataElement, File hdrFile) throws IOException {
        final Document document;

        try {
            document = new SAXBuilder().build(hdrFile);
        } catch (JDOMException e) {
            throw new IOException(MessageFormat.format(
                    "File ''{0}'': Invalid document", hdrFile.getPath()), e);
        }

        final Namespace namespace = document.getRootElement().getNamespace();
        if (namespace == null) {
            throw new IOException(MessageFormat.format(
                    "File ''{0}'': Missing namespace", hdrFile.getPath()));
        }

        addMetadata(metadataElement, document.getRootElement(), namespace);
    }

    private static void addMetadata(MetadataElement metadataElement, Element xmlElement, Namespace namespace) {
        for (final Object o : xmlElement.getChildren()) {
            final Element xmlChild = (Element) o;

            if (xmlChild.getChildren(null, namespace).size() == 0) {
                final String s = xmlChild.getTextNormalize();
                if (s != null && !s.isEmpty()) {
                    metadataElement.addAttribute(
                            new MetadataAttribute(xmlChild.getName(), ProductData.createInstance(s), true));
                }
            } else {
                MetadataElement mdChild = new MetadataElement(xmlChild.getName());
                metadataElement.addElement(mdChild);
                addMetadata(mdChild, xmlChild, namespace);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(512 * (1024 * 1024));
        SmosProductReader smosProductReader = new SmosProductReader(new SmosProductReaderPlugIn());
        final File dir = new File(args[0]);
        final File file = new File(dir, dir.getName() + ".DBL");
        Product product = smosProductReader.readProductNodes(file, null);
        ProductIO.writeProduct(product, "smosproduct_2.dim", null);
    }
}
