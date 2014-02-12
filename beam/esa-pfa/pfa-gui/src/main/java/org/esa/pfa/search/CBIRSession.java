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


import org.esa.pfa.db.DatasetDescriptor;
import org.esa.pfa.fe.PFAApplicationDescriptor;
import org.esa.pfa.fe.op.Patch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * State of the CBIR
 */
public class CBIRSession {

    private List<Patch> queryImageList = new ArrayList<Patch>(4);
    private List<Patch> relevantImageList = new ArrayList<Patch>(50);
    private List<Patch> irrelevantImageList = new ArrayList<Patch>(50);
    private List<Patch> retrievedImageList = new ArrayList<Patch>(500);

    private final PFAApplicationDescriptor applicationDescriptor;
    private int numTrainingImages;
    private int numRetrievedImages;

    private SearchToolStub searchTool = new SearchToolStub();

    public CBIRSession(final PFAApplicationDescriptor applicationDescriptor,
                       final int numTrainingImages, final int numRetrievedImages) {
        this.applicationDescriptor = applicationDescriptor;

        this.numTrainingImages = numTrainingImages;
        this.numRetrievedImages = numRetrievedImages;
    }

    public PFAApplicationDescriptor getApplicationDescriptor() {
        return applicationDescriptor;
    }

    public DatasetDescriptor getDsDescriptor() {
        return searchTool.getDsDescriptor();
    }

    public int getNumTrainingImages() {
        return numTrainingImages;
    }

    public int getNumRetrievedImages() {
        return numRetrievedImages;
    }

    public void addQueryPatch(final Patch queryImage) {
        queryImageList.add(queryImage);
    }

    public Patch[] getQueryPatches() {
        return queryImageList.toArray(new Patch[queryImageList.size()]);
    }

    public void setQueryImages() throws Exception {
        searchTool.setQueryImages(getQueryPatches());

        relevantImageList.clear();
        irrelevantImageList.clear();

        final Patch[] imagesToLabel = searchTool.getImagesToLabel(numTrainingImages);
        for(Patch patch : imagesToLabel) {
            if(patch.getLabel() == 1) {
                relevantImageList.add(patch);
            } else {
                irrelevantImageList.add(patch);
            }
        }
    }

    public Patch[] getRelevantTrainingImages() {
        return relevantImageList.toArray(new Patch[relevantImageList.size()]);
    }

    public Patch[] getIrrelevantTrainingImages() {
        return irrelevantImageList.toArray(new Patch[irrelevantImageList.size()]);
    }

    public void trainModel() throws Exception {
        final List<Patch> labeledList = new ArrayList<Patch>(30);
        labeledList.addAll(relevantImageList);
        labeledList.addAll(irrelevantImageList);

        searchTool.trainModel(labeledList.toArray(new Patch[labeledList.size()]));
    }

    public void retrieveImages() throws Exception {
        retrievedImageList.clear();
        retrievedImageList.addAll(Arrays.asList(searchTool.getRetrievedImages(numRetrievedImages)));
    }

    public Patch[] getRetrievedImages() {
        return retrievedImageList.toArray(new Patch[retrievedImageList.size()]);
    }
}
