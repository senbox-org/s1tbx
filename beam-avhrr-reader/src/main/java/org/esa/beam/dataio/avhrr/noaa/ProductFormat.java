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

package org.esa.beam.dataio.avhrr.noaa;

import com.bc.ceres.binio.SimpleType;
import com.bc.ceres.binio.Type;
import org.esa.beam.dataio.avhrr.BandReader;
import org.esa.beam.dataio.avhrr.calibration.Calibrator;
import org.esa.beam.framework.dataio.ProductIOException;

/**
 * Differentiates the binary formats for AVHRR NOAA products.
 */
enum ProductFormat {

    HRPT_8BIT(12288, SimpleType.BYTE, 2048 * 5, ProductDimension.HRPT) {
        @Override
        public BandReader createCountReader(int channel, KlmAvhrrFile klmAvhrrFile, Calibrator calibrator) {
            return new CountReader8Bit(channel, klmAvhrrFile, calibrator, getElementCount(), getProductDimension().getDataWidth());
        }
    },
    HRPT_10BIT(15872, SimpleType.INT, 3414, ProductDimension.HRPT) {
        @Override
        public BandReader createCountReader(int channel, KlmAvhrrFile klmAvhrrFile, Calibrator calibrator) {
            return new CountReader10Bit(channel, klmAvhrrFile, calibrator, getElementCount(), getProductDimension().getDataWidth());
        }
    },
    HRPT_16BIT(22528, SimpleType.SHORT, 2048 * 5, ProductDimension.HRPT) {
        @Override
        public BandReader createCountReader(int channel, KlmAvhrrFile klmAvhrrFile, Calibrator calibrator) {
            return new CountReader16Bit(channel, klmAvhrrFile, calibrator, getElementCount(), getProductDimension().getDataWidth());
        }
    },

    GAC_8BIT(3584, SimpleType.BYTE, 409 * 5, ProductDimension.GAC) {
        @Override
        public BandReader createCountReader(int channel, KlmAvhrrFile klmAvhrrFile, Calibrator calibrator) {
            return new CountReader8Bit(channel, klmAvhrrFile, calibrator, getElementCount(), getProductDimension().getDataWidth());
        }
    },
    GAC_10BIT(4608, SimpleType.INT, 682, ProductDimension.GAC) {
        @Override
        public BandReader createCountReader(int channel, KlmAvhrrFile klmAvhrrFile, Calibrator calibrator) {
            return new CountReader10Bit(channel, klmAvhrrFile, calibrator, getElementCount(), getProductDimension().getDataWidth());
        }
    },
    GAC_16BIT(5632, SimpleType.SHORT, 409 * 5, ProductDimension.GAC) {
        @Override
        public BandReader createCountReader(int channel, KlmAvhrrFile klmAvhrrFile, Calibrator calibrator) {
            return new CountReader16Bit(channel, klmAvhrrFile, calibrator, getElementCount(), getProductDimension().getDataWidth());
        }
    };

    private final int blockSize;
    private final ProductDimension productDimension;
    private final Type elementType;
    private final int elementCount;

    ProductFormat(int blockSize, Type elementType, int elementCount, ProductDimension productDimension) {
        this.blockSize = blockSize;
        this.elementType = elementType;
        this.elementCount = elementCount;
        this.productDimension = productDimension;
    }


    public int getBlockSize() {
        return blockSize;
    }

    public ProductDimension getProductDimension() {
        return productDimension;
    }

    public Type getElementType() {
        return elementType;
    }

    public int getElementCount() {
        return elementCount;
    }

    public abstract BandReader createCountReader(int channel, KlmAvhrrFile klmAvhrrFile, Calibrator calibrator);

    public static ProductFormat findByBlockSize(int blockSize) throws ProductIOException {
        ProductFormat[] values = ProductFormat.values();
        for (ProductFormat productFormat : values) {
            if (productFormat.getBlockSize() == blockSize) {
                return productFormat;
            }
        }
        throw new ProductIOException("Unsupported AVHRR data record size: " + blockSize);
    }
}
