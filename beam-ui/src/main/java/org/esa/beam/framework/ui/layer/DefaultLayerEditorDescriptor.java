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

package org.esa.beam.framework.ui.layer;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ExtensionFactory;
import com.bc.ceres.core.ExtensionManager;
import com.bc.ceres.core.SingleTypeExtensionFactory;
import com.bc.ceres.core.runtime.ConfigurableExtension;
import com.bc.ceres.core.runtime.ConfigurationElement;
import com.bc.ceres.glayer.LayerType;

/**
 * The default descriptor for a layer editor.
 * <p/>
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
@SuppressWarnings({"UnusedDeclaration"})
public class DefaultLayerEditorDescriptor implements LayerEditorDescriptor, ConfigurableExtension {
    private Class<? extends LayerType> layerTypeClass;
    private Class<? extends LayerEditor> layerEditorClass;

    /**
     * Constructor used by Ceres runtime for creating a dedicated {@link ConfigurationElement}s for this
     * {@code LayerEditorDescriptor}.
     */
    public DefaultLayerEditorDescriptor() {
    }

    /**
     * Used for unit testing only.
     *
     * @param layerTypeClass   The layer type.
     * @param layerEditorClass The layer editor.
     */
    DefaultLayerEditorDescriptor(Class<? extends LayerType> layerTypeClass, Class<? extends LayerEditor> layerEditorClass) {
        Assert.notNull(layerTypeClass, "layerTypeClass");
        Assert.notNull(layerEditorClass, "layerEditorClass");
        this.layerTypeClass = layerTypeClass;
        this.layerEditorClass = layerEditorClass;
    }

    @Override
    public Class<? extends LayerType> getLayerTypeClass() {
        return layerTypeClass;
    }

    @Override
    public Class<? extends LayerEditor> getLayerEditorClass() {
        return layerEditorClass;
    }

    @Override
    public void configure(ConfigurationElement config) throws CoreException {
        ExtensionManager.getInstance().register(layerTypeClass, createExtensionFactory());
    }

    ExtensionFactory createExtensionFactory() {
        return new SingleTypeExtensionFactory<LayerType, LayerEditor>(LayerEditor.class, layerEditorClass);
    }

}
