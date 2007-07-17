package com.bc.ceres.binding.accessors;

import com.bc.ceres.binding.Accessor;

public class DefaultAccessor implements Accessor {
    private Object value;

    public DefaultAccessor() {
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
