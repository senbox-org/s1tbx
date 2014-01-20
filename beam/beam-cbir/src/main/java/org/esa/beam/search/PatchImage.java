/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.beam.search;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Hold query image data
 */
public class PatchImage {

    private static File dummyFile = new File("C:\\out\\x14y13_rgb.png");
    private BufferedImage image = null;
    private final int uid;

    private static int uidCnt = 0;

    public PatchImage() {
        uid = createUniqueID();
    }

    private synchronized int createUniqueID() {
        return uidCnt++;
    }

    public int getID() {
        return uid;
    }

    public BufferedImage getImage() {
        if(image == null) {
            image = loadFile(dummyFile);
        }
        return image;
    }

    private static BufferedImage loadFile(final File file) {
        BufferedImage bufferedImage = null;
        if (file.canRead()) {
            try {
                final BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
                try {
                    bufferedImage = ImageIO.read(fis);
                } finally {
                    fis.close();
                }
            } catch(Exception e) {
                //
            }
        }
        return bufferedImage;
    }

}
