package org.esa.snap.remote.execution.exceptions;

import org.esa.snap.core.gpf.OperatorException;

/**
 * Created by jcoravu on 31/12/2018.
 */
public class OperatorExecutionException extends OperatorException {

    private final ExecutionExceptionType type;

    public OperatorExecutionException(String message, ExecutionExceptionType type) {
        super(message);

        this.type = type;
    }

    public OperatorExecutionException(String message, Throwable cause, ExecutionExceptionType type) {
        super(message, cause);

        this.type = type;
    }

    public OperatorExecutionException(Throwable cause, ExecutionExceptionType type) {
        super(cause);

        this.type = type;
    }

    public ExecutionExceptionType getType() {
        return type;
    }
}
