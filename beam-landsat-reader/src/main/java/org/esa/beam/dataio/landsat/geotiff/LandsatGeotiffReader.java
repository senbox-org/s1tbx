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
import org.esa.beam.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.CropDescriptor;
import javax.media.jai.operator.ScaleDescriptor;
import java.awt.Dimension;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This reader is capable of reading Landsat data products
 * where each bands is distributes as a single GeoTIFF image.
 */
public class LandsatGeotiffReader extends AbstractProductReader {

    private static final float[] wavelengths = {490, 560, 660, 830, 1670, 11500, 2240, 710};
    private static final float[] bandwidths = {66, 82, 67, 128, 217, 1000, 252, 380};
    private static final Map<String, String> bandDescriptions = new HashMap<String,  String>();
    private static final String UNITS = "W/(m^2*sr*µm)";

    static {
        bandDescriptions.put("1", "Visible (30m)");
        bandDescriptions.put("2", "Visible (30m)");
        bandDescriptions.put("3", "Visible (30m)");
        bandDescriptions.put("4", "Near-Infrared (30m)");
        bandDescriptions.put("5", "Near-Infrared (30m)");
        bandDescriptions.put("6", "Thermal (120m)");
        bandDescriptions.put("61", "Thermal - Low Gain (60m)");
        bandDescriptions.put("62", "Thermal - High Gain (60m)");
        bandDescriptions.put("7", "Mid-Infrared (30m)");
        bandDescriptions.put("8", "Panchromatic (15m)");
    }

    private List<Product> bandProducts;

    public LandsatGeotiffReader(LandsatGeotiffReaderPlugin readerPlugin) {
        super(readerPlugin);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        File dir = LandsatGeotiffReaderPlugin.getFileInput(getInput());
        File[] files = dir.listFiles();
        File mtlFile = null;
        for (File file : files) {
            if (LandsatGeotiffReaderPlugin.isMetadataFile(file)) {
                mtlFile = file;
                break;
            }
        }
        if (mtlFile == null) {
            throw new IOException("Can not find metadata file.");
        }
        if (!mtlFile.canRead()) {
            throw new IOException("Can not read metadata file: "+ mtlFile.getAbsolutePath());
        }
        LandsatMetadata landsatMetadata = new LandsatMetadata(new FileReader(mtlFile));
        if (!landsatMetadata.isLandsatTM()) {
            throw new ProductIOException("Product is not a 'Landsat5' product.");
        }
        Dimension refDim = landsatMetadata.getReflectanceDim();
        Dimension thmDim = landsatMetadata.getThermalDim();
        Dimension panDim = landsatMetadata.getPanchromaticDim();
        Dimension productDim = new Dimension(0,0);
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

        addBands(product, landsatMetadata, mtlFile.getParentFile());

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

    private void addBands(Product product, LandsatMetadata landsatMetadata, File folder) throws IOException {
        GeoTiffProductReaderPlugIn plugIn = new GeoTiffProductReaderPlugIn();
        MetadataAttribute[] productAttributes = landsatMetadata.getProductMetadata().getAttributes();
        Pattern pattern = Pattern.compile("BAND(\\d{1,2})_FILE_NAME");
        bandProducts = new ArrayList<Product>();
        for (MetadataAttribute metadataAttribute : productAttributes) {
            String attributeName = metadataAttribute.getName();
            Matcher matcher = pattern.matcher(attributeName);
            if (matcher.matches()) {
                String bandNumber = matcher.group(1);
                String fileName = metadataAttribute.getData().getElemString();
                File bandFile = new File(folder, fileName);
                ProductReader productReader = plugIn.createReaderInstance();
                Product bandProduct = productReader.readProductNodes(bandFile, null);
                if (bandProduct != null) {
                    bandProducts.add(bandProduct);
                    Band srcBand = bandProduct.getBandAt(0);
                    String bandName = "radiance_" + bandNumber;
                    Band band = product.addBand(bandName, srcBand.getDataType());
                    band.setNoDataValue(0.0);
                    band.setNoDataValueUsed(true);
                    String bandIndexNumber = bandNumber.substring(0, 1);
                    int index = Integer.parseInt(bandIndexNumber) - 1;
                    band.setSpectralWavelength(wavelengths[index]);
                    band.setSpectralBandwidth(bandwidths[index]);

                    band.setScalingFactor(landsatMetadata.getScalingFactor(bandNumber));
                    band.setScalingOffset(landsatMetadata.getScalingOffset(bandNumber));

                    band.setDescription(bandDescriptions.get(bandNumber));
                    band.setUnit(UNITS);
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
        super.close();
    }
}