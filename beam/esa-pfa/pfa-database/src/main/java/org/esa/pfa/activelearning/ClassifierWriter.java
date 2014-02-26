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
    String[] patchPaths;

    ClassifierWriter() {}

    public ClassifierWriter(final int numTrainingImages, final int numRetrievedImages, final int numIterations,
                     final ActiveLearning al) {
        this.numTrainingImages = numTrainingImages;
        this.numRetrievedImages = numRetrievedImages;
        this.numIterations = numIterations;
        model = al.getModel();

        savePatchPaths(al);
    }

    private void savePatchPaths(final ActiveLearning al) {
        final Patch[] trainingPatches = al.getTrainingData();
        final List<String> patchPathList = new ArrayList<>(trainingPatches.length);
        for(Patch patch : trainingPatches) {
            patchPathList.add(patch.getPathOnServer());
        }
        patchPaths = patchPathList.toArray(new String[patchPathList.size()]);
    }

    public String[] getPatchPaths() {
        return patchPaths;
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
}
