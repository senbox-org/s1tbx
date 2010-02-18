/*
 * $Id: ProcessorStatusEvent.java,v 1.1 2006/10/10 14:47:34 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.processor;

import java.util.EventObject;

import org.esa.beam.util.Guardian;

/**
 * <code>ProcessorStatusEvent</code>s occur if the status of a processor changes.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 * @see ProcessorStatusListener
 * @see Processor#addProcessorStatusListener
 * @see Processor#removeProcessorStatusListener
 */
public class ProcessorStatusEvent extends EventObject {

    private final Request _request;
    private final int _oldStatus;
    private final int _newStatus;
    private final ProcessorException _exception;

    /**
     * Constructs a new <code>ProcessorStatusEvent</code>.
     *
     * @param processor the processor which caused the event, must not be <code>null</code>
     * @param oldStatus the old processor status
     */
    public ProcessorStatusEvent(Processor processor, int oldStatus) {
        super(processor);
        Guardian.assertNotNull("processor", processor);
        _request = processor.getRequest();
        _oldStatus = oldStatus;
        _newStatus = processor.getCurrentStatus();
        _exception = null; // processor.getLastException();
    }

    /**
     * Returns the processor which caused this event.
     */
    public Processor getProcessor() {
        return (Processor) getSource();
    }

    /**
     * Returns the processing request which was processed for the moment at which this event occured.
     *
     * @return the request, or <code>null</code> if no request was processed while this event was generated
     */
    public Request getRequest() {
        return _request;
    }

    /**
     * Returns the old processor status (before the status change).
     */
    public int getOldStatus() {
        return _oldStatus;
    }

    /**
     * Returns the new processor status (after the status change).
     */
    public int getNewStatus() {
        return _newStatus;
    }

    /**
     * Returns the processor exception for the moment at which this event occured.
     *
     * @return the exception, or <code>null</code> if this event was not caused by an error
     */
    public ProcessorException getException() {
        return _exception;
    }

}
