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


/*
 *
 * @deprecated since BEAM 4.11. Use the {@link org.esa.beam.framework.gpf Graph Processing Framework} instead.
 */
@Deprecated
public class ProcessorConfig {

    private String _name;
    private String _description;

    /**
     * Constructs a new processor configuration with default values.
     */
    public ProcessorConfig() {
        _description = new String("");
        _name = new String("");
    }

    /**
     * Retrieves the name of the processor configuration.
     */
    public String getName() {
        return _name;
    }

    /**
     * Sets the name of the processor configuration
     *
     * @param name the name to be set
     */
    public void setName(String name) {
        _name = name;
    }

    /**
     * Retrieves a description for this processor request.
     */
    public String getDescription() {
        return _description;
    }

    /**
     * Sets a description for this request.
     *
     * @param description the description to be set
     */
    public void setDescription(String description) {
        _description = description;
    }
}




