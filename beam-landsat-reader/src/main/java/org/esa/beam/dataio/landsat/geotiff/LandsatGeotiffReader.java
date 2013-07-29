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

package org.esa.beam.dataio.landsat.geotiff;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.VirtualDir;
import org.esa.beam.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.CropDescriptor;
import javax.media.jai.operator.ScaleDescriptor;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This reader is capable of reading Landsat data products
 * where each bands is distributes as a single GeoTIFF image.
 */
public class LandsatGeotiffReader extends AbstractProductReader {

    private static final String UNITS = "W/(m^2*sr*µm)";

    private LandsatMetadata landsatMetadata;
    private List<Product> bandProducts;
    private VirtualDir input;

    public LandsatGeotiffReader(ProductReaderPlugIn readerPlugin) {
        super(readerPlugin);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        input = LandsatGeotiffReaderPlugin.getInput(getInput());
        String[] list = input.list("");
        File mtlFile = null;
        for (String fileName : list) {
            final File file = input.getFile(fileName);
            if (isMetadataFile(file)) {
                mtlFile = file;
                break;
            }
        }
        if (mtlFile == null) {
            throw new IOException("Can not find metadata file.");
        }
        if (!mtlFile.canRead()) {
            throw new IOException("Can not read metadata file: " + mtlFile.getAbsolutePath());
        }
        landsatMetadata = LandsatMetadataFactory.create(mtlFile);
        Dimension refDim = landsatMetadata.getReflectanceDim();
        Dimension thmDim = landsatMetadata.getThermalDim();
        Dimension panDim = landsatMetadata.getPanchromaticDim();
        Dimension productDim = new Dimension(0, 0);
        productDim = max(productDim, refDim);
        productDim = max(productDim, thmDim);
        productDim = max(productDim, panDim);

        MetadataElement metadataElement = landsatMetadata.getMetaDataElementRoot();
        Product product = new Product(getProductName(mtlFile), landsatMetadata.getProductType(), productDim.width, productDim.height);
        product.setFileLocation(mtlFile);

        product.getMetadataRoot().addElement(metadataElement);

        ProductData.UTC utcCenter = landsatMetadata.getCenterTime();
        product.setStartTime(utcCenter);
        product.setEndTime(utcCenter);

        addBands(product, input);

        return product;
    }

    private static String getProductName(File mtlfile) {
        String filename = mtlfile.getName();
        int extensionIndex = filename.toLowerCase().indexOf("_mtl.txt");
        return filename.substring(0, extensionIndex);
    }

    private static Dimension max(Dimension dim1, Dimension dim2) {
        if (dim2 != null) {
            int width = Math.max(dim1.width, dim2.width);
            int height = Math.max(dim1.height, dim2.height);
            return new Dimension(width, height);
        }
        return dim1;
    }

