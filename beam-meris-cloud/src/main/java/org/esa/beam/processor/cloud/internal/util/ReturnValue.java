/*
 * $Id: ReturnValue.java,v 1.1 2006/10/10 14:47:33 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.processor.cloud.internal.util;


/**
 * Simple structure to provide a return value with an associated error flag.
 * 
 * <p><i><b>IMPORTANT NOTE:</b> 
 * This class belongs to a preliminary API. 
 * It is not (yet) intended to be used by clients and may change in the future.</i></p>
 */
public final class ReturnValue {

    /**
     * The return value.
     */
    public double value;
    /**
     * The error flag.
     */
    public boolean error;
}
