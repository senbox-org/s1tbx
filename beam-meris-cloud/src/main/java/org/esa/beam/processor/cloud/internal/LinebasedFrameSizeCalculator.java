package org.esa.beam.processor.cloud.internal;

import org.esa.beam.util.Guardian;

import java.awt.Rectangle;

/**
 * Created by IntelliJ IDEA.
 * User: marcoz
 * Date: 06.06.2005
 * Time: 10:17:31
 * To change this template use File | Settings | File Templates.
 */

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
