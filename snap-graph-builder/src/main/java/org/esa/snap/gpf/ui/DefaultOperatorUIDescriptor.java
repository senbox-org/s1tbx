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

package org.esa.snap.gpf.ui;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.ConfigurableExtension;
import com.bc.ceres.core.runtime.ConfigurationElement;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Provides a standard implementation for {@link org.esa.beam.framework.ui.application.ToolViewDescriptor}.
 */
@XStreamAlias("toolView")
public class DefaultOperatorUIDescriptor implements OperatorUIDescriptor, ConfigurableExtension {

    private String id;
    @XStreamAlias("operatorName")
    private String operatorName;

    @XStreamAlias("class")
    private Class operatorUIClass;

    public DefaultOperatorUIDescriptor() {
    }

    public String getId() {
        return id;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public OperatorUI createOperatorUI() {
        Assert.state(operatorUIClass != null, "operatorUIClass != null");
        Object object;
        try {
            object = operatorUIClass.newInstance();
        } catch (Throwable e) {
            throw new IllegalStateException("operatorUIClass.newInstance()", e);
        }
        Assert.state(object instanceof OperatorUI, "object instanceof operatorUI");
        return (OperatorUI) object;
    }

    @Override
    public void configure(ConfigurationElement config) throws CoreException {
    }
}
