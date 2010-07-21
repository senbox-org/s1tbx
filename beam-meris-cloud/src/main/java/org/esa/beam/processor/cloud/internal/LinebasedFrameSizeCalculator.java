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

package org.esa.beam.processor.cloud.internal;

import org.esa.beam.util.Guardian;

import java.awt.Rectangle;

/**
 * <p><i><b>IMPORTANT NOTE:</b>
 * This class not an API.
 * It is not intended to be used by clients.</i>
 * </p>
 */
public class LinebasedFrameSizeCalculator implements FrameSizeCalculator {

    private int sceneWidth;
    private int sceneHeight;
    private int maxHeight;

    public LinebasedFrameSizeCalculator(final int width, final int height) {
        sceneWidth = width;
        sceneHeight = height;
        if (height < 240) {
            maxHeight = 1;
        } else {
            maxHeight = 16;
        }
    }

    public void addMinFrameSize(final int width, final int height) {
        Guardian.assertWithinRange("width", width, 0, sceneWidth);
        Guardian.assertWithinRange("height", height, 0, sceneHeight);

        if (maxHeight % height != 0 && (maxHeight * height <= sceneHeight)) {
            maxHeight *= height;
        }
    }

    public Rectangle getMaxFrameSize() {
        return new Rectangle(sceneWidth, maxHeight);
    }

    public int getFrameCount() {
        int frameCount = sceneHeight / maxHeight;
        if (sceneHeight % maxHeight != 0) {
            frameCount++;
        }
        return frameCount;
    }

    public Rectangle getFrameRect(final int frameNumber) {
        Guardian.assertWithinRange("frameNumber", frameNumber, 0, getFrameCount() - 1);

        final int frameHeight;
        if ((frameNumber + 1) * maxHeight > sceneHeight) {
            frameHeight = sceneHeight % maxHeight;
        } else {
            frameHeight = maxHeight;
        }
        return new Rectangle(0, frameNumber * maxHeight, sceneWidth, frameHeight);
    }
}
