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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This reader is capable of reading Landsat data products
 * where each bands is distributes as a single GeoTIFF image.
 */
public class LandsatGeotiffReader extends AbstractProductReader {

    private static final String UNITS = "W/(m^2*sr*µm)";

    final LandsatMetadataFactory landsatMetadataFactory;
    private LandsatMetadata landsatMetadata;
    private List<Product> bandProducts;
    private VirtualDir input;


    public LandsatGeotiffReader(ProductReaderPlugIn readerPlugin, LandsatMetadataFactory landsatMetadataFactory) {
        super(readerPlugin);
        this.landsatMetadataFactory = landsatMetadataFactory;
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        input = LandsatGeotiffReaderPlugin.getInput(getInput());
        String[] list = input.list("");
        File mtlFile = null;
        for (String fileName : list) {
            final File file = input.getFile(fileName);
            if (LandsatGeotiffReaderPlugin.isMetadataFile(file)) {
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
        landsatMetadata = landsatMetadataFactory.create(mtlFile);
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
        final Pattern pattern = landsatMetadata.getBandFileNamePattern();

        bandProducts = new ArrayList<Product>();
        float[] wavelengths = landsatMetadata.getWavelengths();
        float[] bandwidths = landsatMetadata.getBandwidths();
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

                    band.setDescription(landsatMetadata.getBandDescription(bandNumber));
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
        input.close();
        input = null;
        super.close();
    }
}