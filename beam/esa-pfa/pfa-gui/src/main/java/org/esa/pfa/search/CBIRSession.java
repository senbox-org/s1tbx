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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * State of the CBIR
 */
public class CBIRSession {

    private List<PatchImage> queryImageList = new ArrayList<PatchImage>(3);
    private List<PatchImage> relevantImageList = new ArrayList<PatchImage>(50);
    private List<PatchImage> irrelevantImageList = new ArrayList<PatchImage>(50);
    private List<PatchImage> retrievedImageList = new ArrayList<PatchImage>(500);

    public CBIRSession() {

    }

    public void addQueryImage(final PatchImage queryImage) {
        queryImageList.add(queryImage);
    }

    public PatchImage[] getQueryImages() {
        return queryImageList.toArray(new PatchImage[queryImageList.size()]);
    }

    public void trainClassifier() {
        SearchToolStub.instance().trainClassifier(getQueryImages());

        relevantImageList.addAll(Arrays.asList(SearchToolStub.instance().getRelavantTrainingImages()));
        irrelevantImageList.addAll(Arrays.asList(SearchToolStub.instance().getIrrelavantTrainingImages()));
    }

    public PatchImage[] getRelevantTrainingImages() {
        return relevantImageList.toArray(new PatchImage[relevantImageList.size()]);
    }

    public PatchImage[] getIrrelevantTrainingImages() {
        return irrelevantImageList.toArray(new PatchImage[irrelevantImageList.size()]);
    }

    public void retrieveImages(final int numImages) {
        SearchToolStub.instance().retrieveImages(getRelevantTrainingImages(), getIrrelevantTrainingImages());

        retrievedImageList.addAll(Arrays.asList(SearchToolStub.instance().getRetrievedImages(numImages)));
    }

    public PatchImage[] getRetrievedImages() {
        return retrievedImageList.toArray(new PatchImage[retrievedImageList.size()]);
    }
}
