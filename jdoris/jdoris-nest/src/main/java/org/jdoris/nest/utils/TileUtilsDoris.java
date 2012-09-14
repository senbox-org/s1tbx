package org.jdoris.nest.utils;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Tile;
import org.jblas.ComplexDouble;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.FloatMatrix;

import java.awt.*;

/**
* User: pmar@ppolabs.com
* Date: 6/20/11
* Time: 11:19 PM
*/
public class TileUtilsDoris {

    // see javadoc for org.esa.beam.framework.gpf.Tile (interface)
    public static ComplexDoubleMatrix pullComplexDoubleMatrix(Tile tile1, Tile tile2) {

        // TODO: validate input

        final int height = tile1.getHeight();
        final int width = tile1.getWidth();
        ComplexDoubleMatrix result = new ComplexDoubleMatrix(height, width);

        ProductData samples1 = tile1.getRawSamples();
        ProductData samples2 = tile2.getRawSamples();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < tile1.getWidth(); x++) {
                result.put(y, x, new ComplexDouble(samples1.getElemDoubleAt(y * width + x),
                        samples2.getElemDoubleAt(y * width + x)));
            }
        }
        return result;
    }

    // see javadoc for org.esa.beam.framework.gpf.Tile (interface)
    public static DoubleMatrix pullDoubleMatrix(Tile tile) {

        final int height = tile.getHeight();
        final int width = tile.getWidth();
        DoubleMatrix result = new DoubleMatrix(height, width);

        ProductData samples = tile.getRawSamples();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result.put(y, x, samples.getElemDoubleAt(y * width + x));
            }
        }
        return result;
    }

    public static void pushFloatMatrix(FloatMatrix data, Tile tile, Rectangle rect) {
        ProductData samples = tile.getRawSamples(); // checkout
        final int tileWidth = tile.getWidth();
        for (int y = 0, rowIdx = 0; y < rect.getHeight(); y++, rowIdx++) {
            for (int x = 0, columnIdx = 0; x < rect.getWidth(); x++, columnIdx++) {
                samples.setElemFloatAt(y * tileWidth + x, data.get(rowIdx, columnIdx));
            }
        }
        tile.setRawSamples(samples); // commit
    }

    public static void pushFloatMatrix(DoubleMatrix data, Tile tile, Rectangle rect) {

        final ProductData samples = tile.getRawSamples(); // checkout
        final int width = (int) rect.getWidth();

        for (int y = 0, rowIdx = 0; y < rect.getHeight(); y++, rowIdx++) {
            for (int x = 0, columnIdx = 0; x < rect.getWidth(); x++, columnIdx++) {
                samples.setElemFloatAt(y * width + x, (float) data.get(rowIdx, columnIdx));
            }
        }
        tile.setRawSamples(samples); // commit
    }

    public static void pushDoubleMatrix(DoubleMatrix data, Tile tile, Rectangle rect) {

        final ProductData samples = tile.getRawSamples(); // checkout
        final int width = (int) rect.getWidth();

        for (int y = 0, rowIdx = 0; y < rect.getHeight(); y++, rowIdx++) {
            for (int x = 0, columnIdx = 0; x < rect.getWidth(); x++, columnIdx++) {
                samples.setElemDoubleAt(y * width + x, data.get(rowIdx, columnIdx));
            }
        }
        tile.setRawSamples(samples); // commit
    }

    public static void pushDoubleMatrix(DoubleMatrix data, Tile tile, Rectangle rect, int y0, int x0) {

        final ProductData samples = tile.getRawSamples(); // checkout
        final int width = (int) rect.getWidth();

        for (int y = 0, rowIdx = y0; y < rect.getHeight(); y++, rowIdx++) {
            for (int x = 0, columnIdx = x0; x < rect.getWidth(); x++, columnIdx++) {
                samples.setElemDoubleAt(y * width + x, data.get(rowIdx, columnIdx));
            }
        }
        tile.setRawSamples(samples); // commit
    }

    public static void pushFloatMatrix(FloatMatrix data, Tile tile, Rectangle rect, int y0, int x0) {

        final ProductData samples = tile.getRawSamples(); // checkout
        final int width = (int) rect.getWidth();

        for (int y = 0, rowIdx = y0; y < rect.getHeight(); y++, rowIdx++) {
            for (int x = 0, columnIdx = x0; x < rect.getWidth(); x++, columnIdx++) {
                samples.setElemDoubleAt(y * width + x, data.get(rowIdx, columnIdx));
            }
        }
        tile.setRawSamples(samples); // commit
    }

    public static void pushFloatMatrix(DoubleMatrix data, Tile tile, Rectangle rect, int y0, int x0) {

        final ProductData samples = tile.getRawSamples(); // checkout
        final int width = (int) rect.getWidth();

        for (int y = 0, rowIdx = y0; y < rect.getHeight(); y++, rowIdx++) {
            for (int x = 0, columnIdx = x0; x < rect.getWidth(); x++, columnIdx++) {
                samples.setElemDoubleAt(y * width + x, data.get(rowIdx, columnIdx));
            }
        }
        tile.setRawSamples(samples); // commit
    }

    public static void pushDoubleArray2D(double[][] data, Tile tile, Rectangle rect) {

        final ProductData samples = tile.getRawSamples(); // checkout
        final int width = (int) rect.getWidth();

        for (int y = 0, rowIdx = 0; y < rect.getHeight(); y++, rowIdx++) {
            for (int x = 0, columnIdx = 0; x < rect.getWidth(); x++, columnIdx++) {
                samples.setElemDoubleAt(y * width + x, data[rowIdx][columnIdx]);
            }
        }
        tile.setRawSamples(samples); // commit
    }

    public static void pushDoubleMatrix(FloatMatrix data, Tile tile, Rectangle rect) {

        final ProductData samples = tile.getRawSamples(); // checkout
        final int width = (int) rect.getWidth();

        for (int y = 0, rowIdx = 0; y < rect.getHeight(); y++, rowIdx++) {
            for (int x = 0, columnIdx = 0; x < rect.getWidth(); x++, columnIdx++) {
                samples.setElemDoubleAt(y * width + x, (double) data.get(rowIdx, columnIdx));
            }
        }
        tile.setRawSamples(samples); // commit
    }

    public static void pushComplexFloatMatrix(ComplexDoubleMatrix cplxData, Tile tileReal, Tile tileImag, Rectangle rect) {

        // TOO SLOW?
        final int width = (int) rect.getWidth();
        final int height = (int) rect.getHeight();
        final ProductData samplesReal = tileReal.getRawSamples(); // checkout
        final ProductData samplesImag = tileImag.getRawSamples(); // checkout

        int arrayIdx;
        int row = 0;
        int column;

        for (int y = 0; y < height; y++) {
            column = 0;
            for (int x = 0; x < width; x++) {
                arrayIdx = y * width + x;
                samplesReal.setElemFloatAt(arrayIdx, (float) cplxData.real().get(row, column));
                samplesImag.setElemFloatAt(arrayIdx, (float) cplxData.imag().get(row, column));
                column++;
            }
            row++;
        }

        tileReal.setRawSamples(samplesReal); // commit
        tileImag.setRawSamples(samplesImag); // commit

    }




