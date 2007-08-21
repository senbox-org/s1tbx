/*
 * $Id: Quantizer.java,v 1.2 2006/12/08 13:48:37 marcop Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.util.math;

import com.bc.ceres.core.ProgressMonitor;

public class Quantizer {

    /**
     * @deprecated in 4.0, use {@link #quantizeGeneric(Object, boolean, double, double, byte[], int, int, ProgressMonitor)} instead
     */
    public static void quantizeGeneric(final Object srcValues,
                                       final boolean srcUnsigned,
                                       final double srcMin,
                                       final double srcMax,
                                       final byte[] dstValues,
                                       final int dstPos,
                                       final int dstStride) {
        quantizeGeneric(srcValues, srcUnsigned, srcMin, srcMax,
                        dstValues, dstPos, dstStride,
                        ProgressMonitor.NULL);
    }

    public static void quantizeGeneric(final Object srcValues,
                                       final boolean srcUnsigned,
                                       final double srcMin,
                                       final double srcMax,
                                       final byte[] dstValues,
                                       final int dstPos,
                                       final int dstStride,
                                       ProgressMonitor pm) {
        if (srcValues instanceof byte[]) {
            if (srcUnsigned) {
                quantizeUByte((byte[]) srcValues, (int) srcMin, (int) srcMax, dstValues, dstPos, dstStride, pm);
            } else {
                quantizeByte((byte[]) srcValues, (int) srcMin, (int) srcMax, dstValues, dstPos, dstStride, pm);
            }
        } else if (srcValues instanceof short[]) {
            if (srcUnsigned) {
                quantizeUShort((short[]) srcValues, (int) srcMin, (int) srcMax, dstValues, dstPos, dstStride, pm);
            } else {
                quantizeShort((short[]) srcValues, (int) srcMin, (int) srcMax, dstValues, dstPos, dstStride, pm);
            }
        } else if (srcValues instanceof int[]) {
            if (srcUnsigned) {
                quantizeUInt((int[]) srcValues, (int) srcMin, (int) srcMax, dstValues, dstPos, dstStride, pm);
            } else {
                quantizeInt((int[]) srcValues, (int) srcMin, (int) srcMax, dstValues, dstPos, dstStride, pm);
            }
        } else if (srcValues instanceof float[]) {
            quantizeFloat((float[]) srcValues, (float) srcMin, (float) srcMax, dstValues, dstPos, dstStride, pm);
        } else if (srcValues instanceof double[]) {
            quantizeDouble((double[]) srcValues, srcMin, srcMax, dstValues, dstPos, dstStride, pm);
        } else if (srcValues instanceof DoubleList) {
            quantizeDouble((DoubleList) srcValues, srcMin, srcMax, dstValues, dstPos, dstStride, pm);
        } else if (srcValues == null) {
            throw new IllegalArgumentException("srcValues is null");
        } else {
            throw new IllegalArgumentException("srcValues has an illegal type: " + srcValues.getClass());
        }
    }

    /**
     * @deprecated in 4.0, use {@link #quantizeByte(byte[], int, int, byte[], int, int, ProgressMonitor)} instead
     */
    public static void quantizeByte(final byte[] srcValues,
                                    final int srcMin,
                                    final int srcMax,
                                    final byte[] dstValues,
                                    final int dstPos,
                                    final int dstStride) {
        quantizeByte(srcValues, srcMin, srcMax,
                     dstValues, dstPos, dstStride,
                     ProgressMonitor.NULL);
    }

    public static void quantizeByte(final byte[] srcValues,
                                    final int srcMin,
                                    final int srcMax,
                                    final byte[] dstValues,
                                    final int dstPos,
                                    final int dstStride,
                                    ProgressMonitor pm) {
        final int srcSize = srcValues.length;
        final int delta = srcMax - srcMin;
        final float srcScale = 256f / delta;
        final float srcOffset = -srcScale * srcMin;
        int quantizedValue;
        pm.beginTask("Quantize values...", srcSize);
        try {
            if (delta == 0) {
                for (int iSrc = 0, iDst = dstPos; iSrc < srcSize; iSrc++, iDst += dstStride) {
                    dstValues[iDst] = 0;
                    pm.worked(1);
                }
            } else {
                for (int iSrc = 0, iDst = dstPos; iSrc < srcSize; iSrc++, iDst += dstStride) {
                    quantizedValue = (int) (srcScale * srcValues[iSrc] + srcOffset);
                    if (quantizedValue < 0) {
                        quantizedValue = 0;
                    } else if (quantizedValue > 255) {
                        quantizedValue = 255;
                    }
                    dstValues[iDst] = (byte) quantizedValue;
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
    }

    /**
     * @deprecated in 4.0, use {@link #quantizeUByte(byte[], int, int, byte[], int, int, ProgressMonitor)} instead
     */
    public static void quantizeUByte(final byte[] srcValues,
                                     final int srcMin,
                                     final int srcMax,
                                     final byte[] dstValues,
                                     final int dstPos,
                                     final int dstStride) {
        quantizeUByte(srcValues, srcMin, srcMax,
                      dstValues, dstPos, dstStride,
                      ProgressMonitor.NULL);
    }

    public static void quantizeUByte(final byte[] srcValues,
                                     final int srcMin,
                                     final int srcMax,
                                     final byte[] dstValues,
                                     final int dstPos,
                                     final int dstStride,
                                     ProgressMonitor pm) {
        final int srcSize = srcValues.length;
        final int delta = srcMax - srcMin;
        final float srcScale = 256f / delta;
        final float srcOffset = -srcScale * srcMin;
        int quantizedValue;
        pm.beginTask("Quantize values...", srcSize);
        try {
            if (delta == 0) {
                for (int iSrc = 0, iDst = dstPos; iSrc < srcSize; iSrc++, iDst += dstStride) {
                    dstValues[iDst] = 0;
                    pm.worked(1);
                }
            } else {
                for (int iSrc = 0, iDst = dstPos; iSrc < srcSize; iSrc++, iDst += dstStride) {
                    quantizedValue = (int) (srcScale * (srcValues[iSrc] & 0xff) + srcOffset);
                    if (quantizedValue < 0) {
                        quantizedValue = 0;
                    } else if (quantizedValue > 255) {
                        quantizedValue = 255;
                    }
                    dstValues[iDst] = (byte) quantizedValue;
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
    }

    /**
     * @deprecated in 4.0, use {@link #quantizeShort(short[], int, int, byte[], int, int, ProgressMonitor)} instead
     */
    public static void quantizeShort(final short[] srcValues,
                                     final int srcMin,
                                     final int srcMax,
                                     final byte[] dstValues,
                                     final int dstPos,
                                     final int dstStride) {
        quantizeShort(srcValues, srcMin, srcMax,
                      dstValues, dstPos, dstStride,
                      ProgressMonitor.NULL);
    }

    public static void quantizeShort(final short[] srcValues,
                                     final int srcMin,
                                     final int srcMax,
                                     final byte[] dstValues,
                                     final int dstPos,
                                     final int dstStride,
                                     ProgressMonitor pm) {
        final int srcSize = srcValues.length;
        final int delta = srcMax - srcMin;
        final float srcScale = 256f / delta;
        final float srcOffset = -srcScale * srcMin;
        int quantizedValue;
        pm.beginTask("Quantize values...", srcSize);
        try {
            if (delta == 0) {
                for (int iSrc = 0, iDst = dstPos; iSrc < srcSize; iSrc++, iDst += dstStride) {
                    dstValues[iDst] = 0;
                    pm.worked(1);
                }
            } else {
                for (int iSrc = 0, iDst = dstPos; iSrc < srcSize; iSrc++, iDst += dstStride) {
                    quantizedValue = (int) (srcScale * srcValues[iSrc] + srcOffset);
                    if (quantizedValue < 0) {
                        quantizedValue = 0;
                    } else if (quantizedValue > 255) {
                        quantizedValue = 255;
                    }
                    dstValues[iDst] = (byte) quantizedValue;
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
    }

    /**
     * @deprecated in 4.0, use {@link #quantizeUShort(short[], int, int, byte[], int, int, ProgressMonitor)} instead
     */
    public static void quantizeUShort(final short[] srcValues,
                                      final int srcMin,
                                      final int srcMax,
                                      final byte[] dstValues,
                                      final int dstPos,
                                      final int dstStride) {
        quantizeUShort(srcValues, srcMin, srcMax,
                       dstValues, dstPos, dstStride,
                       ProgressMonitor.NULL);
    }

    public static void quantizeUShort(final short[] srcValues,
                                      final int srcMin,
                                      final int srcMax,
                                      final byte[] dstValues,
                                      final int dstPos,
                                      final int dstStride,
                                      ProgressMonitor pm) {
        final int srcSize = srcValues.length;
        final int delta = srcMax - srcMin;
        final float srcScale = 256f / delta;
        final float srcOffset = -srcScale * srcMin;
        int quantizedValue;
        pm.beginTask("Quantize values...", srcSize);
        try {
            if (delta == 0) {
                for (int iSrc = 0, iDst = dstPos; iSrc < srcSize; iSrc++, iDst += dstStride) {
                    dstValues[iDst] = 0;
                    pm.worked(1);
                }
            } else {
                for (int iSrc = 0, iDst = dstPos; iSrc < srcSize; iSrc++, iDst += dstStride) {
                    quantizedValue = (int) (srcScale * (srcValues[iSrc] & 0xffff) + srcOffset);
                    if (quantizedValue < 0) {
                        quantizedValue = 0;
                    } else if (quantizedValue > 255) {
                        quantizedValue = 255;
                    }
                    dstValues[iDst] = (byte) quantizedValue;
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
    }

    /**
     * @deprecated in 4.0, use {@link #quantizeInt(int[], int, int, byte[], int, int, ProgressMonitor)} instead
     */
    public static void quantizeInt(final int[] srcValues,
                                   final int srcMin,
                                   final int srcMax,
                                   final byte[] dstValues,
                                   final int dstPos,
                                   final int dstStride) {
        quantizeInt(srcValues, srcMin, srcMax,
                    dstValues, dstPos, dstStride,
                    ProgressMonitor.NULL);
    }

    public static void quantizeInt(final int[] srcValues,
                                   final int srcMin,
                                   final int srcMax,
                                   final byte[] dstValues,
                                   final int dstPos,
                                   final int dstStride,
                                   ProgressMonitor pm) {
        final int srcSize = srcValues.length;
        final int delta = srcMax - srcMin;
        final float srcScale = 256f / delta;
        final float srcOffset = -srcScale * srcMin;
        int quantizedValue;
        pm.beginTask("Quantize values...", srcSize);
        try {
            if (delta == 0) {
                for (int iSrc = 0, iDst = dstPos; iSrc < srcSize; iSrc++, iDst += dstStride) {
                    dstValues[iDst] = 0;
                    pm.worked(1);
                }
            } else {
                for (int iSrc = 0, iDst = dstPos; iSrc < srcSize; iSrc++, iDst += dstStride) {
                    quantizedValue = (int) (srcScale * srcValues[iSrc] + srcOffset);
                    if (quantizedValue < 0) {
                        quantizedValue = 0;
                    } else if (quantizedValue > 255) {
                        quantizedValue = 255;
                    }
                    dstValues[iDst] = (byte) quantizedValue;
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
    }

    /**
     * @deprecated in 4.0, use {@link #quantizeUInt(int[], long, long, byte[], int, int, ProgressMonitor)} instead
     */
    public static void quantizeUInt(final int[] srcValues,
                                    final long srcMin,
                                    final long srcMax,
                                    final byte[] dstValues,
                                    final int dstPos,
                                    final int dstStride) {
        quantizeUInt(srcValues, srcMin, srcMax,
                     dstValues, dstPos, dstStride,
                     ProgressMonitor.NULL);
    }

    public static void quantizeUInt(final int[] srcValues,
                                    final long srcMin,
                                    final long srcMax,
                                    final byte[] dstValues,
                                    final int dstPos,
                                    final int dstStride,
                                    ProgressMonitor pm) {
        final int srcSize = srcValues.length;
        final long delta = srcMax - srcMin;
        final float srcScale = 256f / delta;
        final float srcOffset = -srcScale * srcMin;
        int quantizedValue;
        pm.beginTask("Quantize values...", srcSize);
        try {
            if (delta == 0) {
                for (int iSrc = 0, iDst = dstPos; iSrc < srcSize; iSrc++, iDst += dstStride) {
                    dstValues[iDst] = 0;
                    pm.worked(1);
                }
            } else {
                for (int iSrc = 0, iDst = dstPos; iSrc < srcSize; iSrc++, iDst += dstStride) {
                    quantizedValue = (int) (srcScale * (srcValues[iSrc] & 0xffffffffL) + srcOffset);
                    if (quantizedValue < 0) {
                        quantizedValue = 0;
                    } else if (quantizedValue > 255) {
                        quantizedValue = 255;
                    }
                    dstValues[iDst] = (byte) quantizedValue;
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
    }

    /**
     * @deprecated in 4.0, use {@link #quantizeFloat(float[], float, float, byte[], int, int, ProgressMonitor)} instead
     */
    public static void quantizeFloat(final float[] srcValues,
                                     final float srcMin,
                                     final float srcMax,
                                     final byte[] dstValues,
                                     final int dstPos,
                                     final int dstStride) {
        quantizeFloat(srcValues, srcMin, srcMax,
                      dstValues, dstPos, dstStride,
                      ProgressMonitor.NULL);
    }

    public static void quantizeFloat(final float[] srcValues,
                                     final float srcMin,
                                     final float srcMax,
                                     final byte[] dstValues,
                                     final int dstPos,
                                     final int dstStride,
                                     ProgressMonitor pm) {
        final int srcSize = srcValues.length;
        final float delta = srcMax - srcMin;
        final float srcScale = 256 / delta;
        final float srcOffset = -srcScale * srcMin;
        int quantizedValue;
        pm.beginTask("Quantize values...", srcSize);
        try {
            if (delta == 0) {
                for (int iSrc = 0, iDst = dstPos; iSrc < srcSize; iSrc++, iDst += dstStride) {
                    dstValues[iDst] = 0;
                    pm.worked(1);
                }
            } else {
                for (int iSrc = 0, iDst = dstPos; iSrc < srcSize; iSrc++, iDst += dstStride) {
                    quantizedValue = (int) (srcScale * srcValues[iSrc] + srcOffset);
                    if (quantizedValue < 0) {
                        quantizedValue = 0;
                    } else if (quantizedValue > 255) {
                        quantizedValue = 255;
                    }
                    dstValues[iDst] = (byte) quantizedValue;
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
    }

    /**
     * @deprecated in 4.0, use {@link #quantizeDouble(double[], double, double, byte[], int, int, ProgressMonitor)} instead
     */
    public static void quantizeDouble(final double[] srcValues,
                                      final double srcMin,
                                      final double srcMax,
                                      final byte[] dstValues,
                                      final int dstPos,
                                      final int dstStride) {
        quantizeDouble(srcValues, srcMin, srcMax,
                       dstValues, dstPos, dstStride,
                       ProgressMonitor.NULL);
    }

    public static void quantizeDouble(final double[] srcValues,
                                      final double srcMin,
                                      final double srcMax,
                                      final byte[] dstValues,
                                      final int dstPos,
                                      final int dstStride,
                                      ProgressMonitor pm) {
        final int srcSize = srcValues.length;
        final double delta = srcMax - srcMin;
        final double srcScale = 256 / delta;
        final double srcOffset = -srcScale * srcMin;
        int quantizedValue;

        pm.beginTask("Quantize values...", srcSize);
        try {
            if (delta == 0) {
                for (int iSrc = 0, iDst = dstPos; iSrc < srcSize; iSrc++, iDst += dstStride) {
                    dstValues[iDst] = 0;
                    pm.worked(1);
                }
            } else {
                for (int iSrc = 0, iDst = dstPos; iSrc < srcSize; iSrc++, iDst += dstStride) {
                    quantizedValue = (int) (srcScale * srcValues[iSrc] + srcOffset);
                    if (quantizedValue < 0) {
                        quantizedValue = 0;
                    } else if (quantizedValue > 255) {
                        quantizedValue = 255;
                    }
                    dstValues[iDst] = (byte) quantizedValue;
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
    }

    /**
     * @deprecated in 4.0, use {@link #quantizeDouble(DoubleList, double, double, byte[], int, int, ProgressMonitor)} instead
     */
    public static void quantizeDouble(final DoubleList srcValues,
                                      final double srcMin,
                                      final double srcMax,
                                      final byte[] dstValues,
                                      final int dstPos,
                                      final int dstStride) {
        quantizeDouble(srcValues, srcMin, srcMax,
                       dstValues, dstPos, dstStride,
                       ProgressMonitor.NULL);
    }

    public static void quantizeDouble(final DoubleList srcValues,
                                      final double srcMin,
                                      final double srcMax,
                                      final byte[] dstValues,
                                      final int dstPos,
                                      final int dstStride,
                                      ProgressMonitor pm) {
        final int srcSize = srcValues.getSize();
        final double delta = srcMax - srcMin;
        final double srcScale = 256 / delta;
        final double srcOffset = -srcScale * srcMin;
        int quantizedValue;

        pm.beginTask("Quantize values...", srcSize);
        try {
            if (delta == 0) {
                for (int iSrc = 0, iDst = dstPos; iSrc < srcSize; iSrc++, iDst += dstStride) {
                    dstValues[iDst] = 0;
                    pm.worked(1);
                }
            } else {
                for (int iSrc = 0, iDst = dstPos; iSrc < srcSize; iSrc++, iDst += dstStride) {
                    quantizedValue = (int) (srcScale * srcValues.getDouble(iSrc) + srcOffset);
                    if (quantizedValue < 0) {
                        quantizedValue = 0;
                    } else if (quantizedValue > 255) {
                        quantizedValue = 255;
                    }
                    dstValues[iDst] = (byte) quantizedValue;
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
    }
}
