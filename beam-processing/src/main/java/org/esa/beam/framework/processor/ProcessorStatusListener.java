/*
 * $Id: ProcessorStatusListener.java,v 1.1 2006/10/10 14:47:34 norman Exp $
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

/**
 * Listens to processing events fired by a <code>Processor</code> instance.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 * @see ProcessorStatusEvent
 * @see Processor#addProcessorStatusListener
 * @see Processor#removeProcessorStatusListener
 */
public interface ProcessorStatusListener {

    /**
     * Called if a new processing request is started.
     *
     * @param event the processor event
     */
    void handleProcessingStarted(ProcessorStatusEvent event);

    /**
     * Called if the current processing request has been successfully completed.
     *
     * @param event the processor event
     */
    void handleProcessingCompleted(ProcessorStatusEvent event);

    /**
     * Called if the current processing request has been aborted.
     *
     * @param event the processor event
     */
    void handleProcessingAborted(ProcessorStatusEvent event);

    /**
     * Called if a processing error occured.
     *
     * @param event the processor event
     */
    void handleProcessingFailed(ProcessorStatusEvent event);

    /**
     * Called if a processing state changed.
     *
     * @param event the processor event
     */
    void handleProcessingStateChanged(ProcessorStatusEvent event);
}
