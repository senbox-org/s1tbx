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

package org.esa.beam.gpf.operators.meris;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;

/**
 * The <code>N1PatcherOp</code> copies an existing N1 file
 * and replaces the data for the radiance band with the data
 * from the given product
 * <p/>
 * Configuration Elements:
 * <ul>
 * <li><b>originalFilePath</b> the path of the N1 file that should be patched
 * <li><b>patchFilePath</b> the path for the patched file
 * </ul>
 *
 * @author Marco Zuehlke
 */
@OperatorMetadata(alias = "N1Patcher", internal = true)
public class N1PatcherOp extends MerisBasisOp {

    // MPH:
    private static final int MPH_PRODUCTNAME_OFFSET = 9;

    private static final int MPH_PRODUCTNAME_LENGTH = 62;

    private static final int MPH_TOT_SIZE_OFFSET = 1076;

    private static final int MPH_TOT_SIZE_LENGTH = 20;

    private static final int MPH_SPH_SIZE_OFFSET = 1114;

    private static final int MPH_SPH_SIZE_LENGTH = 10;

    private static final int MPH_NUM_DSD_OFFSET = 1141;

    private static final int MPH_NUM_DSD_LENGTH = 10;

    private static final int MPH_DSD_SIZE_OFFSET = 1162;

    private static final int MPH_DSD_SIZE_LENGTH = 10;

    private static final int MPH_SIZE = 1247; // Size of main product header

    // DSD:
    private static final int DSD_DS_NAME_OFFSET = 9;

    private static final int DSD_DS_NAME_LENGTH = 28;

    private static final int DSD_DS_TYPE_OFFSET = 47;

    private static final int DSD_DS_OFFSET_LENGTH = 21;

    private static final int DSD_DS_OFFSET_OFFSET = 133;

    private static final int DSD_DS_SIZE_LENGTH = 21;

    private static final int DSD_DS_SIZE_OFFSET = 170;

    private static final int DSD_NUM_DSR_LENGTH = 11;

    private static final int DSD_NUM_DSR_OFFSET = 207;

    private static final int DSD_DSR_SIZE_LENGTH = 11;

    private static final int DSD_DSR_SIZE_OFFSET = 228;

    private static final int DSR_HEADER_SIZE = 13;

    private int dsd_size; // Size of a dataset descriptor

    private int num_dsd; // Number of dataset descriptors

    private int sph_size; // Size of specific product header

    private DatasetDescriptor[] dsDescriptors;

    private ImageInputStream inputStream;
    private ImageOutputStream outputStream;
    private final Object syncObject = new Object();

    @Parameter(description = "The file to which the patched L1b product is written.")
    private File patchedFile = null;

    @SourceProduct(alias = "n1")
    private Product n1Product;
    @SourceProduct(alias = "input")
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;


    @Override
    public void initialize() throws OperatorException {
        targetProduct = createCompatibleProduct(n1Product, "n1Product", "MER_L1");
        for (String bandName : n1Product.getBandNames()) {
            if (!"l1_flags".equals(bandName)) {
                ProductUtils.copyBand(bandName, n1Product, targetProduct);
            }
        }
        ProductUtils.copyFlagBands(n1Product, targetProduct);
        try {
            File originalFileLocation = n1Product.getFileLocation();

            synchronized (syncObject) {
                inputStream = new FileImageInputStream(originalFileLocation);
                outputStream = new FileImageOutputStream(patchedFile);
                byte[] mph = parseMPH();
                byte[] sph = parseSPH();
                copyHeader(mph, sph);
            }
        } catch (IOException e) {
            throw new OperatorException(e);
        }
    }

    private byte[] parseMPH() throws IOException {
        byte[] mph = new byte[MPH_SIZE];

        // read complete MPH
        inputStream.seek(0);
        inputStream.read(mph);

        num_dsd = Integer.parseInt(new String(mph, MPH_NUM_DSD_OFFSET,
                                              MPH_NUM_DSD_LENGTH));
        sph_size = Integer.parseInt(new String(mph, MPH_SPH_SIZE_OFFSET,
                                               MPH_SPH_SIZE_LENGTH));
        dsd_size = Integer.parseInt(new String(mph, MPH_DSD_SIZE_OFFSET,
                                               MPH_DSD_SIZE_LENGTH));
        return mph;
    }

    private byte[] parseSPH() throws IOException {
        byte[] sph = new byte[sph_size];

        // read complete SPH
        inputStream.seek(MPH_SIZE);
        inputStream.read(sph);

        dsDescriptors = new DatasetDescriptor[num_dsd];

        // calculate position of first DSD
        int dsdPtr = (sph_size) - (num_dsd * dsd_size);

        for (int i = 0; i < num_dsd; i++) {
            DatasetDescriptor dsd = new DatasetDescriptor();

            // is this DSD a spare ?
            dsd.isSpare = (sph[dsdPtr] != 'D');
            if (!dsd.isSpare) {
                // remeber offset of DSd (for patching)
                dsd.dsdPtr = MPH_SIZE + dsdPtr;

                dsd.dsType = readCharBuf(sph, dsdPtr + DSD_DS_TYPE_OFFSET);
                dsd.dsOffset = readIntBuf(sph, dsdPtr + DSD_DS_OFFSET_OFFSET
                                               + 1, DSD_DS_OFFSET_LENGTH - 1);
                dsd.dsSize = readIntBuf(sph, dsdPtr + DSD_DS_SIZE_OFFSET + 1,
                                        DSD_DS_SIZE_LENGTH - 1);
                dsd.numDsr = readIntBuf(sph, dsdPtr + DSD_NUM_DSR_OFFSET + 1,
                                        DSD_NUM_DSR_LENGTH - 1);
                dsd.dsrSize = readIntBuf(sph, dsdPtr + DSD_DSR_SIZE_OFFSET + 1,
                                         DSD_DSR_SIZE_LENGTH - 1);
                dsd.dsName = readStringBuf(sph, dsdPtr + DSD_DS_NAME_OFFSET,
                                           DSD_DS_NAME_LENGTH);
            }
            dsDescriptors[i] = dsd;
            dsdPtr += dsd_size;
        }
        return sph;
    }

