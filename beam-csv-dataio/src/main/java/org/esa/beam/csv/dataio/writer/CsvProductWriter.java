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

package org.esa.beam.csv.dataio.writer;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.csv.dataio.Constants;
import org.esa.beam.framework.dataio.AbstractProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.image.DataBuffer;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Allows writing a {@link org.esa.beam.framework.datamodel.Product} in CSV format.
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

    public CsvProductWriter(ProductWriterPlugIn plugIn, int config, Writer writer) {
        super(plugIn);
        this.writer = writer;
        this.config = config;

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
            builder.append(getJavaType(band.getDataType()));
        }
        writeLine(builder.toString());
    }

    private String getJavaType(int dataType) {
        switch (dataType) {
            case ProductData.TYPE_FLOAT32: {
                return "float";
            }
            case ProductData.TYPE_FLOAT64: {
                return "double";
            }
            case ProductData.TYPE_INT8: {
                return "byte";
            }
            case ProductData.TYPE_UINT8: {
                return "ubyte";
            }
            case ProductData.TYPE_INT16: {
                return "short";
            }
            case ProductData.TYPE_UINT16: {
                return "ushort";
            }
            case ProductData.TYPE_INT32: {
                return "int";
            }
            case ProductData.TYPE_UINT32: {
                return "uint";
            }
            default: {
                throw new IllegalArgumentException("Unsupported type '" + ProductData.getTypeString(dataType) + "'.");
            }
        }
    }

    private void writeLine(String line) throws IOException {
        writer.write(line);
        writer.write("\n");
    }

    private void writeProperties() {
        // todo - implement
        if ((config & WRITE_PROPERTIES) != WRITE_PROPERTIES) {
        }
    }

    @Override
    public void writeBandRasterData(Band sourceBand, int sourceOffsetX, int sourceOffsetY,
                                    int sourceWidth, int sourceHeight, ProductData sourceBuffer,
                                    ProgressMonitor pm) throws IOException {
        final Band[] bands = getSourceProduct().getBands();
        final DataBuffer[] dataBuffers = new DataBuffer[bands.length];
        final int[] types = new int[bands.length];
        for (int i = 0; i < bands.length; i++) {
            final Band band = bands[i];
            dataBuffers[i] = band.getSourceImage().getData().getDataBuffer();
            types[i] = band.getDataType();
        }

        int index = 0;
        for (int i = 0; i < getSourceProduct().getSceneRasterWidth(); i++) {
            for (int j = 0; j < getSourceProduct().getSceneRasterHeight(); j++) {
                StringBuilder line = new StringBuilder();
                line.append(getFeatureIdFromMetadata(index));
                for (int k = 0; k < dataBuffers.length; k++) {
                    final DataBuffer buffer = dataBuffers[k];
                    line.append(separator);
                    final Number elem;
                    final int type = types[k];
                    if (ProductData.isIntType(type)) {
                        elem = buffer.getElem(index);
                    } else if (ProductData.TYPE_FLOAT32 == type) {
                        elem = buffer.getElemFloat(index);
                    } else {
                        elem = buffer.getElemDouble(index);
                    }
                    line.append(elem);
                }
                index++;
                writeLine(line.toString());
            }
        }
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
