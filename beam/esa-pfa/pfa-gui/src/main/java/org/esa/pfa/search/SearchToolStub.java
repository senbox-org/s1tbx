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

import java.awt.*;
import java.io.File;

/**
 * Stub for PFA Search Tool on the server
 */
public class SearchToolStub {
    private static SearchToolStub instance = null;

    private PatchQuery db = null;

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

    public void trainClassifier(final PatchImage[] queryImages) {

    }

    public void retrieveImages(final PatchImage[] rel, final PatchImage[] irrel) {

    }

    public PatchImage[] getRelavantTrainingImages() {
        return createDummyImageList(20);
    }

    public PatchImage[] getIrrelavantTrainingImages() {
        return createDummyImageList(20);
    }

    public PatchImage[] getRetrievedImages(final int numImages) {
        return createDummyImageList(numImages);
    }

    private static PatchImage[] createDummyImageList(final int size) {
        final PatchImage[] imageList = new PatchImage[size];
        for(int i=0; i < imageList.length; ++i) {
            imageList[i] = new PatchImage();
        }
        return imageList;
    }
}
