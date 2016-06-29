package org.esa.snap.core.gpf.descriptor.template;

/**
 * Specialized class for template parsing exceptions.
 *
 * @author Cosmin Cara
 */
public class TemplateException extends Exception {

    public TemplateException(Throwable cause) {
        super(cause);
    }

    public TemplateException(String message) {
        super(message);
    }
}
