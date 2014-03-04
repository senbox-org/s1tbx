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
import org.esa.beam.util.io.FileUtils;
import org.esa.pfa.activelearning.ActiveLearning;
import org.esa.pfa.activelearning.ClassifierWriter;
import org.esa.pfa.db.PatchQuery;
import org.esa.pfa.fe.PFAApplicationDescriptor;
import org.esa.pfa.fe.op.Patch;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Stub for PFA Search Tool on the server.
 * TODO major refactorings required (after demo):
 * (1) rename this to Classifier, because it is *the* classifier.
 * (2) introduce ClassifierService interface, which is used by CBIRSession. ClassifierService manages Classifiers (local or remote ones)
 */
public class SearchToolStub {

    private final PFAApplicationDescriptor applicationDescriptor;
    private final String classifierName;
    private final PatchQuery db;
    private final ActiveLearning al;
    private final File classifierFile;

    private int numTrainingImages = 12;
    private int numRetrievedImages = 50;
    private int numHitsMax = 500;
    private String quicklookBandName;

    public SearchToolStub(final PFAApplicationDescriptor applicationDescriptor, final String archiveFolder,
                          final String classifierName, final ProgressMonitor pm) throws Exception {
        this.applicationDescriptor = applicationDescriptor;
        this.classifierName = classifierName;

        final File dbFolder = new File(archiveFolder);
        final File classifierFolder = new File(dbFolder, "Classifiers");
        if (!classifierFolder.exists()) {
            classifierFolder.mkdirs();
        }
        this.classifierFile = new File(classifierFolder, classifierName + ".xml");
        this.quicklookBandName = applicationDescriptor.getDefaultQuicklookFileName();

        db = new PatchQuery(dbFolder, applicationDescriptor.getDefaultFeatureSet());
        al = new ActiveLearning();

        if (classifierFile.exists()) {
            loadClassifier(classifierFile, pm);
        }
    }

    public String getClassifierName() {
        return classifierName;
    }

    public PFAApplicationDescriptor getApplicationDescriptor() {
        return applicationDescriptor;
    }

    public PatchQuery getPatchQuery() {
        return db;
    }

    public void deleteClassifier() throws IOException {
        //todo other clean up
        if (classifierFile.exists()) {
            boolean delete = classifierFile.delete();
            if (!delete) {
                throw new IOException("Failed to delete " + classifierFile);
            }
        }
    }

    public void setNumTrainingImages(final int numTrainingImages) throws Exception {
        this.numTrainingImages = numTrainingImages;
        saveClassifier();
    }

    public int getNumTrainingImages() {
        return numTrainingImages;
    }

    public void setNumRetrievedImages(final int numRetrievedImages) throws Exception {
        this.numRetrievedImages = numRetrievedImages;
        saveClassifier();
    }

    public int getNumRetrievedImages() {
        return numRetrievedImages;
    }

    public int getNumIterations() {
        return al.getNumIterations();
    }

    public void setQuicklookBandName(final String quicklookBandName) {
        this.quicklookBandName = quicklookBandName;
    }

    public void addQueryImage(final Patch patch) {
        al.addQueryImage(patch);
    }

    public Patch[] getQueryImages() {
        final Patch[] queryPatches = al.getQueryPatches();
        getPatchQuicklooks(queryPatches);

        return queryPatches;
    }

    public void populateArchivePatches(final ProgressMonitor pm) throws Exception {
        final Patch[] archivePatches = db.query(applicationDescriptor.getAllQueryExpr(), numHitsMax);

        int numFeaturesQuery = al.getQueryPatches()[0].getFeatures().length;
        int numFeaturesDB = archivePatches[0].getFeatures().length;
        if (numFeaturesDB != numFeaturesQuery) {
            String msg = String.format("Incompatible Database.\n" +
                                       "The patches in the database have %d features.\n" +
                                       "The query patches have %d features.", numFeaturesDB, numFeaturesQuery);
            throw new IllegalArgumentException(msg);
        }

        al.setRandomPatches(archivePatches, pm);
    }

    public void setQueryImages(final Patch[] queryPatches, final ProgressMonitor pm) throws Exception {

        al.resetQuery();
        al.setQueryPatches(queryPatches);
        populateArchivePatches(pm);

        saveClassifier();
    }

