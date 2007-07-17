package com.bc.ceres.binding;

/**
 * Created by IntelliJ IDEA.
 * User: Norman
 * Date: 21.06.2007
 * Time: 10:52:45
 * To change this template use File | Settings | File Templates.
 */
public class ValidationException extends Exception {
    public ValidationException(String message) {
        super(message);

    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
