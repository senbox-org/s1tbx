package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.MultiLevelSource;

import java.awt.geom.AffineTransform;
import java.io.File;

/**
 * @deprectaed since BEAM 4.5, use {@link FileMultiLevelSource} instead.
 */
@Deprecated
public class FileMultiLevelSourceFactory {

    public static MultiLevelSource create(File location, String extension, AffineTransform imageToModelTransform, int levelCount) {
        return FileMultiLevelSource.create(location, extension, imageToModelTransform, levelCount);
    }
}