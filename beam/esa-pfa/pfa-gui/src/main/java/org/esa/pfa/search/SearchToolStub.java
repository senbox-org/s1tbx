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
package org.esa.pfa.search;

import org.esa.pfa.db.PatchQuery;
import org.esa.pfa.fe.op.Patch;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

/**
 * Stub for PFA Search Tool on the server
 */
public class SearchToolStub {
    private static SearchToolStub instance = null;

    private PatchQuery db = null;

    private static final java.net.URL dummyURL = SearchToolStub.class.getClassLoader().getResource("images/sigma0_ql.png");
    private static File dummyFile = new File(dummyURL.getPath());

    private SearchToolStub() {
        try {
            db = new PatchQuery(new File("c:\\temp"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SearchToolStub instance() {
        if(instance == null)
            instance = new SearchToolStub();
        return instance;
    }

    public String[] getAvailableFeatureExtractors(final String mission, final String productType) {
        return new String[] {
            "Algal Bloom Detection", "Urban Area Detection"
        };
    }

    public Dimension getPatchSize(final String featureExtractor) {
        return new Dimension(200, 200);
    }

    public void trainClassifier(final Patch[] queryImages) {

    }

    public void retrieveImages(final Patch[] rel, final Patch[] irrel) {

    }

    public Patch[] getRelavantTrainingImages() {
        return createDummyImageList(20);
    }

    public Patch[] getIrrelavantTrainingImages() {
        return createDummyImageList(20);
    }

    public Patch[] getRetrievedImages(final int numImages) {
        return createDummyImageList(numImages);
    }

    private static Patch[] createDummyImageList(final int size) {
        final Patch[] imageList = new Patch[size];
        for(int i=0; i < imageList.length; ++i) {
            imageList[i] = new Patch(0,0, null, null);
            imageList[i].setImage(loadFile(dummyFile));
        }
        return imageList;
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
