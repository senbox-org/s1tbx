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
import org.esa.beam.framework.gpf.Raster;

import java.awt.Rectangle;

/**
 * Created by marcoz.
 *
 * @author marcoz
 * @version $Revision: $ $Date: $
 */
public class RasterImpl implements Raster {

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


    public boolean isTarget() {
        throw new IllegalStateException("not implemented");
    }

    public final RasterDataNode getRasterDataNode() {
        return rasterDataNode;
    }

    public final Rectangle getRectangle() {
        return new Rectangle(offsetX, offsetY, width, height);
    }

    public final ProductData getDataBuffer() {
        return dataBuffer;
    }

    public void setSampleData(ProductData sampleData) {
        throw new IllegalStateException("not implemented");
    }

    public final int getHeight() {
        return height;
    }

    public final int getOffsetX() {
        return offsetX;
    }

    public final int getOffsetY() {
        return offsetY;
    }

    public final int getWidth() {
        return width;
    }

    public final int getInt(int x, int y) {
        int v = dataBuffer.getElemIntAt(getDataBufferIndex(x, y));
        if (rasterDataNode.isScalingApplied()) {
            v = (int) (rasterDataNode.scale(v) + 0.5);
        }
        // todo - consider no-data value!
        return v;
    }

    public final void setInt(int x, int y, int v) {
        if (rasterDataNode.isScalingApplied()) {
            double d = rasterDataNode.scaleInverse(v);
            dataBuffer.setElemDoubleAt(getDataBufferIndex(x, y), d);
        } else {
            dataBuffer.setElemIntAt(getDataBufferIndex(x, y), v);
        }
    }

    public final float getFloat(int x, int y) {
        float v = dataBuffer.getElemFloatAt(getDataBufferIndex(x, y));
        if (rasterDataNode.isScalingApplied()) {
            v = (float) rasterDataNode.scale(v);
        }
        // todo - consider no-data value!
        return v;
    }

    public final void setFloat(int x, int y, float v) {
        setDouble(x, y, v);
    }

    public final double getDouble(int x, int y) {
        double v = dataBuffer.getElemDoubleAt(getDataBufferIndex(x, y));
        if (rasterDataNode.isScalingApplied()) {
            v = rasterDataNode.scale(v);
        }
        // todo - consider no-data value!
        return v;
    }

    public final void setDouble(int x, int y, double v) {
        if (rasterDataNode.isScalingApplied()) {
            v = rasterDataNode.scaleInverse(v);
        }
        dataBuffer.setElemDoubleAt(getDataBufferIndex(x, y), v);
    }

    public final boolean getBoolean(int x, int y) {
        return dataBuffer.getElemBooleanAt(getDataBufferIndex(x, y));
    }

    public final void setBoolean(int x, int y, boolean v) {
        dataBuffer.setElemBooleanAt(getDataBufferIndex(x, y), v);
    }

    private int getDataBufferIndex(int x, int y) {
        return (y - offsetY) * width + (x - offsetX);
    }
}
