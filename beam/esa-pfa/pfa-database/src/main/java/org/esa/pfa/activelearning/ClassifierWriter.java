package org.esa.pfa.activelearning;

import com.thoughtworks.xstream.XStream;
import libsvm.svm_model;
import org.esa.pfa.fe.op.Patch;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Save a session
 */
public class ClassifierWriter {

    String applicationName;
    int numTrainingImages;
    int numRetrievedImages;
    int numIterations;
    svm_model model;
    PatchInfo[] queryInfo;
    PatchInfo[] patchInfo;

    ClassifierWriter() {}

    public ClassifierWriter(final String applicationName, final int numTrainingImages, final int numRetrievedImages, final ActiveLearning al) {
        this.applicationName = applicationName;
        this.numTrainingImages = numTrainingImages;
        this.numRetrievedImages = numRetrievedImages;
        this.numIterations = al.getNumIterations();
        model = al.getModel();

        queryInfo = getPatchInfo(al.getQueryPatches());
        patchInfo = getPatchInfo(al.getTrainingData());
    }

    private static PatchInfo[] getPatchInfo(final Patch[] patches) {
        final List<PatchInfo> patchInfoList = new ArrayList<>(patches.length);
        for(Patch patch : patches) {
            patchInfoList.add(new PatchInfo(patch));
        }
        return patchInfoList.toArray(new PatchInfo[patchInfoList.size()]);
    }

    public String getApplicationName() {
        return applicationName;
    }

    /**
     * basic information to recreate a patch
     * @return list of PatchInfo
     */
    public PatchInfo[] getPatchInfo() {
        return patchInfo;
    }

    public PatchInfo[] getQueryPatchInfo() {
        return queryInfo;
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
        xStream.alias("classifier", ClassifierWriter.class);
        xStream.setClassLoader(ClassifierWriter.class.getClassLoader());
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
