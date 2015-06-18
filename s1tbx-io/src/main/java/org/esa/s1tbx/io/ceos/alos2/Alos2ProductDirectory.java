/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.ceos.alos2;

import org.esa.s1tbx.io.binary.IllegalBinaryFormatException;
import org.esa.s1tbx.io.ceos.CEOSImageFile;
import org.esa.s1tbx.io.ceos.CeosHelper;
import org.esa.s1tbx.io.ceos.alos.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a product directory.
 * <p/>
 * <p>This class is public for the benefit of the implementation of another (internal) class and its API may
 * change in future releases of the software.</p>
 */
public class Alos2ProductDirectory extends AlosPalsarProductDirectory {

    public Alos2ProductDirectory(final File dir) {
        super(dir);

        constants = new Alos2Constants();
        baseDir = dir;
    }

    @Override
    protected void readProductDirectory() throws IOException, IllegalBinaryFormatException {
        readVolumeDirectoryFile();

        updateProductType();

        leaderFile = new Alos2LeaderFile(
                createInputStream(CeosHelper.getCEOSFile(baseDir, constants.getLeaderFilePrefix())));
        final File trlFile = CeosHelper.getCEOSFile(baseDir, constants.getTrailerFilePrefix());
        if (trlFile != null) {
            trailerFile = new AlosPalsarTrailerFile(createInputStream(trlFile));
        }

        final String[] imageFileNames = CEOSImageFile.getImageFileNames(baseDir, constants.getImageFilePrefix());
        final List<AlosPalsarImageFile> imgArray = new ArrayList<>(imageFileNames.length);
        for (String fileName : imageFileNames) {
            try {
                final AlosPalsarImageFile imgFile = new AlosPalsarImageFile(createInputStream(new File(baseDir, fileName)),
                        getProductLevel(), fileName);
                imgArray.add(imgFile);
            } catch (Exception e) {
                e.printStackTrace();
                // continue
            }
        }
        imageFiles = imgArray.toArray(new AlosPalsarImageFile[imgArray.size()]);

        sceneWidth = imageFiles[0].getRasterWidth();
        sceneHeight = imageFiles[0].getRasterHeight();
        assertSameWidthAndHeightForAllImages(imageFiles, sceneWidth, sceneHeight);

        if (leaderFile.getProductLevel() == AlosPalsarConstants.LEVEL1_0 ||
                leaderFile.getProductLevel() == AlosPalsarConstants.LEVEL1_1) {
            isProductSLC = true;
        }
    }

    public boolean isALOS2() throws IOException {
        final String volumeId = getVolumeId().toUpperCase();
        final String logicalVolumeId = getLogicalVolumeId().toUpperCase();
        return (volumeId.contains("ALOS2") || logicalVolumeId.contains("ALOS2"));
    }

    protected String getMission() {
        return "ALOS2";
    }

    protected String getProductDescription() {
        return Alos2Constants.PRODUCT_DESCRIPTION_PREFIX + leaderFile.getProductLevel();
    }
}
