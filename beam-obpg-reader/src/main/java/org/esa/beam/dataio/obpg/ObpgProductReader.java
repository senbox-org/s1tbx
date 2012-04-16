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
package org.esa.beam.dataio.obpg;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.io.CsvReader;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObpgProductReader extends AbstractProductReader {

    ObpgUtils obpgUtils = new ObpgUtils();
    private Map<Band, Variable> variableMap;
    private boolean mustFlip;
    private NetcdfFile ncfile;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    protected ObpgProductReader(ObpgProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {

        try {
            final HashMap<String, String> l2BandInfoMap = getL2BandInfoMap();
            final HashMap<String, String> l2FlagsInfoMap = getL2FlagsInfoMap();

            final File inFile = ObpgUtils.getInputFile(getInput());
            final String path = inFile.getPath();
            ncfile = NetcdfFile.open(path);
            
            final Product product = obpgUtils.createProductBody(ncfile.getGlobalAttributes());
            mustFlip = obpgUtils.mustFlip(ncfile);
            obpgUtils.addGlobalMetadata(product, ncfile.getGlobalAttributes());
            obpgUtils.addScientificMetadata(product, ncfile);
            variableMap = obpgUtils.addBands(product, ncfile, l2BandInfoMap, l2FlagsInfoMap);
            product.setProductReader(this);
            obpgUtils.addGeocoding(product, ncfile, mustFlip);
            obpgUtils.addBitmaskDefinitions(product, l2FlagsInfoMap);
            product.setFileLocation(inFile);
            return product;
        } catch (IOException e) {
            throw new ProductIOException(e.getMessage());
        }
    }
    
    @Override
    public void close() throws IOException {
        if (ncfile != null) {
            ncfile.close();
        }
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth,
                                                       int sourceHeight,
                                                       int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                                       int destOffsetY, int destWidth, int destHeight,
                                                       ProductData destBuffer,
                                                       ProgressMonitor pm) throws IOException {

        if (mustFlip) {
            sourceOffsetY = destBand.getSceneRasterHeight() - (sourceOffsetY + sourceHeight);
            sourceOffsetX = destBand.getSceneRasterWidth() - (sourceOffsetX + sourceWidth);
        }
        Variable variable = variableMap.get(destBand);
        try {
            readBandData(variable, sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight, destBuffer, pm);
            if (mustFlip) {
                reverse(destBuffer);
            }
        } catch (Exception e) {
            final ProductIOException exception = new ProductIOException(e.getMessage());
            exception.setStackTrace(e.getStackTrace());
            throw exception;
        }
    }
    
    private void readBandData(Variable variable, int sourceOffsetX, int sourceOffsetY, int sourceWidth,
                              int sourceHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException,
                                                                                                   InvalidRangeException {

        final int[] start = new int[] {sourceOffsetY, sourceOffsetX};
        final int[] stride = new int[] {1, 1};
        final int[] count = new int[] {1, sourceWidth};
        Object buffer = destBuffer.getElems();

        int targetIndex = 0;
        pm.beginTask("Reading band '" + variable.getShortName() + "'...", sourceHeight);
        // loop over lines
        try {
            for (int y = 0; y < sourceHeight; y++) {
                if (pm.isCanceled()) {
                    break;
                }
                Section section = new Section(start, count, stride);
                Array array;
                synchronized (ncfile) {
                    array = variable.read(section);
                }
                final Object storage = array.getStorage();
                System.arraycopy(storage, 0, buffer, targetIndex, sourceWidth);
                start[0]++;
                targetIndex += sourceWidth;
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    public static void reverse(ProductData data) {
        final int n = data.getNumElems();
        final int nc = n / 2;
        for (int i1 = 0; i1 < nc; i1++) {
            int i2 = n - 1 - i1;
            double temp = data.getElemDoubleAt(i1);
            data.setElemDoubleAt(i1, data.getElemDoubleAt(i2));
            data.setElemDoubleAt(i2, temp);
        }
    }

    public static void reverse(float[] data) {
        final int n = data.length;
        final int nc = n / 2;
        for (int i1 = 0; i1 < nc; i1++) {
            int i2 = n - 1 - i1;
            float temp = data[i1];
            data[i1] = data[i2];
            data[i2] = temp;
        }
    }

    private synchronized static HashMap<String, String> getL2BandInfoMap() {
        return readTwoColumnTable("l2-band-info.csv");
    }

    private synchronized static HashMap<String, String> getL2FlagsInfoMap() {
        return readTwoColumnTable("l2-flags-info.csv");
    }

    private static HashMap<String, String> readTwoColumnTable(String resourceName) {
        final InputStream stream = ObpgProductReader.class.getResourceAsStream(resourceName);
        if (stream != null) {
            try {
                HashMap<String, String> validExpressionMap = new HashMap<String, String>(32);
                final CsvReader csvReader = new CsvReader(new InputStreamReader(stream), new char[]{';'});
                final List<String[]> table = csvReader.readStringRecords();
                for (String[] strings : table) {
                    if (strings.length == 2) {
                        validExpressionMap.put(strings[0], strings[1]);
                    }
                }
                return validExpressionMap;
            } catch (IOException e) {
                // ?
            } finally {
                try {
                    stream.close();
                } catch (IOException e) {
                    // ok
                }
            }
        }
        return new HashMap<String, String>(0);
    }

}