    private void copyHeader(byte[] mph, byte[] sph) throws IOException {
        outputStream.seek(0);
        outputStream.write(mph);
        outputStream.write(sph);

        for (DatasetDescriptor descriptor : dsDescriptors) {
            byte[] buf = new byte[descriptor.dsSize];

            if (descriptor.dsName == null
                    || !descriptor.dsName.startsWith("Radiance")) {
                inputStream.seek(descriptor.dsOffset);
                inputStream.read(buf);
                outputStream.seek(descriptor.dsOffset);
                outputStream.write(buf);
            }
        }
    }

    private int readIntBuf(final byte[] buf, final int offset,
                           final int length) {
        return Integer.parseInt(new String(buf, offset, length));
    }

    private char readCharBuf(final byte[] buf, final int offset) {
        return (char) buf[offset];
    }

    private String readStringBuf(final byte[] buf, final int offset,
                                 final int length) {
        return new String(buf, offset, length);
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle rectangle = targetTile.getRectangle();
        pm.beginTask("Patching product...", rectangle.height);
        try {
            Tile srcTile = getSourceTile(sourceProduct.getBand(band.getName()), rectangle, pm);
            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    targetTile.setSample(x, y, srcTile.getSampleDouble(x, y));
                }
            }
            synchronized (syncObject) {
                if (band.getName().startsWith("radiance")) {
                    DatasetDescriptor descriptor = getDatasetDescriptorForBand(band);
                    if (descriptor != null) {
                        short[] data = (short[]) srcTile.getRawSamples().getElems();

                        byte[] buf = new byte[rectangle.height * descriptor.dsrSize];
                        final long dsrOffset = descriptor.dsOffset + rectangle.y * descriptor.dsrSize;
                        inputStream.seek(dsrOffset);
                        inputStream.read(buf);
                        outputStream.seek(dsrOffset);
                        for (int y = 0; y < rectangle.height; y++) {
                            outputStream.write(buf, y * descriptor.dsrSize, DSR_HEADER_SIZE);
                            outputStream.skipBytes(
                                    (targetProduct.getSceneRasterWidth() - rectangle.width - rectangle.x) * 2);
                            for (int x = rectangle.width - 1; x >= 0; x--) {
                                outputStream.writeShort(data[x + y * rectangle.width]);
                            }
                            outputStream.skipBytes((rectangle.x) * 2);
                            checkForCancellation(pm);
                            pm.worked(1);
                        }
                    }
                } else if ("l1_flags".equals(band.getName())) {
                    DatasetDescriptor descriptor = getDatasetDescriptorForFlagBand();
                    if (descriptor != null) {
                        byte[] data = (byte[]) srcTile.getRawSamples().getElems();

                        final long dsrOffset = descriptor.dsOffset + rectangle.y * descriptor.dsrSize;
                        outputStream.seek(dsrOffset);
                        for (int y = 0; y < rectangle.height; y++) {
                            outputStream.skipBytes(DSR_HEADER_SIZE);
                            outputStream.skipBytes(targetProduct.getSceneRasterWidth() - rectangle.width - rectangle.x);
                            for (int x = rectangle.width - 1; x >= 0; x--) {
                                outputStream.writeByte(data[x + y * rectangle.width]);
                            }
                            outputStream.skipBytes(rectangle.x);
                            outputStream.skipBytes(targetProduct.getSceneRasterWidth() * 2);
                            checkForCancellation(pm);
                            pm.worked(1);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new OperatorException(e);
        } finally {
            pm.done();
        }
    }

    private DatasetDescriptor getDatasetDescriptorForFlagBand() {
        for (DatasetDescriptor dsDescriptor : dsDescriptors) {
            DatasetDescriptor descriptor = dsDescriptor;
            final String dsName = descriptor.dsName;
            if (dsName != null && dsName.startsWith("Flag")) {
                return descriptor;
            }
        }
        return null;
    }

    private DatasetDescriptor getDatasetDescriptorForBand(Band band) {
        for (DatasetDescriptor dsDescriptor : dsDescriptors) {
            DatasetDescriptor descriptor = dsDescriptor;
            final String dsName = descriptor.dsName;
            if (dsName != null && dsName.startsWith("Radiance")) {
                int beginIndex = dsName.indexOf('(');
                int endIndex = dsName.indexOf(')', beginIndex);
                String bandNumber = dsName.substring(beginIndex + 1, endIndex);
                String bandName = "radiance_" + bandNumber;
                if (bandName.equals(band.getName())) {
                    return descriptor;
                }
            }
        }
        return null;
    }

    @Override
    public void dispose() {
        try {
            synchronized (syncObject) {
                targetProduct.closeIO();
                inputStream.close();
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final class DatasetDescriptor {

        // true, if descriptor is empty
        private boolean isSpare = false;

        private char dsType;

        private int dsSize;

        private long dsOffset;

        private long dsdPtr;

        // num records
        private int numDsr;

        // record size
        private int dsrSize;

        private String dsName;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(N1PatcherOp.class);
        }
    }
}
