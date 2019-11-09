/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.polsarpro;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.dataio.envi.EnviConstants;
import org.esa.snap.dataio.envi.EnviProductReader;
import org.esa.snap.dataio.envi.Header;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.metadata.AbstractMetadataIO;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.esa.snap.engine_utilities.util.ResourceUtils;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PolsarProProductReader extends EnviProductReader {

    private final HashMap<Band, BandInfo> bandInfoMap = new HashMap<>(10);

    PolsarProProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final Path inputPath = ReaderUtils.getPathFromInput(getInput());
        final File inputFile = inputPath.toFile();
        final File folder = inputPath.getParent().toFile();
        final File[] fileList = folder.listFiles();

        if(fileList == null) {
            throw new IOException("no files found in "+folder.toString());
        }

        final List<Header> headerList = new ArrayList<>(fileList.length);
        final HashMap<Header, File> headerFileMap = new HashMap<>(fileList.length);
        Header mainHeader = null;
        File mainHeaderFile = null;

        ResourceUtils.sortFileList(fileList);

        for (File file : fileList) {
            if (file.isDirectory())
                continue;
            if (file.getName().toLowerCase().endsWith("hdr")) {
                final File imgFile = getEnviImageFile(file);
                if (!imgFile.exists())
                    continue;

                try (BufferedReader headerReader = getHeaderReader(file)) {

                    synchronized (headerReader) {
                        final Header header = new Header(headerReader);
                        headerList.add(header);
                        headerFileMap.put(header, file);

                        if (header.getNumBands() > 0 && header.getBandNames() != null) {
                            mainHeader = header;
                            mainHeaderFile = file;
                        }
                    }

                }
            }
        }

        if (mainHeader == null) {
            throw new IOException("Unable to read files");
        }

        String productName;
        if (inputFile.isDirectory()) {
            productName = inputFile.getName();
            if (productName.equalsIgnoreCase("T3") || productName.equalsIgnoreCase("C3") ||
                    productName.equalsIgnoreCase("T4") || productName.equalsIgnoreCase("C4")) {
                productName = inputFile.getParentFile().getName() + '_' + productName;
            }
        } else {
            final String headerFileName = mainHeaderFile.getName();
            productName = headerFileName.substring(0, headerFileName.indexOf('.'));
        }

        final Product product = new Product(productName, mainHeader.getSensorType(),
                mainHeader.getNumSamples(), mainHeader.getNumLines());
        product.setProductReader(this);
        product.setFileLocation(mainHeaderFile);
        product.getMetadataRoot().addElement(mainHeader.getAsMetadata());
        product.setDescription(mainHeader.getDescription());

        try {
            initGeoCoding(product, mainHeader);

            for (Header header : headerList) {
                final int dataType = header.getDataType();
                if (dataType == EnviConstants.TYPE_ID_COMPLEXFLOAT32) {
                    initComplexBands(product, inputFile, header, ProductData.TYPE_FLOAT32);
                } else if (dataType == EnviConstants.TYPE_ID_COMPLEXFLOAT64) {
                    initComplexBands(product, inputFile, header, ProductData.TYPE_FLOAT64);
                } else {
                    initBands(headerFileMap.get(header), product, header);
                }
            }

            applyBeamProperties(product, mainHeader.getBeamProperties());

            addMetadata(product, inputFile);
        } catch (Exception e) {
            throw new IOException(e);
        }
        return product;
    }

    private void initComplexBands(final Product product, final File inputFile, final Header header,
                                         final int bandType) throws IOException {
        final Double dataIgnoreValue = header.getDataIgnoreValue();
        final int width = header.getNumSamples();
        final int height = header.getNumLines();
        final String[] bandNames = header.getBandNames();

        for (String bandName : bandNames) {
            if (!ProductNode.isValidNodeName(bandName)) {
                bandName = StringUtils.createValidName(bandName, null, '_');
            }
            final Band iBand = new Band("i_"+bandName, bandType, width, height);
            iBand.setUnit("real");
            if (dataIgnoreValue != null) {
                iBand.setNoDataValueUsed(true);
                iBand.setNoDataValue(dataIgnoreValue);
            }
            product.addBand(iBand);

            final Band qBand = new Band("q_"+bandName, bandType, width, height);
            qBand.setUnit("imaginary");
            if (dataIgnoreValue != null) {
                qBand.setNoDataValueUsed(true);
                qBand.setNoDataValue(dataIgnoreValue);
            }
            product.addBand(qBand);

            ReaderUtils.createVirtualIntensityBand(product, iBand, qBand, bandName);

            BandInfo bandInfo = new BandInfo();
            bandInfo.isComplex = true;
            bandInfo.header = header;
            final File bandFile = new File(inputFile.getParentFile(), bandName);
            bandInfo.inStream = new FileImageInputStream(bandFile);
            bandInfo.inStream.setByteOrder(header.getJavaByteOrder());

            bandInfoMap.put(iBand, bandInfo);
            bandInfoMap.put(qBand, bandInfo);
        }
    }

    private static void addMetadata(final Product product, final File inputFile) throws IOException {
        if (!AbstractMetadata.hasAbstractedMetadata(product)) {
            final MetadataElement root = product.getMetadataRoot();
            final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, product.getName());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, product.getProductType());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line, product.getSceneRasterWidth());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines, product.getSceneRasterHeight());

            AbstractMetadataIO.loadExternalMetadata(product, absRoot, inputFile);
        }

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        absRoot.setAttributeInt(AbstractMetadata.polsarData, 1);
        // polsarpro data automatically calibrated for Radarsat2 only
        //absRoot.setAttributeInt(AbstractMetadata.abs_calibration_flag, 1);
    }

    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY,
                                          int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY,
                                          Band destBand,
                                          int destOffsetX, int destOffsetY,
                                          int destWidth, int destHeight,
                                          ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        final BandInfo bandInfo = bandInfoMap.get(destBand);
        if (bandInfo != null && bandInfo.isComplex) {

            final int sourceMaxY = sourceOffsetY + sourceHeight - 1;
            Product product = destBand.getProduct();
            final int elemSize = destBuffer.getElemSize();

            final int headerOffset = bandInfo.header.getHeaderOffset();
            final int bandIndex = 0;//product.getBandIndex(destBand.getName());

            // band interleaved by pixel
            int numBands = 2;
            final long lineSizeInBytes = bandInfo.header.getNumSamples() * numBands * elemSize;
            ProductData lineData = ProductData.createInstance(destBuffer.getType(), sourceWidth * numBands);

            pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceMaxY - sourceOffsetY);
            try {
                int destPos = 0;
                for (int sourceY = sourceOffsetY; sourceY <= sourceMaxY; sourceY += sourceStepY) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    synchronized (bandInfo.inStream) {
                        long lineStartPos = headerOffset + sourceY * lineSizeInBytes;
                        bandInfo.inStream.seek(lineStartPos + elemSize * sourceOffsetX * numBands);
                        lineData.readFrom(0, sourceWidth * numBands, bandInfo.inStream);
                    }
                    for (int x = 0; x < sourceWidth; x++) {
                        destBuffer.setElemDoubleAt(destPos++, lineData.getElemDoubleAt(x * numBands + bandIndex));
                    }
                    pm.worked(1);
                }
            } finally {
                pm.done();
            }

        } else {
            super.readBandRasterDataImpl(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight, sourceStepX, sourceStepY,
                                         destBand, destOffsetX, destOffsetY, destWidth, destHeight, destBuffer, pm);
        }
    }

    private static class BandInfo {
        boolean isComplex = false;
        ImageInputStream inStream;
        Header header;
    }
}
