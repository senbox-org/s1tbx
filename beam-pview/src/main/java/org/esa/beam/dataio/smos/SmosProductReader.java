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

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.media.jai.JAI;

import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.glevel.TiledFileLayerImageFactory;
import org.esa.beam.util.io.FileUtils;

import com.bc.ceres.binio.Format;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.LevelImageFactory;
import com.bc.ceres.glevel.LayerImage;
import com.bc.ceres.glevel.IMultiLevelImage;
import com.bc.ceres.glevel.support.MultiLevelImageImpl;


public class SmosProductReader extends AbstractProductReader {
    private static final String SMOS_DGG_DIR_PROPERTY_NAME = "org.esa.beam.pview.smosDggDir";

    private static class BandDescr {
        final String bandName;
        final int btDataIndex;

        private BandDescr(String bandName, int btDataIndex) {
            this.bandName = bandName;
            this.btDataIndex = btDataIndex;
        }
    }

    private static final Map<String, BandDescr> bandDescrMap;
    private SmosFile smosFile;

    static {
        bandDescrMap = new HashMap<String, BandDescr>(6);
        bandDescrMap.put("BT_Value", new BandDescr("BT_Value", 1));
        bandDescrMap.put("BT_Value_Real", new BandDescr("BT_Value_Real", 1));
        bandDescrMap.put("BT_Value_Imag", new BandDescr("BT_Value_Imag", 2));
    }

    private static LayerImage dggridLayerImage;

    SmosProductReader(final SmosProductReaderPlugIn productReaderPlugIn) {
        super(productReaderPlugIn);
    }

    @Override
    protected synchronized Product readProductNodesImpl() throws IOException {
        if (dggridLayerImage == null) {
            String dirPath = System.getProperty(SMOS_DGG_DIR_PROPERTY_NAME);
            if (dirPath == null || !new File(dirPath).exists()) {
                throw new IOException(
                        MessageFormat.format("SMOS products require a DGG image.\nPlease set system property ''{0}''to a valid DGG image directory.", SMOS_DGG_DIR_PROPERTY_NAME));
            }
            try {
                dggridLayerImage = TiledFileLayerImageFactory.create(new File(dirPath), false);
            } catch (IOException e) {
                throw new IOException(MessageFormat.format("Failed to load SMOS DDG ''{0}''", dirPath), e);
            }
        }

        final File file = FileUtils.exchangeExtension(getInputFile(), ".DBL");
        final int productWidth = 16384;
        final int productHeight = 8192;

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
        Product product = new Product(filenameWithoutExtension, formatName, productWidth, productHeight);
        product.setPreferredTileSize(512, 512);
        product.setFileLocation(file);

        if ("MIR_BWLD1C".equals(formatName)
                || "MIR_BWLF1C".equals(formatName)
                || "MIR_BWSD1C".equals(formatName)
                || "MIR_BWSF1C".equals(formatName)) {
            final Band band = product.addBand("BT_Value", ProductData.TYPE_FLOAT32);
            applyBandProperties(band);

        } else if ("MIR_SCLD1C".equals(formatName)
                || "MIR_SCSD1C".equals(formatName)) {
            final Band band = product.addBand("BT_Value", ProductData.TYPE_FLOAT32);
            applyBandProperties(band);
        } else if ("MIR_SCLF1C".equals(formatName)
                || "MIR_SCSF1C".equals(formatName)) {
            final Band band1 = product.addBand("BT_Value_Real", ProductData.TYPE_FLOAT32);
            applyBandProperties(band1);
            final Band band2 = product.addBand("BT_Value_Imag", ProductData.TYPE_FLOAT32);
            applyBandProperties(band2);
        } else {
            throw new IllegalStateException("?");
        }

        return product;
    }

    private RenderedImage createSourceImage(final Band band) {
        final int btDataIndex = bandDescrMap.get(band.getName()).btDataIndex;
        IMultiLevelImage image = new MultiLevelImageImpl(new LevelImageFactory() {
            @Override
            public RenderedImage createLevelImage(int level) {
                return new SmosL1BandOpImage(smosFile, band, btDataIndex, dggridLayerImage.getLevelImage(level), level);
            }});
        return image;
    }

    private RenderedImage createValidMaksImage(final Band band) {
        IMultiLevelImage image = new MultiLevelImageImpl(new LevelImageFactory() {
            @Override
            public RenderedImage createLevelImage(int level) {
                return new SmosL1ValidImage(band, level);
            }});
        return image;
    }

    private void applyBandProperties(Band band) {

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

        band.setNoDataValueUsed(true);
        band.setGeophysicalNoDataValue(-999);
        band.setSourceImage(createSourceImage(band));
        band.setValidMaskImage(createValidMaksImage(band));
    }

//    @Override
//    public void close() throws IOException {
//        imageInputStream.close();
//        super.close();
//    }

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
}
