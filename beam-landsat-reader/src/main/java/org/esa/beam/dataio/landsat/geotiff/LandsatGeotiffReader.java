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
import com.bc.ceres.glevel.MultiLevelImage;
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
import org.esa.beam.jai.ImageManager;

import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.CropDescriptor;
import javax.media.jai.operator.ScaleDescriptor;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.RenderingHints;
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
                    band.setScalingFactor(landsatMetadata.getScalingFactor(bandNumber));
                    band.setScalingOffset(landsatMetadata.getScalingOffset(bandNumber));

                    band.setNoDataValue(0.0);
                    band.setNoDataValueUsed(true);

                    band.setSpectralWavelength(landsatMetadata.getWavelength(bandNumber));
                    band.setSpectralBandwidth(landsatMetadata.getBandwidth(bandNumber));

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
                    band.setSampleCoding(flagCoding);
                    product.getFlagCodingGroup().add(flagCoding);
                    List<Mask> masks = createMasks(product);
                    for (Mask mask : masks) {
                        product.getMaskGroup().add(mask);
                    }

                }
            }
        }

        ImageLayout imageLayout  = new ImageLayout();
        for (Product bandProduct : bandProducts) {
            if (product.getGeoCoding() == null &&
                product.getSceneRasterWidth() == bandProduct.getSceneRasterWidth() &&
                product.getSceneRasterHeight() == bandProduct.getSceneRasterHeight()) {
                product.setGeoCoding(bandProduct.getGeoCoding());
                Dimension tileSize = bandProduct.getPreferredTileSize();
                if (tileSize != null) {
                    tileSize = ImageManager.getPreferredTileSize(bandProduct);
                }
                product.setPreferredTileSize(tileSize);
                imageLayout.setTileWidth(tileSize.width);
                imageLayout.setTileHeight(tileSize.height);
                break;
            }
        }


        for (int i = 0; i < bandProducts.size(); i++) {
            Product bandProduct = bandProducts.get(i);
            Band band = product.getBandAt(i);
            final MultiLevelImage sourceImage = bandProduct.getBandAt(0).getSourceImage();
            if (product.getSceneRasterWidth() == bandProduct.getSceneRasterWidth() &&
                product.getSceneRasterHeight() == bandProduct.getSceneRasterHeight()) {
                band.setSourceImage(sourceImage);
            } else {
                final RenderingHints renderingHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
                PlanarImage image = createScaledImage(product.getSceneRasterWidth(), product.getSceneRasterHeight(),
                                                      bandProduct.getSceneRasterWidth(), bandProduct.getSceneRasterHeight(),
                                                      sourceImage, renderingHints);
                band.setSourceImage(image);
            }
        }
    }

    private List<Mask> createMasks(Product product) {
        ArrayList<Mask> masks = new ArrayList<Mask>();
        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();

        masks.add(Mask.BandMathsType.create("designated_fill",
                                            "Designated Fill",
                                            width, height,
                                            "flags.designated_fill",
                                            ColorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("dropped_frame",
                                            "Dropped Frame",
                                            width, height,
                                            "flags.dropped_frame",
                                            ColorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("terrain_occlusion",
                                            "Terrain Occlusion",
                                            width, height,
                                            "flags.terrain_occlusion",
                                            ColorIterator.next(),
                                            0.5));
        masks.addAll(createConfidenceMasks("water_confidence", "Water confidence", width, height));
        masks.addAll(createConfidenceMasks("vegetation_confidence", "Vegetation confidence", width, height));
        masks.addAll(createConfidenceMasks("snow_ice_confidence", "Snow/ice confidence", width, height));
        masks.addAll(createConfidenceMasks("cirrus_confidence", "Cirrus confidence", width, height));
        masks.addAll(createConfidenceMasks("cloud_confidence", "Cloud confidence", width, height));

        return masks;
    }

    private List<Mask> createConfidenceMasks(String flagMaskBaseName, String descriptionBaseName, int width, int height) {
        List<Mask> masks = new ArrayList<Mask>();
        masks.add(Mask.BandMathsType.create(flagMaskBaseName + "_low",
                                            descriptionBaseName + " 0-35%",
                                            width, height,
                                            "flags." + flagMaskBaseName + "_one and not flags." + flagMaskBaseName + "_two",
                                            ColorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create(flagMaskBaseName + "_mid",
                                            descriptionBaseName + " 36-64%",
                                            width, height,
                                            "not flags." + flagMaskBaseName + "_one and flags." + flagMaskBaseName + "_two",
                                            ColorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create(flagMaskBaseName + "_high",
                                            descriptionBaseName + " 65-100%",
                                            width, height,
                                            "flags." + flagMaskBaseName + "_one and flags." + flagMaskBaseName + "_two",
                                            ColorIterator.next(),
                                            0.5));
        return masks;
    }

    private FlagCoding createFlagCoding(String bandName) {
        FlagCoding flagCoding = new FlagCoding(bandName);
        flagCoding.addFlag("designated_fill", 1, "Designated Fill");
        flagCoding.addFlag("dropped_frame", 2, "Dropped Frame");
        flagCoding.addFlag("terrain_occlusion", 4, "Terrain Occlusion");
        flagCoding.addFlag("reserved_1", 8, "Reserved for a future 1-bit class artifact designation");
        flagCoding.addFlag("water_confidence_one", 16, "Water confidence bit one");
        flagCoding.addFlag("water_confidence_two", 32, "Water confidence bit two");
        flagCoding.addFlag("reserved_2_one", 64, "Reserved for a future 2-bit class artifact designation");
        flagCoding.addFlag("reserved_2_two", 128, "Reserved for a future 2-bit class artifact designation");
        flagCoding.addFlag("vegetation_confidence_one", 256, "Vegetation confidence bit one");
        flagCoding.addFlag("vegetation_confidence_two", 512, "Vegetation confidence bit two");
        flagCoding.addFlag("snow_ice_confidence_one", 1024, "Snow/ice confidence bit one");
        flagCoding.addFlag("snow_ice_confidence_two", 2048, "Snow/ice confidence bit two");
        flagCoding.addFlag("cirrus_confidence_one", 4096, "Cirrus confidence bit one");
        flagCoding.addFlag("cirrus_confidence_two", 8192, "Cirrus confidence bit two");
        flagCoding.addFlag("cloud_confidence_one", 16384, "Cloud confidence bit one");
        flagCoding.addFlag("cloud_confidence_two", 32768, "Cloud confidence bit two");
        return flagCoding;
    }

    private static RenderedOp createScaledImage(int targetWidth, int targetHeight, int sourceWidth, int sourceHeight, RenderedImage srcImg,
                                                RenderingHints renderingHints) {
        float xScale = (float) targetWidth / (float) sourceWidth;
        float yScale = (float) targetHeight / (float) sourceHeight;

        RenderedOp tempImg = ScaleDescriptor.create(srcImg, xScale, yScale, 0.5f, 0.5f,
                                                    Interpolation.getInstance(Interpolation.INTERP_NEAREST),
                                                    renderingHints);
        return CropDescriptor.create(tempImg, 0f, 0f, (float) targetWidth, (float) targetHeight, renderingHints);
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY,
                                          Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
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