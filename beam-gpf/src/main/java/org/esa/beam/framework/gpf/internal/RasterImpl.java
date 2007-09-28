/*
 * $Id: $
 *
 * Copyright (C) 2007 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.framework.gpf.internal;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.Tile;

import java.awt.Rectangle;

/**
 * Created by marcoz.
 *
 * @author marcoz
 * @version $Revision: $ $Date: $
 */
public class RasterImpl implements Tile {

    private final RasterDataNode rasterDataNode;
    private final ProductData dataBuffer;
    private final int offsetX;
    private final int offsetY;
    private final int width;
    private final int height;

    public RasterImpl(RasterDataNode rasterDataNode, Rectangle rectangle, ProductData dataBuffer) {
        this.rasterDataNode = rasterDataNode;
        this.dataBuffer = dataBuffer;
        this.offsetX = rectangle.x;
        this.offsetY = rectangle.y;
        this.width = rectangle.width;
        this.height = rectangle.height;
    }

    public final RasterDataNode getRasterDataNode() {
        return rasterDataNode;
    }

    public final Rectangle getRectangle() {
        return new Rectangle(offsetX, offsetY, width, height);
    }

    public final ProductData getRawSampleData() {
        return dataBuffer;
    }

    public final int getHeight() {
        return height;
    }

    public final int getMinX() {
        return offsetX;
    }

    public final int getMinY() {
        return offsetY;
    }

    public final int getWidth() {
        return width;
    }

    public final int getSampleInt(int x, int y) {
        int v = dataBuffer.getElemIntAt(getDataBufferIndex(x, y));
        if (rasterDataNode.isScalingApplied()) {
            v = (int) (rasterDataNode.scale(v) + 0.5);
        }
        // todo - consider no-data value!
        return v;
    }

    public final void setSample(int x, int y, int v) {
        if (rasterDataNode.isScalingApplied()) {
            double d = rasterDataNode.scaleInverse(v);
            dataBuffer.setElemDoubleAt(getDataBufferIndex(x, y), d);
        } else {
            dataBuffer.setElemIntAt(getDataBufferIndex(x, y), v);
        }
    }

    public final float getSampleFloat(int x, int y) {
        float v = dataBuffer.getElemFloatAt(getDataBufferIndex(x, y));
        if (rasterDataNode.isScalingApplied()) {
            v = (float) rasterDataNode.scale(v);
        }
        // todo - consider no-data value!
        return v;
    }

    public final void setSample(int x, int y, float v) {
        setSample(x, y, (double)v);
    }

    public final double getSampleDouble(int x, int y) {
        double v = dataBuffer.getElemDoubleAt(getDataBufferIndex(x, y));
        if (rasterDataNode.isScalingApplied()) {
            v = rasterDataNode.scale(v);
        }
        // todo - consider no-data value!
        return v;
    }

    public final void setSample(int x, int y, double v) {
        if (rasterDataNode.isScalingApplied()) {
            v = rasterDataNode.scaleInverse(v);
        }
        dataBuffer.setElemDoubleAt(getDataBufferIndex(x, y), v);
    }

    public final boolean getSampleBoolean(int x, int y) {
        return dataBuffer.getElemBooleanAt(getDataBufferIndex(x, y));
    }

    public final void setSample(int x, int y, boolean v) {
        dataBuffer.setElemBooleanAt(getDataBufferIndex(x, y), v);
    }

    private int getDataBufferIndex(int x, int y) {
        return (y - offsetY) * width + (x - offsetX);
    }

    // new interface (not implemented)

    public int getMaxX() {
        throw new IllegalStateException("not implemented");
    }

    public int getMaxY() {
        throw new IllegalStateException("not implemented");
    }

    public boolean isWritable() {
        throw new IllegalStateException("not implemented");
    }

    public void setRawSampleData(ProductData sampleData) {
        boolean result;
        throw new IllegalStateException("not implemented");
    }

    public byte[] getRawSamplesByte() {
        throw new IllegalStateException("not implemented");
    }

    public double[] getRawSamplesDouble() {
        throw new IllegalStateException("not implemented");
    }

    public float[] getRawSamplesFloat() {
        throw new IllegalStateException("not implemented");
    }

    public int[] getRawSamplesInt() {
        throw new IllegalStateException("not implemented");
    }

    public short[] getRawSamplesShort() {
        throw new IllegalStateException("not implemented");
    }

    public int getScanlineOffset() {
        throw new IllegalStateException("not implemented");
    }

    public int getScanlineStride() {
        throw new IllegalStateException("not implemented");
    }
}
