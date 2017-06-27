package org.esa.snap.core.gpf.operators.tooladapter;

import org.esa.snap.core.gpf.descriptor.ToolAdapterOperatorDescriptor;

/**
 * Created by kraftek on 3/4/2016.
 */
public interface ToolAdapterListener {
    void adapterAdded(ToolAdapterOperatorDescriptor operatorDescriptor);
    void adapterRemoved(ToolAdapterOperatorDescriptor operatorDescriptor);
    default void adapterUpdated(ToolAdapterOperatorDescriptor operatorDescriptor) { };
}
