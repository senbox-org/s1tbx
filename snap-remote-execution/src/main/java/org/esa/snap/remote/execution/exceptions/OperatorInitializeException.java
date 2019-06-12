package org.esa.snap.remote.execution.exceptions;

import org.esa.snap.core.gpf.OperatorException;

/**
 * Created by jcoravu on 31/12/2018.
 */
public class OperatorInitializeException extends OperatorException {

    public OperatorInitializeException(String message) {
        super(message);
    }

    public OperatorInitializeException(String message, Throwable cause) {
        super(message, cause);
    }

    public OperatorInitializeException(Throwable cause) {
        super(cause);
    }
}
