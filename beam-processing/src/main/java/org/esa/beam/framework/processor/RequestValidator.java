/*
 * $Id: RequestValidator.java,v 1.1 2006/10/10 14:47:34 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.processor;

/**
 * A validator used to validate processing requests.
 */
public interface RequestValidator {
    /**
     * Validates the given processing request.
     * If a processor is called in interactive mode, this method will always be called
     * from Swing's event dispatching thread. So it is safe to pop-up dialog boxes here.
     *
     * @param processor the processor which wants to process the given request
     * @param request the processing request
     *
     * @return true if the processing request is OK
     */
    boolean validateRequest(Processor processor, Request request);
}
