/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.netcdf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.FileUtils;
import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFileWriteable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class NetCDFWriter extends AbstractProductWriter {

    private File outputFile = null;
    private NetcdfFileWriteable netCDFWriteable = null;

    /**
     * Construct a new instance of a product writer for the given product writer plug-in.
     *
     * @param writerPlugIn the given product writer plug-in, must not be <code>null</code>
     */
    public NetCDFWriter(final ProductWriterPlugIn writerPlugIn) {
        super(writerPlugIn);
    }

    private static float[] getLonData(final Product product, final String lonGridName) {
        final int size = product.getSceneRasterWidth();
        final TiePointGrid lonGrid = product.getTiePointGrid(lonGridName);
        if(lonGrid != null)
            return lonGrid.getPixels(0, 0, size, 1, (float[])null);
        return null;
    }

    private static float[] getLatData(final Product product, final String latGridName) {
        final int size = product.getSceneRasterHeight();
        final TiePointGrid latGrid = product.getTiePointGrid(latGridName);
        if(latGrid != null)
            return latGrid.getPixels(0, 0, 1, size, (float[])null);
        return null;
    }

    private static float[][] getTiePointGridData(final TiePointGrid tpg) {
        final float[][] data = new float[tpg.getRasterHeight()][tpg.getRasterWidth()];
        final ProductData productData = tpg.getData();
        for(int y=0; y < tpg.getRasterHeight(); ++y) {
            final int stride = y * tpg.getRasterWidth();
            for(int x=0; x < tpg.getRasterWidth(); ++x) {
                data[y][x] = productData.getElemFloatAt(stride + x);
            }
        }
        return data;
    }

    /**
     * Writes the in-memory representation of a data product. This method was called by <code>writeProductNodes(product,
     * output)</code> of the AbstractProductWriter.
     *
     * @throws IllegalArgumentException if <code>output</code> type is not one of the supported output sources.
     * @throws java.io.IOException      if an I/O error occurs
     */
    @Override
    protected void writeProductNodesImpl() throws IOException {
        outputFile = null;

        final File file;
        if (getOutput() instanceof String) {
            file = new File((String) getOutput());
        } else {
            file = (File) getOutput();
        }

        outputFile = FileUtils.ensureExtension(file, NetcdfConstants.NETCDF_FORMAT_FILE_EXTENSIONS[0]);
        deleteOutput();

        final Product product = getSourceProduct();

        netCDFWriteable = NetcdfFileWriteable.createNew(outputFile.getAbsolutePath(), true);


        netCDFWriteable.addDimension(NetcdfConstants.LON_VAR_NAMES[0], product.getSceneRasterWidth());
        netCDFWriteable.addDimension(NetcdfConstants.LAT_VAR_NAMES[0], product.getSceneRasterHeight());

        final Group rootGroup = netCDFWriteable.getRootGroup();
        netCDFWriteable.addVariable(NetcdfConstants.LAT_VAR_NAMES[0], DataType.FLOAT,
                new Dimension[]{rootGroup.findDimension(NetcdfConstants.LAT_VAR_NAMES[0])});
        netCDFWriteable.addVariableAttribute(NetcdfConstants.LAT_VAR_NAMES[0], "units", "degrees_north (+N/-S)");
        netCDFWriteable.addVariable(NetcdfConstants.LON_VAR_NAMES[0], DataType.FLOAT,
                new Dimension[]{rootGroup.findDimension(NetcdfConstants.LON_VAR_NAMES[0])});
        netCDFWriteable.addVariableAttribute(NetcdfConstants.LON_VAR_NAMES[0], "units", "degrees_east (+E/-W)");

        for(Band band : product.getBands()) {
            final String name = StringUtils.createValidName(band.getName(), new char[]{'_'}, '_');
            netCDFWriteable.addVariable(name, DataType.DOUBLE,
                    new Dimension[]{rootGroup.findDimension(NetcdfConstants.LAT_VAR_NAMES[0]),
                                    rootGroup.findDimension(NetcdfConstants.LON_VAR_NAMES[0])});
            if(band.getDescription() != null)
                netCDFWriteable.addVariableAttribute(name, "description", band.getDescription());
            if(band.getUnit() != null)
                netCDFWriteable.addVariableAttribute(name, "unit", band.getUnit());
        }

        for(TiePointGrid tpg : product.getTiePointGrids()) {
            final String name = tpg.getName();
            netCDFWriteable.addDimension(name+'x', tpg.getRasterWidth());
            netCDFWriteable.addDimension(name+'y', tpg.getRasterHeight());
            netCDFWriteable.addVariable(name, DataType.FLOAT,
                    new Dimension[]{rootGroup.findDimension(name+'y'), rootGroup.findDimension(name+'x')});
            if(tpg.getDescription() != null)
                netCDFWriteable.addVariableAttribute(name, "description", tpg.getDescription());
            if(tpg.getUnit() != null)
                netCDFWriteable.addVariableAttribute(name, "unit", tpg.getUnit());
        }

        addMetadata(product);

        netCDFWriteable.create();


        final GeoCoding sourceGeoCoding = product.getGeoCoding();
        String latGridName = "latitude";
        String lonGridName = "longitude";
        if (sourceGeoCoding instanceof TiePointGeoCoding) {
            final TiePointGeoCoding geoCoding = (TiePointGeoCoding) sourceGeoCoding;
            latGridName = geoCoding.getLatGrid().getName();
            lonGridName = geoCoding.getLonGrid().getName();
        }

        final float[] latData = getLatData(product, latGridName);
        final float[] lonData = getLonData(product, lonGridName);
        if(latData != null && lonData != null) {
            final Array latNcArray = Array.factory(latData);
            final Array lonNcArray = Array.factory(lonData);

            try {
                netCDFWriteable.write(NetcdfConstants.LAT_VAR_NAMES[0], latNcArray);
                netCDFWriteable.write(NetcdfConstants.LON_VAR_NAMES[0], lonNcArray);

                for(TiePointGrid tpg : product.getTiePointGrids()) {
                    final Array tpgArray = Array.factory(getTiePointGridData(tpg));
                    netCDFWriteable.write(tpg.getName(), tpgArray);
                }
            } catch (InvalidRangeException rangeE) {
                rangeE.printStackTrace();
                throw new RuntimeException(rangeE);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void writeBandRasterData(final Band sourceBand,
                                    final int regionX,
                                    final int regionY,
                                    final int regionWidth,
                                    final int regionHeight,
                                    final ProductData regionData,
                                    ProgressMonitor pm) throws IOException {

        final int[] origin = new int[2];
        origin[1] = regionX;
        origin[0] = regionY;
        try {

            final ArrayDouble dataTemp = new ArrayDouble.D2(regionHeight, regionWidth);
            final Index index = dataTemp.getIndex();

            int i=0;
            for(int y=0; y < regionHeight; ++y) {
                for(int x=0; x < regionWidth; ++x) {
                    index.set(y, x);
                    dataTemp.set(index, regionData.getElemDoubleAt(i));
                    ++i;
                }
            }

            netCDFWriteable.write(sourceBand.getName(), origin, dataTemp);

            pm.worked(1);

        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Deletes the physically representation of the given product from the hard disk.
     */
    public void deleteOutput() {
        if (outputFile != null && outputFile.isFile()) {
            outputFile.delete();
        }
    }

    /**
     * Closes all output streams currently open.
     *
     * @throws java.io.IOException on failure
     */
    public void close() throws IOException {
        netCDFWriteable.close();
    }

    /**
     * Writes all data in memory to disk. After a flush operation, the writer can be closed safely
     *
     * @throws IOException on failure
     */
    public void flush() throws IOException {
        if (netCDFWriteable == null) {
            return;
        }
        netCDFWriteable.flush();
    }

    /**
     * Returns wether the given product node is to be written.
     *
     * @param node the product node
     *
     * @return <code>true</code> if so
     */
    @Override
    public boolean shouldWrite(ProductNode node) {
        return !(node instanceof VirtualBand) && super.shouldWrite(node);
    }

    private void addMetadata(final Product product) {

        final MetadataElement rootElem = product.getMetadataRoot();
        final Group rootGroup = netCDFWriteable.getRootGroup();

        addElements(rootElem, rootGroup);
        addAttributes(rootElem, rootGroup);
    }

    private void addElements(final MetadataElement parentElem, final Group parentGroup) {
        final Map<String, Integer> dupeCntElem = new HashMap<String, Integer>();

        for (int i = 0; i < parentElem.getNumElements(); i++) {
            final MetadataElement subElement = parentElem.getElementAt(i);
            final String name = subElement.getName();
            boolean lastDupe = true;
            for (int j = i+1; j < parentElem.getNumElements(); j++) {
                final MetadataElement dupeElement = parentElem.getElementAt(j);
                if(dupeElement.getName().equals(name)) {
                    Integer cnt = dupeCntElem.get(name);
                    if(cnt == null)
                        dupeCntElem.put(name, 1);
                    else {
                        ++cnt;
                        dupeCntElem.put(name, cnt);
                    }
                    lastDupe = false;
                    break;
                }
            }
            if(dupeCntElem.get(name) != null) {
                int cnt = dupeCntElem.get(name);
                if(lastDupe)
                    ++cnt;
                subElement.setName(subElement.getName()+"."+cnt);
            }

            final Group newGroup = new Group(netCDFWriteable, parentGroup, subElement.getName());
            addAttributes(subElement, newGroup);
            // recurse
            addElements(subElement, newGroup);

            netCDFWriteable.addGroup(parentGroup, newGroup);
        }
    }

    private void addAttributes(final MetadataElement elem, final Group newGroup) {
        final Map<String, Integer> dupeCntAtrib = new HashMap<String, Integer>();

        for (int i = 0; i < elem.getNumAttributes(); i++) {
            final MetadataAttribute attrib = elem.getAttributeAt(i);
            final String name = attrib.getName();
            boolean lastDupe = true;
            for (int j = i+1; j < elem.getNumAttributes(); j++) {
                final MetadataAttribute dupeAtrib = elem.getAttributeAt(j);
                if(dupeAtrib.getName().equals(name)) {
                    Integer cnt = dupeCntAtrib.get(name);
                    if(cnt == null)
                        dupeCntAtrib.put(name, 1);
                    else {
                        ++cnt;
                        dupeCntAtrib.put(name, cnt);
                    }
                    lastDupe = false;
                    break;
                }
            }
            if(dupeCntAtrib.get(name) != null) {
                int cnt = dupeCntAtrib.get(name);
                if(lastDupe)
                    ++cnt;
                attrib.setName(attrib.getName()+"."+cnt);
            }

            final int dataType = attrib.getDataType();
            if(dataType == ProductData.TYPE_FLOAT32 || dataType == ProductData.TYPE_FLOAT64) {
                newGroup.addAttribute(new Attribute(attrib.getName(), elem.getAttributeDouble(attrib.getName(), 0)));
            } else if(dataType == ProductData.TYPE_UTC || attrib.getData() instanceof ProductData.UTC) {
                newGroup.addAttribute(new Attribute(attrib.getName(), NetcdfConstants.UTC_TYPE+elem.getAttributeString(attrib.getName(), " ")));
            } else if(dataType > ProductData.TYPE_INT8 && dataType < ProductData.TYPE_FLOAT32) {
                newGroup.addAttribute(new Attribute(attrib.getName(), elem.getAttributeInt(attrib.getName(), 0)));
            } else {
                newGroup.addAttribute(new Attribute(attrib.getName(), elem.getAttributeString(attrib.getName(), " ")));
            }
        }
    }
}