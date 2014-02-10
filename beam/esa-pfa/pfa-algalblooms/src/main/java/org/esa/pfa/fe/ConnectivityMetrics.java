package org.esa.pfa.fe;

import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.util.ShapeRasterizer;

import java.awt.image.DataBufferByte;
import java.util.Random;

/**
 * @author Norman Fomferra
 */
public class ConnectivityMetrics {

    int connectionCount;
    double connectionRatio;
    int connectionCountMax;
    int occupiedCount;
    int borderCount;
    int insideCount;
    double fractalIndex;
    double maxSegmentLengthMean;
    double maxSegmentLengthSigma;

    private ConnectivityMetrics() {
    }

    public static ConnectivityMetrics compute(Mask mask) {
        ConnectivityMetrics connectivityMetrics = new ConnectivityMetrics();
        connectivityMetrics.run(mask);
        return connectivityMetrics;
    }

    public static ConnectivityMetrics compute(int width, int height, byte[] data) {
        ConnectivityMetrics connectivityMetrics = new ConnectivityMetrics();
        connectivityMetrics.run(width, height, data);
        return connectivityMetrics;
    }

    private void run(Mask mask) {
        run(mask.getRasterWidth(), mask.getRasterHeight(),
            ((DataBufferByte) mask.getSourceImage().getData().getDataBuffer()).getData());
    }

    private void run(final int width, final int height, final byte[] data) {
        int connectionCount = 0;
        int occupiedCount = 0;
        int borderCount = 0;
        int insideCount = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean valC = isSet(data, width, x, y);
                if (valC) {
                    boolean valN = y <= 0 || isSet(data, width, x, y - 1);
                    boolean valS = y >= height - 1 || isSet(data, width, x, y + 1);
                    boolean valW = x <= 0 || isSet(data, width, x - 1, y);
                    boolean valE = x >= width - 1 || isSet(data, width, x + 1, y);
                    int neighborCount = 0;
                    if (valN)
                        neighborCount++;
                    if (valS)
                        neighborCount++;
                    if (valW)
                        neighborCount++;
                    if (valE)
                        neighborCount++;

                    connectionCount += neighborCount;
                    occupiedCount++;

                    if (neighborCount > 0 && neighborCount < 4)
                        borderCount++;
                    if (neighborCount == 4)
                        insideCount++;
                }
            }
        }

        this.occupiedCount = occupiedCount;
        this.connectionCount = connectionCount;
        this.connectionCountMax = 4 * width * height;
        connectionRatio = connectionCount / (double) connectionCountMax;
        this.borderCount = borderCount;
        this.insideCount = insideCount;
        fractalIndex = 2.0 - (insideCount > 0 ? insideCount / ((double) insideCount + (double) borderCount) : 0.0);


        final ShapeRasterizer.BresenhamLineRasterizer lineRasterizer = new ShapeRasterizer.BresenhamLineRasterizer();
        Random random = new Random();
        final int N = 20;

        int[] maxSegmentLengths = new int[N];
        int x1, x2, y1, y2;
        for (int i = 0; i < N; i++) {
            if (random.nextBoolean()) {
                x1 = (int) (random.nextDouble() * width);
                x2 = (int) (random.nextDouble() * width);
                y1 = 0;
                y2 = height - 1;
            } else {
                x1 = 0;
                x2 = width - 1;
                y1 = (int) (random.nextDouble() * height);
                y2 = (int) (random.nextDouble() * height);
            }
            MyLinePixelVisitor visitor = new MyLinePixelVisitor(data, width);
            lineRasterizer.rasterize(x1, y1, x2, y2, visitor);
            visitor.processPixel(visitor.lastVal);
            maxSegmentLengths[i] = visitor.maxSegmentLength;
        }

        double maxSegmentLengthMean = 0;
        for (int i = 0; i < N; i++) {
            int maxSegmentLength = maxSegmentLengths[i];
            maxSegmentLengthMean += maxSegmentLength;
        }
        this.maxSegmentLengthMean = maxSegmentLengthMean;

        double maxSegmentLengthSigma = 0;
        for (int i = 0; i < N; i++) {
            double v = maxSegmentLengthMean - maxSegmentLengths[i];
            maxSegmentLengthSigma += v * v;
        }
        maxSegmentLengthSigma = Math.sqrt(maxSegmentLengthSigma / (N - 1));
        this.maxSegmentLengthSigma = maxSegmentLengthSigma;
    }

    private boolean isSet(byte[] data, int width, int x, int y) {
        return data[y * width + x] != 0;
    }

    private static class MyLinePixelVisitor implements ShapeRasterizer.LinePixelVisitor {
        private final byte[] data;
        private final int width;
        private int segmentLength;
        private int totalPixelCount;
        private byte lastVal;
        int maxSegmentLength;

        public MyLinePixelVisitor(byte[] data, int width) {
            this.data = data;
            this.width = width;
        }

        @Override
        public void visit(int x, int y) {
            byte val = data[y * width + x];
            processPixel(val);
            totalPixelCount++;
        }

        void processPixel(byte val) {
            if (totalPixelCount == 0) {
                lastVal = val;
            }
            if (val != 0) {
                segmentLength++;
            } else if (lastVal != 0) {
                maxSegmentLength = Math.max(maxSegmentLength, segmentLength);
                segmentLength = 0;
            }
            lastVal = val;
        }
    }
}
