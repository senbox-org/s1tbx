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
package org.esa.beam.framework.ui;

import java.awt.event.MouseEvent;

import com.bc.ceres.glayer.support.ImageLayer;


/**
 * A listener interrested in pixel position changes within a source image. You can add
 * <code>PixelPositionListener</code>s to instances of the <code>ImageDisplay</code> UI component.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public interface PixelPositionListener {

    /**
     * Informs a client that the pixel position within the image has changed.
     * 
     * @param baseImageLayer the image layer 
     * @param pixelX        the x position within the image in pixel co-ordinates on the given level
     * @param pixelY        the y position within the image in pixel co-ordinates on the given level
     * @param currentLevel  the current level at which the image is displayed
     * @param pixelPosValid if <code>true</code>, pixel position is valid
     */
    void pixelPosChanged(ImageLayer baseImageLayer, int pixelX, int pixelY, int currentLevel, boolean pixelPosValid, MouseEvent e);

    /**
     * Informs a client that the pixel positions are no longer available.
     */
    void pixelPosNotAvailable();
}
