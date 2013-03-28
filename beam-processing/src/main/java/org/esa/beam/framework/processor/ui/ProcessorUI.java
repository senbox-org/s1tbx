/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.beam.framework.processor.ui;

import org.esa.beam.framework.processor.ProcessorException;

import javax.swing.JComponent;
import java.util.Vector;

// todo - deprecate most methods, only single requests are supported
// todo - deprecate setApp(), application context should be passed to the ProcessorUI factory method of ProcessorApp

/**
 * This interface defines the basic operations any processor user interface has to provide to interact with the
 * processor framework.
 *
 * <p>Clients should use the {@link AbstractProcessorUI} as base class for their implementations, because the
 *  {@link ProcessorUI}
 * interface may change in the future.</p>
 *
 * @deprecated since BEAM 4.11. Use the {@link org.esa.beam.framework.gpf Graph Processing Framework} instead.
 */
@Deprecated
public interface ProcessorUI {

    /**
     * Retrieves the base component for the processor specific user interface classes. This can be any Java Swing
     * containertype.
     */
    JComponent getGuiComponent();

    /**
     * Retrieves the list of requests currently edited.
     */
    Vector getRequests() throws ProcessorException;

    /**
     * Sets a new Request to be edited.
     */
    void setRequests(Vector requests) throws ProcessorException;

    /**
     * Create a set of new default requests.
     */
    void setDefaultRequests() throws ProcessorException;

    /**
     * Sets the processor application context.
     * <p>This method is called only once during the lifetime of a UI and immediately called after creation
     * in the {@link org.esa.beam.framework.processor.Processor#createUI()  createUI()} method of the processor.</p>
     * <p>In most cases the implementations of this method will simply store the processor application
     * context for later use and possibly add a
     * {@link org.esa.beam.framework.processor.RequestValidator RequestValidator} to it.</p>
     *
     * @see ProcessorApp#addRequestValidator(org.esa.beam.framework.processor.RequestValidator)
     * @see ProcessorApp#markIODirChanges(org.esa.beam.framework.param.ParamGroup)
     */
    void setApp(ProcessorApp app);
}

