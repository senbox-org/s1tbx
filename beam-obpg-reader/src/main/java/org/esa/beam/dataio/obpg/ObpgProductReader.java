/*
 * $Id$
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.dataio.obpg;

import org.esa.beam.dataio.obpg.ObpgUtils;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.io.CsvReader;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.DOMBuilder;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.bc.ceres.core.ProgressMonitor;

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
            final BitmaskDef[] defs = getDefaultBitmaskDefs(l2FlagsInfoMap);
            
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
            obpgUtils.addBitmaskDefinitions(product, defs);
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
                                                                                           ProductIOException,
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
                synchronized (variable) {
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

    private static BitmaskDef[] getDefaultBitmaskDefs(HashMap<String, String> l2FlagsInfoMap) {
        final InputStream stream = ObpgProductReader.class.getResourceAsStream("l2-bitmask-definitions.xml");
        if (stream != null) {
            try {
                final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                final DocumentBuilder builder = factory.newDocumentBuilder();
                final org.w3c.dom.Document w3cDocument = builder.parse(stream);
                final Document document = new DOMBuilder().build(w3cDocument);
                final List<Element> children = document.getRootElement().getChildren("Bitmask_Definition");
                final ArrayList<BitmaskDef> bitmaskDefList = new ArrayList<BitmaskDef>(children.size());
                for (Element element : children) {
                    final BitmaskDef bitmaskDef = BitmaskDef.createBitmaskDef(element);
                    final String description = l2FlagsInfoMap.get(bitmaskDef.getName());
                    bitmaskDef.setDescription(description);
                    bitmaskDefList.add(bitmaskDef);
                }
                return bitmaskDefList.toArray(new BitmaskDef[bitmaskDefList.size()]);
            } catch (Exception e) {
                // ?
            } finally {
                try {
                    stream.close();
                } catch (IOException e) {
                    // ?
                }
            }
        }
        return new BitmaskDef[0];
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
