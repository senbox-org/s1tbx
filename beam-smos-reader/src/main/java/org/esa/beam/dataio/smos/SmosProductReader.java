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
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.maptransf.IdentityTransformDescriptor;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.MapProjectionRegistry;
import org.esa.beam.glevel.TiledFileMultiLevelSource;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.util.io.FileUtils;

import javax.media.jai.JAI;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

public class SmosProductReader extends AbstractProductReader {
    private static final String SMOS_DGG_DIR_PROPERTY_NAME = "org.esa.beam.pview.smosDggDir";

    private SmosFile smosFile;

    private static MultiLevelSource dggridMultiLevelSource;
    private static MultiLevelImage dggridMultiLevelImage;

    SmosProductReader(final SmosProductReaderPlugIn productReaderPlugIn) {
        super(productReaderPlugIn);
    }

    @Override
    protected synchronized Product readProductNodesImpl() throws IOException {
        if (dggridMultiLevelSource == null) {
            String dirPath = System.getProperty(SMOS_DGG_DIR_PROPERTY_NAME);
            if (dirPath == null || !new File(dirPath).exists()) {
                throw new IOException(
                        MessageFormat.format("SMOS products require a DGG image.\nPlease set system property ''{0}''to a valid DGG image directory.", SMOS_DGG_DIR_PROPERTY_NAME));
            }
            try {
                dggridMultiLevelSource = TiledFileMultiLevelSource.create(new File(dirPath), false);
            } catch (IOException e) {
                throw new IOException(MessageFormat.format("Failed to load SMOS DDG ''{0}''", dirPath), e);
            }
        }

        dggridMultiLevelImage = new DefaultMultiLevelImage(dggridMultiLevelSource);

        final File file = FileUtils.exchangeExtension(getInputFile(), ".DBL");
        final int productSceneRasterWidth = dggridMultiLevelSource.getImage(0).getWidth();
        final int productSceneRasterHeight = dggridMultiLevelSource.getImage(0).getHeight();

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
            CompoundType.Member[] members = SmosFormats.BROWSE_BT_DATA_TYPE.getMembers();
            for (int fieldIndex = 0; fieldIndex < members.length; fieldIndex++) {
                CompoundType.Member member = members[fieldIndex];
                addBand(product, fieldIndex, member.getName(), memberTypeToBandType(member.getType()), "", -999);
            }

        } else if ("MIR_SCLD1C".equals(formatName)
                || "MIR_SCSD1C".equals(formatName)) {
            addGridCellIdBand(product);
            addBand(product, 1, "BT_Value", ProductData.TYPE_FLOAT32, "K", -999);
        } else if ("MIR_SCLF1C".equals(formatName)
                || "MIR_SCSF1C".equals(formatName)) {
            addGridCellIdBand(product);
            addBand(product, 1, "BT_Value_Real", ProductData.TYPE_FLOAT32, "K", -999);
            addBand(product, 2, "BT_Value_Imag", ProductData.TYPE_FLOAT32, "K", -999);
        } else {
            throw new IllegalStateException("?");
        }

        return product;
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

    private void addBand(Product product, int fieldIndex, String bandName, int bandType, String bandUnit, int noDataValue) {
        final Band band = product.addBand(bandName, bandType);
        applyBandProperties(band, bandUnit, fieldIndex, noDataValue);
    }

    private void addGridCellIdBand(Product product) {
        Band band = product.addBand("Grid_Cell_ID", ProductData.TYPE_UINT32);
        band.setDescription("ID of the cell within the SMOS Discrete Global Grid (ISEA9H)");
        band.setSourceImage(dggridMultiLevelImage);
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
        return new DefaultMultiLevelImage(new SmosMultiLevelSourceFloat(band, fieldIndex, noDataValue));
    }

    private void applyBandProperties(Band band, String unit, int fieldIndex, Number noDataValue) {

        final float min = 67.0f;
        final float max = 317.0f;
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
            points[i] = new ColorPaletteDef.Point(min + ((max - min) * i / (colors.length - 1)), colors[i]);
        }
        final ColorPaletteDef def = new ColorPaletteDef(points);
        final ImageInfo imageInfo = new ImageInfo(def);
        band.setImageInfo(imageInfo);

        band.setUnit(unit);

        band.setNoDataValueUsed(true);
        band.setNoDataValue(noDataValue.doubleValue());
        band.setSourceImage(createBTempSourceImage(band, fieldIndex, noDataValue));
    }

    @Override
    public void close() throws IOException {
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

    private class SmosMultiLevelSourceFloat extends AbstractMultiLevelSource {
        private final Band band;
        private final int fieldIndex;
        private final Number noDataValue;

        public SmosMultiLevelSourceFloat(Band band, int fieldIndex, Number noDataValue) {
            super(dggridMultiLevelSource.getModel());
            this.band = band;
            this.fieldIndex = fieldIndex;
            this.noDataValue = noDataValue;
        }

        @Override
            public RenderedImage createImage(int level) {
            return new SmosL1BandOpImage(smosFile,
                                         band,
                                         fieldIndex,
                                         noDataValue,
                                         dggridMultiLevelSource.getImage(level),
                                         ResolutionLevel.create(getModel(), level));
        }

    }
}
