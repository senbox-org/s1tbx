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
import org.esa.pfa.db.DatasetDescriptor;
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
 * State of the CBIR
 */
public class CBIRSession {

    private List<Patch> relevantImageList = new ArrayList<>(50);
    private List<Patch> irrelevantImageList = new ArrayList<>(50);
    private List<Patch> retrievedImageList = new ArrayList<>(500);

    private String classifierName;
    private PFAApplicationDescriptor applicationDescriptor;

    private SearchToolStub searchTool;

    private ProductOrderBasket productOrderBasket;

    private ProductOrderService productOrderService;

    private final List<CBIRSessionListener> listenerList = new ArrayList<>(1);

    enum Notification { NewSession, NewTrainingImages, ModelTrained };

    private static CBIRSession instance = null;

    private CBIRSession() {
    }

    public static CBIRSession Instance() {
        if(instance == null) {
            instance = new CBIRSession();
        }
        return instance;
    }

    public void initSession(final String classifierName,
                       final PFAApplicationDescriptor applicationDescriptor,
                       final String archivePath, final ProgressMonitor pm) throws Exception {
        this.classifierName = classifierName;
        this.applicationDescriptor = applicationDescriptor;

        searchTool = new SearchToolStub(applicationDescriptor, archivePath, classifierName, pm);
        productOrderBasket = new ProductOrderBasket();
        productOrderService = new ProductOrderService(productOrderBasket);

        fireNotification(Notification.NewSession);
    }

    public String getName() {
        return classifierName;
    }

    public PFAApplicationDescriptor getApplicationDescriptor() {
        return applicationDescriptor;
    }

    public ProductOrderBasket getProductOrderBasket() {
        return productOrderBasket;
    }

    public ProductOrderService getProductOrderService() {
        return productOrderService;
    }

    public DatasetDescriptor getDsDescriptor() {
        return searchTool.getDsDescriptor();
    }

    public boolean deleteClassifier() {
        return searchTool.deleteClassifier();
    }

    public FeatureType[] getEffectiveFeatureTypes()  {
        return searchTool.getPatchQuery().getEffectiveFeatureTypes();
    }

    public void setNumTrainingImages(final int numTrainingImages) throws Exception {
        searchTool.setNumTrainingImages(numTrainingImages);
    }

    public int getNumTrainingImages() {
        return searchTool.getNumTrainingImages();
    }

    public void setNumRetrievedImages(final int numRetrievedImages) throws Exception {
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

    public void setQuicklookBandName(final Patch[] patches, final String quicklookBandName) {
        searchTool.setQuicklookBandName(quicklookBandName);
        //reset patch images
        for(Patch patch : patches) {
            patch.setImage(null);
        }
    }

    public String[] getAvailableQuickLooks(final Patch patch) throws IOException {
        return searchTool.getAvailableQuickLooks(patch);
    }

    public void addQueryPatch(final Patch patch) {
        searchTool.addQueryImage(patch);
    }

    public Patch[] getQueryPatches() {
        return searchTool.getQueryImages();
    }

    public void setQueryImages(final Patch[] queryImages, final ProgressMonitor pm) throws Exception {
        pm.beginTask("Getting Images to Label", 100);
        try {
            searchTool.setQueryImages(queryImages, SubProgressMonitor.create(pm, 50));
            getImagesToLabel(SubProgressMonitor.create(pm, 50));
        } finally {
            pm.done();
        }
    }

    public void populateArchivePatches(final ProgressMonitor pm) throws Exception {
        searchTool.populateArchivePatches(pm);
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
        final Patch[] patches = relevantImageList.toArray(new Patch[relevantImageList.size()]);
        searchTool.getPatchQuicklooks(patches);
        return patches;
    }

    public Patch[] getIrrelevantTrainingImages() {
        final Patch[] patches = irrelevantImageList.toArray(new Patch[irrelevantImageList.size()]);
        searchTool.getPatchQuicklooks(patches);
        return patches;
    }

    public void getImagesToLabel(final ProgressMonitor pm) throws Exception {

        relevantImageList.clear();
        irrelevantImageList.clear();

        final Patch[] imagesToLabel = searchTool.getImagesToLabel(pm);
        for(Patch patch : imagesToLabel) {
            if(patch.getLabel() == Patch.LABEL_RELEVANT) {
                relevantImageList.add(patch);
            } else {
                // default to irrelevant so user only needs to select the relevant
                patch.setLabel(Patch.LABEL_IRRELEVANT);
                irrelevantImageList.add(patch);
            }
        }
        if (!pm.isCanceled()) {
            fireNotification(Notification.NewTrainingImages);
        }
    }

    public void trainModel(final ProgressMonitor pm) throws Exception {
        final List<Patch> labeledList = new ArrayList<Patch>(30);
        labeledList.addAll(relevantImageList);
        labeledList.addAll(irrelevantImageList);

        searchTool.trainModel(labeledList.toArray(new Patch[labeledList.size()]), pm);

        fireNotification(Notification.ModelTrained);
    }

    public void retrieveImages() throws Exception {
        retrievedImageList.clear();
        retrievedImageList.addAll(Arrays.asList(searchTool.getRetrievedImages()));
    }

    public Patch[] getRetrievedImages() {
        final Patch[] patches = retrievedImageList.toArray(new Patch[retrievedImageList.size()]);
        searchTool.getPatchQuicklooks(patches);
        return patches;
    }

    private void fireNotification(final Notification msg) throws Exception {
        for(CBIRSessionListener listener : listenerList) {
            switch (msg) {
                case NewSession:
                    listener.notifyNewSession();
                    break;
                case NewTrainingImages:
                    listener.notifyNewTrainingImages();
                    break;
                case ModelTrained:
                    listener.notifyModelTrained();
                    break;
                default:
                    throw new Exception("Unknown notification message: "+msg);
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

        public void notifyNewSession();

        public void notifyNewTrainingImages();

        public void notifyModelTrained();
    }
}
