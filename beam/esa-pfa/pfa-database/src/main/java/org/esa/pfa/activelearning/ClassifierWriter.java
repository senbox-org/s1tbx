package org.esa.pfa.activelearning;

import com.thoughtworks.xstream.XStream;
import libsvm.svm_model;
import org.esa.pfa.fe.op.Patch;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Save a session
 */
public class ClassifierWriter {

    int numTrainingImages;
    int numRetrievedImages;
    int numIterations;
    svm_model model;
    PatchInfo[] patchInfo;

    ClassifierWriter() {}

    public ClassifierWriter(final int numTrainingImages, final int numRetrievedImages, final int numIterations,
                     final ActiveLearning al) {
        this.numTrainingImages = numTrainingImages;
        this.numRetrievedImages = numRetrievedImages;
        this.numIterations = numIterations;
        model = al.getModel();

        savePatchInfo(al);
    }

    private void savePatchInfo(final ActiveLearning al) {
        final Patch[] trainingPatches = al.getTrainingData();
        final List<PatchInfo> patchInfoList = new ArrayList<>(trainingPatches.length);
        for(Patch patch : trainingPatches) {
            patchInfoList.add(new PatchInfo(patch));
        }
        patchInfo = patchInfoList.toArray(new PatchInfo[patchInfoList.size()]);
    }

    /**
     * basic information to recreate a patch
     * @return list of PatchInfo
     */
    public PatchInfo[] getPatchInfo() {
        return patchInfo;
    }

    public int getNumTrainingImages() {
        return numTrainingImages;
    }

    public int getNumRetrievedImages() {
        return numRetrievedImages;
    }

    public int getNumIterations() {
        return numIterations;
    }

    public svm_model getModel() {
        return model;
    }

    public static ClassifierWriter read(final File file) throws Exception {
        try (FileReader fileReader = new FileReader(file)) {
            ClassifierWriter session = new ClassifierWriter();
            getXStream().fromXML(fileReader, session);
            return session;
        }
    }

    public void write(final File classifierFile) throws IOException {
        try (FileWriter fileWriter = new FileWriter(classifierFile)) {
            getXStream().toXML(this, fileWriter);
        } catch (IOException e) {
            throw new IOException("Unable to write "+classifierFile.getAbsolutePath()+": "+e.getMessage(), e);
        }
    }

    private static XStream getXStream() {
        XStream xStream = new XStream();
        xStream.alias("model", svm_model.class);
        return xStream;
    }

    /**
    * basic information to recreate a patch
    */
    public static class PatchInfo {
        public String path;
        public int patchX;
        public int patchY;
        public int label;

        public PatchInfo(final Patch patch) {
            this.path = patch.getPathOnServer();
            this.patchX = patch.getPatchX();
            this.patchY = patch.getPatchY();
            this.label = patch.getLabel();
        }

        public Patch recreatePatch() {
            final Patch patch = new Patch(patchX, patchY, null, null);
            patch.setPathOnServer(path);
            patch.setLabel(label);

            return patch;
        }
    }
}
