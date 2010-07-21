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

import java.awt.Rectangle;


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