    public Patch[] getImagesToLabel(final ProgressMonitor pm) throws Exception {
        final Patch[] patchesToLabel = al.getMostAmbiguousPatches(numTrainingImages, pm);
        getPatchQuicklooks(patchesToLabel);

        return patchesToLabel;
    }

    public static String[] getAvailableQuickLooks(final Patch patch) throws IOException {
        return PatchQuery.getAvailableQuickLooks(patch);
    }

    /**
     * Not all patches need quicklooks. This function adds quicklooks to the patches requested
     *
     * @param patches the patches to get quicklooks for
     */
    public void getPatchQuicklooks(final Patch[] patches) {
        for (Patch patch : patches) {
            if (patch.getImage() == null) {
                try {
                    URL imageURL = db.retrievePatchImage(patch, quicklookBandName);
                    //todo download image
                    File imageFile = new File(imageURL.getPath());
                    patch.setImage(loadImageFile(imageFile));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void trainModel(final Patch[] labeledImages, final ProgressMonitor pm) throws Exception {
        al.train(labeledImages, pm);

        saveClassifier();
    }

    public Patch[] getRetrievedImages() throws Exception {

        final List<Patch> relavantImages = new ArrayList<>(numRetrievedImages);
        final Patch[] archivePatches = db.query(applicationDescriptor.getAllQueryExpr(), numRetrievedImages * 10);
        al.classify(archivePatches);
        int i = 0;
        for (Patch patch : archivePatches) {
            if (patch.getLabel() == Patch.LABEL_RELEVANT) {
                if (!contains(relavantImages, patch)) {
                    relavantImages.add(patch);
                    ++i;
                }
                if (i >= numRetrievedImages) {
                    break;
                }
            }
        }
        final Patch[] retrievedRelevantImage = relavantImages.toArray(new Patch[relavantImages.size()]);
        getPatchQuicklooks(retrievedRelevantImage);

        return retrievedRelevantImage;
    }

    private static boolean contains(final List<Patch> list, final Patch patch) {
        //todo
        return false;
    }

    private static BufferedImage loadImageFile(final File file) {
        BufferedImage bufferedImage = null;
        if (file.canRead()) {
            try {
                try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file))) {
                    bufferedImage = ImageIO.read(fis);
                }
            } catch (Exception e) {
                //
            }
        }
        return bufferedImage;
    }

    public static String[] getSavedClassifierNames(final String archiveFolder) {
        final List<String> nameList = new ArrayList<>(10);
        final File dbFolder = new File(archiveFolder);
        final File classifierFolder = new File(dbFolder, "Classifiers");
        if (!classifierFolder.exists()) {
            classifierFolder.mkdirs();
        }
        final File[] files = classifierFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".xml");
            }
        });
        for (File file : files) {
            nameList.add(FileUtils.getFilenameWithoutExtension(file));
        }

        return nameList.toArray(new String[nameList.size()]);
    }

    private void loadClassifier(final File classifierFile, final ProgressMonitor pm) throws Exception {
        final ClassifierWriter storedClassifier = ClassifierWriter.read(classifierFile);
        numTrainingImages = storedClassifier.getNumTrainingImages();
        numRetrievedImages = storedClassifier.getNumRetrievedImages();
        int numIterations = storedClassifier.getNumIterations();

        al.setModel(storedClassifier.getModel());

        final Patch[] queryPatches = loadPatches(storedClassifier.getQueryPatchInfo());
        if (queryPatches != null && queryPatches.length > 0) {
            al.setQueryPatches(queryPatches);
        }

        final Patch[] patches = loadPatches(storedClassifier.getPatchInfo());
        if (patches != null && patches.length > 0) {
            al.setTrainingData(patches, numIterations, pm);
        }
    }

    private Patch[] loadPatches(final ClassifierWriter.PatchInfo[] patchInfo) throws Exception {
        if (patchInfo != null && patchInfo.length > 0) {
            final Patch[] patches = new Patch[patchInfo.length];
            int i = 0;
            for (ClassifierWriter.PatchInfo info : patchInfo) {
                final Patch patch = info.recreatePatch();
                final File featureFile = new File(patch.getPathOnServer(), "features.txt");
                patch.readFeatureFile(featureFile, getPatchQuery().getEffectiveFeatureTypes());

                patches[i++] = patch;
            }
            return patches;
        }
        return null;
    }

    private void saveClassifier() throws IOException {
        final ClassifierWriter writer = new ClassifierWriter(numTrainingImages, numRetrievedImages, al);
        writer.write(classifierFile);
    }
}
