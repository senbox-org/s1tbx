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
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class SmosProductReader extends AbstractProductReader {
    private static final String SMOS_DGG_DIR_PROPERTY_NAME = "org.esa.beam.pview.smosDggDir";

    private static MultiLevelImage dggridMultiLevelImage;

    private SmosFile smosFile;
    HashMap<String, BandInfo> bandInfos = new HashMap<String, BandInfo>(17);

    SmosProductReader(final SmosProductReaderPlugIn productReaderPlugIn) {
        super(productReaderPlugIn);
        registerBandInfo("Grid_Cell_ID", "", 0.0, 1.0, -999, 0, 1 << 31,
                         "Unique identifier for Earth fixed grid point (ISEA4H9 DGG).");
        registerBandInfo("Flags", "", 0.0, 1.0, -1, 0, 1 << 16,
                         "L1c flags applicable to the pixel for this " +
                                 "particular integration time.");
        registerBandInfo("BT_Value", "K", 0.0, 1.0, -999, 50.0, 350.0,
                         "Brightness temperature measurement over current " +
                                 "Earth fixed grid point, obtained by DFT " +
                                 "interpolation from L1b data.");
        registerBandInfo("BT_Value_Real", "K", 0.0, 1.0, -999, 50.0, 350.0,
                         "Real component of HH, HV or VV polarisation brightness temperature measurement over current " +
                                 "Earth fixed grid point, obtained by DFT " +
                                 "interpolation from L1b data.");
        registerBandInfo("BT_Value_Imag", "K", 0.0, 1.0, -999, 50.0, 350.0,
                         "Imaginary component of HH, HV or VV polarisation brightness temperature measurement over current " +
                                 "Earth fixed grid point, obtained by DFT " +
                                 "interpolation from L1b data.");
        registerBandInfo("Pixel_Radiometric_Accuracy", "K", 0.0, 50.0 / (1 << 16), -999, 0.0, 5.0,
                         "Error accuracy measurement in the Brightness " +
                                 "Temperature presented in the previous field, " +
                                 "extracted in the direction of the pixel.");
        registerBandInfo("Radiometric_Accuracy_of_Pixel", "K", 0.0, 50.0 / (1 << 16), -999, 0.0, 5.0,
                         "Error accuracy measurement in the Brightness " +
                                 "Temperature presented in the previous field, " +
                                 "extracted in the direction of the pixel.");
        registerBandInfo("Incidence_Angle", "deg", 0.0, 90.0 / (1 << 16), -999, 0.0, 90.0,
                         "Incidence angle value corresponding to the " +
                                 "measured BT value over current Earth fixed " +
                                 "grid point. Measured as angle from pixel to " +
                                 "S/C with respect to the pixel local normal (0� " +
                                 "if vertical)");
        registerBandInfo("Azimuth_Angle", "deg", 0.0, 360.0 / (1 << 16), -999, 0.0, 360.0,
                         "Azimuth angle value corresponding to the " +
                                 "measured BT value over current Earth fixed " +
                                 "grid point. Measured as angle in pixel local " +
                                 "tangent plane from projected pixel to S/C " +
                                 "direction with respect to the local North (0� if" +
                                 "local North)");
        registerBandInfo("Faraday_Rotation_Angle", "deg", 0.0, 360.0 / (1 << 16), -999, 0.0, 360.0,
                         "Faraday rotation angle value corresponding " +
                                 "to the measured BT value over current Earth " +
                                 "fixed grid point. It is computed as the rotation " +
                                 "from antenna to surface (i.e. inverse angle)");
        registerBandInfo("Geometric_Rotation_Angle", "deg", 0.0, 360.0 / (1 << 16), -999, 0.0, 360.0,
                         "Geometric rotation angle value " +
                                 "corresponding to the measured BT value " +
                                 "over current Earth fixed grid point. It is " +
                                 "computed as the rotation from surface to " +
                                 "antenna (i.e. direct angle).");
        registerBandInfo("Footprint_Axis1", "km", 0.0, 100.0 / (1 << 16), -999, 20.0, 30.0,
                         "Elliptical footprint major semi-axis value.");
        registerBandInfo("Footprint_Axis2", "km", 0.0, 100.0 / (1 << 16), -999, 20.0, 30.0,
                         "Elliptical footprint minor semi-axis value.");

        // todo - band info for level 2 (rq-20081208)
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
        final String formatName = format.getName();

        // todo - clean-up, see below
        if (formatName.contains("MIR_BWLD1C")
                || formatName.contains("MIR_BWND1C")
                || formatName.contains("MIR_BWSD1C")) {
            smosFile = new L1cBrowseSmosFile(dblFile, format);
        } else if (formatName.contains("MIR_BWLF1C")
                || formatName.contains("MIR_BWNF1C")
                || formatName.contains("MIR_BWSF1C")) {
            smosFile = new L1cBrowseSmosFile(dblFile, format);
        } else if (formatName.contains("MIR_SCLD1C")
                || formatName.contains("MIR_SCSD1C")) {
            smosFile = new L1cScienceSmosFile(dblFile, format);
        } else if (formatName.contains("MIR_SCLF1C")
                || formatName.contains("MIR_SCSF1C")) {
            smosFile = new L1cScienceSmosFile(dblFile, format);
        } else if (formatName.contains("MIR_OSUDP2")
                || formatName.contains("MIR_SMUDP2")) {
            smosFile = new L2SmosFile(dblFile, format);
        } else {
            throw new IllegalStateException("Illegal SMOS format: " + formatName);
        }

        final int sceneWidth = dggridMultiLevelImage.getWidth();
        final int sceneHeight = dggridMultiLevelImage.getHeight();

        final String productName = FileUtils.getFilenameWithoutExtension(dblFile);
        final String productType = format.getName().substring(12, 22);

        final Product product = new Product(productName, productType, sceneWidth, sceneHeight);
        addMetadata(hdrFile, product.getMetadataRoot());
        product.setPreferredTileSize(512, 512);
        product.setFileLocation(dblFile);
        product.setGeoCoding(createGeoCoding(product));

        // todo - L2 flags
        if (smosFile instanceof L1cSmosFile) {
            final FlagCoding flagCoding = new FlagCoding("SMOS_L1C");
            for (final SmosFormats.FlagDescriptor descriptor : SmosFormats.L1C_FLAGS) {
                flagCoding.addFlag(descriptor.getName(), descriptor.getMask(), descriptor.getDescription());
            }
            product.getFlagCodingGroup().add(flagCoding);
        }

        addGridCellIdBand(product);

        if (smosFile instanceof L1cSmosFile) {
            L1cSmosFile l1cSmosFile = (L1cSmosFile) smosFile;
            CompoundType btDataType = l1cSmosFile.getBtDataType();
            if (formatName.contains("MIR_BWLD1C")
                    || formatName.contains("MIR_BWND1C")
                    || formatName.contains("MIR_BWSD1C")) {
                addDualPolBands(product, btDataType);
            } else if (formatName.contains("MIR_BWLF1C")
                    || formatName.contains("MIR_BWNF1C")
                    || formatName.contains("MIR_BWSF1C")) {
                addFullPolBrowseBands(product, btDataType);
            } else if (formatName.contains("MIR_SCLD1C")
                    || formatName.contains("MIR_SCSD1C")) {
                addDualPolBands(product, btDataType);
            } else if (formatName.contains("MIR_SCLF1C")
                    || formatName.contains("MIR_SCSF1C")) {
                addFullPolScienceBands(product, btDataType);
            }
        } else {
            addSmosL2BandsFromCompound(product, ((L2SmosFile) smosFile).getRetrievalResultsDataType());
        }

        return product;
    }

    public SmosFile getSmosFile() {
        return smosFile;
    }

    public MultiLevelImage getDggridMultiLevelImage() {
        return dggridMultiLevelImage;
    }

    private void addDualPolBands(Product product, CompoundType compoundDataType) {
        final CompoundMember[] members = compoundDataType.getMembers();

        for (int fieldIndex = 0; fieldIndex < members.length; fieldIndex++) {
            final CompoundMember member = members[fieldIndex];
            final String memberName = member.getName();
            final BandInfo bandInfo = bandInfos.get(memberName);

            if (bandInfo != null) {
                if ("Flags".equals(memberName)) {
                    // flags do not depend on polarisation mode, so there is a single flag band only
                    addL1cBand(product, memberName,
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex, SmosFile.POL_MODE_X);
                } else {
                    addL1cBand(product, memberName + "_X",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex, SmosFile.POL_MODE_X);
                    addL1cBand(product, memberName + "_Y",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex, SmosFile.POL_MODE_Y);
                }
            }
        }
    }

    private void addFullPolBrowseBands(Product product, CompoundType compoundDataType) {
        final CompoundMember[] members = compoundDataType.getMembers();

        for (int fieldIndex = 0; fieldIndex < members.length; fieldIndex++) {
            final CompoundMember member = members[fieldIndex];
            final String memberName = member.getName();
            final BandInfo bandInfo = bandInfos.get(memberName);

            if (bandInfo != null) {
                if ("Flags".equals(memberName)) {
                    // flags do not depend on polarisation mode, so there is a single flag band only
                    addL1cBand(product, memberName,
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex, SmosFile.POL_MODE_X);
                    continue;
                }
                if ("BT_Value".equals(memberName)) {
                    addL1cBand(product, memberName + "_X",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex, SmosFile.POL_MODE_X);
                    addL1cBand(product, memberName + "_Y",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex, SmosFile.POL_MODE_Y);
                    addL1cBand(product, memberName + "_XY_Real",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex, SmosFile.POL_MODE_XY1);
                    addL1cBand(product, memberName + "_XY_Imag",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex, SmosFile.POL_MODE_XY2);
                } else {
                    addL1cBand(product, memberName + "_X",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex, SmosFile.POL_MODE_X);
                    addL1cBand(product, memberName + "_Y",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex, SmosFile.POL_MODE_Y);
                    addL1cBand(product, memberName + "_XY",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex, SmosFile.POL_MODE_XY1);
                }
            }
        }
    }

    private void addFullPolScienceBands(Product product, CompoundType compoundDataType) {
        final CompoundMember[] members = compoundDataType.getMembers();

        for (int fieldIndex = 0; fieldIndex < members.length; fieldIndex++) {
            final CompoundMember member = members[fieldIndex];
            final String memberName = member.getName();
            final BandInfo bandInfo = bandInfos.get(memberName);

            if (bandInfo != null) {
                if ("Flags".equals(memberName)) {
                    // flags do not depend on polarisation mode, so there is a single flag band only
                    addL1cBand(product, memberName,
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex, SmosFile.POL_MODE_X);
                    continue;
                }
                if ("BT_Value_Real".equals(memberName)) {
                    addL1cBand(product, "BT_Value_X",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex, SmosFile.POL_MODE_X);
                    addL1cBand(product, "BT_Value_Y",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex, SmosFile.POL_MODE_Y);
                    addL1cBand(product, "BT_Value_XY_Real",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex, SmosFile.POL_MODE_XY1);
                    continue;
                }
                if ("BT_Value_Imag".equals(memberName)) {
                    addL1cBand(product, "BT_Value_XY_Imag",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex, SmosFile.POL_MODE_XY1);
                } else {
                    addL1cBand(product, memberName + "_X",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex, SmosFile.POL_MODE_X);
                    addL1cBand(product, memberName + "_Y",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex, SmosFile.POL_MODE_Y);
                    addL1cBand(product, memberName + "_XY",
                               memberTypeToBandType(member.getType()), bandInfo, fieldIndex, SmosFile.POL_MODE_XY1);
                }
            }
        }
    }

    private void addSmosL2BandsFromCompound(Product product, CompoundType compoundDataType) {
        CompoundMember[] members = compoundDataType.getMembers();
        for (int fieldIndex = 0; fieldIndex < members.length; fieldIndex++) {
            CompoundMember member = members[fieldIndex];
            String bandName = member.getName();
            // todo - band info
            BandInfo bandInfo = new BandInfo(bandName);
            addL2Band(product, fieldIndex, bandName, memberTypeToBandType(member.getType()), bandInfo);
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
            throw new IllegalStateException("type=" + type);
        }
        return bandType;
    }

    private void addL1cBand(Product product, String bandName, int bandType, BandInfo bandInfo, int fieldIndex,
                            int polarisation) {
        final Band band = product.addBand(bandName, bandType);
        band.setScalingFactor(bandInfo.scaleFactor);
        band.setScalingOffset(bandInfo.scaleOffset);
        band.setUnit(bandInfo.unit);
        band.setDescription(bandInfo.description);

        if (bandInfo.noDataValue != null) {
            band.setNoDataValueUsed(true);
            band.setNoDataValue(bandInfo.noDataValue.doubleValue());
        }

        // todo - cleanup
        final FlagCoding flagCoding = product.getFlagCodingGroup().get("SMOS");
        if (bandName.startsWith("Flags") && flagCoding != null) {
            band.setSampleCoding(flagCoding);

            final Random random = new Random(5489);
            for (final String flagName : flagCoding.getFlagNames()) {
                final String name = bandName + "_" + flagName;
                final String expr = bandName + " != " + band.getNoDataValue() + " && " + bandName + "." + flagName;
                final Color color = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
                final BitmaskDef bitmaskDef = new BitmaskDef(name, name, expr, color, 0.5f);
                product.addBitmaskDef(bitmaskDef);
            }
        }

        final GridPointValueProvider valueProvider =
                new L1cGridPointValueProvider((L1cSmosFile) smosFile, fieldIndex, polarisation);

        band.setSourceImage(createSourceImage(valueProvider, band));
        band.setImageInfo(createDefaultImageInfo(bandInfo));
    }

    private void addL2Band(Product product,
                           int fieldIndex,
                           String bandName,
                           int bandType,
                           BandInfo bandInfo) {
        final Band band = product.addBand(bandName, bandType);
        band.setScalingFactor(bandInfo.scaleFactor);
        band.setScalingOffset(bandInfo.scaleOffset);
        band.setUnit(bandInfo.unit);
        band.setDescription(bandInfo.description);

        if (bandInfo.noDataValue != null) {
            band.setNoDataValueUsed(true);
            band.setNoDataValue(bandInfo.noDataValue.doubleValue());
        }

        final GridPointValueProvider gpvp = new L2GridPointValueProvider(smosFile, fieldIndex);
        band.setSourceImage(createSourceImage(gpvp, band));
        band.setImageInfo(createDefaultImageInfo(bandInfo));
    }

    private ImageInfo createDefaultImageInfo(BandInfo bandInfo) {
        Color[] colors = new Color[]{
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
            points[i] = new ColorPaletteDef.Point(
                    bandInfo.min + ((bandInfo.max - bandInfo.min) * i / (colors.length - 1)), colors[i]);
        }
        final ColorPaletteDef def = new ColorPaletteDef(points);
        return new ImageInfo(def);
    }

    private void addGridCellIdBand(Product product) {
        final SmosProductReader.BandInfo bandInfo = bandInfos.get("Grid_Cell_ID");
        final Band band = product.addBand("Grid_Cell_ID", ProductData.TYPE_UINT32);

        band.setDescription(bandInfo.description);
        band.setSourceImage(dggridMultiLevelImage);
        band.setImageInfo(createDefaultImageInfo(bandInfo));
    }

    private GeoCoding createGeoCoding(Product product) {
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

    @Override
    public void close() throws IOException {
        smosFile.close();
        super.close();
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

    @Override
    protected synchronized void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY,
                                                       int sourceWidth, int sourceHeight, int sourceStepX,
                                                       int sourceStepY, Band destBand, int destOffsetX, int destOffsetY,
                                                       int destWidth, int destHeight, ProductData destBuffer,
                                                       ProgressMonitor pm) throws IOException {
        RenderedImage image = destBand.getSourceImage();
        java.awt.image.Raster data = image.getData(new Rectangle(destOffsetX,
                                                                 destOffsetY,
                                                                 destWidth,
                                                                 destHeight));
        data.getDataElements(destOffsetX, destOffsetY, destWidth, destHeight, destBuffer.getElems());
    }

    public static void main(String[] args) throws IOException {
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(512 * (1024 * 1024));
        SmosProductReader smosProductReader = new SmosProductReader(new SmosProductReaderPlugIn());
        final File dir = new File(args[0]);
        final File file = new File(dir, dir.getName() + ".DBL");
        Product product = smosProductReader.readProductNodes(file, null);
        ProductIO.writeProduct(product, "smosproduct_2.dim", null);
    }

    private static MultiLevelImage createSourceImage(GridPointValueProvider valueProvider, Band band) {
        return new DefaultMultiLevelImage(new SmosMultiLevelSource(valueProvider, dggridMultiLevelImage, band));
    }

    // todo - use this metadata in ceres-binio
    private static class BandInfo {
        String name;
        String unit = "";
        double scaleOffset;
        double scaleFactor;
        Number noDataValue;
        double min;
        double max;
        String description;

        private BandInfo(String name) {
            this(name, "", 0.0, 1.0, -999.0, 0.0, 10000.0, "");
        }

        private BandInfo(String name, String unit, double scaleOffset, double scaleFactor, Number noDataValue, double min, double max, String description) {
            this.name = name;
            this.unit = unit;
            this.scaleOffset = scaleOffset;
            this.scaleFactor = scaleFactor;
            this.noDataValue = noDataValue;
            this.min = min;
            this.max = max;
            this.description = description;
        }
    }

    private void registerBandInfo(String name, String unit, double scaleOffset, double scaleFactor, Number noDataValue, double min, double max, String description) {
        regBD(new BandInfo(name, unit, scaleOffset, scaleFactor, noDataValue, min, max, description));
    }

    private void regBD(BandInfo value) {
        bandInfos.put(value.name, value);
    }


    static void addMetadata(File hdrFile, MetadataElement mdRootElement) throws IOException {
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

        bibo(namespace, document.getRootElement(), mdRootElement);
    }

    private static void bibo(Namespace namespace, Element xmlElement, MetadataElement mdElement) {
        List children = xmlElement.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Element xmlChild = (Element) children.get(i);
            if (xmlChild.getChildren(null, namespace).size() == 0) {
                String s = xmlChild.getTextNormalize();
                if (s != null && !s.isEmpty()) {
                    mdElement.addAttribute(
                            new MetadataAttribute(xmlChild.getName(), ProductData.createInstance(s), true));
                }
            } else {
                MetadataElement mdChild = new MetadataElement(xmlChild.getName());
                mdElement.addElement(mdChild);
                bibo(namespace, xmlChild, mdChild);
            }
        }
    }
}
