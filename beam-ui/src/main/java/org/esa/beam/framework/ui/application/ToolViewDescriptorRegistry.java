package org.esa.beam.framework.ui.application;

public interface ToolViewDescriptorRegistry {
    public ToolViewDescriptor[] getToolViewDescriptors();

    public ToolViewDescriptor getToolViewDescriptor(String viewDescriptorId);
}
