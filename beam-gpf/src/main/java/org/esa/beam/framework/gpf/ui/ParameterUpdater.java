/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.framework.gpf.ui;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;

import java.util.Map;

/**
 * WARNING: This class belongs to a preliminary API and may change in future releases.
 *
 * Enables reaction to parameter save and load request.
 *
 * @author Marco Zühlke
 */
public interface ParameterUpdater {

    /**
     * Called before the parameter map is saved. The implementer should update the given map.
     *
     * @param parameterMap The parameter map
     */
    void handleParameterSaveRequest(Map<String,Object> parameterMap) throws ValidationException, ConversionException;

    /**
     * Called after the parameter ap has been loaded. The implementer
     * should update his internal model from the given map.
     *
     * @param parameterMap The parameter map
     */
    void handleParameterLoadRequest(Map<String,Object> parameterMap) throws ValidationException, ConversionException;
}
