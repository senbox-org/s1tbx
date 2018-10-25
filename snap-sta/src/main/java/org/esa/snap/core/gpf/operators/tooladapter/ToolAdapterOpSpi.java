/*
 * Copyright (C) 2014-2015 CS SI
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
 *  with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.core.gpf.operators.tooladapter;

import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.template.FileTemplate;

/**
 * The SPI class for ToolAdapterOp.
 *
 * @author Lucian Barbulescu.
 */
public class ToolAdapterOpSpi extends OperatorSpi {

    private FileTemplate template;

    /**
     * Default constructor.
     */
    public ToolAdapterOpSpi() {
        super(ToolAdapterOp.class);
    }

    /**
     * Constructor.
     *
     * @param operatorDescriptor the operator adapterFolder to be used.
     */
    public ToolAdapterOpSpi(OperatorDescriptor operatorDescriptor) {
        super(operatorDescriptor);
        this.template = ((ToolAdapterOperatorDescriptor) operatorDescriptor).getTemplate();
    }

    public FileTemplate getTemplate() { return this.template; }
}
