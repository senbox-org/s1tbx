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

    private List<Patch> queryImageList = new ArrayList<>(4);
    private List<Patch> relevantImageList = new ArrayList<>(50);
    private List<Patch> irrelevantImageList = new ArrayList<>(50);
    private List<Patch> retrievedImageList = new ArrayList<>(500);

    private final PFAApplicationDescriptor applicationDescriptor;

    private final SearchToolStub searchTool;

    public CBIRSession(final String classifierName,
                       final PFAApplicationDescriptor applicationDescriptor,
                       final String archivePath) throws Exception {
        this.applicationDescriptor = applicationDescriptor;

        this.searchTool = new SearchToolStub(archivePath, classifierName, applicationDescriptor.getAllQueryExpr());
    }

    public PFAApplicationDescriptor getApplicationDescriptor() {
        return applicationDescriptor;
    }

    public DatasetDescriptor getDsDescriptor() {
        return searchTool.getDsDescriptor();
    }

    public void setNumTrainingImages(final int numTrainingImages) {
        searchTool.setNumTrainingImages(numTrainingImages);
    }

    public int getNumTrainingImages() {
        return searchTool.getNumTrainingImages();
    }

    public void setNumRetrievedImages(final int numRetrievedImages) {
        searchTool.setNumRetrievedImages(numRetrievedImages);
    }

    public int getNumRetrievedImages() {
        return searchTool.getNumRetrievedImages();
    }

    public int getNumIterations() {
        return searchTool.getNumIterations();
    }

    public static String[] getSavedClassifierNames(final String archiveFolder) {
        return SearchToolStub.getSavedClassifierNames(archiveFolder);
    }

    public void clearQueryPatches() {
        queryImageList.clear();
    }

    public void addQueryPatch(final Patch queryImage) {
        queryImageList.add(queryImage);
    }

    public Patch[] getQueryPatches() {
        return queryImageList.toArray(new Patch[queryImageList.size()]);
    }

    public void setQueryImages() throws Exception {
        searchTool.setQueryImages(getQueryPatches());
        getImagesToLabel();
    }

    public void reassignTrainingImage(final Patch patch) {
        if(patch.getLabel() == Patch.LABEL_RELEVANT) {
            int index = irrelevantImageList.indexOf(patch);
            if(index != -1) {
                irrelevantImageList.remove(index);
                relevantImageList.add(patch);
            }
        } else if(patch.getLabel() == Patch.LABEL_IRRELEVANT) {
            int index = relevantImageList.indexOf(patch);
            if(index != -1) {
                relevantImageList.remove(index);
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

    public void getImagesToLabel() throws Exception {

        relevantImageList.clear();
        irrelevantImageList.clear();

        final Patch[] imagesToLabel = searchTool.getImagesToLabel();
        for(Patch patch : imagesToLabel) {
            if(patch.getLabel() == Patch.LABEL_RELEVANT) {
                relevantImageList.add(patch);
            } else {
                // default to irrelevant so user only needs to select the relevant
                patch.setLabel(Patch.LABEL_IRRELEVANT);
                irrelevantImageList.add(patch);
            }
        }
    }

    public void trainModel() throws Exception {
        final List<Patch> labeledList = new ArrayList<Patch>(30);
        labeledList.addAll(relevantImageList);
        labeledList.addAll(irrelevantImageList);

        searchTool.trainModel(labeledList.toArray(new Patch[labeledList.size()]));
    }

    public void retrieveImages() throws Exception {
        retrievedImageList.clear();
        retrievedImageList.addAll(Arrays.asList(searchTool.getRetrievedImages()));
    }

    public Patch[] getRetrievedImages() {
        return retrievedImageList.toArray(new Patch[retrievedImageList.size()]);
    }
}
