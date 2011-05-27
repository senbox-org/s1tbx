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

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ExtensionFactory;
import com.bc.ceres.core.ExtensionManager;
import com.bc.ceres.core.SingleTypeExtensionFactory;
import com.bc.ceres.core.runtime.ConfigurableExtension;
import com.bc.ceres.core.runtime.ConfigurationElement;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;

/**
 * The default descriptor for a layer editor. Layer editors are configured in your Module Manifest {@code module.xml}
 * by extending the extension point {@code layerEditors} with {@code layerEditor} elements:
 * <p/>
 * <pre>
 * &lt;extension point="beam-ui:layerEditors"&gt;
 *       &lt;layerEditor&gt;
 *           &lt;editorFactory&gt;org.esa.beam.visat.toolviews.layermanager.editors.VectorDataLayerEditorFactory&lt;/editorFactory&gt;
 *           &lt;layer&gt;org.esa.beam.framework.ui.product.VectorDataLayer&lt;/layer&gt;
 *       &lt;/layerEditor&gt;
 *       &lt;layerEditor&gt;
 *           &lt;editor&gt;org.esa.beam.visat.toolviews.layermanager.editors.ImageLayerEditor&lt;/editor&gt;
 *           &lt;layerType&gt;com.bc.ceres.glayer.support.ImageLayer$Type&lt;/layerType&gt;
 *       &lt;/layerEditor&gt;
 * &lt;extension&gt;
 * </pre>
 * <p/>
 * <p/>
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
@SuppressWarnings({"UnusedDeclaration"})
public class DefaultLayerEditorDescriptor implements LayerEditorDescriptor, ConfigurableExtension {
    private Class<? extends Layer> layerClass;
    private Class<? extends LayerType> layerTypeClass;
    private Class<? extends LayerEditor> layerEditorClass;
    private Class<? extends ExtensionFactory> layerEditorFactoryClass;

    /**
     * Constructor used by Ceres runtime for creating a dedicated {@link ConfigurationElement} for this
     * {@link LayerEditorDescriptor}.
     */
    public DefaultLayerEditorDescriptor() {
    }

    @Override
    public Class<? extends Layer> getLayerClass() {
        return layerClass;
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
    public Class<? extends ExtensionFactory> getLayerEditorFactoryClass() {
        return layerEditorFactoryClass;
    }

    @Override
    public void configure(ConfigurationElement config) throws CoreException {
        if (layerClass != null) {
            ExtensionManager.getInstance().register(layerClass, createExtensionFactory());
        }
        if (layerTypeClass != null) {
            ExtensionManager.getInstance().register(layerTypeClass, createExtensionFactory());
        }
    }

    /**
     * Creates an extension factory that maps an instances of a {@link Layer} or
     * a {@link LayerType} to an instance of a {@link LayerEditor}.
     * <p/>
     * Clients may override in order to provide their own {@code ExtensionFactory}.
     *
     * @return An appropriate extension factory.
     */
    protected ExtensionFactory createExtensionFactory() {
        if (layerEditorClass != null) {
            return new SingleTypeExtensionFactory<LayerType, LayerEditor>(LayerEditor.class, layerEditorClass);
        } else if (layerEditorFactoryClass != null) {
            try {
                return layerEditorFactoryClass.newInstance();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        } else {
            throw new IllegalStateException("Either 'layerEditorClass' or 'layerEditorFactoryClass' must be non-null");
        }
    }

    void setLayerClass(Class<? extends Layer> layerClass) {
        this.layerClass = layerClass;
    }

    void setLayerEditorClass(Class<? extends LayerEditor> layerEditorClass) {
        this.layerEditorClass = layerEditorClass;
    }

    void setLayerEditorFactoryClass(Class<? extends ExtensionFactory> layerEditorFactoryClass) {
        this.layerEditorFactoryClass = layerEditorFactoryClass;
    }

    void setLayerTypeClass(Class<? extends LayerType> layerTypeClass) {
        this.layerTypeClass = layerTypeClass;
    }
}
