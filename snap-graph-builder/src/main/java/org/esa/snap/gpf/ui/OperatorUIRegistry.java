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
package org.esa.snap.gpf.ui;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.Activator;
import com.bc.ceres.core.runtime.ModuleContext;
import org.esa.beam.BeamCoreActivator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An <code>OperatorUIRegistry</code> provides access to operator user interfaces as described by their OperatorUIDescriptor.
 */
public class OperatorUIRegistry implements Activator {

    private static OperatorUIRegistry instance;
    private Map<String, OperatorUIDescriptor> operatorUIDescriptors;

    public OperatorUIRegistry() {
        instance = this;
    }

    @Override
    public void start(ModuleContext moduleContext) throws CoreException {
        registerOperatorUIs(moduleContext);
    }

    @Override
    public void stop(ModuleContext moduleContext) throws CoreException {
        operatorUIDescriptors = null;
        instance = null;
    }

    public static OperatorUIRegistry getInstance() {
        return instance;
    }

    public OperatorUIDescriptor[] getOperatorUIDescriptors() {
        return operatorUIDescriptors.values().toArray(new OperatorUIDescriptor[operatorUIDescriptors.values().size()]);
    }

    public OperatorUIDescriptor getOperatorUIDescriptor(final String operatorName) {
        return operatorUIDescriptors.get(operatorName);
    }

    private void registerOperatorUIs(ModuleContext moduleContext) {
        List<OperatorUIDescriptor> operatorUIDescriptorList = BeamCoreActivator.loadExecutableExtensions(moduleContext,
                "OperatorUIs",
                "OperatorUI",
                OperatorUIDescriptor.class);
        operatorUIDescriptors = new HashMap<>(2 * operatorUIDescriptorList.size());
        for (OperatorUIDescriptor operatorUIDescriptor : operatorUIDescriptorList) {
            final String opName = operatorUIDescriptor.getOperatorName();
            final OperatorUIDescriptor existingDescriptor = operatorUIDescriptors.get(opName);
            if (existingDescriptor != null) {
                moduleContext.getLogger().info(String.format("OperatorUI [%s] has been redeclared for [%s]!\n",
                        operatorUIDescriptor.getId(), opName));
            }
            operatorUIDescriptors.put(opName, operatorUIDescriptor);
        }
    }
}
