/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf.ui;


import com.bc.ceres.binding.dom.XppDomElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;

import javax.swing.*;
import java.util.Map;

/**
 * An <code>OperatorUI</code> is used as a user interface for an <code>Operator</code>.
 */
public interface OperatorUIDescriptor {

    public String getId();

    public String getOperatorName();

    public OperatorUI createOperatorUI();
}
