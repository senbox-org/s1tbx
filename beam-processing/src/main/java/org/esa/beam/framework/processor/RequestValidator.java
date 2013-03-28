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
package org.esa.beam.framework.processor;

/**
 * A validator used to validate processing requests.
 *
 * @deprecated since BEAM 4.11. Use the {@link org.esa.beam.framework.gpf Graph Processing Framework} instead.
 */
@Deprecated
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
