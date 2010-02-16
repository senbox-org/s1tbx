package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.CoreException;

public class ResolveException extends CoreException {

    public ResolveException(String message) {
        super(message);
    }

    public ResolveException(String message, Throwable cause) {
        super(message, cause);
    }
}
