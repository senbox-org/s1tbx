package com.bc.ceres.jai.operator;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;

class SourceImageFactory {
    static BufferedImage createOneBandedUShortImage(int w, int h, short[] data) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_USHORT_GRAY);
        DataBufferUShort buffer = (DataBufferUShort) image.getRaster().getDataBuffer();
        System.arraycopy(data, 0, buffer.getData(), 0, w * h);
        return image;
    }
}
