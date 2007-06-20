/*
 * $Id: HeaderEntryNotFoundException.java,v 1.1 2006/09/18 06:34:32 marcop Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.dataio.envisat;

import org.esa.beam.framework.dataio.ProductIOException;

/**
 * Thrown if an entry in an ENVISAT header could not be found.
 */
public class HeaderEntryNotFoundException extends ProductIOException {

    private static final long serialVersionUID = -946209513433311834L;

    /**
     * Constructs a new exception with the given error message.
     *
     * @param message the error message
     */
    public HeaderEntryNotFoundException(String message) {
        super(message);
    }
}