/*
        // TODO: generify, refactor and document
        private static DoubleMatrix pullDoubleMatrix(Tile tile) {
            return new DoubleMatrix(tile.getHeight(), tile.getWidth(), tile.getSamplesDouble());
        }

        private static ComplexDoubleMatrix pullComplexDoubleMatrix(Tile tile1, Tile tile2) {
            return new ComplexDoubleMatrix(pullDoubleMatrix(tile1), pullDoubleMatrix(tile2));
        }

        private static FloatMatrix pullFloatMatrix(Tile tile) {
            return new FloatMatrix(tile.getHeight(), tile.getWidth(), tile.getSamplesFloat());
        }

        private static ComplexFloatMatrix pullComplexFloatMatrix(Tile tile1, Tile tile2) {
            return new ComplexFloatMatrix(pullFloatMatrix(tile1), pullFloatMatrix(tile2));
        }

        private static void pushArray(final double[] dataArray, final Tile tile) {
//            tile.setRawSamples(ProductData.createInstance(dataArray));
            tile.setSamples(dataArray);
        }

        private static void pushMatrix(final DoubleMatrix dataMatrix, final Tile tile) {
            pushArray(dataMatrix.toArray(), tile);
        }

        private static void pushMatrix(final FloatMatrix dataMatrix, final Tile tile) {
            pushArray(dataMatrix.toArray(), tile);
        }

        private static void pushArray(final float[] dataArray, final Tile tile) {
            tile.setSamples(dataArray);
//            tile.setRawSamples(ProductData.createInstance(dataArray));
        }

        @Deprecated
        private static ComplexDoubleMatrix pullComplexDoubleMatrix(final Rectangle rect, final Tile tile0, final Tile tile1) {
            DoubleMatrix matrix0 = pullDoubleMatrix(rect, tile0);
            DoubleMatrix matrix1 = pullDoubleMatrix(rect, tile1);
            return new ComplexDoubleMatrix(matrix0, matrix1);
        }

        @Deprecated
        private static DoubleMatrix pullDoubleMatrix(final Rectangle rect, final Tile tile) {
            final double[] data = pullDoubleArray(rect, tile);
            return new DoubleMatrix(rect.height, rect.width, data);
        }

        @Deprecated
        private static double[] pullDoubleArray(final Rectangle rect, final Tile tile) {
            final RenderedImage image = tile.getRasterDataNode().getSourceImage();
            return image.getData(rect).getSamples(rect.x, rect.y, rect.width, rect.height, 0, (double[]) null);
        }

        @Deprecated
        private static ComplexFloatMatrix pullComplexFloatMatrix(final Rectangle rect, final Tile tile0, final Tile tile1) {
            FloatMatrix matrix0 = pullFloatMatrix(rect, tile0);
            FloatMatrix matrix1 = pullFloatMatrix(rect, tile1);
            return new ComplexFloatMatrix(matrix0, matrix1);
        }

        @Deprecated
        private static FloatMatrix pullFloatMatrix(final Rectangle rect, final Tile tile) {
            final float[] data = pullFloatArray(rect, tile);
            return new FloatMatrix(rect.height, rect.width, data);
        }

        @Deprecated
        private static float[] pullFloatArray(final Rectangle rect, final Tile tile) {
            final RenderedImage image = tile.getRasterDataNode().getSourceImage();
            return image.getData(rect).getSamples(rect.x, rect.y, rect.width, rect.height, 0, (float[]) null);
        }
        public static void pushDoubleMatrix(DoubleMatrix data, Tile targetTile, Rectangle rect) {
            for (int i = rect.x, ii = 0; i < rect.getWidth(); i++, ii++) {
                for (int j = rect.y, jj = 0; j < rect.getHeight(); j++, jj++) {
                    targetTile.setSample(i, j, data.get(ii, jj));
                }
            }
        }
*/


}
