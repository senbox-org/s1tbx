package org.esa.beam.binning.operator.metadata;

import org.esa.beam.framework.gpf.descriptor.OperatorDescriptor;

import java.io.File;

public class GlobalMetaParameter {

    private OperatorDescriptor descriptor;
    private File outputFile;
    private String startDateTime;
    private Double periodDuration;

    public OperatorDescriptor getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(OperatorDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(String startDateTime) {
        this.startDateTime = startDateTime;
    }

    public Double getPeriodDuration() {
        return periodDuration;
    }

    public void setPeriodDuration(Double periodDuration) {
        this.periodDuration = periodDuration;
    }
}
