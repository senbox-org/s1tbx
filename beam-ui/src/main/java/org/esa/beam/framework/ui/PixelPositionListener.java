/*
 * $Id: PixelPositionListener.java,v 1.1 2006/10/10 14:47:38 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
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
