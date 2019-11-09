/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.gamma;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.s1tbx.io.gamma.header.Header;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reader for stamps insar products
 */
public class GammaReader extends SARReader {

    private Header header;
    private boolean isComplex = false;
    private boolean isCoregistered = false;
    private Map<Band, ImageInputStream> bandImageInputStreamMap = new HashMap<>();

    public GammaReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    protected Product readProductNodesImpl() throws IOException {
        final Path inputParPath = getPathFromInput(getInput());
        final File inputParFile = inputParPath.toFile();

        try (BufferedReader headerReader = new BufferedReader(new FileReader(inputParFile))) {
            header = new Header(headerReader);
            isComplex = isComplex(inputParFile);
            isCoregistered = isCoregistered(inputParFile);

            String productType = isComplex ? "SLC" : header.getDataType();
            final Product product = new Product(header.getName(), productType,
                    header.getNumSamples(), header.getNumLines());
            product.setProductReader(this);
            product.setFileLocation(inputParFile);
            product.setDescription(header.getDescription());
            product.getMetadataRoot().addElement(header.getAsMetadata());

            final int dataType = getDataType();
            final File[] imageFiles = findImageFiles(inputParFile);

            for (File imgFile : imageFiles) {
                final ImageInputStream inStream = new FileImageInputStream(imgFile);
                inStream.setByteOrder(header.getJavaByteOrder());

                if (isComplex) {
                    final Band tgtBandI = new Band("i_" + imgFile.getName(), dataType, header.getNumSamples(), header.getNumLines());
                    tgtBandI.setUnit("real");
                    product.addBand(tgtBandI);

                    final Band tgtBandQ = new Band("q_" + imgFile.getName(), dataType, header.getNumSamples(), header.getNumLines());
                    tgtBandQ.setUnit("imaginary");
                    product.addBand(tgtBandQ);

                    bandImageInputStreamMap.put(tgtBandI, inStream);
                    bandImageInputStreamMap.put(tgtBandQ, inStream);

                    ReaderUtils.createVirtualIntensityBand(product, tgtBandI, tgtBandQ, imgFile.getName());
                    ReaderUtils.createVirtualPhaseBand(product, tgtBandI, tgtBandQ, "_" + imgFile.getName());
                } else {
                    final Band tgtBand = new Band(imgFile.getName(), dataType, header.getNumSamples(), header.getNumLines());
                    product.addBand(tgtBand);

                    bandImageInputStreamMap.put(tgtBand, inStream);
                }
            }

            addGeoCoding(product);

            addMetaData(product);

            return product;
        }
    }

    private boolean isComplex(final File file) {
        String name = file.getName().toLowerCase();
        name = FileUtils.getFilenameWithoutExtension(name);
        String ext = FileUtils.getExtension(name);
        return ext != null && (ext.endsWith("slc") || ext.endsWith("diff"));
    }

    private boolean isCoregistered(final File file) {
        String name = file.getName().toLowerCase();
        name = FileUtils.getFilenameWithoutExtension(name);
        String ext = FileUtils.getExtension(name);
        return ext != null && ext.endsWith("rslc");
    }

    private int getDataType() throws IOException {
        final String imageFormat = header.getDataType();
        if ("SCOMPLEX".equals(imageFormat)) {
            return ProductData.TYPE_INT16;
        } else if ("FCOMPLEX".endsWith(imageFormat)) {
            return ProductData.TYPE_FLOAT32;
        }
        return ProductData.TYPE_FLOAT32;
    }

