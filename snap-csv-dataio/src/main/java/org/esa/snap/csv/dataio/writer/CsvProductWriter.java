/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.csv.dataio.writer;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.AbstractProductWriter;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.csv.dataio.Constants;

import java.awt.image.DataBuffer;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Allows writing a {@link Product} in CSV format.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class CsvProductWriter extends AbstractProductWriter {

    public static final int WRITE_PROPERTIES = 1;
    public static final int WRITE_FEATURES = 2;

    protected final int config;
    protected Writer writer;

    private String separator;
    private boolean productWritten;

    public CsvProductWriter(ProductWriterPlugIn plugIn, int config, Writer writer) {
        super(plugIn);
        this.writer = writer;
        this.config = config;
        this.productWritten = false;
    }

    @Override
    protected void writeProductNodesImpl() throws IOException {
        if (writer == null) {
            writer = new FileWriter(new File(getOutput().toString()));
        }
        getSeparatorFromMetaData();
        writeProperties();
        writeHeader();
    }

    private void writeHeader() throws IOException {
        final Band[] bands = getSourceProduct().getBands();
        StringBuilder builder = new StringBuilder();
        builder.append(getFeatureIdColumnNameFromMetadata());
        for (final Band band : bands) {
            builder.append(separator);
            builder.append(band.getName());
            builder.append(":");
            final int dataType = band.getGeophysicalImage().getData().getDataBuffer().getDataType();
            builder.append(getJavaType(dataType));
        }
        final TiePointGrid[] tiePointGrids = getSourceProduct().getTiePointGrids();
        for (final TiePointGrid tpg : tiePointGrids) {
            builder.append(separator);
            builder.append(tpg.getName());
            builder.append(":");
            final int dataType = tpg.getGeophysicalImage().getData().getDataBuffer().getDataType();
            builder.append(getJavaType(dataType));
        }
        writeLine(builder.toString());
    }

    private String getJavaType(int dataType) {
        switch (dataType) {
            case DataBuffer.TYPE_FLOAT: {
                return "float";
            }
            case DataBuffer.TYPE_DOUBLE: {
                return "double";
            }
            case DataBuffer.TYPE_BYTE: {
                return "byte";
            }
            case DataBuffer.TYPE_SHORT: {
                return "short";
            }
            case DataBuffer.TYPE_USHORT: {
                return "ushort";
            }
            case DataBuffer.TYPE_INT: {
                return "int";
            }
            default: {
                throw new IllegalArgumentException("Unsupported type '" + dataType + "'.");
            }
        }
    }

    private void writeLine(String line) throws IOException {
        writer.write(line);
        writer.write("\n");
    }

    private void writeProperties() throws IOException {
        if ((config & WRITE_PROPERTIES) != WRITE_PROPERTIES) {
            return;
        }
        writeLine("#" + Constants.PROPERTY_NAME_SCENE_RASTER_WIDTH + "=" + getSourceProduct().getSceneRasterWidth());
    }

    @Override
    public void writeBandRasterData(Band sourceBand, int sourceOffsetX, int sourceOffsetY,
                                    int sourceWidth, int sourceHeight, ProductData sourceBuffer,
                                    ProgressMonitor pm) throws IOException {
        if (productWritten) {
            return;
        }
        final Band[] bands = getSourceProduct().getBands();
        final DataBuffer[] bandDataBuffers = new DataBuffer[bands.length];
        final int[] bandTypes = new int[bands.length];
        for (int i = 0; i < bands.length; i++) {
            final Band band = bands[i];
            bandDataBuffers[i] = band.getGeophysicalImage().getData().getDataBuffer();
            bandTypes[i] = bandDataBuffers[i].getDataType();
        }

        final TiePointGrid[] tiePointGrids = getSourceProduct().getTiePointGrids();
        final DataBuffer[] tpgDataBuffers = new DataBuffer[tiePointGrids.length];
        final int[] tpgTypes = new int[tiePointGrids.length];
        for (int i = 0; i < tiePointGrids.length; i++) {
            final TiePointGrid tpg = tiePointGrids[i];
            tpgDataBuffers[i] = tpg.getGeophysicalImage().getData().getDataBuffer();
            tpgTypes[i] = tpgDataBuffers[i].getDataType();
        }

        for (int j = 0; j < getSourceProduct().getSceneRasterHeight(); j++) {
            for (int i = 0; i < getSourceProduct().getSceneRasterWidth(); i++) {
                StringBuilder line = new StringBuilder();
                final int index = j * getSourceProduct().getSceneRasterWidth() + i;
                line.append(getFeatureIdFromMetadata(index));
                for (int k = 0; k < bandDataBuffers.length; k++) {
                    final DataBuffer buffer = bandDataBuffers[k];
                    line.append(separator);
                    final Number elem;
                    final int type = bandTypes[k];
                    if (type == DataBuffer.TYPE_INT || type == DataBuffer.TYPE_SHORT ||
                        type == DataBuffer.TYPE_USHORT || type == DataBuffer.TYPE_BYTE) {
                        elem = buffer.getElem(index);
                    } else if (type == DataBuffer.TYPE_FLOAT) {
                        elem = buffer.getElemFloat(index);
                    } else if (type == DataBuffer.TYPE_DOUBLE) {
                        elem = buffer.getElemDouble(index);
                    } else {
                        throw new IllegalArgumentException("Undefined band data type '" + type + "' in source product.");
                    }
                    line.append(elem);
                }

                for (int k = 0; k < tpgDataBuffers.length; k++) {
                    final DataBuffer buffer = tpgDataBuffers[k];
                    line.append(separator);
                    final Number elem;
                    final int type = tpgTypes[k];
                    if (type == DataBuffer.TYPE_INT || type == DataBuffer.TYPE_SHORT ||
                        type == DataBuffer.TYPE_USHORT || type == DataBuffer.TYPE_BYTE) {
                        elem = buffer.getElem(index);
                    } else if (type == DataBuffer.TYPE_FLOAT) {
                        elem = buffer.getElemFloat(index);
                    } else if (type == DataBuffer.TYPE_DOUBLE) {
                        elem = buffer.getElemDouble(index);
                    } else {
                        throw new IllegalArgumentException("Undefined tie point grid data type '" + type + "' in source product.");
                    }
                    line.append(elem);
                }

                writeLine(line.toString());
            }
        }

        productWritten = true;
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }

    @Override
    public void deleteOutput() throws IOException {
    }

    private void getSeparatorFromMetaData() {
        separator = Constants.DEFAULT_SEPARATOR;
    }

    private String getFeatureIdFromMetadata(int rowIndex) {
        // todo - get feature id for row from metadata, if existing
        return rowIndex + "";
    }

    private String getFeatureIdColumnNameFromMetadata() {
        // todo - implement metadata search
        return "featureId";
    }
}
