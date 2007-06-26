package org.esa.beam.processor.cloud.internal;

import java.awt.Rectangle;

/**
 * Created by IntelliJ IDEA.
 * User: marcoz
 * Date: 06.06.2005
 * Time: 10:17:13
 * To change this template use File | Settings | File Templates.
 */

/**
 * <p><i><b>IMPORTANT NOTE:</b>
 * This class not an API.
 * It is not intended to be used by clients.</i>
 * </p>
 */public interface FrameSizeCalculator {

    public void addMinFrameSize(final int width, final int height);

    public Rectangle getMaxFrameSize();

    public int getFrameCount();
    
    public Rectangle getFrameRect(int frameNumber);
}