    private File[] findImageFiles(final File parFile) {
        final File[] files = parFile.getParentFile().listFiles();
        final String baseName = FileUtils.getFilenameWithoutExtension(parFile);
        final List<File> fileList = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith(baseName) && !file.getName().equals(parFile.getName())) {
                    fileList.add(file);
                }
            }
        }
        return fileList.toArray(new File[fileList.size()]);
    }

    private File getImageFile(final File parFile) {
        return new File(parFile.getParentFile(), FileUtils.getFilenameWithoutExtension(parFile));
    }

    private void addGeoCoding(final Product product) {

    }

    private void addMetaData(final Product product) {
        final MetadataElement root = product.getMetadataRoot();
        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, header.getName());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION, header.getSensorType());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SPH_DESCRIPTOR, header.getDescription());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, isComplex ? "SLC" : header.getDataType());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.sample_type, isComplex ? "COMPLEX" : "DETECTED");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line, header.getNumSamples());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines, header.getNumLines());

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, header.getStartTime());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, header.getEndTime());
        product.setStartTime(header.getStartTime());
        product.setEndTime(header.getEndTime());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval, header.getLineTimeInterval());

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.radar_frequency, header.getRadarFrequency());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency, header.getPRF());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_looks, header.getRangeLooks());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks, header.getAzimuthLooks());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.coregistered_stack, isCoregistered ? 1 : 0);

    }

    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY,
                                          int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY,
                                          Band destBand,
                                          int destOffsetX, int destOffsetY,
                                          int destWidth, int destHeight,
                                          ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {

        final ImageInputStream inStream = bandImageInputStreamMap.get(destBand);

        if (isComplex) {

            final int sourceMaxY = sourceOffsetY + sourceHeight;
            final int elemSize = destBuffer.getElemSize();
            int bandIndex = 0;
            if (destBand.getUnit() != null && destBand.getUnit().equals(Unit.IMAGINARY)) {
                bandIndex = 1;
            }

            // band interleaved by pixel
            int numInterleaved = 2;
            final long lineSizeInBytes = header.getNumSamples() * numInterleaved * elemSize;
            ProductData lineData = ProductData.createInstance(destBuffer.getType(), sourceWidth * numInterleaved);

            pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceMaxY - sourceOffsetY);
            try {
                final long xPos = elemSize * sourceOffsetX * numInterleaved;
                final int xLength = sourceWidth * numInterleaved;
                int dstCnt = 0;
                for (int sourceY = sourceOffsetY; sourceY < sourceMaxY; sourceY += sourceStepY) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    synchronized (inStream) {
                        inStream.seek(sourceY * lineSizeInBytes + xPos);
                        lineData.readFrom(0, xLength, inStream);
                    }
                    for (int x = 0; x < sourceWidth; x++) {
                        destBuffer.setElemDoubleAt(dstCnt++, lineData.getElemDoubleAt(x * numInterleaved + bandIndex));
                    }
                    pm.worked(1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                pm.done();
            }

        } else {
            final int sourceMaxY = sourceOffsetY + sourceHeight - 1;
            Product product = destBand.getProduct();
            final int elemSize = destBuffer.getElemSize();

            final int headerOffset = header.getHeaderOffset();
            final int bandIndex = product.getBandIndex(destBand.getName());

            final long lineSizeInBytes = header.getNumSamples() * elemSize;
            ProductData lineData = ProductData.createInstance(destBuffer.getType(), sourceWidth);

            pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceMaxY - sourceOffsetY);
            try {
                int destPos = 0;
                for (int sourceY = sourceOffsetY; sourceY <= sourceMaxY; sourceY += sourceStepY) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    synchronized (inStream) {
                        long lineStartPos = headerOffset + sourceY * lineSizeInBytes;
                        inStream.seek(lineStartPos + elemSize * sourceOffsetX);
                        lineData.readFrom(0, sourceWidth, inStream);
                    }
                    for (int x = 0; x < sourceWidth; x++) {
                        destBuffer.setElemDoubleAt(destPos++, lineData.getElemDoubleAt(x + bandIndex));
                    }
                    pm.worked(1);
                }
            } catch (Exception e) {

            } finally {
                pm.done();
            }
        }
    }

    @Override
    public void close() throws IOException {
        for (ImageInputStream inStream : bandImageInputStreamMap.values()) {
            if (inStream != null) {
                inStream.close();
            }
        }
        super.close();
    }
}