    private void addBands(Product product, VirtualDir folder) throws IOException {
        final GeoTiffProductReaderPlugIn plugIn = new GeoTiffProductReaderPlugIn();
        final MetadataAttribute[] productAttributes = landsatMetadata.getProductMetadata().getAttributes();
        final Pattern pattern = landsatMetadata.getOpticalBandFileNamePattern();

        bandProducts = new ArrayList<Product>();
        for (MetadataAttribute metadataAttribute : productAttributes) {
            String attributeName = metadataAttribute.getName();
            Matcher matcher = pattern.matcher(attributeName);
            if (matcher.matches()) {
                String bandNumber = matcher.group(1);
                String fileName = metadataAttribute.getData().getElemString();

                File bandFile = folder.getFile(fileName);
                ProductReader productReader = plugIn.createReaderInstance();
                Product bandProduct = productReader.readProductNodes(bandFile, null);
                if (bandProduct != null) {
                    bandProducts.add(bandProduct);
                    Band srcBand = bandProduct.getBandAt(0);
                    String bandName = landsatMetadata.getBandNamePrefix(bandNumber);
                    Band band = product.addBand(bandName, srcBand.getDataType());
                    band.setNoDataValue(0.0);
                    band.setNoDataValueUsed(true);

                    band.setSpectralWavelength(landsatMetadata.getWavelength(bandNumber));
                    band.setSpectralBandwidth(landsatMetadata.getBandwidth(bandNumber));

                    band.setScalingFactor(landsatMetadata.getScalingFactor(bandNumber));
                    band.setScalingOffset(landsatMetadata.getScalingOffset(bandNumber));

                    band.setDescription(landsatMetadata.getBandDescription(bandNumber));
                    band.setUnit(UNITS);
                }
            } else if (attributeName.equals(landsatMetadata.getQualityBandNameKey())) {
                String fileName = metadataAttribute.getData().getElemString();
                File bandFile = folder.getFile(fileName);
                ProductReader productReader = plugIn.createReaderInstance();
                Product bandProduct = productReader.readProductNodes(bandFile, null);
                if (bandProduct != null) {
                    bandProducts.add(bandProduct);
                    Band srcBand = bandProduct.getBandAt(0);
                    String bandName = "flags";

                    Band band = product.addBand(bandName, srcBand.getDataType());
                    band.setNoDataValue(0.0);
                    band.setNoDataValueUsed(true);
                    band.setDescription("Quality Band");

                    FlagCoding flagCoding = createFlagCoding(bandName);
                    for (String flagName : flagCoding.getFlagNames()) {
                        MetadataAttribute flag = flagCoding.getFlag(flagName);
                        List<Mask> masks = createMasks(product, flagName, flag);
                        for (Mask mask : masks) {
                            product.getMaskGroup().add(mask);
                        }
                    }

                    band.setSampleCoding(flagCoding);
                    product.getFlagCodingGroup().add(flagCoding);
                }
            }
        }

        for (Product bandProduct : bandProducts) {
            if (product.getGeoCoding() == null &&
                product.getSceneRasterWidth() == bandProduct.getSceneRasterWidth() &&
                product.getSceneRasterHeight() == bandProduct.getSceneRasterHeight()) {
                product.setGeoCoding(bandProduct.getGeoCoding());
                break;
            }
        }

        for (int i = 0; i < bandProducts.size(); i++) {
            Product bandProduct = bandProducts.get(i);
            Band band = product.getBandAt(i);
            if (product.getSceneRasterWidth() == bandProduct.getSceneRasterWidth() &&
                product.getSceneRasterHeight() == bandProduct.getSceneRasterHeight()) {
                band.setSourceImage(bandProduct.getBandAt(0).getSourceImage());
            } else {
                PlanarImage image = createScaledImage(product.getSceneRasterWidth(), product.getSceneRasterHeight(),
                                                      bandProduct.getSceneRasterWidth(), bandProduct.getSceneRasterHeight(),
                                                      bandProduct.getBandAt(0).getSourceImage());
                band.setSourceImage(image);
            }
        }
    }

    private List<Mask> createMasks(Product product, String flagName, MetadataAttribute flag) {
        ArrayList<Mask> masks = new ArrayList<Mask>();
        Mask mask = Mask.BandMathsType.create(flagName,
                                              flag.getDescription(),
                                              product.getSceneRasterWidth(),
                                              product.getSceneRasterHeight(),
                                              "'flags." + flagName + "'",
                                              ColorIterator.next(),
                                              0.5F);
        masks.add(mask);
        if (flagName.contains("_medium")) {
            String expression = "'flags." + flagName + "' and '" + "flags." +
                                flagName.replace("_medium", "_low") + "'";
            Mask highMask = Mask.BandMathsType.create(flagName.replace("_medium", "_high"),
                                                      flag.getDescription().replace("36-64%", "65-100%"),
                                                      product.getSceneRasterWidth(),
                                                      product.getSceneRasterHeight(),
                                                      expression,
                                                      ColorIterator.next(),
                                                      0.5F);
            masks.add(highMask);
        }
        return masks;
    }

