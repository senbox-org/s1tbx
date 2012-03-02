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

package org.esa.beam.csv.dataio.reader;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.csv.dataio.CsvProductFile;
import org.esa.beam.csv.dataio.CsvProductSource;
import org.esa.beam.csv.dataio.CsvProductSourceParser;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import java.io.IOException;

/**
 * The CsvProductReader is able to read a CSV file into a product.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class CsvProductReader extends AbstractProductReader {

    private CsvProductSource source;
    private CsvProductSourceParser parser;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    protected CsvProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        try {
            parser = new CsvProductFile(getInput().toString());
            source = parser.parse();
        } catch (CsvProductFile.ParseException e) {
            throw new IOException(e);
        }
        final int sceneRasterWidth = source.getRecordCount();
        // todo - get name and type from properties, if existing
        final Product product = new Product("name", "type", sceneRasterWidth, 1);
        for(AttributeDescriptor descriptor : source.getFeatureType().getAttributeDescriptors()) {
            if(isAccessibleBandType(descriptor.getType().getBinding())) {
                int type = getProductDataType(descriptor.getType().getBinding());
                product.addBand(descriptor.getName().toString(), type);
            }
        }
        // todo - somehow handle attributes which are of no band type, such as utc
        // todo - put properties into metadata
        return product;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        try {
            parser.parseRecords();
        } catch (CsvProductFile.ParseException e) {
            throw new IOException(e);
        }

        final SimpleFeature[] simpleFeatures = toSimpleFeatureArray(source.getFeatureCollection(), sourceOffsetX, destWidth);
        final Object[] elems = new Object[simpleFeatures.length];
        int featureIndex = 0;
        for (SimpleFeature simpleFeature : simpleFeatures) {
            final Object attribute = simpleFeature.getAttribute(destBand.getName());
            elems[featureIndex++] = attribute;
        }
        getProductData(elems, destBuffer);
    }

    private SimpleFeature[] toSimpleFeatureArray(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection, int sourceOffsetX, int destWidth) {
        final Object[] objects = featureCollection.toArray(new Object[featureCollection.size()]);
        final SimpleFeature[] simpleFeatures = new SimpleFeature[objects.length];
        for (int i = sourceOffsetX; i < destWidth; i++) {
            simpleFeatures[i] = (SimpleFeature)objects[i];
        }
        return simpleFeatures;
    }

    private void getProductData(Object[] elems, ProductData destBuffer) {
        switch (destBuffer.getType()) {
            case ProductData.TYPE_FLOAT32: {
                for (int i = 0; i < elems.length; i++) {
                    final Object elem = elems[i];
                    destBuffer.setElemFloatAt(i, (Float) elem);
                }
                break;
            }
            case ProductData.TYPE_FLOAT64: {
                for (int i = 0; i < elems.length; i++) {
                    final Object elem = elems[i];
                    destBuffer.setElemDoubleAt(i, (Double) elem);
                }
                break;
            }
            case ProductData.TYPE_INT8: {
                for (int i = 0; i < elems.length; i++) {
                    final Object elem = elems[i];
                    destBuffer.setElemIntAt(i, (Byte) elem);
                }
                break;
            }
            case ProductData.TYPE_INT16: {
                for (int i = 0; i < elems.length; i++) {
                    final Object elem = elems[i];
                    destBuffer.setElemIntAt(i, (Short) elem);
                }
                break;
            }
            case ProductData.TYPE_INT32: {
                for (int i = 0; i < elems.length; i++) {
                    final Object elem = elems[i];
                    destBuffer.setElemIntAt(i, (Integer) elem);
                }
                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupported type '" + ProductData.getTypeString(destBuffer.getType()) + "'.");
            }
        }
    }

    private int getProductDataType(Class<?> type) {
        if (type.getSimpleName().toLowerCase().equals("string")) {
            return ProductData.TYPE_ASCII;
        } else if (type.getSimpleName().toLowerCase().equals("float")) {
            return ProductData.TYPE_FLOAT32;
        } else if (type.getSimpleName().toLowerCase().equals("double")) {
            return ProductData.TYPE_FLOAT64;
        } else if (type.getSimpleName().toLowerCase().equals("byte")) {
            return ProductData.TYPE_INT8;
        } else if (type.getSimpleName().toLowerCase().equals("short")) {
            return ProductData.TYPE_INT16;
        } else if (type.getSimpleName().toLowerCase().equals("integer")) {
            return ProductData.TYPE_INT32;
        } else if(type.getSimpleName().toLowerCase().equals("utc")) {
            return ProductData.TYPE_UTC;
        }
        throw new IllegalArgumentException("Unsupported type '" + type + "'.");
    }

    private boolean isAccessibleBandType(Class<?> type) {
        final String className = type.getSimpleName().toLowerCase();
        return className.equals("float") ||
               className.equals("double") ||
               className.equals("byte") ||
               className.equals("short") ||
               className.equals("integer");
    }
}
