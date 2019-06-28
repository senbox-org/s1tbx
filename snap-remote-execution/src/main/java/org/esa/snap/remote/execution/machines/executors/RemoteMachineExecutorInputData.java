package org.esa.snap.remote.execution.machines.executors;

/**
 * Created by jcoravu on 10/1/2019.
 */
public class RemoteMachineExecutorInputData {

    private final String graphRelativeFilePath;
    private final String outputProductRelativeFilePath;
    private final String[] graphRelativeSourceProductFilePaths;

    public RemoteMachineExecutorInputData(String graphRelativeFilePath, String outputProductRelativeFilePath, String[] graphRelativeSourceProductFilePaths) {
        this.graphRelativeFilePath = graphRelativeFilePath;
        this.outputProductRelativeFilePath = outputProductRelativeFilePath;
        this.graphRelativeSourceProductFilePaths = graphRelativeSourceProductFilePaths;
    }

    public String getGraphRelativeFilePath() {
        return graphRelativeFilePath;
    }

    public String getOutputProductRelativeFilePath() {
        return outputProductRelativeFilePath;
    }

    public String[] getGraphRelativeSourceProductFilePaths() {
        return graphRelativeSourceProductFilePaths;
    }
}
