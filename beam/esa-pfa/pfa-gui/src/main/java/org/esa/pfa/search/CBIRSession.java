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

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.pfa.fe.PFAApplicationDescriptor;
import org.esa.pfa.fe.op.FeatureType;
import org.esa.pfa.fe.op.Patch;
import org.esa.pfa.ordering.ProductOrderBasket;
import org.esa.pfa.ordering.ProductOrderService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Encapsulates the state of a user's CBIR session.
 */
public class CBIRSession {

    private enum Notification {
        NewClassifier,
        DeleteClassifier,
        NewTrainingImages,
        ModelTrained
    }

    private static CBIRSession instance = null;

    private final ProductOrderBasket productOrderBasket;
    private final ProductOrderService productOrderService;

    private final List<Patch> relevantImageList = new ArrayList<>(50);
    private final List<Patch> irrelevantImageList = new ArrayList<>(50);
    private final List<Patch> retrievedImageList = new ArrayList<>(500);
    private final List<CBIRSessionListener> listenerList = new ArrayList<>(1);
    private SearchToolStub classifier;

    private CBIRSession() {
        productOrderBasket = new ProductOrderBasket();
        productOrderService = new ProductOrderService(productOrderBasket);
    }

    public static CBIRSession getInstance() {
        if (instance == null) {
            instance = new CBIRSession();
        }
        return instance;
    }

    public boolean hasClassifier() {
        return classifier != null;
    }

    public SearchToolStub getClassifier() {
        return classifier;
    }

