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

package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.MultiLevelSource;

import java.awt.geom.AffineTransform;
import java.io.File;

/**
 * @deprecated since BEAM 4.5, use {@link FileMultiLevelSource} instead.
 */
@Deprecated
public class FileMultiLevelSourceFactory {

    public static MultiLevelSource create(File location, String extension, AffineTransform imageToModelTransform, int levelCount) {
        return FileMultiLevelSource.create(location, extension, imageToModelTransform, levelCount);
    }
}