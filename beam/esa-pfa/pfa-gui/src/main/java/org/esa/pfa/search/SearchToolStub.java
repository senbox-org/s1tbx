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

import org.esa.beam.util.io.FileUtils;
import org.esa.pfa.activelearning.ActiveLearning;
import org.esa.pfa.activelearning.ClassifierWriter;
import org.esa.pfa.db.DatasetDescriptor;
import org.esa.pfa.db.PatchQuery;
import org.esa.pfa.fe.PFAApplicationDescriptor;
import org.esa.pfa.fe.op.Patch;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Stub for PFA Search Tool on the server
 */
public class SearchToolStub {

    private final PFAApplicationDescriptor applicationDescriptor;
    private final PatchQuery db;
    private final ActiveLearning al;
    private final File classifierFile;

    private int numTrainingImages = 12;
    private int numRetrievedImages = 50;
    private int numHitsMax = 500;

    public SearchToolStub(PFAApplicationDescriptor applicationDescriptor, String archiveFolder, String classifierName) throws Exception {
        this.applicationDescriptor = applicationDescriptor;

        final File dbFolder = new File(archiveFolder);
        final File classifierFolder = new File(dbFolder, "Classifiers");
        if (!classifierFolder.exists()) {
            classifierFolder.mkdirs();
        }
        this.classifierFile = new File(classifierFolder, classifierName + ".xml");

        db = new PatchQuery(dbFolder, applicationDescriptor.getDefaultFeatureSet());
        al = new ActiveLearning();

        if (classifierFile.exists()) {
            loadClassifier(classifierFile);
        }
    }

    public PatchQuery getPatchQuery() {
        return db;
    }

    public DatasetDescriptor getDsDescriptor() {
        return db.getDsDescriptor();
    }

    public boolean deleteClassifier() {
        //todo other clean up
        return classifierFile.delete();
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

    public void addQueryImage(final Patch patch) {
        al.addQueryImage(patch);
    }

    public Patch[] getQueryImages() {
        final Patch[] queryPatches = al.getQueryPatches();
        getPatchQuicklooks(queryPatches);

        return queryPatches;
    }

    public void populateArchivePatches() throws Exception {
        final Patch[] archivePatches = db.query(applicationDescriptor.getAllQueryExpr(), numHitsMax);

        int numFeaturesQuery = al.getQueryPatches()[0].getFeatures().length;
        int numFeaturesDB = archivePatches[0].getFeatures().length;
        if (numFeaturesDB != numFeaturesQuery) {
            String msg = String.format("Incompatible Database.\n" +
                    "The patches in the database have %d features.\n" +
                    "The query patches have %d features.", numFeaturesDB, numFeaturesQuery);
            throw new IllegalArgumentException(msg);
        }

        al.setRandomPatches(archivePatches);
    }

    public void setQueryImages(final Patch[] queryPatches) throws Exception {

        al.resetQuery();
        al.setQueryPatches(queryPatches);
        populateArchivePatches();

        saveClassifier();
    }

    public Patch[] getImagesToLabel() throws Exception {
        final Patch[] patchesToLabel = al.getMostAmbiguousPatches(numTrainingImages);
        getPatchQuicklooks(patchesToLabel);

        return patchesToLabel;
    }

    /**
     * Not all patches need quicklooks. This function adds quicklooks to the patches requested
     * @param patches the patches to get quicklooks for
     */
    private void getPatchQuicklooks(final Patch[] patches) {
        for (Patch patch : patches) {
            if (patch.getImage() == null) {
                try {
                    URL imageURL = db.retrievePatchImage(patch, applicationDescriptor.getDefaultQuicklookFileName());
                    //todo download image
                    File imageFile = new File(imageURL.getPath());
                    patch.setImage(loadImageFile(imageFile));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void trainModel(Patch[] labeledImages) throws Exception {
        al.train(labeledImages);

        saveClassifier();
    }

    public Patch[] getRetrievedImages() throws Exception {

        final List<Patch> relavantImages = new ArrayList<>(numRetrievedImages);
        final Patch[] archivePatches = db.query(applicationDescriptor.getAllQueryExpr(), numRetrievedImages*100);
        al.classify(archivePatches);
        int i=0;
        for(Patch patch : archivePatches) {
            if(patch.getLabel() == Patch.LABEL_RELEVANT) {
                if(!contains(relavantImages, patch)) {
                    relavantImages.add(patch);
                    ++i;
                }
                if(i >= numRetrievedImages) {
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

    private void loadClassifier(final File classifierFile) throws Exception {
        final ClassifierWriter storedClassifier = ClassifierWriter.read(classifierFile);
        numTrainingImages = storedClassifier.getNumTrainingImages();
        numRetrievedImages = storedClassifier.getNumRetrievedImages();
        int numIterations = storedClassifier.getNumIterations();

        al.setModel(storedClassifier.getModel());

        final Patch[] queryPatches = loadPatches(storedClassifier.getQueryPatchInfo());
        if(queryPatches != null && queryPatches.length > 0) {
            al.setQueryPatches(queryPatches);
        }

        final Patch[] patches = loadPatches(storedClassifier.getPatchInfo());
        if(patches != null && patches.length > 0) {
            al.setTrainingData(patches, numIterations);
        }
    }

    private Patch[] loadPatches(final ClassifierWriter.PatchInfo[] patchInfo) throws Exception {
        if(patchInfo != null && patchInfo.length > 0) {
            final Patch[] patches = new Patch[patchInfo.length];
            int i = 0;
            for(ClassifierWriter.PatchInfo info : patchInfo) {
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
