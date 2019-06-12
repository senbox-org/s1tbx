package org.esa.snap.remote.execution.exceptions;

import org.esa.snap.core.gpf.OperatorException;

/**
 * Created by jcoravu on 23/5/2019.
 */
public class WaitingTimeoutException extends OperatorException {

    public WaitingTimeoutException(String message) {
        super(message);
    }
}
