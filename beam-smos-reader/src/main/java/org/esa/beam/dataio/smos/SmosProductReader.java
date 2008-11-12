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

import com.bc.ceres.binio.Format;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.Type;
import com.bc.ceres.binio.SimpleType;
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

import javax.media.jai.JAI;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;

public class SmosProductReader extends AbstractProductReader {
    private static final String SMOS_DGG_DIR_PROPERTY_NAME = "org.esa.beam.pview.smosDggDir";

    private static MultiLevelImage dggridMultiLevelImage;

    private SmosFile smosFile;
    HashMap<String, BandInfo> bandInfos = new HashMap<String, BandInfo>(17);

    SmosProductReader(final SmosProductReaderPlugIn productReaderPlugIn) {
        super(productReaderPlugIn);
        registerBandInfo("Grid_Cell_ID", "", 0.0, 1.0, -999, 0, 1<<31,
              "Unique identifier for Earth fixed grid point (ISEA4H9 DGG).");
        registerBandInfo("Flags", "", 0.0, 1.0, -1, 0, 1<<16,
              "L1c flags applicable to the pixel for this " +
                "particular integration time.");
        registerBandInfo("BT_Value", "K", 0.0, 1.0,  -999, 50.0, 350.0,
              "Brightness temperature measurement over current " +
                "Earth fixed grid point, obtained by DFT " +
                "interpolation from L1b data.");
        registerBandInfo("BT_Value_Real", "K", 0.0, 1.0,  -999, 50.0, 350.0,
              "Real component of HH, HV or VV polarisation brightness temperature measurement over current " +
                "Earth fixed grid point, obtained by DFT " +
                "interpolation from L1b data.");
        registerBandInfo("BT_Value_Imag", "K", 0.0, 1.0,  -999, 50.0, 350.0,
              "Imaginary component of HH, HV or VV polarisation brightness temperature measurement over current " +
                "Earth fixed grid point, obtained by DFT " +
                "interpolation from L1b data.");
        registerBandInfo("Radiometric_Accuracy_of_Pixel", "K", 0.0, 50.0/(1<<16),  -999, 0.0, 5.0,
              "Error accuracy measurement in the Brightness " +
                "Temperature presented in the previous field, " +
                "extracted in the direction of the pixel.");
        registerBandInfo("Incidence_Angle", "deg", 0.0, 90.0/(1<<16),  -999, 0.0, 90.0,
              "Incidence angle value corresponding to the " +
                      "measured BT value over current Earth fixed " +
                      "grid point. Measured as angle from pixel to " +
                      "S/C with respect to the pixel local normal (0º " +
                      "if vertical)");
        registerBandInfo("Azimuth_Angle", "deg", 0.0, 360.0/(1<<16),  -999, 0.0, 360.0,
              "Azimuth angle value corresponding to the " +
                "measured BT value over current Earth fixed " +
                "grid point. Measured as angle in pixel local " +
                "tangent plane from projected pixel to S/C " +
                "direction with respect to the local North (0º if" +
                "local North)");
        registerBandInfo("Faraday_Rotation_Angle", "deg", 0.0, 360.0/(1<<16),  -999, 0.0, 360.0,
              "Faraday rotation angle value corresponding " +
                      "to the measured BT value over current Earth " +
                      "fixed grid point. It is computed as the rotation " +
                      "from antenna to surface (i.e. inverse angle)");
        registerBandInfo("Geometric_Rotation_Angle", "deg", 0.0, 360.0/(1<<16),  -999, 0.0, 360.0,
              "Geometric rotation angle value " +
                      "corresponding to the measured BT value " +
                      "over current Earth fixed grid point. It is " +
                      "computed as the rotation from surface to " +
                      "antenna (i.e. direct angle).");
        registerBandInfo("Footprint_Axis1", "km", 0.0, 100.0/(1<<16),  -999, 20.0, 30.0,
              "Elliptical footprint major semi-axis value.");
        registerBandInfo("Footprint_Axis2", "km", 0.0, 100.0/(1<<16),  -999, 20.0, 30.0,
              "Elliptical footprint minor semi-axis value.");
    }

    @Override
    protected synchronized Product readProductNodesImpl() throws IOException {
        if (dggridMultiLevelImage == null) {
            String dirPath = System.getProperty(SMOS_DGG_DIR_PROPERTY_NAME);
            if (dirPath == null || !new File(dirPath).exists()) {
                throw new IOException(
                        MessageFormat.format("SMOS products require a DGG image.\nPlease set system property ''{0}''to a valid DGG image directory.", SMOS_DGG_DIR_PROPERTY_NAME));
            }

            try {
                MultiLevelSource dggridMultiLevelSource = TiledFileMultiLevelSource.create(new File(dirPath), false);
                dggridMultiLevelImage = new DefaultMultiLevelImage(dggridMultiLevelSource);
            } catch (IOException e) {
                throw new IOException(MessageFormat.format("Failed to load SMOS DDG ''{0}''", dirPath), e);
            }
        }


        final File file = FileUtils.exchangeExtension(getInputFile(), ".DBL");
        final int productSceneRasterWidth = dggridMultiLevelImage.getWidth();
        final int productSceneRasterHeight = dggridMultiLevelImage.getHeight();

        final String filenameWithoutExtension = FileUtils.getFilenameWithoutExtension(file);

        String formatName = null;
        final String[] formatNames = SmosFormats.getInstance().getFormatNames();
        for (String formatName1 : formatNames) {
            if (filenameWithoutExtension.contains(formatName1)) {
                formatName = formatName1;
            }
        }
        if (formatName == null) {
            throw new IOException("Unrecognized SMOS format.");
        }

        Format format = SmosFormats.getInstance().getFormat(formatName);
        if (format == null) {
            throw new IllegalStateException("format == null");
        }
        smosFile = new SmosFile(file, format);
        Product product = new Product(filenameWithoutExtension, formatName, productSceneRasterWidth, productSceneRasterHeight);
        product.setPreferredTileSize(512, 512);
        product.setFileLocation(file);
        product.setGeoCoding(createGeoCoding(product));

        if ("MIR_BWLD1C".equals(formatName)
                || "MIR_BWLF1C".equals(formatName)
                || "MIR_BWSD1C".equals(formatName)
                || "MIR_BWSF1C".equals(formatName)) {
            addGridCellIdBand(product);
            addSmosBands(product, SmosFormats.BROWSE_BT_DATA_TYPE);
        } else if ("MIR_SCLD1C".equals(formatName)
                || "MIR_SCSD1C".equals(formatName)) {
            addGridCellIdBand(product);
            addSmosBands(product, SmosFormats.D1C_BT_DATA_TYPE);
        } else if ("MIR_SCLF1C".equals(formatName)
                || "MIR_SCSF1C".equals(formatName)) {
            addGridCellIdBand(product);
            addSmosBands(product, SmosFormats.F1C_BT_DATA_TYPE);
        } else {
            throw new IllegalStateException("Illegal SMOS format: "+formatName);
        }

        return product;
    }