    private FlagCoding createFlagCoding(String bandName) {
        FlagCoding flagCoding = new FlagCoding(bandName);
        flagCoding.addFlag("designated_fill", 1, "Designated Fill");
        flagCoding.addFlag("dropped_frame", 2, "Dropped Frame");
        flagCoding.addFlag("terrain_occlusion", 4, "Terrain Occlusion");
//        flagCoding.addFlag("Reserved", 8, "Reserved");
        flagCoding.addFlag("water_confidence_low", 16, "Water confidence 0-35%");
        flagCoding.addFlag("water_confidence_medium", 32, "Water confidence 36-64%");
//        flagCoding.addFlag("Water_confidence_high", 48, "Water confidence 64-100%");
//        flagCoding.addFlag("Reserved", 64, "Reserved for a future 2-bit class artifact designation");
//        flagCoding.addFlag("Reserved", 128, "Reserved for a future 2-bit class artifact designation");
        flagCoding.addFlag("vegetation_confidence_low", 256, "Vegetation confidence 0-35%");
        flagCoding.addFlag("vegetation_confidence_medium", 512, "Vegetation confidence 36-64%");
//        flagCoding.addFlag("Vegetation confidence_high", 768, "Vegetation confidence 65-100%");
        flagCoding.addFlag("snow/ice_confidence_low", 1024, "Snow/ice confidence 0-35%");
        flagCoding.addFlag("snow/ice_confidence_medium", 2048, "Snow/ice confidence 36-64%");
//        flagCoding.addFlag("Snow/ice_confidence_high", 3072, "Snow/ice confidence 65-100%");
        flagCoding.addFlag("cirrus_confidence_low", 4096, "Cirrus confidence 0-35%");
        flagCoding.addFlag("cirrus_confidence_medium", 8192, "Cirrus confidence 36-64%");
//        flagCoding.addFlag("Cirrus_confidence_high", 12288, "Cirrus confidence 65-100%");
        flagCoding.addFlag("cloud_confidence_low", 16384, "Cloud confidence 0-35%");
        flagCoding.addFlag("cloud_confidence_medium", 32768, "Cloud confidence 36-64%");
//        flagCoding.addFlag("Cloud_confidence_high", 49152, "Cloud confidence 65-100%");
        return flagCoding;
    }

    private static RenderedOp createScaledImage(int targetWidth, int targetHeight, int sourceWidth, int sourceHeight, RenderedImage srcImg) {
        float xScale = (float) targetWidth / (float) sourceWidth;
        float yScale = (float) targetHeight / (float) sourceHeight;
        RenderedOp tempImg = ScaleDescriptor.create(srcImg, xScale, yScale, 0.5f, 0.5f,
                                                    Interpolation.getInstance(Interpolation.INTERP_NEAREST), null);
        return CropDescriptor.create(tempImg, 0f, 0f, (float) targetWidth, (float) targetHeight, null);
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {
        // all bands use source images as source for its data
        throw new IllegalStateException();
    }

    @Override
    public void close() throws IOException {
        for (Product bandProduct : bandProducts) {
            bandProduct.closeIO();
        }
        bandProducts.clear();
        input.close();
        input = null;
        super.close();
    }

    static boolean isMetadataFile(File file) {
        final String filename = file.getName().toLowerCase();
        return filename.endsWith("_mtl.txt");
    }

    private static class ColorIterator {

        static ArrayList<Color> colors;
        static Iterator<Color> colorIterator;

        static {
            colors = new ArrayList<Color>();
            colors.add(Color.red);
            colors.add(Color.red);
            colors.add(Color.red);
            colors.add(Color.blue);
            colors.add(Color.blue.darker());
            colors.add(Color.blue.darker().darker());
            colors.add(Color.green);
            colors.add(Color.green.darker());
            colors.add(Color.green.darker().darker());
            colors.add(Color.yellow);
            colors.add(Color.yellow.darker());
            colors.add(Color.yellow.darker().darker());
            colors.add(Color.magenta);
            colors.add(Color.magenta.darker());
            colors.add(Color.magenta.darker().darker());
            colors.add(Color.pink);
            colors.add(Color.pink.darker());
            colors.add(Color.pink.darker().darker());
            colorIterator = colors.iterator();
        }

        static Color next() {
            if (!colorIterator.hasNext()) {
                colorIterator = colors.iterator();
            }
            return colorIterator.next();
        }
    }

}