    public void createClassifier(final String classifierName,
                                 final PFAApplicationDescriptor applicationDescriptor,
                                 final String archivePath,
                                 final ProgressMonitor pm) throws Exception {
        try {
            classifier = new SearchToolStub(applicationDescriptor, archivePath, classifierName, pm);
            fireNotification(Notification.NewClassifier, classifier);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void deleteClassifier() throws Exception {
        try {
            SearchToolStub deletedClassifier = classifier;
            classifier.deleteClassifier();
            classifier = null;
            fireNotification(Notification.DeleteClassifier, deletedClassifier);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }


    public String getClassifierName() {
        return classifier.getClassifierName();
    }

    public PFAApplicationDescriptor getApplicationDescriptor() {
        return classifier.getApplicationDescriptor();
    }

    public ProductOrderBasket getProductOrderBasket() {
        return productOrderBasket;
    }

    public ProductOrderService getProductOrderService() {
        return productOrderService;
    }

    public FeatureType[] getEffectiveFeatureTypes() {
        return classifier.getPatchQuery().getEffectiveFeatureTypes();
    }

    public void setNumTrainingImages(final int numTrainingImages) throws Exception {
        classifier.setNumTrainingImages(numTrainingImages);
    }

    public int getNumTrainingImages() {
        return classifier.getNumTrainingImages();
    }

    public void setNumRetrievedImages(final int numRetrievedImages) throws Exception {
        classifier.setNumRetrievedImages(numRetrievedImages);
    }

    public int getNumRetrievedImages() {
        return classifier.getNumRetrievedImages();
    }

    public int getNumIterations() {
        return classifier.getNumIterations();
    }

    public static String[] getSavedClassifierNames(final String archiveFolder) {
        return SearchToolStub.getSavedClassifierNames(archiveFolder);
    }

    public void setQuicklookBandName(final Patch[] patches, final String quicklookBandName) {
        classifier.setQuicklookBandName(quicklookBandName);
        //reset patch images
        for (Patch patch : patches) {
            patch.setImage(null);
        }
    }

    public String[] getAvailableQuickLooks(final Patch patch) throws IOException {
        return classifier.getAvailableQuickLooks(patch);
    }

    public void addQueryPatch(final Patch patch) {
        classifier.addQueryImage(patch);
    }

    public Patch[] getQueryPatches() {
        return classifier.getQueryImages();
    }

    public void setQueryImages(final Patch[] queryImages, final ProgressMonitor pm) throws Exception {
        pm.beginTask("Getting Images to Label", 100);
        try {
            classifier.setQueryImages(queryImages, SubProgressMonitor.create(pm, 50));
            getImagesToLabel(SubProgressMonitor.create(pm, 50));
        } finally {
            pm.done();
        }
    }

    public void populateArchivePatches(final ProgressMonitor pm) throws Exception {
        classifier.populateArchivePatches(pm);
    }

    public void reassignTrainingImage(final Patch patch) {
        if (patch.getLabel() == Patch.LABEL_RELEVANT) {
            int index = irrelevantImageList.indexOf(patch);
            if (index != -1) {
                irrelevantImageList.remove(index);
                relevantImageList.add(patch);
            }
        } else if (patch.getLabel() == Patch.LABEL_IRRELEVANT) {
            int index = relevantImageList.indexOf(patch);
            if (index != -1) {
                relevantImageList.remove(index);
                irrelevantImageList.add(patch);
            }
        }
    }

    public Patch[] getRelevantTrainingImages() {
        final Patch[] patches = relevantImageList.toArray(new Patch[relevantImageList.size()]);
        classifier.getPatchQuicklooks(patches);
        return patches;
    }

    public Patch[] getIrrelevantTrainingImages() {
        final Patch[] patches = irrelevantImageList.toArray(new Patch[irrelevantImageList.size()]);
        classifier.getPatchQuicklooks(patches);
        return patches;
    }

    public void getImagesToLabel(final ProgressMonitor pm) throws Exception {

        relevantImageList.clear();
        irrelevantImageList.clear();

        final Patch[] imagesToLabel = classifier.getImagesToLabel(pm);
        for (Patch patch : imagesToLabel) {
            if (patch.getLabel() == Patch.LABEL_RELEVANT) {
                relevantImageList.add(patch);
            } else {
                // default to irrelevant so user only needs to select the relevant
                patch.setLabel(Patch.LABEL_IRRELEVANT);
                irrelevantImageList.add(patch);
            }
        }
        if (!pm.isCanceled()) {
            fireNotification(Notification.NewTrainingImages, classifier);
        }
    }

    public void trainModel(final ProgressMonitor pm) throws Exception {
        final List<Patch> labeledList = new ArrayList<Patch>(30);
        labeledList.addAll(relevantImageList);
        labeledList.addAll(irrelevantImageList);

        classifier.trainModel(labeledList.toArray(new Patch[labeledList.size()]), pm);

        fireNotification(Notification.ModelTrained, classifier);
    }

    public void retrieveImages() throws Exception {
        retrievedImageList.clear();
        retrievedImageList.addAll(Arrays.asList(classifier.getRetrievedImages()));
    }

    public Patch[] getRetrievedImages() {
        final Patch[] patches = retrievedImageList.toArray(new Patch[retrievedImageList.size()]);
        classifier.getPatchQuicklooks(patches);
        return patches;
    }

    private void fireNotification(final Notification msg, SearchToolStub classifier) throws Exception {
        for (CBIRSessionListener listener : listenerList) {
            switch (msg) {
                case NewClassifier:
                    listener.notifyNewClassifier(classifier);
                    break;
                case DeleteClassifier:
                    listener.notifyDeleteClassifier(classifier);
                    break;
                case NewTrainingImages:
                    listener.notifyNewTrainingImages(classifier);
                    break;
                case ModelTrained:
                    listener.notifyModelTrained(classifier);
                    break;
                default:
                    throw new Exception("Unknown notification message: " + msg);
            }
        }
    }

    public void addListener(final CBIRSessionListener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    public void removeListener(final CBIRSessionListener listener) {
        listenerList.remove(listener);
    }

    public interface CBIRSessionListener {

        void notifyNewClassifier(SearchToolStub classifier);

        void notifyNewTrainingImages(SearchToolStub classifier);

        void notifyModelTrained(SearchToolStub classifier);

        void notifyDeleteClassifier(SearchToolStub classifier);
    }
}