    private void addSmosBands(Product product, CompoundType compoundDataType) {
        CompoundType.Member[] members = compoundDataType.getMembers();
        for (int fieldIndex = 0; fieldIndex < members.length; fieldIndex++) {
            CompoundType.Member member = members[fieldIndex];
            String bandName = member.getName();
            BandInfo bandInfo = bandInfos.get(bandName);
            if (bandInfo != null) {
                addBand(product, fieldIndex, bandName, memberTypeToBandType(member.getType()), bandInfo);
            }
        }
    }

    private static int memberTypeToBandType(Type type) {
        int bandType;
        if (type.equals(SimpleType.BYTE)) {
            bandType = ProductData.TYPE_INT8;
        }else if (type.equals(SimpleType.UBYTE)) {
            bandType = ProductData.TYPE_UINT8;
        }else if (type.equals(SimpleType.SHORT)) {
            bandType = ProductData.TYPE_INT16;
        }else if (type.equals(SimpleType.USHORT)) {
            bandType = ProductData.TYPE_UINT16;
        }else if (type.equals(SimpleType.INT)) {
            bandType = ProductData.TYPE_INT32;
        }else if (type.equals(SimpleType.UINT)) {
            bandType = ProductData.TYPE_UINT32;
        }else if (type.equals(SimpleType.FLOAT)) {
            bandType = ProductData.TYPE_FLOAT32;
        }else if (type.equals(SimpleType.DOUBLE)) {
            bandType = ProductData.TYPE_FLOAT64;
        }else {
            throw new IllegalStateException("type="+type);
        }
        return bandType;
    }

    private void addBand(Product product, int fieldIndex, String bandName, int bandType, BandInfo bandInfo) {
        final Band band = product.addBand(bandName, bandType);
        band.setScalingFactor(bandInfo.scaleFactor);
        band.setScalingOffset(bandInfo.scaleOffset);
        band.setUnit(bandInfo.unit);
        band.setDescription(bandInfo.description);
        if (bandInfo.noDataValue != null) {
            band.setNoDataValueUsed(true);
            band.setNoDataValue(bandInfo.noDataValue.doubleValue());
        }
        band.setSourceImage(createBTempSourceImage(band, fieldIndex, bandInfo.noDataValue));
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
            points[i] = new ColorPaletteDef.Point(bandInfo.min + ((bandInfo.max - bandInfo.min) * i / (colors.length - 1)), colors[i]);
        }
        final ColorPaletteDef def = new ColorPaletteDef(points);
        return new ImageInfo(def);
    }

    private void addGridCellIdBand(Product product) {
        SmosProductReader.BandInfo bandInfo = bandInfos.get("Grid_Cell_ID");
        Band band = product.addBand("Grid_Cell_ID", ProductData.TYPE_UINT32);
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

    private MultiLevelImage createBTempSourceImage(final Band band, int fieldIndex, Number noDataValue) {
        SmosMultiLevelSource smosMultiLevelSource = new SmosMultiLevelSource(dggridMultiLevelImage, smosFile, band, fieldIndex, noDataValue);
        return new DefaultMultiLevelImage(smosMultiLevelSource);
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

//        Assert.state(sourceOffsetX == destOffsetX, "sourceOffsetX != destOffsetX");
//        Assert.state(sourceOffsetY == destOffsetY, "sourceOffsetY != destOffsetY");
//        Assert.state(sourceStepX == 1, "sourceStepX != 1");
//        Assert.state(sourceStepY == 1, "sourceStepY != 1");
//        Assert.state(sourceWidth == destWidth, "sourceWidth != destWidth");
//        Assert.state(sourceHeight == destHeight, "sourceHeight != destHeight");
//


    }

    public static void main(String[] args) throws IOException {
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(512 * (1024 * 1024));
        SmosProductReader smosProductReader = new SmosProductReader(new SmosProductReaderPlugIn());
        final File dir = new File(args[0]);
        final File file = new File(dir, dir.getName() + ".DBL");
        Product product = smosProductReader.readProductNodes(file, null);
        ProductIO.writeProduct(product, "smosproduct_2.dim", null);
    }

    private class BandInfo {
        String name;
        String unit = "";
        double scaleOffset;
        double scaleFactor;
        Number noDataValue;
        double min;
        double max;
        String description;

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

}
