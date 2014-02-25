package org.esa.pfa.activelearning;

import com.thoughtworks.xstream.XStream;
import libsvm.svm_model;

import java.io.*;

/**
 * Save a session
 */
public class ClassifierWriter {

    int numTrainingImages;
    int numRetrievedImages;
    int numIterations;
    svm_model model;

    ClassifierWriter() {}

    public ClassifierWriter(final int numTrainingImages, final int numRetrievedImages, final int numIterations,
                     final ActiveLearning al) {
        this.numTrainingImages = numTrainingImages;
        this.numRetrievedImages = numRetrievedImages;
        this.numIterations = numIterations;
        model = al.getModel();
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
            throw new IOException("Unable to write "+classifierFile.getAbsolutePath()+": "+e.getMessage());
        }
    }

    private static XStream getXStream() {
        XStream xStream = new XStream();
        xStream.alias("model", svm_model.class);
        return xStream;
    }
}
